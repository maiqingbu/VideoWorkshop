package com.videoworkshop.core.ffmpeg.operators

import com.arthenica.ffmpegkit.FFmpegKit
import com.videoworkshop.core.ffmpeg.FfmpegEngine
import com.videoworkshop.core.ffmpeg.FfprobeHelper
import com.videoworkshop.domain.model.ABTransportConfig
import com.videoworkshop.domain.model.ABTransportMode
import com.videoworkshop.domain.model.DurationStrategy
import com.videoworkshop.domain.model.TimelineSegment
import com.videoworkshop.domain.model.VideoClip
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

/**
 * AB 搬运进度。
 *
 * 由 [AVStreamSwapper.swap] 发射，描述单次搬运任务的进度与终态。
 *
 * @param progress   当前进度（0.0 ~ 1.0），完成时为 1.0
 * @param currentMs  已处理时长（毫秒）
 * @param totalMs    总时长（毫秒），用于换算百分比
 * @param outputPath 输出路径，仅完成时非空
 * @param error      失败时的异常，仅失败时非空
 */
data class ABTransportProgress(
    val progress: Float,
    val currentMs: Long,
    val totalMs: Long,
    val outputPath: String? = null,
    val error: Throwable? = null
) {
    /** 是否为完成态（outputPath 非空且无错误）。 */
    val isCompleted: Boolean get() = outputPath != null && error == null

    /** 是否为失败态。 */
    val isFailed: Boolean get() = error != null
}

/**
 * FFmpeg 执行句柄，用于取消正在执行的会话。
 *
 * 抽象 [com.arthenica.ffmpegkit.Session] 的取消能力，
 * 使测试代码无需依赖 FFmpegKit（core-ffmpeg 中为 `compileOnly`）。
 */
fun interface FfmpegHandle {
    /** 取消当前会话。 */
    fun cancel()
}

/**
 * FFmpeg 异步执行网关。
 *
 * 抽象 [FfmpegEngine.executeAsync] 的执行能力，使其可在单元测试中替换为 Fake 实现。
 *
 * 接口签名刻意不暴露 FFmpegKit 类型（[com.arthenica.ffmpegkit.Session]、
 * [com.arthenica.ffmpegkit.StatisticsCallback] 等），因为 FFmpegKit 在
 * core-ffmpeg 模块中为 `compileOnly`，不会出现在测试 classpath 上。
 */
fun interface FfmpegExecutor {
    /**
     * 异步执行一条 FFmpeg 命令。
     *
     * @param command    完整命令字符串（含 `ffmpeg` 前缀）。
     * @param durationMs 总时长（毫秒），用于换算进度百分比。
     * @param onProgress 进度回调，参数为 0f..1f。
     * @param onComplete 完成回调，参数为 `true` 表示执行成功。
     * @return 执行句柄，可用于取消会话。
     */
    fun executeAsync(
        command: String,
        durationMs: Long,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ): FfmpegHandle
}

/**
 * [FfmpegExecutor] 的默认实现，委托给 [FfmpegEngine]。
 *
 * 取消时通过 [FFmpegKit.cancel] 按 sessionId 精确取消该次执行启动的会话，
 * 与 [FfmpegEngine.cancel]（取消全局当前会话）等价但更精确，
 * 避免多实例并发时误取消他人会话。
 */
object DefaultFfmpegExecutor : FfmpegExecutor {
    override fun executeAsync(
        command: String,
        durationMs: Long,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ): FfmpegHandle {
        val session = FfmpegEngine.executeAsync(
            command = command,
            onProgress = onProgress,
            onComplete = onComplete,
            durationMs = durationMs
        )
        // 按 sessionId 精确取消该会话（与 FfmpegEngine.cancel 内部使用相同的 FFmpegKit.cancel 调用）
        val sessionId = session.sessionId
        return FfmpegHandle { FFmpegKit.cancel(sessionId) }
    }
}

/**
 * AB 搬运音视频流分离重组器。
 *
 * 以 A 视频作为音频源、B 视频作为画面源，按 [ABTransportConfig.mode] 合成输出：
 * - [ABTransportMode.PURE_REPLACE]：B 画面 + A 音频（纯替换）
 * - [ABTransportMode.MIX]：B 画面 + (A 音频 + B 原声) 混音
 *
 * 执行流程：
 * 1. 通过 [FfprobeHelper] 探测 A/B 视频时长，用于进度换算；
 * 2. 构建 FFmpeg 命令（输入 0 = B 画面源，输入 1 = A 音频源）；
 * 3. 委托 [FfmpegExecutor] 异步执行，通过 [callbackFlow] 将回调转为 [Flow]；
 * 4. 消费端取消收集时，通过 [FfmpegHandle.cancel] 取消 FFmpeg 会话。
 *
 * 与 [com.videoworkshop.core.ffmpeg.enhance] 下命令构建器（[com.videoworkshop.core.ffmpeg.enhance.AudioMixer]
 * 等）的差异：后者仅构建命令字符串，不负责执行；本类负责完整的
 * 「探测 → 构建 → 执行 → 进度 → 取消」链路。
 *
 * @param executor   FFmpeg 执行网关，默认 [DefaultFfmpegExecutor] 委托 [FfmpegEngine]。
 * @param probeVideo 视频信息探测函数，默认委托 [FfprobeHelper.getVideoInfo]。
 */
class AVStreamSwapper(
    private val executor: FfmpegExecutor = DefaultFfmpegExecutor,
    private val probeVideo: suspend (String) -> VideoClip = { FfprobeHelper.getVideoInfo(it) }
) {

    /**
     * 执行 AB 搬运。
     *
     * 在挂起阶段完成视频探测与命令构建，返回的 [Flow] 为冷流，
     * 收集时才真正调用 [FfmpegExecutor.executeAsync] 执行 FFmpeg。
     *
     * 流发射序列：
     * - 若干条进行中的 [ABTransportProgress]（progress 0f..1f，outputPath = null）
     * - 最终一条：成功时 progress=1f 且 outputPath 非空；失败时 error 非空
     *
     * 取消语义：消费端取消收集时，会触发 [FfmpegHandle.cancel] 取消 FFmpeg 会话。
     *
     * @param config 搬运配置。
     * @return 进度流。
     */
    suspend fun swap(config: ABTransportConfig): Flow<ABTransportProgress> {
        // 1. 探测视频时长（挂起，在调用方协程中执行，便于提前失败）
        val clipA = probeVideo(config.videoAPath)
        val clipB = probeVideo(config.videoBPath)
        val totalMs = chooseTotalMs(clipA, clipB, config)

        // 2. 构建命令
        val command = buildCommand(config)

        // 3. 返回冷流：收集时才执行 FFmpeg
        return callbackFlow {
            val handle = executor.executeAsync(
                command = command,
                durationMs = totalMs,
                onProgress = { progress ->
                    val currentMs = (progress * totalMs.toFloat()).toLong()
                    trySend(
                        ABTransportProgress(
                            progress = progress,
                            currentMs = currentMs,
                            totalMs = totalMs
                        )
                    )
                },
                onComplete = { success ->
                    if (success) {
                        trySend(
                            ABTransportProgress(
                                progress = 1f,
                                currentMs = totalMs,
                                totalMs = totalMs,
                                outputPath = config.outputPath
                            )
                        )
                    } else {
                        trySend(
                            ABTransportProgress(
                                progress = 0f,
                                currentMs = 0L,
                                totalMs = totalMs,
                                error = FfmpegSwapException("FFmpeg 执行失败，返回码非 0")
                            )
                        )
                    }
                    close()
                }
            )

            // 消费端取消收集时取消 FFmpeg 会话
            awaitClose {
                handle.cancel()
            }
        }
    }

    // ===== 内部：时长选择 =====

    /**
     * 根据时长策略选择用于进度换算的总时长（毫秒）。
     *
     * - [DurationStrategy.TRUNCATE]：取 min(A, B)
     * - [DurationStrategy.LOOP]：取 max(A, B)
     * - [DurationStrategy.CUSTOM]：以 B（画面源）的自定义片段为准，回退到 B 全长
     */
    internal fun chooseTotalMs(
        clipA: VideoClip,
        clipB: VideoClip,
        config: ABTransportConfig
    ): Long {
        return when (config.durationStrategy) {
            DurationStrategy.TRUNCATE -> minOf(clipA.duration, clipB.duration)
            DurationStrategy.LOOP -> maxOf(clipA.duration, clipB.duration)
            DurationStrategy.CUSTOM -> config.segmentB?.durationMs ?: clipB.duration
        }
    }

    // ===== 内部：命令构建 =====

    /**
     * 构建 AB 搬运 FFmpeg 命令。
     *
     * 输入顺序：输入 0 = B（画面源），输入 1 = A（音频源）。
     * 命令结构：
     * ```
     * ffmpeg [-ss X -to Y] -i "B" [-ss X -to Y] -i "A" \
     *   [PURE_REPLACE: -map 0:v:0 -map 1:a:0] \
     *   [MIX: -filter_complex "..." -map 0:v:0 -map [aout]] \
     *   -c:v copy -c:a aac [-shortest] -y "OUTPUT"
     * ```
     *
     * @param config 搬运配置。
     * @return 完整的 FFmpeg 命令字符串（含 `ffmpeg` 前缀）。
     */
    internal fun buildCommand(config: ABTransportConfig): String {
        val parts = mutableListOf<String>()
        parts.add("ffmpeg")

        // 输入 0：B 画面源（可选自定义片段 -ss/-to）
        appendInput(parts, config.videoBPath, config.segmentB)

        // 输入 1：A 音频源（可选自定义片段 -ss/-to）
        appendInput(parts, config.videoAPath, config.segmentA)

        when (config.mode) {
            ABTransportMode.PURE_REPLACE -> {
                // B 画面 + A 音频
                parts.add("-map"); parts.add("0:v:0")
                parts.add("-map"); parts.add("1:a:0")
            }
            ABTransportMode.MIX -> {
                // B 画面 + (A 音频 + B 原声) 混音
                parts.add("-filter_complex"); parts.add(buildMixFilter(config))
                parts.add("-map"); parts.add("0:v:0")
                parts.add("-map"); parts.add(LABEL_AOUT)
            }
        }

        // 编码器：视频直接复制（仅替换/混音音频），音频统一 AAC
        parts.add("-c:v"); parts.add("copy")
        parts.add("-c:a"); parts.add("aac")

        // 时长策略：TRUNCATE 截断到短者
        if (config.durationStrategy == DurationStrategy.TRUNCATE) {
            parts.add("-shortest")
        }

        // 覆盖输出
        parts.add("-y")
        parts.add(config.outputPath)

        return parts.joinToString(" ") { quoteIfNeeded(it) }
    }

    /**
     * 追加一个输入（含可选 -ss/-to 片段）。
     */
    private fun appendInput(
        parts: MutableList<String>,
        path: String,
        segment: TimelineSegment?
    ) {
        segment?.let { seg ->
            parts.add("-ss"); parts.add(formatTime(seg.startMs))
            parts.add("-to"); parts.add(formatTime(seg.endMs))
        }
        parts.add("-i"); parts.add(path)
    }

    /**
     * 构建 MIX 模式的 filter_complex。
     *
     * 输入 1（A 音频）按 [ABTransportConfig.volumeRatioA] 调整音量，
     * 输入 0（B 原声）按 [ABTransportConfig.volumeRatioB] 调整音量，
     * 再通过 amix 混合为 [LABEL_AOUT]。
     *
     * 例如：`[1:a]volume=1.00[a1];[0:a]volume=0.50[a2];[a1][a2]amix=inputs=2:duration=longest:dropout=0[aout]`
     */
    private fun buildMixFilter(config: ABTransportConfig): String {
        val va = formatVolume(config.volumeRatioA)
        val vb = formatVolume(config.volumeRatioB)
        return buildString {
            append("[1:a]volume=$va[a1]")
            append(";")
            append("[0:a]volume=$vb[a2]")
            append(";")
            append("[a1][a2]amix=inputs=2:duration=longest:dropout=0")
            append(LABEL_AOUT)
        }
    }

    /** 毫秒转 FFmpeg 时间字符串（秒，保留 3 位小数）。固定使用 [Locale.US] 避免本地化小数分隔符。 */
    private fun formatTime(ms: Long): String = String.format(Locale.US, "%.3f", ms / 1000.0)

    /** 音量格式化（2 位小数）。固定使用 [Locale.US] 避免本地化小数分隔符。 */
    private fun formatVolume(v: Float): String = String.format(Locale.US, "%.2f", v)

    /**
     * 含空格或冒号的 token 加引号；flag（`-` 开头）与滤镜标签（`[` 开头）原样输出。
     *
     * FFmpeg 流选择器（如 `0:v:0`、`1:a:0`）使用 `:` 作分隔符但**不应**被加引号，
     * 否则 FFmpeg 会将其识别为文件名导致 `-map` 失败。故需先排除流选择器再做引号判定。
     */
    private fun quoteIfNeeded(token: String): String {
        if (token.startsWith("-") || token.startsWith("[")) return token
        if (isStreamSpecifier(token)) return token
        if (token.any { it.isWhitespace() || it == ':' }) return "\"$token\""
        return token
    }

    /** 判断是否为 FFmpeg 流选择器（形如 `0:v:0`、`1:a:0`、`0:s:1`）。 */
    private fun isStreamSpecifier(token: String): Boolean {
        return STREAM_SPECIFIER_REGEX.matches(token)
    }

    private companion object {
        const val LABEL_AOUT = "[aout]"

        /** 匹配 FFmpeg 流选择器：`输入索引:流类型:流索引`，如 `0:v:0`、`1:a:0`、`0:s:1`。 */
        val STREAM_SPECIFIER_REGEX = Regex("""\d+:[avs]:\d+""")
    }
}

/**
 * AB 搬运执行异常。
 *
 * 当 FFmpeg 命令执行失败（返回码非 0）时，由 [AVStreamSwapper.swap]
 * 作为 [ABTransportProgress.error] 发射。
 */
class FfmpegSwapException(message: String) : RuntimeException(message)

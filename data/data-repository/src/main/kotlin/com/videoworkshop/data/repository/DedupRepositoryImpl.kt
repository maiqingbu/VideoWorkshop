package com.videoworkshop.data.repository

import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.core.ffmpeg.FfmpegEngine
import com.videoworkshop.core.ffmpeg.FfprobeHelper
import com.videoworkshop.core.ffmpeg.enhance.AudioMixer
import com.videoworkshop.core.ffmpeg.enhance.StickerOverlayer
import com.videoworkshop.core.ffmpeg.enhance.SubtitleBurner
import com.videoworkshop.core.ffmpeg.enhance.ThumbnailExtractor
import com.videoworkshop.core.ffmpeg.operators.AVStreamSwapper
import com.videoworkshop.core.ffmpeg.pipeline.DedupCommandBuilder
import com.videoworkshop.domain.model.ABTransportConfig
import com.videoworkshop.domain.model.ABTransportProgress
import com.videoworkshop.domain.model.DedupConfig
import com.videoworkshop.domain.model.DedupProgress
import com.videoworkshop.domain.model.EnhanceConfig
import com.videoworkshop.domain.model.VideoClip
import com.videoworkshop.domain.repository.DedupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * [DedupRepository] 实现：基于 FFmpeg 的视频去重、信息解析、增强与 AB 搬运。
 *
 * 去重流程：
 * 1. 通过 [FfprobeHelper] 获取视频元信息（时长、码率、帧率等）
 * 2. 使用 [DedupCommandBuilder] 根据配置拼装 FFmpeg 去重命令
 * 3. 调用 [FfmpegEngine.executeAsync] 异步执行，将进度回调转换为 [Flow]
 * 4. 8 项去重算法分步骤报告进度（stepIndex 0-7）
 *
 * 增强流程：
 * - 根据配置依次执行音频混合、字幕烧录、贴纸叠加
 * - 每步输出作为下一步输入，最终输出到指定路径
 * - 总体进度 = (已完成步骤 + 当前步骤进度) / 总步骤数
 *
 * AB 搬运流程：
 * - 委托 [AVStreamSwapper] 完成「探测 → 命令构建 → 执行 → 进度推送」完整链路
 * - AVStreamSwapper 内部已封装 PURE_REPLACE/MIX 模式与 TRUNCATE/LOOP/CUSTOM 时长策略
 * - 本仓库仅负责：调用 [AVStreamSwapper.swap]，并将 core-ffmpeg 层的进度类型
 *   映射为 domain 层的 [ABTransportProgress]（error 由 Throwable 投射为 String）
 * - 探测阶段异常（文件不存在、A 视频无音轨等）通过 [flow] + [catch] 转为
 *   单条 error 进度发射，避免上层调用方需要 try/catch
 *
 * @param dispatchers 协程调度器
 * @param avStreamSwapper AB 搬运执行器，由 DI 提供默认实例（基于 [FfmpegEngine]）
 */
class DedupRepositoryImpl @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val avStreamSwapper: AVStreamSwapper
) : DedupRepository {

    override suspend fun dedupVideo(
        inputPath: String,
        outputPath: String,
        config: DedupConfig
    ): Flow<DedupProgress> {
        // 1. 获取视频元信息
        val videoInfo = withContext(dispatchers.io) {
            FfprobeHelper.getVideoInfo(inputPath)
        }

        // 2. 构建去重命令
        val command = DedupCommandBuilder.build(inputPath, outputPath, videoInfo, config)
        val durationMs = videoInfo.duration

        // 3. 返回进度流
        return channelFlow {
            // 发送初始进度
            trySend(DedupProgress(DEDUP_STEPS[0], 0, TOTAL_STEPS, 0f))

            // 4. 异步执行 FFmpeg，进度回调转 Flow
            val success = suspendCancellableCoroutine { cont ->
                FfmpegEngine.executeAsync(
                    command = command,
                    onProgress = { progress ->
                        val stepIndex = (progress * TOTAL_STEPS).toInt()
                            .coerceIn(0, TOTAL_STEPS - 1)
                        trySend(
                            DedupProgress(
                                currentStep = DEDUP_STEPS[stepIndex],
                                stepIndex = stepIndex,
                                totalSteps = TOTAL_STEPS,
                                progress = progress
                            )
                        )
                    },
                    onComplete = { success ->
                        if (cont.isActive) {
                            cont.resume(success)
                        }
                    },
                    durationMs = durationMs
                )
                cont.invokeOnCancellation {
                    FfmpegEngine.cancel()
                }
            }

            if (success) {
                trySend(
                    DedupProgress(
                        currentStep = "完成",
                        stepIndex = TOTAL_STEPS - 1,
                        totalSteps = TOTAL_STEPS,
                        progress = 1f
                    )
                )
            } else {
                close(IOException("FFmpeg 去重执行失败"))
                return@channelFlow
            }
        }
    }

    override suspend fun getVideoInfo(path: String): VideoClip =
        withContext(dispatchers.io) {
            FfprobeHelper.getVideoInfo(path)
        }

    override suspend fun hasAudioTrack(path: String): Boolean =
        withContext(dispatchers.io) {
            FfprobeHelper.hasAudioTrack(path)
        }

    override suspend fun extractKeyframes(
        videoPath: String,
        count: Int,
        outputDir: String
    ): List<String> = withContext(dispatchers.io) {
        if (count <= 0) return@withContext emptyList()
        val clip = runCatching { FfprobeHelper.getVideoInfo(videoPath) }.getOrNull()
            ?: return@withContext emptyList()
        if (clip.duration <= 0L) return@withContext emptyList()

        val dir = File(outputDir).apply { if (!exists()) mkdirs() }
        val durationSec = clip.duration / 1000.0
        // 均匀采样：在 [0, duration] 内取 count 个点
        val steps = if (count == 1) listOf(0.0) else (0 until count).map { it.toDouble() / (count - 1) * durationSec }
        val results = mutableListOf<String>()
        val baseName = File(videoPath).nameWithoutExtension
        steps.forEachIndexed { index, ts ->
            val out = File(dir, "${baseName}_kf_$index.jpg").absolutePath
            val cmd = ThumbnailExtractor.buildExtractCommand(videoPath, ts.toFloat(), out)
            val ok = runCatching { FfmpegEngine.execute(cmd) }.getOrDefault(false)
            if (ok && File(out).exists()) results.add(out)
        }
        results
    }

    override suspend fun enhanceVideo(
        inputPath: String,
        outputPath: String,
        config: EnhanceConfig
    ): Flow<Float> {
        // 预构建增强步骤（含临时文件创建）
        val steps = prepareEnhanceSteps(inputPath, outputPath, config)

        return channelFlow {
            val totalSteps = steps.size
            if (totalSteps == 0) {
                trySend(1f)
                return@channelFlow
            }

            for ((index, step) in steps.withIndex()) {
                val stepIndex = index
                val success = suspendCancellableCoroutine { cont ->
                    FfmpegEngine.executeAsync(
                        command = step.command,
                        onProgress = { progress ->
                            val overall = (stepIndex + progress) / totalSteps.toFloat()
                            trySend(overall.coerceIn(0f, 1f))
                        },
                        onComplete = { s ->
                            if (cont.isActive) {
                                cont.resume(s)
                            }
                        },
                        durationMs = step.durationMs
                    )
                    cont.invokeOnCancellation {
                        FfmpegEngine.cancel()
                    }
                }

                if (!success) {
                    close(IOException("增强步骤失败: ${step.name}"))
                    return@channelFlow
                }
            }

            trySend(1f)
        }
    }

    override suspend fun abTransport(config: ABTransportConfig): Flow<ABTransportProgress> {
        // 预检查：A/B 视频文件必须存在，避免 probe 阶段抛出晦涩的 FFmpeg 错误
        val missingFile = listOf(config.videoAPath, config.videoBPath)
            .firstOrNull { !File(it).exists() }
        if (missingFile != null) {
            return flow {
                emit(
                    ABTransportProgress(
                        progress = 0f,
                        currentMs = 0L,
                        totalMs = 0L,
                        error = "视频文件不存在: $missingFile"
                    )
                )
            }
        }

        // 调用 AVStreamSwapper.swap()，并将 core-ffmpeg 进度类型映射为 domain 进度类型
        // - swap() 是 suspend：probe 阶段失败会抛异常，通过 catch 转为单条 error 进度
        // - 返回的 Flow 在收集阶段可能发射 error 进度（FFmpeg 失败），同样通过 catch 兜底
        return try {
            avStreamSwapper.swap(config)
                .map { coreProgress ->
                    ABTransportProgress(
                        progress = coreProgress.progress,
                        currentMs = coreProgress.currentMs,
                        totalMs = coreProgress.totalMs,
                        outputPath = coreProgress.outputPath,
                        error = coreProgress.error?.message
                    )
                }
                .catch { throwable ->
                    // 兜底：收集阶段未预期的异常（如 A 视频无音轨导致 FFmpeg 异常退出）
                    emit(
                        ABTransportProgress(
                            progress = 0f,
                            currentMs = 0L,
                            totalMs = 0L,
                            error = throwable.message ?: "AB 搬运执行失败"
                        )
                    )
                }
        } catch (throwable: Throwable) {
            // probe 阶段失败（如 FfprobeHelper 解析失败、文件不可读、A 视频无音轨等）
            flow {
                emit(
                    ABTransportProgress(
                        progress = 0f,
                        currentMs = 0L,
                        totalMs = 0L,
                        error = throwable.message ?: "AB 搬运初始化失败"
                    )
                )
            }
        }
    }

    // ===== 增强步骤构建 =====

    /**
     * 增强步骤定义。
     *
     * @param name       步骤名称
     * @param command    FFmpeg 命令
     * @param durationMs 视频时长（毫秒），用于进度换算
     */
    private data class EnhanceStep(
        val name: String,
        val command: String,
        val durationMs: Long
    )

    /**
     * 准备增强步骤，创建所需临时文件并构建 FFmpeg 命令链。
     */
    private suspend fun prepareEnhanceSteps(
        inputPath: String,
        outputPath: String,
        config: EnhanceConfig
    ): List<EnhanceStep> = withContext(dispatchers.io) {
        val videoInfo = FfprobeHelper.getVideoInfo(inputPath)
        val durationMs = videoInfo.duration
        val durationSec = durationMs / 1000.0
        val outputDir = File(outputPath).parentFile ?: File(System.getProperty("java.io.tmpdir"))
        if (!outputDir.exists()) outputDir.mkdirs()

        val pendingSteps = mutableListOf<Pair<String, (String, String) -> String>>()

        // 步骤1: 音频混合（有 BGM 时启用）
        if (config.bgm != null) {
            val silentAudioPath = File(outputDir, "silent_voice.aac").absolutePath
            createSilentAudio(silentAudioPath, durationSec)
            val bgmPath = config.bgm
            pendingSteps.add("音频混合" to { input, output ->
                AudioMixer.buildMixCommand(input, silentAudioPath, bgmPath, output)
            })
        }

        // 步骤2: 字幕烧录
        if (config.subtitle) {
            val srtPath = File(outputDir, "subtitle.srt").absolutePath
            createSrtFile(srtPath, config.copy ?: "")
            val style = config.subtitleStyle ?: DEFAULT_SUBTITLE_STYLE
            pendingSteps.add("字幕烧录" to { input, output ->
                SubtitleBurner.buildBurnCommand(input, srtPath, output, style)
            })
        }

        // 步骤3: 贴纸叠加
        if (config.stickers.isNotEmpty()) {
            val stickerPath = config.stickers[0]
            val endTimeSec = durationSec.toFloat()
            pendingSteps.add("贴纸叠加" to { input, output ->
                StickerOverlayer.buildOverlayCommand(
                    videoPath = input,
                    stickerPath = stickerPath,
                    outputPath = output,
                    x = 0,
                    y = 0,
                    startTime = 0f,
                    endTime = endTimeSec
                )
            })
        }

        // 构建命令链：每步输出作为下一步输入
        val steps = mutableListOf<EnhanceStep>()
        var currentInput = inputPath
        for ((index, step) in pendingSteps.withIndex()) {
            val (name, builder) = step
            val isLast = index == pendingSteps.lastIndex
            val stepOutput = if (isLast) {
                outputPath
            } else {
                File(outputDir, "enhance_step$index.mp4").absolutePath
            }
            val command = builder(currentInput, stepOutput)
            steps.add(EnhanceStep(name, command, durationMs))
            currentInput = stepOutput
        }

        steps
    }

    /**
     * 使用 FFmpeg 创建静音音频文件，用作 AudioMixer 的 voicePath 占位。
     */
    private fun createSilentAudio(path: String, durationSec: Double) {
        val command = "ffmpeg -f lavfi -i anullsrc=r=44100:cl=mono " +
            "-t $durationSec -q:a 9 -acodec aac -y \"$path\""
        FfmpegEngine.execute(command)
    }

    /**
     * 创建简单的 SRT 字幕文件，将文案作为单条字幕。
     */
    private fun createSrtFile(path: String, text: String) {
        val srtContent = buildString {
            appendLine("1")
            appendLine("00:00:00,000 --> 00:00:10,000")
            appendLine(text.ifBlank { " " })
        }
        File(path).writeText(srtContent)
    }

    private companion object {
        /** 去重总步骤数（对应 8 项算法） */
        const val TOTAL_STEPS = 8

        /** 8 项去重算法步骤名称 */
        val DEDUP_STEPS = listOf(
            "修改MD5",
            "调整帧率",
            "修改码率",
            "裁剪变换",
            "镜像翻转",
            "色彩偏移",
            "音频重塑",
            "清理元数据"
        )

        /** 默认字幕样式（ASS force_style） */
        const val DEFAULT_SUBTITLE_STYLE =
            "FontName=Arial,FontSize=24,PrimaryColour=&H00FFFFFF," +
                "OutlineColour=&H00000000,BorderStyle=1,Outline=2,Shadow=0"
    }
}

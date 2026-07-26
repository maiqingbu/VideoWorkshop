package com.videoworkshop.core.ffmpeg.pipeline

import com.videoworkshop.core.ffmpeg.hw.HardwareEncoderDetector
import com.videoworkshop.domain.model.ABTransportConfig
import com.videoworkshop.domain.model.ABTransportMode

/**
 * AB 音轨替换命令构建器。
 *
 * 基于 [ABTransportConfig] 构建 FFmpeg 命令参数列表，实现 AB 搬运场景下的音轨替换与混合：
 * - [ABTransportMode.PURE_REPLACE]：用 A 的音频替换 B 的原声，输出 B 画面 + A 音频
 * - [ABTransportMode.MIX]：将 A 的音频与 B 的原声按比例混合，输出 B 画面 + 混合音频
 *
 * 输入约定（与 [com.videoworkshop.core.ffmpeg.operators.AVStreamSwapper] 保持一致）：
 * - 输入 0 = B 视频（画面源）
 * - 输入 1 = A 视频（音频源）
 *
 * 因此流映射固定为：
 * - 视频取自 B：`-map 0:v:0`
 * - 音频取自 A（PURE_REPLACE 时为 `1:a:0`）或混音结果（MIX 时为 `[aout]`）
 *
 * 该构建器通过组合 [MultiInputCommandBuilder] 完成命令装配，返回参数列表
 * （不含 `ffmpeg` 前缀），与 [MultiInputCommandBuilder] 风格一致。
 *
 * 视频编码策略：
 * - 默认 `-c:v copy`（直接复制 B 的视频流，因 AB 搬运不改变画面）
 * - 可通过 [reencodeVideo] 启用重编码，此时由 [HardwareEncoderDetector] 选择
 *   `h264_mediacodec`（硬编）或 `libx264`（软编）
 *
 * 命令结构（按 [build] 返回顺序）：
 * ```
 * -i <B> -i <A> \
 *   [-filter_complex "[1:a][0:a]amix=inputs=2:duration=first:weights='A_w B_w'[aout]"] \  // 仅 MIX
 *   -map 0:v:0 -map <1:a:0 | [aout]> \
 *   -c:v <copy | h264_mediacodec | libx264> \
 *   -c:a aac \
 *   <output>
 * ```
 *
 * 使用示例（纯替换）：
 * ```
 * val args = AVSwapCommandBuilder().build(
 *     ABTransportConfig(
 *         videoAPath = "/A.mp4",
 *         videoBPath = "/B.mp4",
 *         mode = ABTransportMode.PURE_REPLACE,
 *         durationStrategy = DurationStrategy.TRUNCATE,
 *         outputPath = "/out.mp4"
 *     )
 * )
 * ```
 */
class AVSwapCommandBuilder {

    /** amix 滤镜输出标签。 */
    private val labelAout = "[aout]"

    /** 是否对视频流重新编码。`false`（默认）时使用 `-c:v copy`。 */
    private var reencodeVideoEnabled: Boolean = false

    /**
     * 设置是否对视频流重新编码。
     *
     * - `false`（默认）：使用 `-c:v copy` 直接复制 B 的视频流
     * - `true`：通过 [HardwareEncoderDetector] 选择 `h264_mediacodec`（硬编）或 `libx264`（软编）
     *
     * @param enabled 是否启用重编码。
     * @return 当前构建器，支持链式调用。
     */
    fun reencodeVideo(enabled: Boolean): AVSwapCommandBuilder {
        reencodeVideoEnabled = enabled
        return this
    }

    /**
     * 根据 [ABTransportConfig] 构建 FFmpeg 命令参数列表。
     *
     * 输入顺序：输入 0 = B（画面源），输入 1 = A（音频源），与
     * [com.videoworkshop.core.ffmpeg.operators.AVStreamSwapper] 保持一致。
     *
     * @param config AB 搬运配置。
     * @return 参数列表（不含 `ffmpeg` 前缀），每个元素为一个独立 token。
     */
    fun build(config: ABTransportConfig): List<String> {
        val builder = MultiInputCommandBuilder()
            .addInput(config.videoBPath)   // 输入 0：B（画面源）
            .addInput(config.videoAPath)   // 输入 1：A（音频源）

        when (config.mode) {
            ABTransportMode.PURE_REPLACE -> {
                // 纯替换：B 画面 + A 音频
                builder.addMap("0:v:0")   // 取 B 的视频流
                builder.addMap("1:a:0")   // 取 A 的音频流
            }
            ABTransportMode.MIX -> {
                // 混合：B 画面 + (A 音频 + B 原声) 混音
                val filter = buildAmixFilter(config.volumeRatioA, config.volumeRatioB)
                builder.filterComplex(filter)
                builder.addMap("0:v:0")       // 取 B 的视频流
                builder.addMap(labelAout)     // 取混音结果
            }
        }

        // 视频编码器：默认 copy，启用重编码时由 HardwareEncoderDetector 选择
        val videoCodec = if (reencodeVideoEnabled) {
            HardwareEncoderDetector.getBestEncoder()
        } else {
            "copy"
        }
        builder.videoCodec(videoCodec)

        // 音频编码器固定为 aac
        builder.audioCodec("aac")

        // 输出路径
        builder.output(config.outputPath)

        return builder.build()
    }

    /**
     * 构建 amix 滤镜图。
     *
     * 输入顺序：输入 0 = B（原声 `[0:a]`），输入 1 = A（音频源 `[1:a]`）。
     * 为使 `weights` 第一个值对应 A 音量、第二个值对应 B 音量（与
     * [ABTransportConfig.volumeRatioA] / [ABTransportConfig.volumeRatioB] 的语义一致），
     * amix 的输入顺序写为 `[1:a][0:a]`（A 先 B 后）。
     *
     * 输出时长跟随第一路输入（A 的音频，`duration=first`）。
     *
     * @param volumeA A 音量权重（对应 [ABTransportConfig.volumeRatioA]）。
     * @param volumeB B 音量权重（对应 [ABTransportConfig.volumeRatioB]）。
     * @return filter_complex 滤镜图字符串。
     */
    private fun buildAmixFilter(volumeA: Float, volumeB: Float): String {
        return "[1:a][0:a]amix=inputs=2:duration=first:weights='${volumeA} ${volumeB}'$labelAout"
    }
}

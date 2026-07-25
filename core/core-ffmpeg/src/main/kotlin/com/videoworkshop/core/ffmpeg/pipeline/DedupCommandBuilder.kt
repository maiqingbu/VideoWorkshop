package com.videoworkshop.core.ffmpeg.pipeline

import com.videoworkshop.core.ffmpeg.algorithms.AudioReshaper
import com.videoworkshop.core.ffmpeg.algorithms.BitrateModifier
import com.videoworkshop.core.ffmpeg.algorithms.ColorShifter
import com.videoworkshop.core.ffmpeg.algorithms.CropTransformer
import com.videoworkshop.core.ffmpeg.algorithms.FpsAdjuster
import com.videoworkshop.core.ffmpeg.algorithms.Md5Modifier
import com.videoworkshop.core.ffmpeg.algorithms.MetadataCleaner
import com.videoworkshop.core.ffmpeg.algorithms.MirrorFlipper
import com.videoworkshop.core.ffmpeg.hw.HardwareEncoderDetector
import com.videoworkshop.domain.model.VideoClip

/**
 * 去重命令构建器（核心）。
 *
 * 根据 [DedupConfig] 的各开关和 [DedupStrength] 的强度参数，
 * 结合原始视频信息 [VideoClip]，拼装一条完整的 FFmpeg 命令。
 *
 * 命令结构：
 * ```
 * ffmpeg -i "INPUT" \
 *   [-filter_complex "[0:v]VF[vout];AUDIO" | -vf "VF"] \
 *   -map [vout|0:v] -map [aout|0:a?] \
 *   -c:v {h264_mediacodec|libx264} [-b:v BITRATE] -pix_fmt yuv420p \
 *   -c:a aac \
 *   [-map_metadata -1 -map_chapters -1] \
 *   [-fflags +bitexact] \
 *   -y "OUTPUT"
 * ```
 *
 * 视频滤镜按开关动态拼接：fps → crop → hflip → hue
 * 音频滤镜使用 equalizer + anoisesrc（需注入时长），通过 -filter_complex 实现。
 */
object DedupCommandBuilder {

    /** filter_complex 中视频输出标签。 */
    private const val LABEL_VOUT = "[vout]"

    /** filter_complex 中音频输出标签。 */
    private const val LABEL_AOUT = "[aout]"

    /**
     * 构建去重 FFmpeg 命令。
     *
     * @param inputPath  输入视频路径。
     * @param outputPath 输出视频路径。
     * @param videoInfo  原始视频信息（提供码率、时长等）。
     * @param config     去重配置（开关 + 强度）。
     * @return 完整的 FFmpeg 命令字符串（不含 `ffmpeg` 前缀，由 [com.videoworkshop.core.ffmpeg.FfmpegEngine] 执行）。
     */
    fun build(
        inputPath: String,
        outputPath: String,
        videoInfo: VideoClip,
        config: DedupConfig
    ): String {
        val cmd = StringBuilder()
        cmd.append("ffmpeg")
        cmd.append(" -i \"$inputPath\"")

        val videoFilters = buildVideoFilters(config)
        val useComplexFilter = config.audioReshape

        if (useComplexFilter) {
            appendComplexFilter(cmd, videoFilters, config, videoInfo)
        } else {
            appendSimpleFilter(cmd, videoFilters)
        }

        // 编码器
        val encoder = HardwareEncoderDetector.getBestEncoder()
        cmd.append(" -c:v $encoder")

        // 码率
        if (config.bitrateModify && videoInfo.bitrate > 0L) {
            val targetBitrate = BitrateModifier.buildParam(videoInfo.bitrate, config.strength)
            cmd.append(" -b:v $targetBitrate")
        }

        // 像素格式（h264_mediacodec 要求 yuv420p）
        cmd.append(" -pix_fmt yuv420p")

        // 音频编码
        cmd.append(" -c:a aac")

        // 元数据清理
        if (config.metadataClean) {
            cmd.append(" ${MetadataCleaner.buildParam()}")
        }

        // MD5 修改
        if (config.md5Modify) {
            cmd.append(" ${Md5Modifier.buildParam()}")
        }

        // 覆盖输出
        cmd.append(" -y")
        cmd.append(" \"$outputPath\"")

        return cmd.toString()
    }

    /**
     * 构建视频滤镜链（逗号分隔）。
     *
     * 顺序：fps → crop → hflip → hue
     */
    private fun buildVideoFilters(config: DedupConfig): String {
        val filters = mutableListOf<String>()
        if (config.fpsAdjust) {
            filters.add(FpsAdjuster.buildFilter(config.strength))
        }
        if (config.cropTransform) {
            filters.add(CropTransformer.buildFilter(config.strength))
        }
        if (config.mirrorFlip) {
            filters.add(MirrorFlipper.buildFilter())
        }
        if (config.colorShift) {
            filters.add(ColorShifter.buildFilter(config.strength))
        }
        return filters.joinToString(",")
    }

    /**
     * 使用 -filter_complex 拼接视频和音频滤镜（音频重塑场景）。
     *
     * 视频滤镜链输出为 [LABEL_VOUT]，音频滤镜图输出为 [LABEL_AOUT]。
     */
    private fun appendComplexFilter(
        cmd: StringBuilder,
        videoFilters: String,
        config: DedupConfig,
        videoInfo: VideoClip
    ) {
        val filterParts = mutableListOf<String>()

        // 视频滤镜
        if (videoFilters.isNotEmpty()) {
            filterParts.add("[0:v]$videoFilters$LABEL_VOUT")
        }

        // 音频滤镜（equalizer + anoisesrc + amix）
        val durationSec = videoInfo.duration / 1000.0
        filterParts.add(AudioReshaper.buildFilter(config.strength, durationSec))

        cmd.append(" -filter_complex \"${filterParts.joinToString(";")}\"")

        // 映射输出流
        if (videoFilters.isNotEmpty()) {
            cmd.append(" -map $LABEL_VOUT")
        } else {
            cmd.append(" -map 0:v")
        }
        cmd.append(" -map $LABEL_AOUT")
    }

    /**
     * 使用 -vf 拼接视频滤镜（无音频重塑场景）。
     */
    private fun appendSimpleFilter(cmd: StringBuilder, videoFilters: String) {
        if (videoFilters.isNotEmpty()) {
            cmd.append(" -vf \"$videoFilters\"")
        }
        cmd.append(" -map 0:v -map 0:a?")
    }
}

package com.videoworkshop.core.ffmpeg.enhance

import com.videoworkshop.core.ffmpeg.hw.HardwareEncoderDetector

/**
 * 字幕烧录器。
 *
 * 使用 FFmpeg 的 `subtitles` 滤镜将 SRT 字幕硬编码（烧录）到视频画面中，
 * 支持 ASS 样式覆盖（force_style）。
 *
 * 烧录后的字幕成为画面的一部分，无需播放器支持外挂字幕，
 * 适用于各平台的带货视频分发。
 */
object SubtitleBurner {

    /**
     * 构建字幕烧录命令。
     *
     * @param videoPath  原始视频路径。
     * @param srtPath    SRT 字幕文件路径。
     * @param outputPath 输出视频路径。
     * @param style      ASS 样式字符串，例如 `FontName=Arial,FontSize=24,PrimaryColour=&H00FFFFFF`。
     * @return 完整的 FFmpeg 命令字符串。
     */
    fun buildBurnCommand(
        videoPath: String,
        srtPath: String,
        outputPath: String,
        style: String
    ): String {
        // subtitles 滤镜中 ':' 是选项分隔符，路径中的 ':' 需转义
        val escapedPath = srtPath.replace("\\", "\\\\").replace(":", "\\:")
        val encoder = HardwareEncoderDetector.getBestEncoder()

        return buildString {
            append("ffmpeg")
            append(" -i \"$videoPath\"")
            append(" -vf \"subtitles=$escapedPath:force_style='$style'\"")
            append(" -c:v $encoder")
            append(" -pix_fmt yuv420p")
            append(" -c:a copy")
            append(" -y \"$outputPath\"")
        }
    }
}

package com.videoworkshop.core.ffmpeg.enhance

/**
 * 缩略图提取器。
 *
 * 使用 FFmpeg 从视频中提取指定时间点的单帧画面作为缩略图。
 *
 * 采用 `-ss` 前置 `-i` 的快速 seek 方式，先跳转到目标时间附近的关键帧，
 * 再解码输出单帧，适用于列表展示、封面生成等场景。
 */
object ThumbnailExtractor {

    /**
     * 构建缩略图提取命令。
     *
     * @param videoPath  视频文件路径。
     * @param timestamp  截图时间点（秒）。
     * @param outputPath 输出图片路径（如 `.jpg`、`.png`）。
     * @return 完整的 FFmpeg 命令字符串。
     */
    fun buildExtractCommand(
        videoPath: String,
        timestamp: Float,
        outputPath: String
    ): String {
        return buildString {
            append("ffmpeg")
            // -ss 前置 -i：快速 seek 到目标时间附近的关键帧
            append(" -ss $timestamp")
            append(" -i $videoPath")
            // 仅输出 1 帧
            append(" -frames:v 1")
            // JPEG 质量（2 = 高质量，范围 2-31，越小越好）
            append(" -q:v 2")
            append(" -y $outputPath")
        }
    }
}

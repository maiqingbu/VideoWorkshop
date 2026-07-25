package com.videoworkshop.core.ffmpeg.enhance

import com.videoworkshop.core.ffmpeg.hw.HardwareEncoderDetector

/**
 * 贴纸叠加器。
 *
 * 使用 FFmpeg 的 `overlay` 滤镜将贴纸图片（PNG 等）叠加到视频画面上，
 * 支持指定位置和显示时间段。
 *
 * 适用于在带货视频中添加品牌 Logo、促销标签、动态贴纸等。
 */
object StickerOverlayer {

    /** filter_complex 中视频输出标签。 */
    private const val LABEL_VOUT = "[vout]"

    /**
     * 构建贴纸叠加命令。
     *
     * @param videoPath  原始视频路径。
     * @param stickerPath 贴纸图片路径（PNG，建议带透明通道）。
     * @param outputPath  输出视频路径。
     * @param x          贴纸左上角 X 坐标（像素）。
     * @param y          贴纸左上角 Y 坐标（像素）。
     * @param startTime  贴纸开始显示时间（秒）。
     * @param endTime    贴纸结束显示时间（秒）。
     * @return 完整的 FFmpeg 命令字符串。
     */
    fun buildOverlayCommand(
        videoPath: String,
        stickerPath: String,
        outputPath: String,
        x: Int,
        y: Int,
        startTime: Float,
        endTime: Float
    ): String {
        val encoder = HardwareEncoderDetector.getBestEncoder()

        return buildString {
            append("ffmpeg")
            append(" -i \"$videoPath\"")
            append(" -i \"$stickerPath\"")

            // overlay 滤镜：将贴纸叠加到视频上，仅在指定时间段显示
            append(" -filter_complex \"")
            append("[0:v][1:v]overlay=$x:$y:enable='between(t,$startTime,$endTime)'")
            append(LABEL_VOUT)
            append("\"")

            // 映射输出
            append(" -map $LABEL_VOUT")
            append(" -map 0:a?")

            // 编码
            append(" -c:v $encoder")
            append(" -pix_fmt yuv420p")
            append(" -c:a aac")
            append(" -y \"$outputPath\"")
        }
    }
}

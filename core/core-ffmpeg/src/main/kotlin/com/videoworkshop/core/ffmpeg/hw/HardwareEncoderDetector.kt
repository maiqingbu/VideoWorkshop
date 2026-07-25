package com.videoworkshop.core.ffmpeg.hw

import android.media.MediaCodecList

/**
 * 硬件编码器检测器。
 *
 * 检测当前设备是否支持通过 Android MediaCodec 进行 H.264 硬件编码，
 * 用于在 FFmpeg 命令中选择 `h264_mediacodec`（硬编）或 `libx264`（软编）。
 *
 * 检测结果会被缓存，避免重复查询 [MediaCodecList]。
 */
object HardwareEncoderDetector {

    /** H.264 的 MIME 类型。 */
    private const val MIME_AVC = "video/avc"

    /** FFmpegKit 中的硬件编码器名称。 */
    private const val ENCODER_HARDWARE = "h264_mediacodec"

    /** FFmpegKit 中的软件编码器名称。 */
    private const val ENCODER_SOFTWARE = "libx264"

    /** 缓存的检测结果，避免重复查询。 */
    @Volatile
    private var cachedSupport: Boolean? = null

    /**
     * 检测设备是否支持 H.264 硬件编码。
     *
     * 通过 [MediaCodecList] 枚举所有编码器，查找支持 `video/avc`（H.264）的编码器。
     * 只要存在任一 H.264 编码器（无论硬件还是软件实现），FFmpegKit 的
     * `h264_mediacodec` 即可使用。
     *
     * @return `true` 表示支持 h264_mediacodec 编码器。
     */
    fun isHardwareEncoderSupported(): Boolean {
        cachedSupport?.let { return it }
        val supported = try {
            queryEncoderSupport()
        } catch (e: Exception) {
            false
        }
        cachedSupport = supported
        return supported
    }

    /**
     * 获取最佳可用编码器名称。
     *
     * 优先返回硬件编码器 `h264_mediacodec`，不支持时回退到软件编码器 `libx264`。
     *
     * @return 编码器名称字符串。
     */
    fun getBestEncoder(): String {
        return if (isHardwareEncoderSupported()) ENCODER_HARDWARE else ENCODER_SOFTWARE
    }

    /**
     * 查询 MediaCodec 编码器支持情况。
     */
    private fun queryEncoderSupport(): Boolean {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        return codecList.codecInfos.any { info ->
            info.isEncoder && info.supportedTypes.any { type ->
                type.equals(MIME_AVC, ignoreCase = true)
            }
        }
    }
}

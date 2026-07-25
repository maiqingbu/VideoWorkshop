package com.videoworkshop.domain.model

/**
 * 本地视频片段的元信息。
 *
 * @param path     文件路径
 * @param duration 时长（毫秒）
 * @param width    宽度（像素）
 * @param height   高度（像素）
 * @param size     文件大小（字节）
 * @param fps      帧率
 * @param bitrate  码率（bps）
 * @param mimeType MIME 类型，例如 "video/mp4"
 */
data class VideoClip(
    val path: String,
    val duration: Long,
    val width: Int,
    val height: Int,
    val size: Long,
    val fps: Float,
    val bitrate: Long,
    val mimeType: String
)

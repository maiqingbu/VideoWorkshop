package com.videoworkshop.domain.model

/**
 * AB 搬运产物信息。
 *
 * @param outputPath     输出文件路径
 * @param durationMs     实际时长（毫秒）
 * @param thumbnailPath  缩略图路径，null 表示未生成
 * @param width          宽度（像素）
 * @param height         高度（像素）
 */
data class ABTransportResult(
    val outputPath: String,
    val durationMs: Long,
    val thumbnailPath: String?,
    val width: Int,
    val height: Int
)

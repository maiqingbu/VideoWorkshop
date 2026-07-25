package com.videoworkshop.domain.model

/**
 * 商品关联的视频源信息。
 *
 * @param url       视频下载地址
 * @param coverUrl  封面图地址，可能为空
 * @param duration  视频时长（毫秒）
 * @param format    视频格式，例如 "mp4"
 */
data class VideoSource(
    val url: String,
    val coverUrl: String?,
    val duration: Long,
    val format: String
)

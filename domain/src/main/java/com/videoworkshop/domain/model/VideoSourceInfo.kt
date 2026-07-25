package com.videoworkshop.domain.model

/**
 * 视频源信息（用于展示与上报的归一化结构）。
 *
 * @param url       视频地址
 * @param coverUrl  封面图地址
 * @param duration  视频时长（毫秒）
 * @param format    视频格式，例如 "mp4"
 */
data class VideoSourceInfo(
    val url: String,
    val coverUrl: String,
    val duration: Long,
    val format: String
)

package com.videoworkshop.data.alliance.jd

import com.videoworkshop.data.alliance.jd.model.JdMediaDto
import com.videoworkshop.data.alliance.jd.model.JdMediaResponse
import com.videoworkshop.domain.model.VideoSource

/**
 * 京东联盟媒体 DTO -> domain [VideoSource] 映射。
 *
 * 京东联盟的媒体查询接口只返回视频/图片信息，不含完整商品基础信息，
 * 因此这里只映射为 [VideoSource]；商品基础字段由仓库层 Mock 补全。
 */

/** 单条媒体 -> 视频源。 */
fun JdMediaDto.toVideoSource(): VideoSource? {
    val url = videoUrl?.takeIf { it.isNotBlank() } ?: return null
    return VideoSource(
        url = url,
        coverUrl = videoImage,
        duration = duration ?: 0L,
        format = format?.takeIf { it.isNotBlank() } ?: extractFormat(url),
    )
}

/** 响应 -> 视频源列表（过滤空 URL）。 */
fun JdMediaResponse.toVideoSources(): List<VideoSource> =
    data.mapNotNull { it.toVideoSource() }

/** 从视频 URL 中提取格式扩展名（小写），默认 `mp4`。 */
private fun extractFormat(url: String): String {
    val queryStripped = url.substringBefore('?')
    val dot = queryStripped.lastIndexOf('.')
    return if (dot >= 0 && dot < queryStripped.length - 1) {
        queryStripped.substring(dot + 1).lowercase()
    } else {
        "mp4"
    }
}

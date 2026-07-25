package com.videoworkshop.data.alliance.taobao

import com.videoworkshop.data.alliance.AllianceProvider
import com.videoworkshop.data.alliance.taobao.model.TaobaoItemDetailDto
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.VideoSource

/**
 * 淘宝商品 DTO -> domain [Goods] 映射。
 */
fun TaobaoItemDetailDto.toGoods(): Goods {
    val item = item ?: error("淘宝商品详情缺失 item 字段")
    val video = item.video
    val videoSources = if (video != null && !video.url.isNullOrBlank()) {
        listOf(
            VideoSource(
                url = video.url,
                coverUrl = video.coverUrl,
                duration = video.duration ?: 0L,
                format = extractFormat(video.url),
            )
        )
    } else {
        emptyList()
    }
    return Goods(
        id = item.numIid?.toString().orEmpty(),
        provider = AllianceProvider.TAOBAO,
        name = item.title.orEmpty(),
        price = item.price?.toDoubleOrNull() ?: 0.0,
        originalPrice = null,
        commissionRate = 0.0,
        promoUrl = null,
        imageUrl = item.pictUrl,
        videoSources = videoSources,
    )
}

/** 从视频 URL 中提取格式扩展名（小写，不含 `.`），默认 `mp4`。 */
private fun extractFormat(url: String): String {
    val queryStripped = url.substringBefore('?')
    val dot = queryStripped.lastIndexOf('.')
    return if (dot >= 0 && dot < queryStripped.length - 1) {
        queryStripped.substring(dot + 1).lowercase()
    } else {
        "mp4"
    }
}

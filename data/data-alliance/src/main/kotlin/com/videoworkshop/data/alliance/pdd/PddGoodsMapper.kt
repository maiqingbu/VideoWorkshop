package com.videoworkshop.data.alliance.pdd

import com.videoworkshop.data.alliance.AllianceProvider
import com.videoworkshop.data.alliance.pdd.model.PddGoodsDetailDto
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.VideoSource

/**
 * 拼多多商品 DTO -> domain [Goods] 映射。
 *
 * 注意：`min_group_price` 单位为分，需除以 100 转换为元。
 */
fun PddGoodsDetailDto.toGoods(goodsId: String): Goods {
    val detail = goodsDetailResponse
    val name = detail?.goodsName.orEmpty()
    val priceYuan = (detail?.minGroupPrice ?: 0L) / 100.0
    val imageUrl = detail?.goodsGalleryUrls?.firstOrNull()
    val videoSources = (detail?.videoUrls.orEmpty())
        .filter { it.isNotBlank() }
        .map { url ->
            VideoSource(
                url = url,
                coverUrl = imageUrl,
                duration = 0L,
                format = extractFormat(url),
            )
        }
    return Goods(
        id = goodsId,
        provider = AllianceProvider.PDD,
        name = name,
        price = priceYuan,
        originalPrice = null,
        commissionRate = 0.0,
        promoUrl = null,
        imageUrl = imageUrl,
        videoSources = videoSources,
    )
}

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

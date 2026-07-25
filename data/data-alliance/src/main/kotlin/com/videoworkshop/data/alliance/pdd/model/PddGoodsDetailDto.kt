package com.videoworkshop.data.alliance.pdd.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 拼多多 DDK `pdd.ddk.goods.detail` 商品详情响应。
 *
 * 顶层包裹 `goods_detail_response` 节点。
 */
@Serializable
data class PddGoodsDetailDto(
    @SerialName("goods_detail_response")
    val goodsDetailResponse: PddGoodsDetail? = null,
)

/**
 * 拼多多商品详情节点。
 *
 * @param goodsName         商品名称
 * @param minGroupPrice     拼团最低价（单位：分）
 * @param videoUrls         商品视频地址列表
 * @param goodsGalleryUrls  商品主图画廊地址列表
 */
@Serializable
data class PddGoodsDetail(
    @SerialName("goods_name") val goodsName: String? = null,
    @SerialName("min_group_price") val minGroupPrice: Long? = null,
    @SerialName("video_urls") val videoUrls: List<String> = emptyList(),
    @SerialName("goods_gallery_urls") val goodsGalleryUrls: List<String> = emptyList(),
)

/** Retrofit 接口返回类型别名，保持与 API 定义一致。 */
typealias PddGoodsDetailResponse = PddGoodsDetailDto

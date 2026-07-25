package com.videoworkshop.data.alliance.taobao.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 淘宝 TOP `taobao.tbk.item.info.get` 商品详情响应。
 *
 * 顶层即 `item` 节点（资质申请后真实响应可能再包裹一层
 * `tbk_item_info_get_response`，此处按业务字段建模，忽略外层信封）。
 */
@Serializable
data class TaobaoItemDetailDto(
    @SerialName("item") val item: TaobaoItemDto? = null,
)

/**
 * 淘宝商品节点。
 */
@Serializable
data class TaobaoItemDto(
    @SerialName("num_iid") val numIid: Long? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("price") val price: String? = null,
    @SerialName("pict_url") val pictUrl: String? = null,
    @SerialName("video") val video: TaobaoItemVideoDto? = null,
)

/**
 * 淘宝商品主图视频。
 */
@Serializable
data class TaobaoItemVideoDto(
    @SerialName("url") val url: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    /** 视频时长（毫秒）。 */
    @SerialName("duration") val duration: Long? = null,
)

/** Retrofit 接口返回类型别名，保持与 API 定义一致。 */
typealias TaobaoItemDetailResponse = TaobaoItemDetailDto

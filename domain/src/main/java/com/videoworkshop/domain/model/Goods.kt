package com.videoworkshop.domain.model

/**
 * 联盟商品信息。
 *
 * @param id              商品 ID
 * @param provider        所属联盟平台
 * @param name            商品名称
 * @param price           当前价格
 * @param originalPrice   原价，可能为空
 * @param commissionRate  佣金比例（0.0 ~ 1.0）
 * @param promoUrl        推广链接，可能为空
 * @param imageUrl        商品主图地址，可能为空
 * @param videoSources    关联的视频源列表
 */
data class Goods(
    val id: String,
    val provider: AllianceProvider,
    val name: String,
    val price: Double,
    val originalPrice: Double?,
    val commissionRate: Double,
    val promoUrl: String?,
    val imageUrl: String?,
    val videoSources: List<VideoSource>
)

package com.videoworkshop.domain.model

/**
 * 项目创建时锁定的商品快照。
 *
 * 使用快照可避免商品价格、标题、链接变化后破坏旧项目。
 * 支持联盟商品、链接解析商品和用户手动创建的自有商品。
 *
 * @param id              快照唯一 ID（UUID 字符串）
 * @param projectId       所属项目 ID
 * @param provider        联盟平台提供方
 * @param externalGoodsId 外部商品 ID（联盟侧 / 链接解析，可空）
 * @param name            商品名称
 * @param price           当前价格（元，可空）
 * @param originalPrice   原价（元，可空）
 * @param commissionRate  佣金比例（0.0 ~ 1.0，可空）
 * @param promoUrl        推广/购买链接（可空）
 * @param imageUrls       商品图片 URL 列表
 * @param videoUrls       商品视频 URL 列表
 * @param sellingPoints   卖点列表
 * @param capturedAt      快照捕获时间戳（毫秒）
 */
data class GoodsSnapshot(
    val id: String,
    val projectId: String,
    val provider: AllianceProvider,
    val externalGoodsId: String? = null,
    val name: String,
    val price: Double? = null,
    val originalPrice: Double? = null,
    val commissionRate: Double? = null,
    val promoUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val videoUrls: List<String> = emptyList(),
    val sellingPoints: List<String> = emptyList(),
    val capturedAt: Long = System.currentTimeMillis()
)
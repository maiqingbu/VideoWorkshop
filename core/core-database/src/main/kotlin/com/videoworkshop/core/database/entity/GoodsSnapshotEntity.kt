package com.videoworkshop.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 商品快照实体。
 *
 * 项目创建时锁定商品信息，避免原商品变更后破坏旧项目。
 *
 * @param id              快照唯一 ID（UUID 字符串）
 * @param projectId       所属项目 ID
 * @param provider        联盟平台提供方（[AllianceProvider] 的 name）
 * @param externalGoodsId 外部商品 ID（可空）
 * @param name            商品名称
 * @param price           当前价格（可空）
 * @param originalPrice   原价（可空）
 * @param commissionRate  佣金比例（可空）
 * @param promoUrl        推广链接（可空）
 * @param imageUrls       商品图片 URL 列表
 * @param videoUrls       商品视频 URL 列表
 * @param sellingPoints   卖点列表
 * @param capturedAt      快照捕获时间戳（毫秒）
 */
@Entity(
    tableName = "goods_snapshots",
    indices = [
        Index("projectId")
    ]
)
data class GoodsSnapshotEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val provider: String,
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
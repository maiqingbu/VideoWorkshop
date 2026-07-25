package com.videoworkshop.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 商品实体（联盟选品缓存）。
 *
 * @param id              商品唯一标识（联盟侧 ID）。
 * @param source          来源平台：taobao / jd / pdd。
 * @param name            商品名称。
 * @param price           单价（元）。
 * @param commissionRate  佣金比例（0.0 - 1.0）。
 * @param promoUrl        推广/购买链接。
 * @param imageUrl        主图 URL。
 * @param videoUrl        官方素材视频 URL（可空）。
 * @param createdAt       入库时间戳。
 */
@Entity(tableName = "goods")
data class GoodsEntity(
    @PrimaryKey
    val id: String,
    val source: String,
    val name: String,
    val price: Double,
    val commissionRate: Double,
    val promoUrl: String,
    val imageUrl: String,
    val videoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

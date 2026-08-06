package com.videoworkshop.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 创作项目实体。
 *
 * @param id              项目唯一 ID（UUID 字符串）
 * @param title           项目标题
 * @param type            项目类型（[ProjectType] 的 name）
 * @param status          项目状态（[ProjectStatus] 的 name）
 * @param goodsSnapshotId 关联商品快照 ID（可空）
 * @param targetPlatforms 目标发布平台集合（以 [PublishTarget] 的 name 存储）
 * @param coverAssetId    封面素材 ID（可空）
 * @param createdAt       创建时间戳（毫秒）
 * @param updatedAt       最近更新时间戳（毫秒）
 * @param lastOpenedAt    最近打开时间戳（毫秒）
 */
@Entity(
    tableName = "projects",
    indices = [
        Index("updatedAt"),
        Index("status")
    ]
)
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val type: String,
    val status: String,
    val goodsSnapshotId: String? = null,
    val targetPlatforms: Set<String> = emptySet(),
    val coverAssetId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis()
)
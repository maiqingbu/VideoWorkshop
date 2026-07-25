package com.videoworkshop.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 草稿实体（文案/脚本/视频配置等可继续编辑的内容）。
 *
 * @param id         草稿唯一标识。
 * @param type       草稿类型：script / copy / video_config。
 * @param goodsId    关联商品 ID（可空）。
 * @param content    草稿正文。
 * @param createdAt  创建时间戳。
 * @param updatedAt  最近更新时间戳。
 */
@Entity(
    tableName = "drafts",
    indices = [Index("goodsId")]
)
data class DraftEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val goodsId: String? = null,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

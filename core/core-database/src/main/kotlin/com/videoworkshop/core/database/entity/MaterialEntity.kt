package com.videoworkshop.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 素材实体（本地导入/生成/官方素材的索引）。
 *
 * @param id         素材唯一标识。
 * @param localPath  本地文件绝对路径。
 * @param source     来源：import / generated / official。
 * @param type       类型：video / image / audio。
 * @param thumbnail  缩略图本地路径（可空）。
 * @param tags       标签集合（经 [com.videoworkshop.core.database.converter.Converters] 转换）。
 * @param createdAt  入库时间戳。
 */
@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey
    val id: String,
    val localPath: String,
    val source: String,
    val type: String,
    val thumbnail: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

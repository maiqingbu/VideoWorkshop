package com.videoworkshop.domain.model

/**
 * 素材库实体。
 *
 * @param id        素材 ID
 * @param path      本地文件路径或内容 URI 字符串
 * @param source    素材来源描述：import / generated / official
 * @param type      素材类型，例如 "video" / "image" / "audio"
 * @param thumbnail 缩略图本地路径（可空）
 * @param tags      标签集合，用于编辑与筛选
 * @param note      备注，用户编辑保存的描述信息
 * @param createdAt 创建时间戳（毫秒）
 */
data class MaterialEntity(
    val id: Long,
    val path: String,
    val source: String,
    val type: String,
    val thumbnail: String? = null,
    val tags: List<String> = emptyList(),
    val note: String = "",
    val createdAt: Long
)

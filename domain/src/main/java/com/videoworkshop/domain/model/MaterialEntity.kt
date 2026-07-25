package com.videoworkshop.domain.model

/**
 * 素材库实体。
 *
 * @param id        素材 ID
 * @param path      本地文件路径
 * @param source    素材来源描述
 * @param type      素材类型，例如 "video" / "image" / "audio"
 * @param createdAt 创建时间戳（毫秒）
 */
data class MaterialEntity(
    val id: Long,
    val path: String,
    val source: String,
    val type: String,
    val createdAt: Long
)

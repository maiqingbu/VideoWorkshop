package com.videoworkshop.domain.model

/**
 * 草稿记录。
 *
 * @param id          草稿 ID
 * @param type        内容形式
 * @param goodsId     关联商品 ID
 * @param content     文案内容
 * @param mediaPaths  媒体文件路径列表
 * @param createdAt   创建时间戳（毫秒）
 */
data class Draft(
    val id: Long,
    val type: ContentType,
    val goodsId: String,
    val content: String,
    val mediaPaths: List<String>,
    val createdAt: Long
)

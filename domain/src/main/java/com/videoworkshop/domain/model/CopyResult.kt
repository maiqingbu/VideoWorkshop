package com.videoworkshop.domain.model

/**
 * AI 生成的文案结果。
 *
 * @param title         标题
 * @param body          正文
 * @param sellingPoints 卖点列表
 * @param tags          话题标签列表
 */
data class CopyResult(
    val title: String,
    val body: String,
    val sellingPoints: List<String>,
    val tags: List<String>
)

package com.videoworkshop.data.ai.llm.model

import kotlinx.serialization.Serializable

/**
 * OpenAI 兼容的 `/v1/chat/completions` 响应体。
 *
 * 仅保留文案生成所需的最小字段，配合 [kotlinx.serialization.json.Json] 的
 * `ignoreUnknownKeys` 自动忽略其余扩展字段。
 */
@Serializable
data class ChatResponse(
    val choices: List<Choice> = emptyList()
)

@Serializable
data class Choice(
    val message: ChatMessage? = null,
    val index: Int = 0,
    val finish_reason: String? = null
)

package com.videoworkshop.data.ai.llm.model

import kotlinx.serialization.Serializable

/**
 * OpenAI 兼容协议的单条对话消息。
 *
 * @param role    角色标识：system / user / assistant
 * @param content 文本内容
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

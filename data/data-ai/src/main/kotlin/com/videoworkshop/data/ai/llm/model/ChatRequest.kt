package com.videoworkshop.data.ai.llm.model

import kotlinx.serialization.Serializable

/**
 * OpenAI 兼容的 `/v1/chat/completions` 请求体。
 *
 * 字段命名与 OpenAI / DeepSeek / 通义千问兼容模式保持一致。
 *
 * @param model       模型标识，如 deepseek-chat / qwen-max
 * @param messages    对话消息列表
 * @param temperature 采样温度
 * @param max_tokens  最大生成 token 数
 * @param stream      是否流式返回（本项目统一关闭）
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.8,
    val max_tokens: Int = 2000,
    val stream: Boolean = false
)

package com.videoworkshop.data.ai.llm

import com.videoworkshop.data.ai.llm.model.ChatMessage

/**
 * 大模型 Provider 抽象。
 *
 * 多 Provider 通过 [LlmProviderRouter] 按优先级轮换；任一 Provider 失败时自动降级到下一个。
 */
interface LlmProvider {

    /** Provider 名称，用于日志与错误定位。 */
    val name: String

    /** OpenAI 兼容基地址，如 `https://api.deepseek.com/v1`。 */
    val baseUrl: String

    /**
     * 发起一次对话补全。
     *
     * @param messages 对话上下文
     * @param apiKey   API Key（由调用方从 core-datastore 读取后传入）
     * @return 模型生成的文本
     */
    suspend fun chat(messages: List<ChatMessage>, apiKey: String): String
}

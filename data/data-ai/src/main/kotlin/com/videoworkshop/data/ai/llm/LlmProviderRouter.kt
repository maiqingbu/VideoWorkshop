package com.videoworkshop.data.ai.llm

import com.videoworkshop.data.ai.llm.model.ChatMessage
import java.io.IOException
import javax.inject.Inject

/**
 * 多 Provider 轮换路由器（核心）。
 *
 * 按 [providers] 顺序依次尝试：DeepSeek -> Qwen -> (ERNIE 可扩展)。
 * 每个 Provider 独立 try-catch，失败自动降级；全部失败时抛出聚合异常。
 *
 * @param providers 按优先级排列的 Provider 列表，由 Hilt 注入。
 */
class LlmProviderRouter @Inject constructor(
    private val providers: @JvmSuppressWildcards List<LlmProvider>
) {

    /**
     * 按优先级发起对话补全。
     *
     * @param messages 对话上下文
     * @param apiKey   API Key（当前统一使用 DeepSeek Key，由 [com.videoworkshop.data.ai.AiRepositoryImpl] 传入）
     * @return 首个成功 Provider 的生成文本
     */
    suspend fun chat(messages: List<ChatMessage>, apiKey: String): String {
        require(providers.isNotEmpty()) { "未配置任何 LLM Provider" }

        var lastError: Throwable? = null
        for (provider in providers) {
            try {
                return provider.chat(messages, apiKey)
            } catch (e: Throwable) {
                lastError = e
                // 降级到下一个 Provider 继续尝试
            }
        }
        throw IOException(
            "全部 LLM Provider 均调用失败，最近错误: ${lastError?.message}",
            lastError
        )
    }
}

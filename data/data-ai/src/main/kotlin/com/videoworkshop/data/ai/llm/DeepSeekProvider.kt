package com.videoworkshop.data.ai.llm

import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.data.ai.di.DeepSeekApi
import com.videoworkshop.data.ai.llm.model.ChatMessage
import com.videoworkshop.data.ai.llm.model.ChatRequest
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * DeepSeek Provider，优先级最高。
 *
 * 走 OpenAI 兼容协议 `POST {baseUrl}/chat/completions`，
 * 模型固定为 `deepseek-chat`。
 */
class DeepSeekProvider @Inject constructor(
    @DeepSeekApi private val api: LlmApi,
    private val dispatchers: DispatcherProvider
) : LlmProvider {

    override val name: String = "DeepSeek"
    override val baseUrl: String = BASE_URL

    override suspend fun chat(messages: List<ChatMessage>, apiKey: String): String =
        withContext(dispatchers.io) {
            val request = ChatRequest(model = MODEL, messages = messages)
            val response = api.chatCompletion("Bearer $apiKey", request)
            response.choices
                .firstOrNull()?.message?.content
                ?.takeIf { it.isNotBlank() }
                ?: throw IOException("DeepSeek 返回空内容")
        }

    companion object {
        const val BASE_URL: String = "https://api.deepseek.com/v1"
        const val MODEL: String = "deepseek-chat"
    }
}

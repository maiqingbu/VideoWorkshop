package com.videoworkshop.data.ai.llm

import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.data.ai.di.QwenApi
import com.videoworkshop.data.ai.llm.model.ChatMessage
import com.videoworkshop.data.ai.llm.model.ChatRequest
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * 通义千问 Provider，DeepSeek 失败时降级使用。
 *
 * 阿里云 DashScope 的 OpenAI 兼容模式：
 * `POST {baseUrl}/chat/completions`，模型固定为 `qwen-max`。
 */
class QwenProvider @Inject constructor(
    @QwenApi private val api: LlmApi,
    private val dispatchers: DispatcherProvider
) : LlmProvider {

    override val name: String = "Qwen"
    override val baseUrl: String = BASE_URL

    override suspend fun chat(messages: List<ChatMessage>, apiKey: String): String =
        withContext(dispatchers.io) {
            val request = ChatRequest(model = MODEL, messages = messages)
            val response = api.chatCompletion("Bearer $apiKey", request)
            response.choices
                .firstOrNull()?.message?.content
                ?.takeIf { it.isNotBlank() }
                ?: throw IOException("Qwen 返回空内容")
        }

    companion object {
        const val BASE_URL: String = "https://dashscope.aliyuncs.com/compatible-mode/v1"
        const val MODEL: String = "qwen-max"
    }
}

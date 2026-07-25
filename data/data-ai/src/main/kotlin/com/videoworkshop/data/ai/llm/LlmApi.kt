package com.videoworkshop.data.ai.llm

import com.videoworkshop.data.ai.llm.model.ChatRequest
import com.videoworkshop.data.ai.llm.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI 兼容大模型 Retrofit 接口。
 *
 * 各 Provider（DeepSeek / Qwen / ERNIE）共用同一接口，
 * 仅 [Retrofit] 基地址不同，由 [com.videoworkshop.data.ai.di.AiModule] 注入限定实例。
 */
interface LlmApi {

    /**
     * @param auth    形如 `Bearer {apiKey}`，由调用方拼装
     * @param request 请求体
     */
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): ChatResponse
}

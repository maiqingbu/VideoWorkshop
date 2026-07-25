package com.videoworkshop.data.ai.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.videoworkshop.core.datastore.PreferenceRepository
import com.videoworkshop.data.ai.AiRepositoryImpl
import com.videoworkshop.data.ai.llm.DeepSeekProvider
import com.videoworkshop.data.ai.llm.LlmApi
import com.videoworkshop.data.ai.llm.LlmProvider
import com.videoworkshop.data.ai.llm.QwenProvider
import com.videoworkshop.domain.repository.AiRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Qualifier
import javax.inject.Singleton

/** 标识 DeepSeek 的 [LlmApi] 实例。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeepSeekApi

/** 标识通义千问的 [LlmApi] 实例。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class QwenApi

/**
 * AI 模块 Hilt 装配：提供 [AiRepository] 绑定、多 Provider 的 [LlmApi] Retrofit 实例、
 * [LlmProvider] 优先级列表与 [PreferenceRepository]。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    companion object {

        @Provides
        @Singleton
        @DeepSeekApi
        fun provideDeepSeekApi(okHttpClient: OkHttpClient, json: Json): LlmApi =
            buildLlmApi(DeepSeekProvider.BASE_URL, okHttpClient, json)

        @Provides
        @Singleton
        @QwenApi
        fun provideQwenApi(okHttpClient: OkHttpClient, json: Json): LlmApi =
            buildLlmApi(QwenProvider.BASE_URL, okHttpClient, json)

        /**
         * Provider 优先级：DeepSeek -> Qwen -> (ERNIE 可扩展追加)。
         * 顺序即降级顺序，由 [com.videoworkshop.data.ai.llm.LlmProviderRouter] 消费。
         */
        @Provides
        @Singleton
        fun provideLlmProviders(
            deepSeek: DeepSeekProvider,
            qwen: QwenProvider
        ): List<LlmProvider> = listOf(deepSeek, qwen)

        @Provides
        @Singleton
        fun providePreferenceRepository(
            @ApplicationContext context: Context
        ): PreferenceRepository = PreferenceRepository(context)

        private fun buildLlmApi(baseUrl: String, okHttpClient: OkHttpClient, json: Json): LlmApi =
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(LlmApi::class.java)
    }
}

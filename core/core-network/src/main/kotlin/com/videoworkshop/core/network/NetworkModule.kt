package com.videoworkshop.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.videoworkshop.core.common.AppConstants
import com.videoworkshop.core.common.DefaultDispatcherProvider
import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.core.network.interceptor.LoggingInterceptor
import com.videoworkshop.core.network.interceptor.RetryInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 网络层 Hilt 模块：统一提供 [Json]、[OkHttpClient]、[Retrofit] 与 [DispatcherProvider]。
 *
 * 拦截器链顺序：Retry -> Logging -> 业务请求。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** 默认后端基地址（可按需替换为各联盟/AI 网关地址）。 */
    private const val BASE_URL = "https://api.videoworkshop.com/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): LoggingInterceptor = LoggingInterceptor()

    @Provides
    @Singleton
    fun provideRetryInterceptor(): RetryInterceptor = RetryInterceptor()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: LoggingInterceptor,
        retryInterceptor: RetryInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(retryInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(AppConstants.NETWORK_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(AppConstants.NETWORK_READ_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(AppConstants.NETWORK_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
}

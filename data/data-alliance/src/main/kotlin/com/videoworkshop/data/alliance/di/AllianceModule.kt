package com.videoworkshop.data.alliance.di

import android.content.Context
import androidx.work.WorkManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.data.alliance.AllianceRepositoryImpl
import com.videoworkshop.data.alliance.jd.JdUnionApi
import com.videoworkshop.data.alliance.jd.JdUnionClient
import com.videoworkshop.data.alliance.pdd.PddDdkApi
import com.videoworkshop.data.alliance.pdd.PddDdkClient
import com.videoworkshop.data.alliance.taobao.TaobaoTbkApi
import com.videoworkshop.data.alliance.taobao.TaobaoTopClient
import com.videoworkshop.domain.repository.GoodsRepository
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

/**
 * 联盟数据层 Hilt 模块。
 *
 * 主要职责：
 * 1. 为三个联盟平台分别提供独立的 [Retrofit]（不同网关基地址）与对应 API 接口；
 * 2. 提供各平台客户端（凭证为占位空串，资质申请后替换为真实值即可启用真实调用）；
 * 3. 绑定 [GoodsRepository] -> [AllianceRepositoryImpl]；
 * 4. 提供 [WorkManager]，供 [com.videoworkshop.data.alliance.download.DownloadQueue] 使用。
 *
 * [com.videoworkshop.data.alliance.download.VideoDownloader] 与
 * [com.videoworkshop.data.alliance.download.DownloadQueue] 均通过 @Inject 构造函数自动注入，
 * 无需在此显式声明。
 */
@Module
@InstallIn(SingletonComponent::class)
object AllianceModule {

    // ===== 资质占位凭证（资质申请后替换为 BuildConfig / 运行时配置注入）=====
    private const val TAOBAO_APP_KEY = ""
    private const val TAOBAO_APP_SECRET = ""
    private const val JD_APP_KEY = ""
    private const val JD_APP_SECRET = ""
    private const val PDD_CLIENT_ID = ""
    private const val PDD_CLIENT_SECRET = ""

    // ===== 各联盟网关基地址 =====
    private const val TAOBAO_BASE_URL = "https://eco.taobao.com/"
    private const val JD_BASE_URL = "https://api.jd.com/"
    private const val PDD_BASE_URL = "https://gw-api.pinduoduo.com/"

    // ----- Retrofit -----

    @Provides
    @Singleton
    @TaobaoRetrofit
    fun provideTaobaoRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(TAOBAO_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @JdRetrofit
    fun provideJdRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(JD_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @PddRetrofit
    fun providePddRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(PDD_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    // ----- API 接口 -----

    @Provides
    @Singleton
    fun provideTaobaoTbkApi(@TaobaoRetrofit retrofit: Retrofit): TaobaoTbkApi =
        retrofit.create(TaobaoTbkApi::class.java)

    @Provides
    @Singleton
    fun provideJdUnionApi(@JdRetrofit retrofit: Retrofit): JdUnionApi =
        retrofit.create(JdUnionApi::class.java)

    @Provides
    @Singleton
    fun providePddDdkApi(@PddRetrofit retrofit: Retrofit): PddDdkApi =
        retrofit.create(PddDdkApi::class.java)

    // ----- 平台客户端（凭证占位，资质申请后替换）-----

    @Provides
    @Singleton
    fun provideTaobaoTopClient(
        api: TaobaoTbkApi,
        dispatchers: DispatcherProvider,
    ): TaobaoTopClient = TaobaoTopClient(
        appKey = TAOBAO_APP_KEY,
        appSecret = TAOBAO_APP_SECRET,
        api = api,
        dispatchers = dispatchers,
    )

    @Provides
    @Singleton
    fun provideJdUnionClient(
        api: JdUnionApi,
        dispatchers: DispatcherProvider,
    ): JdUnionClient = JdUnionClient(
        appKey = JD_APP_KEY,
        appSecret = JD_APP_SECRET,
        api = api,
        dispatchers = dispatchers,
    )

    @Provides
    @Singleton
    fun providePddDdkClient(
        api: PddDdkApi,
        dispatchers: DispatcherProvider,
    ): PddDdkClient = PddDdkClient(
        clientId = PDD_CLIENT_ID,
        clientSecret = PDD_CLIENT_SECRET,
        api = api,
        dispatchers = dispatchers,
    )

    // ----- 仓库绑定 -----

    @Provides
    @Singleton
    fun provideGoodsRepository(impl: AllianceRepositoryImpl): GoodsRepository = impl

    // ----- WorkManager（下载队列依赖）-----

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}

/** 标识淘宝 TOP 网关的 Retrofit 实例。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TaobaoRetrofit

/** 标识京东联盟网关的 Retrofit 实例。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JdRetrofit

/** 标识拼多多 DDK 网关的 Retrofit 实例。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PddRetrofit

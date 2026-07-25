package com.videoworkshop.data.publish.di

import com.videoworkshop.data.publish.ShareIntentPublisher
import com.videoworkshop.domain.repository.PublishRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 发布数据层 Hilt 模块。
 *
 * 将 [ShareIntentPublisher] 绑定到领域层 [PublishRepository] 接口，
 * 供上层（ViewModel / UseCase）通过依赖注入获取发布能力。
 *
 * [ShareIntentPublisher] 通过 @Inject 构造函数自动注入
 * [android.content.Context]、[com.videoworkshop.data.publish.ClipboardHelper]、
 * [com.videoworkshop.data.publish.FileProviderHelper] 与
 * [com.videoworkshop.core.common.DispatcherProvider]，
 * 无需在此显式声明 @Provides。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PublishModule {

    @Binds
    @Singleton
    abstract fun bindPublishRepository(impl: ShareIntentPublisher): PublishRepository
}

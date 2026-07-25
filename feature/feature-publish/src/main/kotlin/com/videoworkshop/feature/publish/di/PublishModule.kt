package com.videoworkshop.feature.publish.di

import com.videoworkshop.domain.repository.PublishRepository
import com.videoworkshop.domain.usecase.PublishContentUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * 发布流程 Hilt 模块。
 *
 * 领域层用例 [PublishContentUseCase] 为无注解的纯构造类，
 * 这里在 ViewModel 作用域内提供其实例，依赖由数据层绑定的
 * [PublishRepository] 注入。
 */
@Module
@InstallIn(ViewModelComponent::class)
object PublishModule {

    @Provides
    @ViewModelScoped
    fun providePublishContentUseCase(
        publishRepository: PublishRepository
    ): PublishContentUseCase = PublishContentUseCase(publishRepository)
}

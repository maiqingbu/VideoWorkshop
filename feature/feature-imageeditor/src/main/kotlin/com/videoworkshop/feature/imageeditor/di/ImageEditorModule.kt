package com.videoworkshop.feature.imageeditor.di

import com.videoworkshop.domain.repository.AiRepository
import com.videoworkshop.domain.usecase.GenerateImageCopyUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * 图文编辑器 Hilt 模块。
 *
 * 领域层用例 [GenerateImageCopyUseCase] 为无注解的纯构造类，
 * 这里在 ViewModel 作用域内提供其实例，依赖由数据层绑定的
 * [AiRepository] 注入。
 */
@Module
@InstallIn(ViewModelComponent::class)
object ImageEditorModule {

    @Provides
    @ViewModelScoped
    fun provideGenerateImageCopyUseCase(
        aiRepository: AiRepository
    ): GenerateImageCopyUseCase = GenerateImageCopyUseCase(aiRepository)
}

package com.videoworkshop.feature.goods.di

import com.videoworkshop.domain.repository.GoodsRepository
import com.videoworkshop.domain.usecase.SearchGoodsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * 商品搜索 Feature 的 Hilt 模块。
 *
 * [SearchGoodsUseCase] 是 domain 层普通类（无 @Inject 构造），
 * 在此通过 [GoodsRepository]（由 data-alliance 在 SingletonComponent 提供）构造并注入到 ViewModel。
 */
@Module
@InstallIn(ViewModelComponent::class)
object GoodsUseCaseModule {

    @Provides
    fun provideSearchGoodsUseCase(
        goodsRepository: GoodsRepository
    ): SearchGoodsUseCase = SearchGoodsUseCase(goodsRepository)
}

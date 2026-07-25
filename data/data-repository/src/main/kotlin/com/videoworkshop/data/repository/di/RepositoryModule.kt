package com.videoworkshop.data.repository.di

import com.videoworkshop.data.repository.DedupRepositoryImpl
import com.videoworkshop.data.repository.DraftRepositoryImpl
import com.videoworkshop.data.repository.MaterialRepositoryImpl
import com.videoworkshop.domain.repository.DedupRepository
import com.videoworkshop.domain.repository.DraftRepository
import com.videoworkshop.domain.repository.MaterialRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据仓库层 Hilt 模块。
 *
 * 将三个仓库实现绑定到对应的领域层接口：
 * - [DedupRepository]    -> [DedupRepositoryImpl]
 * - [MaterialRepository] -> [MaterialRepositoryImpl]
 * - [DraftRepository]    -> [DraftRepositoryImpl]
 *
 * 各实现类通过 @Inject 构造函数自动注入所需的 DAO、
 * [com.videoworkshop.core.common.DispatcherProvider] 等依赖，
 * 无需在此显式声明 @Provides。
 *
 * [MaterialDao] 与 [DraftDao] 由
 * [com.videoworkshop.core.database.DatabaseModule] 提供；
 * [DispatcherProvider] 由
 * [com.videoworkshop.core.network.NetworkModule] 提供。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDedupRepository(impl: DedupRepositoryImpl): DedupRepository

    @Binds
    @Singleton
    abstract fun bindMaterialRepository(impl: MaterialRepositoryImpl): MaterialRepository

    @Binds
    @Singleton
    abstract fun bindDraftRepository(impl: DraftRepositoryImpl): DraftRepository
}

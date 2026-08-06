package com.videoworkshop.data.project

import com.videoworkshop.domain.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 项目数据模块 Hilt 绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProjectDataModule {

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository
}
package com.videoworkshop.core.database

import android.content.Context
import androidx.room.Room
import com.videoworkshop.core.common.AppConstants
import com.videoworkshop.core.database.dao.DraftDao
import com.videoworkshop.core.database.dao.GoodsDao
import com.videoworkshop.core.database.dao.GoodsSnapshotDao
import com.videoworkshop.core.database.dao.MaterialDao
import com.videoworkshop.core.database.dao.ProjectDao
import com.videoworkshop.core.database.dao.TaskDao
import com.videoworkshop.core.database.migration.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库层 Hilt 模块：提供 [VideoWorkshopDb] 单例与各 DAO。
 *
 * 数据库升级通过显式 Migration 完成，不使用 destructive migration。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VideoWorkshopDb =
        Room.databaseBuilder(
            context,
            VideoWorkshopDb::class.java,
            AppConstants.DATABASE_NAME
        )
            .addMigrations(MIGRATION_2_3)
            .build()

    @Provides
    fun provideGoodsDao(db: VideoWorkshopDb): GoodsDao = db.goodsDao()

    @Provides
    fun provideMaterialDao(db: VideoWorkshopDb): MaterialDao = db.materialDao()

    @Provides
    fun provideTaskDao(db: VideoWorkshopDb): TaskDao = db.taskDao()

    @Provides
    fun provideDraftDao(db: VideoWorkshopDb): DraftDao = db.draftDao()

    @Provides
    fun provideProjectDao(db: VideoWorkshopDb): ProjectDao = db.projectDao()

    @Provides
    fun provideGoodsSnapshotDao(db: VideoWorkshopDb): GoodsSnapshotDao = db.goodsSnapshotDao()
}
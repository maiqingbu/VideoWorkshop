package com.videoworkshop.core.database

import android.content.Context
import androidx.room.Room
import com.videoworkshop.core.common.AppConstants
import com.videoworkshop.core.database.dao.DraftDao
import com.videoworkshop.core.database.dao.GoodsDao
import com.videoworkshop.core.database.dao.MaterialDao
import com.videoworkshop.core.database.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库层 Hilt 模块：提供 [VideoWorkshopDb] 单例与各 DAO。
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
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideGoodsDao(db: VideoWorkshopDb): GoodsDao = db.goodsDao()

    @Provides
    fun provideMaterialDao(db: VideoWorkshopDb): MaterialDao = db.materialDao()

    @Provides
    fun provideTaskDao(db: VideoWorkshopDb): TaskDao = db.taskDao()

    @Provides
    fun provideDraftDao(db: VideoWorkshopDb): DraftDao = db.draftDao()
}

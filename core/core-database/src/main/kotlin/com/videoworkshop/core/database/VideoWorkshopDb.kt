package com.videoworkshop.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.videoworkshop.core.database.converter.Converters
import com.videoworkshop.core.database.dao.DraftDao
import com.videoworkshop.core.database.dao.GoodsDao
import com.videoworkshop.core.database.dao.MaterialDao
import com.videoworkshop.core.database.dao.TaskDao
import com.videoworkshop.core.database.entity.DraftEntity
import com.videoworkshop.core.database.entity.GoodsEntity
import com.videoworkshop.core.database.entity.MaterialEntity
import com.videoworkshop.core.database.entity.TaskEntity

/**
 * VideoWorkshop 本地数据库。
 *
 * 包含 4 张表：商品、素材、任务、草稿。
 *
 * version 2: 新增 [MaterialEntity.note] 字段；tags 字段早前已存在。
 * 数据库模块已配置 fallbackToDestructiveMigration，开发阶段允许丢弃重建。
 */
@Database(
    entities = [
        GoodsEntity::class,
        MaterialEntity::class,
        TaskEntity::class,
        DraftEntity::class,
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VideoWorkshopDb : RoomDatabase() {

    abstract fun goodsDao(): GoodsDao
    abstract fun materialDao(): MaterialDao
    abstract fun taskDao(): TaskDao
    abstract fun draftDao(): DraftDao
}

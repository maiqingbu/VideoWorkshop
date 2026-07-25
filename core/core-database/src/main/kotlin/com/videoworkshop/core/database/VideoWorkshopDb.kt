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
 */
@Database(
    entities = [
        GoodsEntity::class,
        MaterialEntity::class,
        TaskEntity::class,
        DraftEntity::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VideoWorkshopDb : RoomDatabase() {

    abstract fun goodsDao(): GoodsDao
    abstract fun materialDao(): MaterialDao
    abstract fun taskDao(): TaskDao
    abstract fun draftDao(): DraftDao
}

package com.videoworkshop.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.videoworkshop.core.database.converter.Converters
import com.videoworkshop.core.database.dao.DraftDao
import com.videoworkshop.core.database.dao.GoodsDao
import com.videoworkshop.core.database.dao.GoodsSnapshotDao
import com.videoworkshop.core.database.dao.MaterialDao
import com.videoworkshop.core.database.dao.ProjectDao
import com.videoworkshop.core.database.dao.TaskDao
import com.videoworkshop.core.database.entity.DraftEntity
import com.videoworkshop.core.database.entity.GoodsEntity
import com.videoworkshop.core.database.entity.GoodsSnapshotEntity
import com.videoworkshop.core.database.entity.MaterialEntity
import com.videoworkshop.core.database.entity.ProjectEntity
import com.videoworkshop.core.database.entity.TaskEntity

/**
 * VideoWorkshop 本地数据库。
 *
 * version 2: 新增 [MaterialEntity.note] 字段；tags 字段早前已存在。
 * version 3: P1-01 新增 [ProjectEntity] 与 [GoodsSnapshotEntity] 表，建立项目制数据模型。
 * 启用 Schema 导出，数据库升级必须提供显式 Migration，禁止 destructive migration。
 */
@Database(
    entities = [
        GoodsEntity::class,
        MaterialEntity::class,
        TaskEntity::class,
        DraftEntity::class,
        ProjectEntity::class,
        GoodsSnapshotEntity::class,
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VideoWorkshopDb : RoomDatabase() {

    abstract fun goodsDao(): GoodsDao
    abstract fun materialDao(): MaterialDao
    abstract fun taskDao(): TaskDao
    abstract fun draftDao(): DraftDao
    abstract fun projectDao(): ProjectDao
    abstract fun goodsSnapshotDao(): GoodsSnapshotDao
}
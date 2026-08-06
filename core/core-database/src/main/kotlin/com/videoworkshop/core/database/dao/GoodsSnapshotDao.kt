package com.videoworkshop.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.videoworkshop.core.database.entity.GoodsSnapshotEntity
import kotlinx.coroutines.flow.Flow

/**
 * 商品快照数据访问对象。
 */
@Dao
interface GoodsSnapshotDao {

    @Query("SELECT * FROM goods_snapshots WHERE projectId = :projectId")
    fun observeByProject(projectId: String): Flow<List<GoodsSnapshotEntity>>

    @Query("SELECT * FROM goods_snapshots WHERE id = :id")
    suspend fun getById(id: String): GoodsSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GoodsSnapshotEntity)

    @Query("DELETE FROM goods_snapshots WHERE id = :id")
    suspend fun deleteById(id: String)
}
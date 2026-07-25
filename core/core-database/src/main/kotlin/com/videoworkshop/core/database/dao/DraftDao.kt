package com.videoworkshop.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.videoworkshop.core.database.entity.DraftEntity
import kotlinx.coroutines.flow.Flow

/**
 * 草稿数据访问对象。
 */
@Dao
interface DraftDao {

    @Query("SELECT * FROM drafts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DraftEntity>>

    @Query("SELECT * FROM drafts WHERE type = :type ORDER BY updatedAt DESC")
    fun observeByType(type: String): Flow<List<DraftEntity>>

    @Query("SELECT * FROM drafts WHERE goodsId = :goodsId ORDER BY updatedAt DESC")
    fun observeByGoods(goodsId: String): Flow<List<DraftEntity>>

    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun getById(id: String): DraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DraftEntity)

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM drafts")
    suspend fun clear()
}

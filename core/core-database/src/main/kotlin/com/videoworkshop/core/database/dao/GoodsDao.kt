package com.videoworkshop.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.videoworkshop.core.database.entity.GoodsEntity
import kotlinx.coroutines.flow.Flow

/**
 * 商品数据访问对象。
 */
@Dao
interface GoodsDao {

    @Query("SELECT * FROM goods ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GoodsEntity>>

    @Query("SELECT * FROM goods WHERE source = :source ORDER BY createdAt DESC")
    fun observeBySource(source: String): Flow<List<GoodsEntity>>

    @Query("SELECT * FROM goods WHERE id = :id")
    suspend fun getById(id: String): GoodsEntity?

    @Query("SELECT * FROM goods WHERE source = :source ORDER BY createdAt DESC")
    suspend fun getBySource(source: String): List<GoodsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GoodsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<GoodsEntity>)

    @Delete
    suspend fun delete(entity: GoodsEntity)

    @Query("DELETE FROM goods WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM goods")
    suspend fun clear()
}

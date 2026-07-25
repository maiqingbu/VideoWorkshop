package com.videoworkshop.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.videoworkshop.core.database.entity.MaterialEntity
import kotlinx.coroutines.flow.Flow

/**
 * 素材数据访问对象。
 */
@Dao
interface MaterialDao {

    @Query("SELECT * FROM materials ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE type = :type ORDER BY createdAt DESC")
    fun observeByType(type: String): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE id = :id")
    suspend fun getById(id: String): MaterialEntity?

    @Query("SELECT * FROM materials WHERE localPath = :path LIMIT 1")
    suspend fun getByPath(path: String): MaterialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MaterialEntity>)

    @Query("DELETE FROM materials WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM materials")
    suspend fun clear()
}

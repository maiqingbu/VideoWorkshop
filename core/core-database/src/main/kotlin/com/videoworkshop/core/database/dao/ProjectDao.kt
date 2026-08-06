package com.videoworkshop.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.videoworkshop.core.database.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * 项目数据访问对象。
 */
@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeById(id: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProjectEntity)

    @Update
    suspend fun update(entity: ProjectEntity)

    @Query("UPDATE projects SET status = 'ARCHIVED', updatedAt = :timestamp WHERE id = :id")
    suspend fun archiveById(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE projects SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun touchLastOpened(id: String, timestamp: Long)
}
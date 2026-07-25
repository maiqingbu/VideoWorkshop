package com.videoworkshop.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.videoworkshop.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * 处理任务数据访问对象。
 */
@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TaskEntity)

    /**
     * 局部更新任务状态与进度，避免整行覆写。
     */
    @Query(
        "UPDATE tasks SET status = :status, progress = :progress, outputPath = :outputPath, updatedAt = :updatedAt " +
            "WHERE id = :id"
    )
    suspend fun updateProgress(
        id: String,
        status: String,
        progress: Int,
        outputPath: String?,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tasks")
    suspend fun clear()
}

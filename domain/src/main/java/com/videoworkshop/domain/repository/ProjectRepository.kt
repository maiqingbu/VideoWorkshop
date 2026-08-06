package com.videoworkshop.domain.repository

import com.videoworkshop.domain.model.Project
import com.videoworkshop.domain.model.ProjectStatus
import kotlinx.coroutines.flow.Flow

/**
 * 创作项目仓库。
 *
 * 页面只依赖本接口，不直接访问数据库。
 */
interface ProjectRepository {

    /**
     * 创建项目，返回项目 ID。
     */
    suspend fun createProject(project: Project): String

    /**
     * 观察最近项目，按更新时间倒序，最多 [limit] 条。
     */
    fun observeRecentProjects(limit: Int): Flow<List<Project>>

    /**
     * 观察全部项目，按更新时间倒序。
     */
    fun observeAllProjects(): Flow<List<Project>>

    /**
     * 获取单个项目（一次性）。
     */
    suspend fun getProject(id: String): Project?

    /**
     * 观察单个项目。
     */
    fun observeProject(id: String): Flow<Project?>

    /**
     * 更新项目（标题、状态、封面等）。
     */
    suspend fun updateProject(project: Project)

    /**
     * 归档项目（软删除，状态改为 [ProjectStatus.ARCHIVED]）。
     */
    suspend fun archiveProject(id: String)

    /**
     * 物理删除项目（含确认删除场景）。
     */
    suspend fun deleteProject(id: String)

    /**
     * 更新项目最近打开时间。
     */
    suspend fun touchLastOpened(id: String, timestamp: Long = System.currentTimeMillis())
}
package com.videoworkshop.data.project

import com.videoworkshop.core.database.dao.ProjectDao
import com.videoworkshop.core.database.entity.ProjectEntity
import com.videoworkshop.domain.model.AllianceProvider
import com.videoworkshop.domain.model.Project
import com.videoworkshop.domain.model.ProjectStatus
import com.videoworkshop.domain.model.ProjectType
import com.videoworkshop.domain.model.PublishTarget
import com.videoworkshop.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [ProjectRepository] 实现：基于 Room [ProjectDao] 的项目读写。
 *
 * 在 Repository 内完成 [ProjectEntity] 与领域 [Project] 的映射，
 * 领域层不依赖数据库实体。
 */
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao
) : ProjectRepository {

    override suspend fun createProject(project: Project): String {
        projectDao.upsert(project.toEntity())
        return project.id
    }

    override fun observeRecentProjects(limit: Int): Flow<List<Project>> =
        projectDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun observeAllProjects(): Flow<List<Project>> =
        projectDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getProject(id: String): Project? =
        projectDao.getById(id)?.toDomain()

    override fun observeProject(id: String): Flow<Project?> =
        projectDao.observeById(id).map { it?.toDomain() }

    override suspend fun updateProject(project: Project) {
        projectDao.update(project.toEntity())
    }

    override suspend fun archiveProject(id: String) {
        projectDao.archiveById(id)
    }

    override suspend fun deleteProject(id: String) {
        projectDao.deleteById(id)
    }

    override suspend fun touchLastOpened(id: String, timestamp: Long) {
        projectDao.touchLastOpened(id, timestamp)
    }

    // ===== 映射 =====

    private fun Project.toEntity(): ProjectEntity = ProjectEntity(
        id = id,
        title = title,
        type = type.name,
        status = status.name,
        goodsSnapshotId = goodsSnapshotId,
        targetPlatforms = targetPlatforms.map { it.name }.toSet(),
        coverAssetId = coverAssetId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt
    )

    private fun ProjectEntity.toDomain(): Project = Project(
        id = id,
        title = title,
        type = type.toType(),
        status = status.toStatus(),
        goodsSnapshotId = goodsSnapshotId,
        targetPlatforms = targetPlatforms.mapNotNull { it.toPublishTarget() }.toSet(),
        coverAssetId = coverAssetId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt
    )

    private fun String.toType(): ProjectType =
        runCatching { ProjectType.valueOf(this) }.getOrDefault(ProjectType.VIDEO_COMMERCE)

    private fun String.toStatus(): ProjectStatus =
        runCatching { ProjectStatus.valueOf(this) }.getOrDefault(ProjectStatus.DRAFT)

    private fun String.toPublishTarget(): PublishTarget? =
        runCatching { PublishTarget.valueOf(this) }.getOrNull()
}

/** [AllianceProvider] 字符串反序列化辅助（供后续商品快照映射复用）。 */
internal fun String.toAllianceProvider(): AllianceProvider =
    runCatching { AllianceProvider.valueOf(this) }.getOrDefault(AllianceProvider.TAOBAO)
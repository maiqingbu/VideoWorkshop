package com.videoworkshop.domain.usecase.project

import com.videoworkshop.domain.repository.ProjectRepository
import javax.inject.Inject

/**
 * 归档项目用例。
 *
 * 软删除：将项目状态置为 [com.videoworkshop.domain.model.ProjectStatus.ARCHIVED]，
 * 不物理删除数据，保留可恢复能力。
 */
class ArchiveProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(id: String) {
        projectRepository.archiveProject(id)
    }
}

/**
 * 删除项目用例（确认删除场景下的物理删除）。
 */
class DeleteProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(id: String) {
        projectRepository.deleteProject(id)
    }
}
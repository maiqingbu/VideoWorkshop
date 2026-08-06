package com.videoworkshop.domain.usecase.project

import com.videoworkshop.domain.model.Project
import com.videoworkshop.domain.repository.ProjectRepository
import javax.inject.Inject

/**
 * 更新项目用例。
 *
 * 用于重命名、修改封面、更新状态等场景。
 * 自动更新 [Project.updatedAt] 时间戳。
 */
class UpdateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(project: Project) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        projectRepository.updateProject(updated)
    }
}
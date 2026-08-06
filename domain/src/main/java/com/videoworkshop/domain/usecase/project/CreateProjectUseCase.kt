package com.videoworkshop.domain.usecase.project

import com.videoworkshop.domain.model.Project
import com.videoworkshop.domain.model.ProjectStatus
import com.videoworkshop.domain.model.ProjectType
import com.videoworkshop.domain.repository.ProjectRepository
import java.util.UUID
import javax.inject.Inject

/**
 * 创建项目用例。
 *
 * 生成项目 ID（UUID 字符串），初始状态为 [ProjectStatus.DRAFT]。
 */
class CreateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(
        title: String,
        type: ProjectType
    ): String {
        val now = System.currentTimeMillis()
        val project = Project(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { type.defaultTitle() },
            type = type,
            status = ProjectStatus.DRAFT,
            createdAt = now,
            updatedAt = now,
            lastOpenedAt = now
        )
        return projectRepository.createProject(project)
    }

    private fun ProjectType.defaultTitle(): String = when (this) {
        ProjectType.VIDEO_COMMERCE -> "视频带货项目"
        ProjectType.IMAGE_COMMERCE -> "图文带货项目"
        ProjectType.AB_RECOMPOSE -> "AB 搬运项目"
        ProjectType.VIDEO_REWORK -> "视频二创项目"
        ProjectType.LONG_VIDEO_CLIP -> "长视频切片项目"
    }
}
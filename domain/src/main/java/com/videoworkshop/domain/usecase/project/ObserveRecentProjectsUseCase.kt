package com.videoworkshop.domain.usecase.project

import com.videoworkshop.domain.model.Project
import com.videoworkshop.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 观察最近项目用例。
 *
 * 首页「最近项目」与项目列表页使用。
 */
class ObserveRecentProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    operator fun invoke(limit: Int = DEFAULT_LIMIT): Flow<List<Project>> =
        projectRepository.observeRecentProjects(limit)

    private companion object {
        const val DEFAULT_LIMIT = 20
    }
}
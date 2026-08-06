package com.videoworkshop.domain.usecase.project

import com.videoworkshop.domain.model.Project
import com.videoworkshop.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取项目用例。
 *
 * 项目详情页通过路由中的 projectId 恢复状态。
 */
class GetProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    /** 一次性获取，可用于初始化或校验。 */
    suspend fun get(id: String): Project? = projectRepository.getProject(id)

    /** 观察项目，详情页实时订阅状态变化。 */
    fun observe(id: String): Flow<Project?> = projectRepository.observeProject(id)
}
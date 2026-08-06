package com.videoworkshop.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.domain.model.Project
import com.videoworkshop.domain.model.ProjectStatus
import com.videoworkshop.domain.usecase.project.ObserveRecentProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 项目列表 UI 状态。
 */
data class ProjectListUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * 项目列表 ViewModel。
 *
 * 订阅最近项目数据，并按 [ProjectFilter] 进行真实筛选。
 * 供项目列表页使用。
 */
@HiltViewModel
class ProjectListViewModel @Inject constructor(
    observeRecentProjectsUseCase: ObserveRecentProjectsUseCase
) : ViewModel() {

    private val _filter = MutableStateFlow(ProjectFilter.ALL)

    /** 当前筛选条件。 */
    val filter: StateFlow<ProjectFilter> = _filter.asStateFlow()

    val uiState: StateFlow<ProjectListUiState> =
        combine(observeRecentProjectsUseCase(limit = LIST_LIMIT), _filter) { projects, filter ->
            val filtered = when (filter) {
                ProjectFilter.ALL -> projects
                else -> projects.filter { it.status.matches(filter) }
            }
            ProjectListUiState(projects = filtered, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProjectListUiState(isLoading = true)
        )

    /** 切换筛选条件。 */
    fun setFilter(filter: ProjectFilter) {
        _filter.value = filter
    }

    private companion object {
        const val LIST_LIMIT = 100
    }
}

/** 项目状态是否匹配某个筛选条件。 */
private fun ProjectStatus.matches(filter: ProjectFilter): Boolean = when (filter) {
    ProjectFilter.ALL -> true
    ProjectFilter.PREPARING -> this == ProjectStatus.PREPARING || this == ProjectStatus.PROCESSING
    ProjectFilter.READY_TO_PUBLISH -> this == ProjectStatus.READY_TO_PUBLISH
    ProjectFilter.PUBLISHED -> this == ProjectStatus.PUBLISHED
    ProjectFilter.ARCHIVED -> this == ProjectStatus.ARCHIVED
}
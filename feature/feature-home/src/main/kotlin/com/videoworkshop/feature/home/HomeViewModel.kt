package com.videoworkshop.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.domain.model.ContentType
import com.videoworkshop.domain.model.Project
import com.videoworkshop.domain.usecase.project.ObserveRecentProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页 ViewModel。
 *
 * 持有当前选中的创作模式 [selectedMode] 与最近项目 [recentProjects]。
 * 选择模式后由路由侧观察 [uiState] 中的 [HomeUiState.selectedMode] 完成跳转，
 * 随后调用 [consumeSelectedMode] 清空，避免重复导航。
 *
 * 最近项目接 [ObserveRecentProjectsUseCase] 真实数据，首页横向列表展示最近 [RECENT_PROJECT_LIMIT] 条。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeRecentProjectsUseCase: ObserveRecentProjectsUseCase
) : ViewModel() {

    private val _selectedMode = MutableStateFlow<ContentType?>(null)
    private val _isLoading = MutableStateFlow(false)

    /** 当前选中的创作模式（视频 / 图文），默认未选中。 */
    val selectedMode: StateFlow<ContentType?> = _selectedMode.asStateFlow()

    /** 最近项目列表，最多展示 [RECENT_PROJECT_LIMIT] 条。 */
    val recentProjects: StateFlow<List<Project>> =
        observeRecentProjectsUseCase(limit = RECENT_PROJECT_LIMIT)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** 聚合后的首页 UI 状态，供 [HomeScreen] 单一订阅。 */
    val uiState: StateFlow<HomeUiState> =
        combine(_selectedMode, recentProjects, _isLoading) { mode, projects, loading ->
            HomeUiState(
                selectedMode = mode,
                recentProjects = projects,
                isLoading = loading
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    /** 选择「视频带货」模式。 */
    fun selectVideoMode() {
        _selectedMode.value = ContentType.VIDEO
    }

    /** 选择「图文带货」模式。 */
    fun selectImageMode() {
        _selectedMode.value = ContentType.IMAGE
    }

    /** 路由侧完成导航后调用，清空已选模式。 */
    fun consumeSelectedMode() {
        _selectedMode.value = null
    }

    /** 下拉刷新：触发最近项目重新订阅（Flow 自动刷新，此处仅同步加载态）。 */
    fun refresh() {
        _isLoading.value = true
        viewModelScope.launch {
            // 项目列表由 Flow 实时订阅，刷新仅维持短暂加载态避免闪烁。
            delay(600)
            _isLoading.value = false
        }
    }

    private companion object {
        /** 最近项目展示数量上限。 */
        const val RECENT_PROJECT_LIMIT = 5
    }
}
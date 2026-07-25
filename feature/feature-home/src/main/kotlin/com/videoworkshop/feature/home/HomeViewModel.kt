package com.videoworkshop.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.domain.model.ContentType
import com.videoworkshop.domain.model.Draft
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
 * 持有当前选中的创作模式 [selectedMode] 与最近草稿 [recentDrafts]。
 * 选择模式后由路由侧观察 [uiState] 中的 [HomeUiState.selectedMode] 完成跳转，
 * 随后调用 [consumeSelectedMode] 清空，避免重复导航。
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _selectedMode = MutableStateFlow<ContentType?>(null)
    private val _recentDrafts = MutableStateFlow<List<Draft>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    /** 当前选中的创作模式（视频 / 图文），默认未选中。 */
    val selectedMode: StateFlow<ContentType?> = _selectedMode.asStateFlow()

    /** 最近草稿列表，当前阶段先用空列表占位。 */
    val recentDrafts: StateFlow<List<Draft>> = _recentDrafts.asStateFlow()

    /** 聚合后的首页 UI 状态，供 [HomeScreen] 单一订阅。 */
    val uiState: StateFlow<HomeUiState> =
        combine(_selectedMode, _recentDrafts, _isLoading) { mode, drafts, loading ->
            HomeUiState(
                selectedMode = mode,
                recentDrafts = drafts,
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

    /** 下拉刷新：模拟短暂加载后恢复（草稿当前为空列表）。 */
    fun refresh() {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            delay(600)
            _isLoading.value = false
        }
    }
}

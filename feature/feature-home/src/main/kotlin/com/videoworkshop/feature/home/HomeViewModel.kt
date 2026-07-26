package com.videoworkshop.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.domain.model.ContentType
import com.videoworkshop.domain.model.Draft
import com.videoworkshop.domain.repository.DraftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 首页 ViewModel。
 *
 * 持有当前选中的创作模式 [selectedMode] 与最近草稿 [recentDrafts]。
 * 选择模式后由路由侧观察 [uiState] 中的 [HomeUiState.selectedMode] 完成跳转，
 * 随后调用 [consumeSelectedMode] 清空，避免重复导航。
 *
 * 最近草稿接 [DraftRepository] 真实数据，首页横向列表展示最近 [RECENT_DRAFT_LIMIT] 条。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val draftRepository: DraftRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _selectedMode = MutableStateFlow<ContentType?>(null)
    private val _recentDrafts = MutableStateFlow<List<Draft>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    /** 当前选中的创作模式（视频 / 图文），默认未选中。 */
    val selectedMode: StateFlow<ContentType?> = _selectedMode.asStateFlow()

    /** 最近草稿列表，最多展示 [RECENT_DRAFT_LIMIT] 条。 */
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

    init {
        refresh()
    }

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

    /**
     * 下拉刷新：重新加载草稿列表 + 模拟短暂加载状态。
     */
    fun refresh() {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) {
                    draftRepository.getDrafts()
                        .sortedByDescending { it.createdAt }
                        .take(RECENT_DRAFT_LIMIT)
                }
            }.onSuccess { drafts ->
                _recentDrafts.value = drafts
            }
            // 让 loading 至少展示 600ms，避免闪烁
            delay(600)
            _isLoading.value = false
        }
    }

    private companion object {
        /** 最近草稿展示数量上限。 */
        const val RECENT_DRAFT_LIMIT = 5
    }
}

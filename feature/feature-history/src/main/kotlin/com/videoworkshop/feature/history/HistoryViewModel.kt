package com.videoworkshop.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.domain.model.Draft
import com.videoworkshop.domain.repository.DraftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 发布记录列表的 UI 状态。
 *
 * @param loading 是否正在加载
 * @param drafts   已发布草稿列表
 * @param error    错误信息（一次性）
 * @param expandedId 当前展开详情的草稿 ID，null 表示无展开
 */
data class HistoryUiState(
    val loading: Boolean = false,
    val drafts: List<Draft> = emptyList(),
    val error: String? = null,
    val expandedId: Long? = null
)

/**
 * 发布记录 ViewModel。
 *
 * 从 [DraftRepository] 读取已发布草稿列表，并提供刷新与展开详情能力。
 * 当前 Draft 模型暂未包含 status 字段，骨架实现中将仓库返回的全部草稿
 * 视为「已发布」记录展示，后续若扩展 Draft 模型可在此处增加过滤。
 *
 * @param draftRepository 草稿仓库
 * @param dispatchers     协程调度器
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val draftRepository: DraftRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(loading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /**
     * 重新加载已发布草稿列表。
     */
    fun refresh() {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) {
                    // TODO: Draft 模型当前无 status 字段，暂返回全部草稿作为「已发布」记录
                    draftRepository.getDrafts()
                }
            }.onSuccess { drafts ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    drafts = drafts,
                    error = null
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = it.message ?: "加载失败"
                )
            }
        }
    }

    /**
     * 切换某个草稿的详情展开状态。再次点击同一项会收起。
     */
    fun toggleExpanded(draftId: Long) {
        val current = _uiState.value.expandedId
        _uiState.value = _uiState.value.copy(
            expandedId = if (current == draftId) null else draftId
        )
    }

    /**
     * 清除一次性错误提示。
     */
    fun consumeError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

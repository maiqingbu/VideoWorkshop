package com.videoworkshop.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 发布记录入口 Composable。
 *
 * 收集 [HistoryViewModel] 的 UI 状态并交给 [HistoryScreen] 渲染，
 * 同时向上层暴露 [onBack] 与 [onRepublish] 回调。
 *
 * @param onBack      返回上一级
 * @param onRepublish 重新发布回调，参数为草稿 ID（字符串形式），由上层
 *                    跳转到 PublishRoute 并复用草稿内容
 * @param viewModel   通过 Hilt 注入的发布记录 ViewModel
 */
@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    onRepublish: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onItemClick = viewModel::toggleExpanded,
        onRepublish = { draftId -> onRepublish(draftId.toString()) },
        onErrorConsume = viewModel::consumeError
    )
}

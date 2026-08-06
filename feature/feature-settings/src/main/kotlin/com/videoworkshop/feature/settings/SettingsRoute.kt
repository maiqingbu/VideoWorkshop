package com.videoworkshop.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 设置页入口 Composable。
 *
 * 收集 [SettingsViewModel] 的 UI 状态并交给 [SettingsScreen] 渲染，
 * 同时桥接编辑 / 清理缓存等回调到 ViewModel。
 *
 * @param viewModel 通过 Hilt 注入的设置页 ViewModel
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        state = state,
        onStartEdit = viewModel::startEdit,
        onSaveEdit = viewModel::saveEdit,
        onCancelEdit = viewModel::cancelEdit,
        onClearCache = viewModel::clearCache,
        onThemeModeChange = viewModel::setThemeMode,
        onConsumeToast = viewModel::consumeToast
    )
}

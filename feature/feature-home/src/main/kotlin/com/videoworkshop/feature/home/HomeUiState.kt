package com.videoworkshop.feature.home

import com.videoworkshop.domain.model.ContentType
import com.videoworkshop.domain.model.Project

/**
 * 首页 UI 状态。
 *
 * @param selectedMode    当前选中的创作模式（视频 / 图文），用于触发路由跳转后清空。
 * @param recentProjects  最近项目列表，首页横向滚动展示。
 * @param isLoading       下拉刷新中标记，驱动 PullToRefresh 指示器。
 */
data class HomeUiState(
    val selectedMode: ContentType? = null,
    val recentProjects: List<Project> = emptyList(),
    val isLoading: Boolean = false
)
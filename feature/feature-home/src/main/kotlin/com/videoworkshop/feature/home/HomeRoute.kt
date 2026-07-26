package com.videoworkshop.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.videoworkshop.domain.model.ContentType

/**
 * 首页路由常量。
 */
object HomeRoute {
    const val route = "home"
}

/**
 * 将首页接入导航图。
 *
 * 点击「视频带货」跳转 `goods/video`，点击「图文带货」跳转 `goods/image`，
 * 点击「AB 搬运」卡片跳转 `abtransport` 路由，
 * 点击快捷入口「发布记录」跳转 `history` 路由。
 *
 * 通过观察 [HomeUiState.selectedMode] 触发一次性导航，跳转后立即清空，避免回退时重复触发。
 */
fun NavGraphBuilder.homeNavGraph(navController: NavController) {
    composable(HomeRoute.route) {
        val viewModel: HomeViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(uiState.selectedMode) {
            uiState.selectedMode?.let { mode ->
                val target = when (mode) {
                    ContentType.VIDEO -> "goods/video"
                    ContentType.IMAGE -> "goods/image"
                }
                navController.navigate(target)
                viewModel.consumeSelectedMode()
            }
        }

        HomeScreen(
            uiState = uiState,
            onVideoMode = viewModel::selectVideoMode,
            onImageMode = viewModel::selectImageMode,
            onABTransport = {
                navController.navigate("abtransport?videoA=")
            },
            onRefresh = viewModel::refresh,
            onQuickAction = { action ->
                when (action) {
                    QuickAction.MATERIAL -> navController.navigate("material")
                    QuickAction.PUBLISH_RECORDS -> navController.navigate("history")
                }
            }
        )
    }
}

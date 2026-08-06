package com.videoworkshop.feature.home

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * 工作台路由常量。
 */
object HomeRoute {
    const val route = "workbench"
}

/**
 * 将首页接入导航图。
 *
 * 「新建项目」打开项目列表页并自动弹出新建弹层；最近项目点击进入项目详情；
 * 常用工具按各自目标跳转（音画重组→AB 搬运、素材处理→素材 Tab、图文制作→图文带货、商品库→商品库）。
 */
fun NavGraphBuilder.homeNavGraph(navController: NavController) {
    composable(HomeRoute.route) {
        val viewModel: HomeViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        HomeScreen(
            uiState = uiState,
            onRefresh = viewModel::refresh,
            onQuickAction = { action ->
                when (action) {
                    QuickAction.MATERIAL -> navController.navigate("material")
                    QuickAction.PROJECTS -> navController.navigate("project")
                }
            },
            onCreateProject = {
                // 打开项目列表页并自动弹出新建弹层（统一新建入口，不再走旧红渐变页）
                navController.navigate("project?openCreate=true")
            },
            onProjectClick = { projectId ->
                navController.navigate("project/${Uri.encode(projectId)}")
            },
            onToolSelected = { tool ->
                when (tool) {
                    WorkbenchTool.AB_RECOMPOSE -> navController.navigate("abtransport?videoA=")
                    WorkbenchTool.MATERIAL_PROCESS -> navController.navigate("material")
                    WorkbenchTool.IMAGE_MAKE -> navController.navigate("goods/image")
                    WorkbenchTool.GOODS_LIBRARY -> navController.navigate("goods-library")
                }
            }
        )
    }
}

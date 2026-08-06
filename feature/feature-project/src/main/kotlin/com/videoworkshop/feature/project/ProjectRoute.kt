package com.videoworkshop.feature.project

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * 项目模块路由常量。
 */
object ProjectRoute {
    /** 项目列表（底部导航「项目」Tab 入口） */
    const val PROJECT_LIST = "project"

    /** 项目详情 */
    const val PROJECT_DETAIL = "project/{projectId}"

    /** 项目列表（带 openCreate 参数，进入后自动弹出新建弹层） */
    fun list(openCreate: Boolean = false): String =
        if (openCreate) "$PROJECT_LIST?openCreate=true" else PROJECT_LIST

    fun detail(projectId: String): String =
        "project/${Uri.encode(projectId)}"
}

/**
 * 项目模块导航图。
 *
 * @param onBack 返回上级
 */
fun NavGraphBuilder.projectNavGraph(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    // 项目列表（顶层 Tab）
    composable(
        route = "${ProjectRoute.PROJECT_LIST}?openCreate={openCreate}",
        arguments = listOf(
            navArgument("openCreate") {
                type = NavType.BoolType
                defaultValue = false
            }
        )
    ) { entry ->
        val viewModel: ProjectListViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val filter by viewModel.filter.collectAsStateWithLifecycle()

        val createViewModel: ProjectCreateViewModel = hiltViewModel()
        val createUiState by createViewModel.uiState.collectAsStateWithLifecycle()

        // 创建成功后跳转详情
        LaunchedEffect(createUiState.createdProjectId) {
            createUiState.createdProjectId?.let { projectId ->
                navController.navigate(ProjectRoute.detail(projectId))
                createViewModel.consumeCreated()
            }
        }

        ProjectListScreen(
            uiState = uiState,
            selectedFilter = filter,
            onFilterChange = viewModel::setFilter,
            onProjectClick = { projectId ->
                navController.navigate(ProjectRoute.detail(projectId))
            },
            onBack = onBack,
            initialOpenCreate = entry.arguments?.getBoolean("openCreate") ?: false,
            createUiState = createUiState,
            onTitleChange = createViewModel::onTitleChange,
            onCreate = createViewModel::create
        )
    }

    // 项目详情
    composable(
        route = ProjectRoute.PROJECT_DETAIL,
        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
    ) { entry ->
        val projectId = entry.arguments?.getString("projectId").orEmpty()

        val viewModel: ProjectDetailViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(projectId) {
            viewModel.load(projectId)
        }

        ProjectDetailScreen(
            uiState = uiState,
            onBack = onBack,
            onRename = viewModel::renameProject,
            onArchive = viewModel::archiveProject,
            onDelete = viewModel::deleteProject
        )
    }
}
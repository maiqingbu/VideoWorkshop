package com.videoworkshop.feature.history

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

/**
 * 发布记录模块路由常量与导航图注册。
 *
 * 路由形如 `history`，无参数。集成到 AppNavHost 时调用 [historyNavGraph]，
 * 由上层传入 [onBack] 与 [onRepublish]（参数为草稿 ID 字符串）。
 */
object HistoryRoutes {
    /** 发布记录列表路由。 */
    const val HISTORY = "history"

    /** 便于外部按需构建路由字符串。 */
    fun historyRoute(): String = HISTORY
}

/**
 * 在导航图中注册发布记录页面。
 *
 * @param onBack       返回上一级
 * @param onRepublish  重新发布回调，参数为草稿 ID 字符串（用于跳转 PublishRoute）
 */
fun NavGraphBuilder.historyNavGraph(
    navController: NavHostController,
    onBack: () -> Unit,
    onRepublish: (String) -> Unit
) {
    composable(route = HistoryRoutes.HISTORY) {
        HistoryRoute(
            onBack = onBack,
            onRepublish = onRepublish
        )
    }
}

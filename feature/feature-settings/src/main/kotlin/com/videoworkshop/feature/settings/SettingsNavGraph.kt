package com.videoworkshop.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

/**
 * 设置模块路由常量与导航图注册。
 *
 * 路由形如 `settings`，无参数。集成到 AppNavHost 时调用 [settingsNavGraph]。
 */
object SettingsRoutes {
    /** 设置页路由。 */
    const val SETTINGS = "settings"

    /** 便于外部按需构建路由字符串。 */
    fun settingsRoute(): String = SETTINGS
}

/**
 * 在导航图中注册设置页（「我的」Tab 入口）。
 *
 * @param navController 导航控制器
 */
fun NavGraphBuilder.settingsNavGraph(navController: NavHostController) {
    composable(route = SettingsRoutes.SETTINGS) {
        SettingsRoute()
    }
}

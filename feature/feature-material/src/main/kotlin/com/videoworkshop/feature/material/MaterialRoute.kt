package com.videoworkshop.feature.material

import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * 素材库路由定义。
 */
object MaterialRoute {
    const val route = "material"
}

/**
 * 注册素材库导航图。
 *
 * 素材卡片操作菜单中的「去重 / AB 搬运 / 制作带货视频」分别通过
 * [onDedup]、[onABTransport]、[onEnhance] 回调跳出当前页面，
 * 由 [com.videoworkshop.app.nav.AppNavHost] 提供具体目标路由。
 *
 * @param navController 导航控制器
 * @param onDedup       接收素材本地路径，跳去重页
 * @param onABTransport 接收素材本地路径，跳 AB 搬运页
 * @param onEnhance     接收素材本地路径，跳带货包装页
 */
fun NavGraphBuilder.materialNavGraph(
    navController: NavHostController,
    onDedup: (String) -> Unit = {},
    onABTransport: (String) -> Unit = {},
    onEnhance: (String) -> Unit = {}
) {
    composable(route = MaterialRoute.route) {
        MaterialScreen(
            onBack = { navController.popBackStack() },
            onDedup = onDedup,
            onABTransport = onABTransport,
            onEnhance = onEnhance
        )
    }
}

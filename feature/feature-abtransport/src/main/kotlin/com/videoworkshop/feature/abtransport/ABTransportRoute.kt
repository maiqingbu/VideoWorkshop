package com.videoworkshop.feature.abtransport

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * AB 搬运页面路由定义。
 *
 * 路由形态：
 * - `abtransport`：无预填参数，A/B 均未选择
 * - `abtransport?videoA={path}`：从素材库「AB 搬运」菜单进入时预填 A 视频
 *
 * 路径参数 [videoA] 为可选参数，导航时需做 URI 编解码。
 */
object ABTransportRoute {
    /** 路由模板（含可选 query 参数）。 */
    const val route = "abtransport?videoA={videoA}"

    /** 构造无预填路由。 */
    fun createRoute(): String = "abtransport?videoA="

    /**
     * 构造预填 A 视频的路由字符串，自动对路径做 URI 编码。
     */
    fun createRoute(videoAPath: String): String =
        "abtransport?videoA=${Uri.encode(videoAPath)}"
}

/**
 * 注册 AB 搬运页面导航图。
 *
 * 合成完成后的结果页提供「去重」「AI 包装」两个跳转入口：
 * - [onDedup]：跳去重页，参数为合成产物路径
 * - [onEnhance]：跳带货包装页（AI 包装），参数为合成产物路径
 *
 * 产物自动保存到素材库由 ViewModel 内部完成，无需 UI 入口。
 *
 * @param navController 导航控制器
 * @param onDedup       接收产物路径，跳去重页
 * @param onEnhance     接收产物路径，跳 AI 包装页
 */
fun NavGraphBuilder.abTransportNavGraph(
    navController: NavHostController,
    onDedup: (String) -> Unit = {},
    onEnhance: (String) -> Unit = {}
) {
    composable(
        route = ABTransportRoute.route,
        arguments = listOf(
            navArgument("videoA") {
                type = NavType.StringType
                defaultValue = ""
            }
        )
    ) {
        ABTransportScreen(
            onBack = { navController.popBackStack() },
            onDedup = onDedup,
            onEnhance = onEnhance
        )
    }
}

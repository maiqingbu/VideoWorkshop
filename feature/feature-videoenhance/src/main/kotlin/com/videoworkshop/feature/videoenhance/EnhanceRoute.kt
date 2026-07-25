package com.videoworkshop.feature.videoenhance

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * 带货包装页面路由定义。
 *
 * 路径参数：
 * - [videoPath] 已去重的视频文件路径
 * - [goodsId]   关联商品 ID（无商品上下文时为占位值 "none"）
 */
object EnhanceRoute {
    /** 路由模板。 */
    const val route = "enhance/{videoPath}/{goodsId}"

    /**
     * 构造带货包装页路由字符串，自动做 URI 编码。
     */
    fun createRoute(videoPath: String, goodsId: String): String =
        "enhance/${Uri.encode(videoPath)}/${Uri.encode(goodsId)}"
}

/**
 * 注册带货包装页面导航图。
 *
 * 合成完成后自动导航至发布页（publish），类型为 video。
 */
fun NavGraphBuilder.videoEnhanceNavGraph(navController: NavHostController) {
    composable(
        route = EnhanceRoute.route,
        arguments = listOf(
            navArgument("videoPath") { type = NavType.StringType },
            navArgument("goodsId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val goodsId = Uri.decode(backStackEntry.arguments?.getString("goodsId").orEmpty())
        EnhanceScreen(
            onBack = { navController.popBackStack() },
            onCompleted = { outputPath ->
                val encodedFile = Uri.encode(outputPath)
                val encodedGoods = Uri.encode(goodsId)
                navController.navigate("publish/video/$encodedFile/$encodedGoods")
            }
        )
    }
}

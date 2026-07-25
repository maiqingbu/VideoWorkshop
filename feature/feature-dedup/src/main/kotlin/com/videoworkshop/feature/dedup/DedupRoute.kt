package com.videoworkshop.feature.dedup

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * 去重页面路由定义。
 *
 * 路径参数 [videoPath] 为待去重的视频文件路径，导航时需做 URI 编解码。
 */
object DedupRoute {
    /** 路由模板。 */
    const val route = "dedup/{videoPath}"

    /**
     * 构造去重页路由字符串，自动对路径做 URI 编码。
     */
    fun createRoute(videoPath: String): String = "dedup/${Uri.encode(videoPath)}"
}

/**
 * 注册去重页面导航图。
 *
 * 去重完成后，"下一步"将跳转至带货包装页（enhance），
 * 由于去重阶段不持有商品上下文，goodsId 以占位值 "none" 传递，
 * 由带货包装页降级为通用包装流程。
 */
fun NavGraphBuilder.dedupNavGraph(navController: NavHostController) {
    composable(
        route = DedupRoute.route,
        arguments = listOf(navArgument("videoPath") { type = NavType.StringType })
    ) {
        DedupScreen(
            onBack = { navController.popBackStack() },
            onNext = { outputPath ->
                val encoded = Uri.encode(outputPath)
                navController.navigate("enhance/$encoded/none")
            }
        )
    }
}

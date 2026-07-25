package com.videoworkshop.feature.publish

import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * 发布流程路由定义。
 *
 * 路由形如 `publish/{type}/{filePath}/{goodsId}`：
 * - type     内容形式（video / image）
 * - filePath 待发布文件路径或合成内容标识
 * - goodsId  关联商品 ID
 */
object PublishRoute {
    const val route = "publish/{type}/{filePath}/{goodsId}"

    const val ARG_TYPE = "type"
    const val ARG_FILE_PATH = "filePath"
    const val ARG_GOODS_ID = "goodsId"

    fun buildRoute(type: String, filePath: String, goodsId: String): String =
        "publish/$type/$filePath/$goodsId"
}

/**
 * 注册发布流程导航图。
 *
 * 解析 [type]/[filePath]/[goodsId] 三个参数并注入 [PublishScreen]，
 * 发布完成或返回时交由 [navController] 处理导航。
 */
fun NavGraphBuilder.publishNavGraph(navController: NavHostController) {
    composable(
        route = PublishRoute.route,
        arguments = listOf(
            navArgument(PublishRoute.ARG_TYPE) { type = NavType.StringType },
            navArgument(PublishRoute.ARG_FILE_PATH) { type = NavType.StringType },
            navArgument(PublishRoute.ARG_GOODS_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val args = backStackEntry.arguments
        val type = args?.getString(PublishRoute.ARG_TYPE).orEmpty()
        val filePath = args?.getString(PublishRoute.ARG_FILE_PATH).orEmpty()
        val goodsId = args?.getString(PublishRoute.ARG_GOODS_ID).orEmpty()

        PublishScreen(
            type = type,
            filePath = filePath,
            goodsId = goodsId,
            onBack = { navController.popBackStack() },
            onDone = { navController.popBackStack() }
        )
    }
}

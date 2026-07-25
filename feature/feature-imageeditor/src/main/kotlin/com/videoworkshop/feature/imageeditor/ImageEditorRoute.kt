package com.videoworkshop.feature.imageeditor

import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * 图文编辑器路由定义。
 *
 * 路由形如 `image_editor/{goodsId}`，进入编辑器时需携带关联商品 ID，
 * 用于加载商品信息并基于其生成图文带货内容。
 */
object ImageEditorRoute {
    const val route = "image_editor/{goodsId}"

    /** 商品 ID 路由参数名。 */
    const val ARG_GOODS_ID = "goodsId"

    /** 依据商品 ID 拼装完整路由地址。 */
    fun buildRoute(goodsId: String): String = "image_editor/$goodsId"
}

/**
 * 注册图文编辑器导航图。
 *
 * 在 [NavGraphBuilder] 上挂载 [ImageEditorScreen] 入口，
 * 解析 `goodsId` 参数并向下传递；发布动作跳转到发布流程
 * （内容形式为 image）。
 */
fun NavGraphBuilder.imageEditorNavGraph(navController: NavHostController) {
    composable(
        route = ImageEditorRoute.route,
        arguments = listOf(
            navArgument(ImageEditorRoute.ARG_GOODS_ID) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val goodsId = backStackEntry.arguments
            ?.getString(ImageEditorRoute.ARG_GOODS_ID)
            .orEmpty()

        ImageEditorScreen(
            goodsId = goodsId,
            onBack = { navController.popBackStack() },
            onPublish = {
                // 图文内容以合成标识作为 filePath，进入发布流程
                val filePath = "image_content_$goodsId"
                navController.navigate("publish/image/$filePath/$goodsId")
            }
        )
    }
}

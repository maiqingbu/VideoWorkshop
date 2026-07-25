package com.videoworkshop.feature.goods

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.videoworkshop.domain.model.ContentType

/**
 * 商品搜索页路由常量。
 */
object GoodsRoute {
    const val route = "goods/{mode}"
}

/**
 * 将商品搜索页接入导航图。
 *
 * 从导航参数 `mode`（video / image）确定创作模式：
 * - 视频模式选中商品后跳转「导入素材」`import/video`；
 * - 图文模式选中商品后跳转「图文编辑器」`image_editor/{goodsId}`。
 */
fun NavGraphBuilder.goodsNavGraph(navController: NavController) {
    composable(
        route = GoodsRoute.route,
        arguments = listOf(navArgument("mode") { type = NavType.StringType })
    ) {
        val viewModel: GoodsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val mode = uiState.mode

        LaunchedEffect(uiState.selectedGoods) {
            uiState.selectedGoods?.let { goods ->
                navigateAfterSelection(navController, mode, goods.id)
                viewModel.consumeSelectedGoods()
            }
        }

        GoodsScreen(
            uiState = uiState,
            onBack = { navController.popBackStack() },
            onQueryChange = viewModel::search,
            onProviderSelected = viewModel::selectProvider,
            onGoodsSelected = viewModel::selectGoods,
            onManualLinkSubmit = { link ->
                navigateAfterSelection(navController, mode, link)
            }
        )
    }
}

/**
 * 选中商品 / 手动链接后的统一跳转。
 *
 * @param goodsId 商品 ID（手动链接场景下为粘贴的链接文本）。
 */
private fun navigateAfterSelection(
    navController: NavController,
    mode: ContentType,
    goodsId: String
) {
    when (mode) {
        ContentType.VIDEO -> navController.navigate("import/video")
        ContentType.IMAGE -> navController.navigate("image_editor/$goodsId")
    }
}

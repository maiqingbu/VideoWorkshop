package com.videoworkshop.feature.goods

import android.net.Uri
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
 *
 * - [route] 普通「带货选品」入口，按 mode 区分视频 / 图文。
 * - [libraryRoute] 「商品库」独立入口（首页快捷入口），复用同一 Composable，
 *   mode 默认按 VIDEO 处理，UI 标题显示为「商品库」。
 */
object GoodsRoute {
    const val route = "goods/{mode}"
    const val libraryRoute = "goods-library"
}

/**
 * 将商品搜索页接入导航图。
 *
 * 从导航参数 `mode`（video / image）确定创作模式：
 * - 视频模式选中商品后跳转「素材库」`material`（FIX-01：原 import/video 路由未注册导致闪退）；
 * - 图文模式选中商品后跳转「图文编辑器」`image_editor/{goodsId}`。
 *
 * 同时注册 [GoodsRoute.libraryRoute] 顶层「商品库」入口，复用同一 ViewModel / Screen，
 * 通过 SavedStateHandle 中是否含 mode 参数区分标题。
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
                handleManualLink(viewModel, navController, mode, link)
            },
            onConsumeError = viewModel::consumeError
        )
    }

    // 顶层「商品库」入口：复用 GoodsScreen，标题由 isLibrary 标识切换为「商品库」
    composable(route = GoodsRoute.libraryRoute) {
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
                handleManualLink(viewModel, navController, mode, link)
            },
            onConsumeError = viewModel::consumeError
        )
    }
}

/**
 * 处理手动输入的商品链接：先用 [GoodsViewModel.parseManualLink] 正则提取商品 ID，
 * 解析失败时触发 Snackbar 提示；解析成功则跳转下一步（FIX-02）。
 */
private fun handleManualLink(
    viewModel: GoodsViewModel,
    navController: NavController,
    mode: ContentType,
    link: String
) {
    val goodsId = viewModel.parseManualLink(link)
    if (goodsId != null) {
        navigateAfterSelection(navController, mode, goodsId)
    } else {
        viewModel.onManualLinkFailed()
    }
}

/**
 * 选中商品 / 手动链接后的统一跳转。
 *
 * FIX-01：视频模式原跳 `import/video` 该路由从未注册 NavGraph 导致闪退，改为 `material`。
 * FIX-02：所有外部字符串入路由前必须 [Uri.encode]，避免 `/` `?` `=` 破坏路由解析。
 *
 * @param goodsId 商品 ID（手动链接场景下为正则提取出的数字 ID）。
 */
private fun navigateAfterSelection(
    navController: NavController,
    mode: ContentType,
    goodsId: String
) {
    when (mode) {
        ContentType.VIDEO -> navController.navigate("material")
        ContentType.IMAGE -> navController.navigate("image_editor/${Uri.encode(goodsId)}")
    }
}

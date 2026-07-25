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
 */
fun NavGraphBuilder.materialNavGraph(navController: NavHostController) {
    composable(route = MaterialRoute.route) {
        MaterialScreen(
            onBack = { navController.popBackStack() }
        )
    }
}

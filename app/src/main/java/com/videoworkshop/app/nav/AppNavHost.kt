package com.videoworkshop.app.nav

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.videoworkshop.core.ui.components.BottomTab
import com.videoworkshop.core.ui.components.VWBottomBar
import com.videoworkshop.core.ui.components.VideoWorkshopBottomTabs
import com.videoworkshop.feature.abtransport.abTransportNavGraph
import com.videoworkshop.feature.dedup.DedupRoute
import com.videoworkshop.feature.dedup.dedupNavGraph
import com.videoworkshop.feature.goods.goodsNavGraph
import com.videoworkshop.feature.history.historyNavGraph
import com.videoworkshop.feature.home.homeNavGraph
import com.videoworkshop.feature.imageeditor.imageEditorNavGraph
import com.videoworkshop.feature.material.materialNavGraph
import com.videoworkshop.feature.publish.publishNavGraph
import com.videoworkshop.feature.settings.settingsNavGraph
import com.videoworkshop.feature.videoenhance.EnhanceRoute
import com.videoworkshop.feature.videoenhance.videoEnhanceNavGraph

/**
 * 路由常量集中声明。
 *
 * 路由参数遵循 [Uri.encode] 编码规则，避免 `/` `?` `=` 等字符破坏路由解析。
 */
object Routes {
    const val HOME = "home"
    const val MATERIAL = "material"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val GOODS_LIBRARY = "goods-library"

    const val GOODS = "goods/{mode}"
    const val DEDUP = "dedup/{videoPath}"
    const val ENHANCE = "enhance/{videoPath}/{goodsId}"
    const val IMAGE_EDITOR = "image_editor/{goodsId}"
    const val PUBLISH = "publish/{type}/{filePath}/{goodsId}"
    const val ABTRANSPORT = "abtransport?videoA={videoA}"

    fun goods(mode: String): String = "goods/${Uri.encode(mode)}"
    fun dedup(videoPath: String): String = "dedup/${Uri.encode(videoPath)}"
    fun enhance(videoPath: String, goodsId: String): String =
        "enhance/${Uri.encode(videoPath)}/${Uri.encode(goodsId)}"

    fun imageEditor(goodsId: String): String = "image_editor/${Uri.encode(goodsId)}"
    fun publish(type: String, filePath: String, goodsId: String): String =
        "publish/${Uri.encode(type)}/${Uri.encode(filePath)}/${Uri.encode(goodsId)}"
}

/**
 * 顶层 Tab 路由集合：仅这 4 个路由会展示 [VWBottomBar]。
 */
private val TopLevelRoutes: Set<String> = setOf(
    Routes.HOME,
    Routes.MATERIAL,
    Routes.HISTORY,
    Routes.SETTINGS
)

/**
 * 应用根导航容器。
 *
 * 包裹 [Scaffold] + [VWBottomBar]：
 * - 仅在 4 个顶层路由（home/material/history/settings）显示底部 Tab 栏；
 * - Tab 切换使用 `popUpTo(startDestination) { saveState = true }` 避免栈堆积，
 *   同时启用 `launchSingleTop` 与 `restoreState`，符合 Navigation Compose 官方推荐做法；
 * - 当前 Tab 高亮由 [NavController.currentBackStackEntryAsState] 提供，
 *   通过规范化路由名（截取 `?` 之前部分）后匹配。
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val normalizedRoute = currentRoute?.substringBefore('?')
    val showBottomBar = normalizedRoute in TopLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                VWBottomBar(
                    currentRoute = normalizedRoute ?: Routes.HOME,
                    onTabSelected = { tab -> navigateToTopLevel(navController, tab) },
                    tabs = VideoWorkshopBottomTabs
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(300)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            homeNavGraph(navController)
            goodsNavGraph(navController)
            dedupNavGraph(navController)
            videoEnhanceNavGraph(navController)
            imageEditorNavGraph(navController)
            materialNavGraph(
                navController = navController,
                // 素材卡片操作菜单回调：跳转到对应路由
                onDedup = { path -> navController.navigate(DedupRoute.createRoute(path)) },
                // AB 搬运：预填 A 视频路径，进入 abtransport 路由
                onABTransport = { path -> navController.navigate("abtransport?videoA=${Uri.encode(path)}") },
                // 制作带货视频跳带货包装页，goodsId 占位为 "none"
                onEnhance = { path -> navController.navigate(EnhanceRoute.createRoute(path, "none")) }
            )
            publishNavGraph(navController)
            abTransportNavGraph(
                navController = navController,
                onDedup = { path -> navController.navigate(DedupRoute.createRoute(path)) },
                onEnhance = { path -> navController.navigate(EnhanceRoute.createRoute(path, "none")) }
            )
            historyNavGraph(
                navController = navController,
                onBack = { navController.popBackStack() },
                onRepublish = { draftId ->
                    // 重新发布：跳转 publish 路由，复用草稿 ID
                    navController.navigate("publish/video/${Uri.encode(draftId)}/none")
                }
            )
            settingsNavGraph(navController)
        }
    }
}

/**
 * 顶层 Tab 跳转。
 *
 * 使用 `popUpTo(startDestination) { saveState = true }` + `launchSingleTop = true` + `restoreState = true`
 * 三件套，确保：
 * - 顶层切换不会堆叠多个同路由实例；
 * - 每个顶层 Tab 的回退栈与 UI 状态独立保存；
 * - 用户在多个 Tab 间切换时不会重置已经存在的状态。
 */
private fun navigateToTopLevel(navController: NavHostController, tab: BottomTab) {
    navController.navigate(tab.route) {
        // 从回退栈中弹出至起点，保留起点之上的其它顶层 Tab 状态
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        // 避免重复创建同一顶层路由实例
        launchSingleTop = true
        // 切回该 Tab 时恢复其之前的回退栈与状态
        restoreState = true
    }
}

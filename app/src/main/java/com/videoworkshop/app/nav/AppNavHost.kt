package com.videoworkshop.app.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.videoworkshop.feature.dedup.DedupRoute
import com.videoworkshop.feature.dedup.dedupNavGraph
import com.videoworkshop.feature.goods.goodsNavGraph
import com.videoworkshop.feature.home.HomeRoute
import com.videoworkshop.feature.home.homeNavGraph
import com.videoworkshop.feature.imageeditor.imageEditorNavGraph
import com.videoworkshop.feature.material.materialNavGraph
import com.videoworkshop.feature.publish.publishNavGraph
import com.videoworkshop.feature.videoenhance.videoEnhanceNavGraph

object Routes {
    const val HOME = "home"
    const val GOODS = "goods/{mode}"
    const val IMPORT = "import/{mode}"
    const val DEDUP = "dedup/{videoPath}"
    const val ENHANCE = "enhance/{videoPath}/{goodsId}"
    const val IMAGE_EDITOR = "image_editor/{goodsId}"
    const val MATERIAL = "material"
    const val PUBLISH = "publish/{type}/{filePath}/{goodsId}"

    fun goods(mode: String) = "goods/$mode"
    fun dedup(videoPath: String) = "dedup/$videoPath"
    fun enhance(videoPath: String, goodsId: String) = "enhance/$videoPath/$goodsId"
    fun imageEditor(goodsId: String) = "image_editor/$goodsId"
    fun publish(type: String, filePath: String, goodsId: String) = "publish/$type/$filePath/$goodsId"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
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
        materialNavGraph(navController)
        publishNavGraph(navController)
    }
}

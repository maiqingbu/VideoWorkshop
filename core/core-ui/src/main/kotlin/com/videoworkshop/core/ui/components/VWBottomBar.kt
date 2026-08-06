package com.videoworkshop.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航 Tab 描述。
 */
data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * VideoWorkshop 素简工坊 4 个底部 Tab：工作台 / 项目 / 素材 / 设置。
 *
 * 对应路由：
 * - workbench   工作台
 * - project     项目
 * - material    素材
 * - settings    设置
 */
val VideoWorkshopBottomTabs: List<BottomTab> = listOf(
    BottomTab(route = "workbench", label = "工作台", icon = Icons.Filled.Home),
    BottomTab(route = "project", label = "项目", icon = Icons.Filled.PlayArrow),
    BottomTab(route = "material", label = "素材", icon = Icons.Filled.Collections),
    BottomTab(route = "settings", label = "设置", icon = Icons.Filled.Settings),
)

/**
 * 4 Tab 底部导航栏。
 *
 * @param currentRoute 当前高亮路由（已规范化后的顶层路由名）。
 * @param onTabSelected 点击 Tab 回调。
 * @param tabs Tab 列表，默认使用 [VideoWorkshopBottomTabs]。
 */
@Composable
fun VWBottomBar(
    currentRoute: String,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<BottomTab> = VideoWorkshopBottomTabs,
) {
    NavigationBar(modifier = modifier) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                label = { Text(text = tab.label) },
                alwaysShowLabel = true
            )
        }
    }
}

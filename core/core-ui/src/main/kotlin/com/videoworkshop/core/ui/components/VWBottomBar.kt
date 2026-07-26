package com.videoworkshop.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
 * VideoWorkshop 默认 4 个底部 Tab：首页 / 素材库 / 发布记录 / 我的。
 *
 * 对应路由：
 * - home         首页
 * - material     素材库
 * - history      发布记录
 * - settings      我的
 */
val VideoWorkshopBottomTabs: List<BottomTab> = listOf(
    BottomTab(route = "home", label = "首页", icon = Icons.Filled.Home),
    BottomTab(route = "material", label = "素材库", icon = Icons.Filled.Collections),
    BottomTab(route = "history", label = "发布记录", icon = Icons.Filled.History),
    BottomTab(route = "settings", label = "我的", icon = Icons.Filled.Person),
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

package com.videoworkshop.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 素简工坊间距 Token。
 *
 * 统一使用 4 / 8 / 12 / 16 / 20 / 24 / 32 dp。
 * 主要规则：
 * - 页面左右边距：16dp
 * - 页面顶部内容间距：12~16dp
 * - 大区块间距：24dp
 * - 列表项内边距：14~16dp
 * - 触控区域不得小于 48dp
 */
object VWSpacing {
    /** 4dp 最小间距 */
    val xs = 4.dp

    /** 8dp */
    val sm = 8.dp

    /** 12dp */
    val md = 12.dp

    /** 16dp 页面左右边距、列表项内边距 */
    val lg = 16.dp

    /** 20dp */
    val xl = 20.dp

    /** 24dp 大区块间距 */
    val xxl = 24.dp

    /** 32dp */
    val xxxl = 32.dp

    /** 最小触控区域 48dp */
    val touchTarget: Dp = 48.dp
}
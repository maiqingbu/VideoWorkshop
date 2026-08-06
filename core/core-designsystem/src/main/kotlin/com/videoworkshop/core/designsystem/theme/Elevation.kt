package com.videoworkshop.core.designsystem.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/**
 * 素简工坊阴影 Token。
 *
 * 规则：
 * - 普通页面：不使用阴影。
 * - 可点击浮层或悬浮底栏：最高 2dp。
 * - 卡片主要通过背景、边框和留白形成层级。
 * - 禁止 4dp、6dp、8dp、10dp 的常态阴影。
 */
object VWElevation {
    /** 无阴影 */
    val none: Dp = 0.dp

    /** 可点击浮层/悬浮底栏最大阴影 2dp */
    val raised: Dp = 2.dp
}

/** @deprecated 旧设计系统阴影 10dp，新代码禁止使用。 */
@Deprecated("旧设计系统阴影，新代码禁止使用。请使用 VWElevation。", level = DeprecationLevel.WARNING)
val LegacyShadowElevation: Dp = 10.dp
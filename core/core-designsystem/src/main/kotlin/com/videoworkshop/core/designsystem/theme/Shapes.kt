package com.videoworkshop.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 素简工坊圆角 Token。
 *
 * 场景：
 * - 小标签 6dp
 * - 输入框、普通按钮 10dp
 * - 列表容器、普通卡片 12dp
 * - 底部弹层顶部 20dp
 * - 图片缩略图 8~10dp
 * 禁止把所有组件统一做成 20dp 大圆角。
 */
object VWRadius {
    /** 小标签 6dp */
    val badge = RoundedCornerShape(6.dp)

    /** 图片缩略图 8dp */
    val thumbnail = RoundedCornerShape(8.dp)

    /** 输入框、普通按钮 10dp */
    val field = RoundedCornerShape(10.dp)

    /** 列表容器、普通卡片 12dp */
    val card = RoundedCornerShape(12.dp)

    /** 底部弹层顶部 20dp */
    val bottomSheetTop = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
}

/**
 * 映射到 Material3 [Shapes]。
 */
val VWShapes: Shapes = Shapes(
    extraSmall = VWRadius.badge,
    small = VWRadius.thumbnail,
    medium = VWRadius.card,
    large = VWRadius.card,
    extraLarge = VWRadius.bottomSheetTop
)

/** @deprecated 请使用 [VWRadius.card]。 */
@Deprecated("旧设计系统圆角，新代码请使用 VWRadius。")
val CardShape = RoundedCornerShape(14.dp)

/** @deprecated 请使用 [VWRadius.field]。 */
@Deprecated("旧设计系统圆角，新代码请使用 VWRadius。")
val ButtonShape = RoundedCornerShape(12.dp)

/** 输入框统一圆角（兼容旧代码） */
val FieldShape = RoundedCornerShape(10.dp)

/** 标签/徽章统一圆角（兼容旧代码） */
val BadgeShape = RoundedCornerShape(6.dp)

/** 全圆角（胶囊形） */
val PillShape = RoundedCornerShape(50)
package com.videoworkshop.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * VideoWorkshop 自定义 Material3 形状体系。
 *
 * 统一圆角风格，呼应电商活力的现代感。
 */
val VWShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** 卡片统一圆角 */
val CardShape = RoundedCornerShape(14.dp)

/** 按钮统一圆角 */
val ButtonShape = RoundedCornerShape(12.dp)

/** 输入框统一圆角 */
val FieldShape = RoundedCornerShape(10.dp)

/** 标签/徽章统一圆角 */
val BadgeShape = RoundedCornerShape(6.dp)

/** 全圆角（胶囊形） */
val PillShape = RoundedCornerShape(50)

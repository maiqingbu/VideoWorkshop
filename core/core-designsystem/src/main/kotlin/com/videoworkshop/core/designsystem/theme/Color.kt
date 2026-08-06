package com.videoworkshop.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// 素简工坊 · 亮色主题 Token（新设计系统）
// 设计方向：安静、清楚、耐看、可信。主色为低饱和松墨绿，不使用商业红。
// =============================================================================
/** 页面背景（纸色） */
val PaperBackground = Color(0xFFF5F5F1)

/** 内容区域（纯白） */
val PaperSurface = Color(0xFFFFFFFF)

/** 次级区域、输入背景 */
val PaperSurfaceMuted = Color(0xFFEFEFEB)

/** 主文字（墨色） */
val InkPrimary = Color(0xFF202521)

/** 辅助文字 */
val InkSecondary = Color(0xFF626963)

/** 占位与弱提示 */
val InkTertiary = Color(0xFF929893)

/** 边框、分隔线 */
val BorderDefault = Color(0xFFE1E3DE)

/** 主操作、选中状态（松墨绿） */
val PinePrimary = Color(0xFF416756)

/** 主操作按下态 */
val PinePressed = Color(0xFF315244)

/** 选中背景、浅色标签 */
val PineContainer = Color(0xFFE2ECE6)

/** 主操作上的文字（白） */
val PineOnPrimary = Color(0xFFFFFFFF)

/** 可点击链接蓝 */
val LinkBlue = Color(0xFF466D86)

/** 成功 */
val SuccessGreen = Color(0xFF43775A)

/** 警告 */
val WarningAmber = Color(0xFF94692F)

/** 错误、删除 */
val ErrorRed = Color(0xFFB34B40)

/** 仅用于商品价格（与 Error 同色） */
val PriceRed = Color(0xFFB34B40)

// =============================================================================
// 素简工坊 · 暗色主题 Token
// =============================================================================
/** 暗色页面背景 */
val DarkBackgroundSurface = Color(0xFF111512)

/** 暗色内容区域 */
val DarkSurfaceSurface = Color(0xFF1A201C)

/** 暗色次级区域 */
val DarkSurfaceMuted = Color(0xFF242A26)

/** 暗色主文字 */
val DarkTextPrimary = Color(0xFFEDF0ED)

/** 暗色辅助文字 */
val DarkTextSecondary = Color(0xFFADB4AE)

/** 暗色边框 */
val DarkBorder = Color(0xFF343B36)

/** 暗色主操作（浅松墨绿） */
val DarkPrimarySurface = Color(0xFF83A893)

/** 暗色选中背景 */
val DarkPrimaryContainerSurface = Color(0xFF294337)

// =============================================================================
// 品牌主色：电商活力红（已废弃，仅保留兼容）
// =============================================================================
/** @deprecated 新设计系统使用 [PinePrimary]，此字段仅用于旧页面兼容。 */
@Deprecated("旧设计系统颜色，新代码禁止引用。请使用 PinePrimary。")
val BrandRed = Color(0xFFE94560)

/** @deprecated 旧设计系统颜色，新代码禁止引用。 */
@Deprecated("旧设计系统颜色，新代码禁止引用。请使用 PinePressed。")
val BrandRedDark = Color(0xFFC73E55)

/** @deprecated 旧设计系统颜色，新代码禁止引用。 */
@Deprecated("旧设计系统颜色，新代码禁止引用。请使用 PinePrimary。")
val BrandRedLight = Color(0xFFFF6B8A)

// =============================================================================
// 品牌深色系（已废弃，仅保留兼容）
// =============================================================================
/** @deprecated 旧设计系统颜色，新代码禁止引用。 */
@Deprecated("旧设计系统颜色，新代码禁止引用。请使用 DarkBackgroundSurface。")
val BrandDark = Color(0xFF1A1A2E)

/** @deprecated 旧设计系统颜色，新代码禁止引用。 */
@Deprecated("旧设计系统颜色，新代码禁止引用。请使用 DarkSurfaceSurface。")
val BrandNavy = Color(0xFF16213E)

// =============================================================================
// 语义色（已废弃，仅保留兼容）
// =============================================================================
/** @deprecated 新设计系统使用 [SuccessGreen] 等状态色。 */
@Deprecated("旧设计系统颜色，新代码禁止引用。请使用 SuccessGreen / ErrorRed / WarningAmber。")
val SemanticSuccess = Color(0xFF2ECC71)
/** @deprecated */
@Deprecated("旧设计系统颜色，新代码禁止引用。")
val SemanticSuccessContainer = Color(0xFFD6F5E3)

/** @deprecated */
@Deprecated("旧设计系统颜色，新代码禁止引用。请使用 WarningAmber。")
val SemanticWarning = Color(0xFFFFA500)
/** @deprecated */
@Deprecated("旧设计系统颜色，新代码禁止引用。")
val SemanticWarningContainer = Color(0xFFFFEAD0)

/** @deprecated */
@Deprecated("旧设计系统颜色，新代码禁止引用。请使用 LinkBlue。")
val SemanticInfo = Color(0xFF3B82F6)
/** @deprecated */
@Deprecated("旧设计系统颜色，新代码禁止引用。")
val SemanticInfoContainer = Color(0xFFD8E8FF)

/** @deprecated */
@Deprecated("旧设计系统颜色，新代码禁止引用。请使用 ErrorRed。")
val SemanticError = Color(0xFFE53935)
/** @deprecated */
@Deprecated("旧设计系统颜色，新代码禁止引用。")
val SemanticErrorContainer = Color(0xFFFFDAD6)

/** @deprecated */
@Deprecated("旧设计系统颜色，新代码禁止引用。请使用 InkTertiary。")
val NeutralGray = Color(0xFF9E9E9E)
/** @deprecated */
@Deprecated("旧设计系统颜色，新代码禁止引用。请使用 BorderDefault。")
val NeutralGrayLight = Color(0xFFE0E0E0)
/** @deprecated */
@Deprecated("旧设计系统颜色，新代码禁止引用。请使用 InkSecondary。")
val NeutralGrayDark = Color(0xFF616161)

// =============================================================================
// Light 主题配色（基于素简工坊新 Token）
// =============================================================================
val LightPrimary = PinePrimary
val LightOnPrimary = PineOnPrimary
val LightPrimaryContainer = PineContainer
val LightOnPrimaryContainer = InkPrimary

val LightSecondary = PinePrimary
val LightOnSecondary = PineOnPrimary
val LightSecondaryContainer = PineContainer
val LightOnSecondaryContainer = InkPrimary

val LightTertiary = LinkBlue
val LightOnTertiary = PineOnPrimary
val LightTertiaryContainer = PineContainer
val LightOnTertiaryContainer = InkPrimary

val LightBackground = PaperBackground
val LightOnBackground = InkPrimary
val LightSurface = PaperSurface
val LightOnSurface = InkPrimary
val LightSurfaceVariant = PaperSurfaceMuted
val LightOnSurfaceVariant = InkSecondary
val SurfaceTintLight = LightPrimary

val LightError = ErrorRed
val LightOnError = PineOnPrimary
val LightErrorContainer = ErrorRed.copy(alpha = 0.12f)
val LightOnErrorContainer = ErrorRed

val LightOutline = BorderDefault
val LightOutlineVariant = BorderDefault
val LightInverseSurface = InkPrimary
val LightInverseOnSurface = PaperBackground
val LightInversePrimary = PineContainer
val LightScrim = Color(0xFF000000)

// =============================================================================
// Dark 主题配色（基于素简工坊新 Token）
// =============================================================================
val DarkPrimary = Color(0xFF83A893)
val DarkOnPrimary = Color(0xFF121A15)
val DarkPrimaryContainer = Color(0xFF294337)
val DarkOnPrimaryContainer = DarkTextPrimary

val DarkSecondary = Color(0xFF83A893)
val DarkOnSecondary = Color(0xFF121A15)
val DarkSecondaryContainer = Color(0xFF294337)
val DarkOnSecondaryContainer = DarkTextPrimary

val DarkTertiary = LinkBlue
val DarkOnTertiary = PineOnPrimary
val DarkTertiaryContainer = Color(0xFF294337)
val DarkOnTertiaryContainer = DarkTextPrimary

val DarkBackground = DarkBackgroundSurface
val DarkOnBackground = DarkTextPrimary
val DarkSurface = DarkSurfaceSurface
val DarkOnSurface = DarkTextPrimary
val DarkSurfaceVariant = DarkSurfaceMuted
val DarkOnSurfaceVariant = DarkTextSecondary
val SurfaceTintDark = Color(0xFF83A893)

val DarkError = Color(0xFFE08A80)
val DarkOnError = Color(0xFF3B0D0A)
val DarkErrorContainer = Color(0xFF5A2A25)
val DarkOnErrorContainer = Color(0xFFE08A80)

val DarkOutline = DarkBorder
val DarkOutlineVariant = DarkBorder
val DarkInverseSurface = DarkTextPrimary
val DarkInverseOnSurface = DarkBackgroundSurface
val DarkInversePrimary = Color(0xFF294337)
val DarkScrim = Color(0xFF000000)

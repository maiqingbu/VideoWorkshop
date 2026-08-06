package com.videoworkshop.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * VideoWorkshop 素简工坊字体体系。
 *
 * 使用 Android 系统中文无衬线字体，取消 Material 默认偏西文的巨大 Display 字号与额外字间距。
 * 中文排版规则：
 * - 所有中文样式 letterSpacing = 0.sp。
 * - 不大量使用粗体，600 为最高常规字重。
 * - 不使用全大写英文。
 */
object VWTypeScale {
    /** 页面标题 22sp/600/30sp */
    val pageTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    )

    /** 页面副标题 13sp/400/20sp */
    val pageSubtitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )

    /** 分区标题 16sp/600/24sp */
    val sectionTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )

    /** 列表主标题 15sp/500/22sp */
    val listTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    /** 正文 14sp/400/22sp */
    val body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    /** 辅助文字 13sp/400/20sp */
    val caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )

    /** 标签、时间 12sp/400-500/18sp */
    val label = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )

    /** 主按钮 15sp/500/22sp */
    val button = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )
}

/**
 * 映射到 Material3 [Typography] 的字体体系。
 *
 * 保留 Material 语义槽位，但字号与字重按素简工坊规范覆盖。
 */
val VWTypography: Typography = Typography(
    // ===== Display 映射到页面标题（22sp），避免过大西文展示字号 =====
    displayLarge = VWTypeScale.pageTitle,
    displayMedium = VWTypeScale.pageTitle,
    displaySmall = VWTypeScale.sectionTitle,

    // ===== Headline =====
    headlineLarge = VWTypeScale.pageTitle,
    headlineMedium = VWTypeScale.sectionTitle,
    headlineSmall = VWTypeScale.sectionTitle,

    // ===== Title =====
    titleLarge = VWTypeScale.pageTitle,
    titleMedium = VWTypeScale.sectionTitle,
    titleSmall = VWTypeScale.listTitle,

    // ===== Body =====
    bodyLarge = VWTypeScale.body,
    bodyMedium = VWTypeScale.body,
    bodySmall = VWTypeScale.label,

    // ===== Label =====
    labelLarge = VWTypeScale.button,
    labelMedium = VWTypeScale.caption,
    labelSmall = VWTypeScale.label
)

/**
 * 链接/可点击文字样式（带下划线，主色调）。
 */
val LinkTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    textDecoration = TextDecoration.Underline
)
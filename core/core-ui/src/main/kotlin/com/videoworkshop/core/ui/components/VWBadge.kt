package com.videoworkshop.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.videoworkshop.core.designsystem.theme.BadgeShape
import com.videoworkshop.core.designsystem.theme.NeutralGray
import com.videoworkshop.core.designsystem.theme.SemanticError
import com.videoworkshop.core.designsystem.theme.SemanticInfo
import com.videoworkshop.core.designsystem.theme.SemanticSuccess
import com.videoworkshop.core.designsystem.theme.SemanticWarning

/**
 * 徽章变体，对应业务状态色彩。
 *
 * - [GREEN]  绿色：成功/已完成/已发布
 * - [BLUE]   蓝色：进行中/信息
 * - [ORANGE] 橙色：待处理/警告
 * - [RED]    红色：失败/错误
 * - [GRAY]   灰色：中性/草稿
 */
enum class BadgeType(val containerColor: Color, val contentColor: Color) {
    GREEN(containerColor = SemanticSuccess, contentColor = Color.White),
    BLUE(containerColor = SemanticInfo, contentColor = Color.White),
    ORANGE(containerColor = SemanticWarning, contentColor = Color.White),
    RED(containerColor = SemanticError, contentColor = Color.White),
    GRAY(containerColor = NeutralGray, contentColor = Color.White)
}

/**
 * 状态徽章：小尺寸圆角标签，用于列表项标记状态。
 */
@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    type: BadgeType = BadgeType.GRAY,
) {
    Text(
        text = text,
        modifier = modifier
            .background(color = type.containerColor, shape = BadgeShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = type.contentColor,
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.labelSmall
    )
}

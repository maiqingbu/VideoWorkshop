package com.videoworkshop.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.videoworkshop.core.designsystem.theme.VWSpacing
import com.videoworkshop.core.designsystem.theme.VWTypeScale

/**
 * 素简工坊通用列表行。
 *
 * 结构：leading 图标（可选）+ 主标题 + 副标题（可选）+ trailing（可选）。
 * 列表项之间优先使用分隔线，不重复制造悬浮卡片。
 *
 * @param title 主标题（最多两行）
 * @param subtitle 副标题（最多两行，弱色）
 * @param leading 左侧图标/缩略图
 * @param trailing 右侧内容（状态标签、箭头、文字等）
 * @param onClick 为 null 时不可点击
 */
@Composable
fun VWListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showTrailingArrow: Boolean = false,
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .heightIn(min = VWSpacing.touchTarget)
        .padding(horizontal = VWSpacing.lg, vertical = VWSpacing.md)
        .let { m ->
            if (onClick != null) m.clickable(onClick = onClick) else m
        }

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VWSpacing.sm)
    ) {
        if (leading != null) {
            leading()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = VWTypeScale.listTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = VWTypeScale.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
        if (showTrailingArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
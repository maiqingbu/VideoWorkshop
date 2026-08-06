package com.videoworkshop.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.videoworkshop.core.designsystem.theme.VWRadius
import com.videoworkshop.core.designsystem.theme.VWSpacing
import com.videoworkshop.core.designsystem.theme.VWTypeScale

/**
 * 素简工坊项目列表行。
 *
 * 结构：72×72 缩略图 + 标题 / 类型 · 状态 / 时间 + 右侧操作。
 * 纵向列表，标题最多两行，状态必须包含文字。
 *
 * @param title 项目标题
 * @param typeLabel 类型文字（如"视频项目"）
 * @param status 状态文字（如"制作中"）
 * @param timeLabel 时间文字（如"8 月 4 日 19:42"）
 * @param thumbnail 缩略图内容（默认显示类型图标占位）
 * @param trailing 右侧操作区（如"⋯"菜单）
 */
@Composable
fun VWProjectRow(
    title: String,
    typeLabel: String,
    status: String,
    timeLabel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    thumbnailIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = VWSpacing.lg, vertical = VWSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 缩略图 72×72
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = VWRadius.thumbnail
                ),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailIcon != null) {
                Icon(
                    imageVector = thumbnailIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.width(VWSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = VWTypeScale.listTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$typeLabel · $status",
                style = VWTypeScale.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = timeLabel,
                style = VWTypeScale.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(VWSpacing.sm))
            trailing()
        }
    }
}
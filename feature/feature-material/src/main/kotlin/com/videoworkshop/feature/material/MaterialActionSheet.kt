package com.videoworkshop.feature.material

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 素材操作菜单。
 *
 * 以底部弹层形式展示素材的可用操作：预览/去重/AB 搬运/制作带货视频/编辑/重命名/删除。
 * 「删除」触发二次确认对话框。
 *
 * @param onDismiss     关闭回调
 * @param onPreview     预览
 * @param onDedup       去重
 * @param onABTransport AB 搬运
 * @param onEnhance     制作带货视频
 * @param onEdit        编辑（标签/备注）
 * @param onRename      重命名
 * @param onDelete      删除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialActionSheet(
    onDismiss: () -> Unit,
    onPreview: () -> Unit = {},
    onDedup: () -> Unit = {},
    onABTransport: () -> Unit = {},
    onEnhance: () -> Unit = {},
    onEdit: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            ActionRow(
                icon = Icons.Filled.PlayCircleOutline,
                title = "预览",
                onClick = {
                    onDismiss()
                    onPreview()
                }
            )
            ActionRow(
                icon = Icons.Filled.CompareArrows,
                title = "去重",
                onClick = {
                    onDismiss()
                    onDedup()
                }
            )
            ActionRow(
                icon = Icons.Filled.ContentCopy,
                title = "AB 搬运",
                onClick = {
                    onDismiss()
                    onABTransport()
                }
            )
            ActionRow(
                icon = Icons.Filled.Shop,
                title = "制作带货视频",
                onClick = {
                    onDismiss()
                    onEnhance()
                }
            )
            ActionRow(
                icon = Icons.Filled.Edit,
                title = "编辑",
                onClick = {
                    onDismiss()
                    onEdit()
                }
            )
            ActionRow(
                icon = Icons.Filled.Label,
                title = "重命名",
                onClick = {
                    onDismiss()
                    onRename()
                }
            )
            ActionRow(
                icon = Icons.Filled.Delete,
                title = "删除",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = {
                    showDeleteConfirm = true
                }
            )
        }
    }

    // 删除二次确认
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除素材") },
            text = { Text("确认删除该素材？删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDismiss()
                        onDelete()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = titleColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor,
            fontWeight = FontWeight.Medium
        )
    }
}

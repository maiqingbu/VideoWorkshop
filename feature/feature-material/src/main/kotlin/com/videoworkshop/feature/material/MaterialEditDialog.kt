package com.videoworkshop.feature.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.videoworkshop.domain.model.MaterialEntity

/**
 * 素材编辑对话框。
 *
 * 支持修改素材的「名称」「标签」「备注」三个字段：
 * - 名称：单行文本框（只读展示路径，不写入数据库）
 * - 标签：FlowRow 展示已有标签 + 输入框 + 新增按钮 + 单项删除
 * - 备注：多行文本框
 *
 * @param material  待编辑的素材
 * @param onDismiss 关闭回调
 * @param onSave    保存回调，参数为新的标签集合与备注
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaterialEditDialog(
    material: MaterialEntity,
    onDismiss: () -> Unit,
    onSave: (tags: List<String>, note: String) -> Unit
) {
    val tags = remember { mutableStateListOf(*material.tags.toTypedArray()) }
    var note by remember { mutableStateOf(material.note) }
    var tagInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑素材") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 名称（只读展示，不写入；以文件名作为展示）
                val displayName = remember(material) {
                    material.path.substringAfterLast('/').ifBlank { "素材 ${material.id}" }
                }
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("文件名") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 标签：FlowRow + 输入新增
                Text(
                    text = "标签",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = { tags.remove(tag) },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "移除 $tag",
                                    modifier = Modifier.padding(0.dp)
                                )
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("新增标签") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val trimmed = tagInput.trim()
                                if (trimmed.isNotEmpty() && trimmed !in tags) {
                                    tags.add(trimmed)
                                }
                                tagInput = ""
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "新增")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val trimmed = tagInput.trim()
                            if (trimmed.isNotEmpty() && trimmed !in tags) {
                                tags.add(trimmed)
                            }
                            tagInput = ""
                        }
                    )
                )

                // 备注
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(tags.toList(), note.trim())
                    onDismiss()
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

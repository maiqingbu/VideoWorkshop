package com.videoworkshop.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.videoworkshop.core.designsystem.theme.InkSecondary
import com.videoworkshop.core.designsystem.theme.InkTertiary
import com.videoworkshop.core.designsystem.theme.PinePrimary
import com.videoworkshop.core.designsystem.theme.SemanticSuccess
import com.videoworkshop.core.designsystem.theme.VWSpacing
import com.videoworkshop.core.designsystem.theme.VWTypeScale
import com.videoworkshop.core.ui.components.VWListRow
import com.videoworkshop.core.ui.components.VWTopAppBar

/**
 * 设置页（素简工坊分组列表）。
 *
 * 采用分组列表：智能服务 / 商品平台 / 导出与存储 / 外观 / 关于。
 * 每行只显示名称、状态与箭头，不展示大盘凭证卡；清理缓存使用次级按钮，不使用渐变红。
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onStartEdit: (SettingsField) -> Unit,
    onSaveEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onClearCache: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    onConsumeToast: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var pendingPlatform by remember { mutableStateOf<Platform?>(null) }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbarHostState.showSnackbar(it)
            onConsumeToast()
        }
    }

    val editingField = state.editingFieldKey?.let { SettingsField.from(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        VWTopAppBar(title = "设置")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = VWSpacing.sm)
        ) {
            // ===== 智能服务 =====
            item { GroupHeader("智能服务") }
            item {
                ServiceRow(
                    label = "文案服务",
                    configured = state.ai.deepSeekKey.isConfigured,
                    onClick = { onStartEdit(SettingsField.DEEPSEEK_KEY) }
                )
            }
            item {
                ServiceRow(
                    label = "语音识别",
                    configured = state.ai.azureKey.isConfigured,
                    onClick = { onStartEdit(SettingsField.AZURE_KEY) }
                )
            }
            item {
                ServiceRow(
                    label = "智能配音",
                    configured = state.ai.groqKey.isConfigured,
                    onClick = { onStartEdit(SettingsField.GROQ_KEY) }
                )
            }

            // ===== 商品平台 =====
            item { GroupHeader("商品平台") }
            items(Platform.entries) { platform ->
                PlatformRow(
                    platform = platform,
                    connected = platform.isConnected(state),
                    onClick = { pendingPlatform = platform }
                )
            }

            // ===== 导出与存储 =====
            item { GroupHeader("导出与存储") }
            item {
                VWListRow(
                    title = "缓存管理",
                    trailing = {
                        Text(
                            text = SettingsViewModel.formatCacheSize(state.cache.sizeBytes),
                            style = VWTypeScale.body,
                            color = InkSecondary
                        )
                    },
                    showTrailingArrow = true,
                    onClick = { showClearCacheDialog = true }
                )
            }

            // ===== 外观 =====
            item { GroupHeader("外观") }
            item {
                VWListRow(
                    title = "深色模式",
                    trailing = {
                        Text(
                            text = themeModeLabel(state.themeMode),
                            style = VWTypeScale.body,
                            color = InkSecondary
                        )
                    },
                    showTrailingArrow = true,
                    onClick = { showThemeDialog = true }
                )
            }

            // ===== 关于 =====
            item { GroupHeader("关于") }
            item {
                VWListRow(
                    title = "版本",
                    trailing = {
                        Text(
                            text = state.about.versionName.ifBlank { "—" },
                            style = VWTypeScale.body,
                            color = InkSecondary
                        )
                    }
                )
            }
            item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            item {
                VWListRow(
                    title = "诊断信息",
                    subtitle = state.about.githubUrl,
                    showTrailingArrow = true,
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.about.githubUrl)))
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (editingField != null) {
        CredentialEditDialog(
            field = editingField,
            initialValue = state.editingInitialValue,
            onSave = onSaveEdit,
            onDismiss = onCancelEdit
        )
    }

    if (pendingPlatform != null) {
        PlatformFieldDialog(
            platform = pendingPlatform!!,
            onSelect = {
                onStartEdit(it)
                pendingPlatform = null
            },
            onDismiss = { pendingPlatform = null }
        )
    }

    if (showThemeDialog) {
        ThemeModeDialog(
            current = state.themeMode,
            onSelect = {
                onThemeModeChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("清理缓存") },
            text = { Text("将清理 cacheDir 与临时产物，不影响素材库已导入文件。当前缓存 ${SettingsViewModel.formatCacheSize(state.cache.sizeBytes)}。") },
            confirmButton = {
                TextButton(onClick = {
                    onClearCache()
                    showClearCacheDialog = false
                }) {
                    Text("清理", color = PinePrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("取消") }
            }
        )
    }
}

/** 分组标题。 */
@Composable
private fun GroupHeader(title: String) {
    Text(
        text = title,
        style = VWTypeScale.sectionTitle,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = VWSpacing.lg, vertical = VWSpacing.sm)
    )
}

/** 智能服务行：名称 + 已配置/未配置状态。未配置用灰色，不使用橙色警告。 */
@Composable
private fun ServiceRow(label: String, configured: Boolean, onClick: () -> Unit) {
    VWListRow(
        title = label,
        trailing = {
            Text(
                text = if (configured) "已配置" else "未配置",
                style = VWTypeScale.body,
                color = if (configured) SemanticSuccess else InkTertiary
            )
        },
        showTrailingArrow = true,
        onClick = onClick
    )
    RowDivider()
}

/** 商品平台行。 */
@Composable
private fun PlatformRow(platform: Platform, connected: Boolean, onClick: () -> Unit) {
    VWListRow(
        title = platform.label,
        trailing = {
            Text(
                text = if (connected) "已连接" else "未连接",
                style = VWTypeScale.body,
                color = if (connected) SemanticSuccess else InkTertiary
            )
        },
        showTrailingArrow = true,
        onClick = onClick
    )
    RowDivider()
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = VWSpacing.lg),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/** 商品平台字段选择对话框：选择该平台要编辑的 AppKey / Secret。 */
@Composable
private fun PlatformFieldDialog(
    platform: Platform,
    onSelect: (SettingsField) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(platform.label) },
        text = {
            Column {
                platform.fields.forEach { field ->
                    Text(
                        text = field.label,
                        style = VWTypeScale.body,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .androidx_compose_clickable { onSelect(field) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 深色模式三选对话框。 */
@Composable
private fun ThemeModeDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Triple("system", "跟随系统", "随系统外观自动切换"),
        Triple("light", "浅色", "始终使用浅色主题"),
        Triple("dark", "深色", "始终使用深色主题")
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("深色模式") },
        text = {
            Column {
                options.forEach { (mode, label, desc) ->
                    Text(
                        text = "$label　${if (mode == current) "✓" else ""}",
                        style = VWTypeScale.body,
                        color = if (mode == current) PinePrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .androidx_compose_clickable { onSelect(mode) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// 局部点击扩展：避免工具栏占用，直接在行文本上绑定点击。
private fun Modifier.androidx_compose_clickable(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))

private fun themeModeLabel(mode: String): String = when (mode) {
    "light" -> "浅色"
    "dark" -> "深色"
    else -> "跟随系统"
}

/** 商品平台枚举：对应字段与连接状态。 */
private enum class Platform(val label: String) {
    TAOBAO("淘宝联盟"),
    JD("京东联盟"),
    PDD("多多进宝");

    val fields: List<SettingsField>
        get() = when (this) {
            TAOBAO -> listOf(SettingsField.TAOBAO_APPKEY, SettingsField.TAOBAO_SECRET)
            JD -> listOf(SettingsField.JD_APPKEY, SettingsField.JD_SECRET)
            PDD -> listOf(SettingsField.PDD_CLIENT_ID, SettingsField.PDD_CLIENT_SECRET)
        }

    fun isConnected(state: SettingsUiState): Boolean = when (this) {
        TAOBAO -> state.alliance.taobaoAppKey.isConfigured && state.alliance.taobaoSecret.isConfigured
        JD -> state.alliance.jdAppKey.isConfigured && state.alliance.jdSecret.isConfigured
        PDD -> state.alliance.pddClientId.isConfigured && state.alliance.pddClientSecret.isConfigured
    }
}

/**
 * 凭证编辑对话框：单字段输入，密文显示。
 */
@Composable
private fun CredentialEditDialog(
    field: SettingsField,
    initialValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable(field.key) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = field.label)
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                label = { Text("输入 ${field.label}") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
package com.videoworkshop.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.videoworkshop.core.designsystem.theme.BrandRed
import com.videoworkshop.core.designsystem.theme.NeutralGray
import com.videoworkshop.core.designsystem.theme.SemanticInfo
import com.videoworkshop.core.designsystem.theme.SemanticSuccess
import com.videoworkshop.core.designsystem.theme.SemanticWarning
import com.videoworkshop.core.ui.components.GradientButton
import com.videoworkshop.core.ui.components.VWTopBar

/**
 * 设置页主入口 Composable。
 *
 * 顶部使用 [VWTopBar] 标题「我的」，内容区以分组卡片形式展示：
 * - AI 设置：DeepSeek / Azure / Groq 三项 API Key
 * - 联盟凭证：淘宝 / 京东 / 拼多多 AppKey/AppSecret
 * - 关于：版本号 / 构建时间 / GitHub 链接
 * - 缓存清理：显示当前缓存大小与清理按钮
 *
 * 凭证编辑通过 [CredentialEditDialog] 弹窗完成。
 *
 * @param state      UI 状态
 * @param onStartEdit 点击某个凭证项触发，参数为待编辑字段
 * @param onSaveEdit  保存编辑值
 * @param onCancelEdit 取消编辑
 * @param onClearCache 清理缓存
 * @param onConsumeToast 清除一次性 toast
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onStartEdit: (SettingsField) -> Unit,
    onSaveEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onClearCache: () -> Unit,
    onConsumeToast: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbarHostState.showSnackbar(it)
            onConsumeToast()
        }
    }

    val editingField = state.editingFieldKey?.let { SettingsField.from(it) }

    Scaffold(
        topBar = { VWTopBar(title = "我的") },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ===== AI 设置 =====
            item {
                SettingsGroupCard(
                    title = "AI 设置",
                    subtitle = "API Key 用于运行时注入 data-ai 模块",
                    icon = Icons.Filled.Brush,
                    iconTint = BrandRed
                ) {
                    CredentialRow(
                        label = state.ai.deepSeekKey.label,
                        masked = state.ai.deepSeekKey.masked,
                        configured = state.ai.deepSeekKey.isConfigured,
                        onClick = { onStartEdit(SettingsField.DEEPSEEK_KEY) }
                    )
                    CredentialRow(
                        label = state.ai.azureKey.label,
                        masked = state.ai.azureKey.masked,
                        configured = state.ai.azureKey.isConfigured,
                        onClick = { onStartEdit(SettingsField.AZURE_KEY) }
                    )
                    CredentialRow(
                        label = state.ai.groqKey.label,
                        masked = state.ai.groqKey.masked,
                        configured = state.ai.groqKey.isConfigured,
                        onClick = { onStartEdit(SettingsField.GROQ_KEY) }
                    )
                }
            }

            // ===== 联盟凭证 =====
            item {
                SettingsGroupCard(
                    title = "联盟凭证",
                    subtitle = if (state.alliance.anyConfigured) {
                        "已配置，将使用真实接口"
                    } else {
                        "未配置，将使用 Mock 数据"
                    },
                    icon = Icons.Filled.Store,
                    iconTint = if (state.alliance.anyConfigured) SemanticSuccess else SemanticWarning,
                    subtitleTint = if (state.alliance.anyConfigured) SemanticSuccess else SemanticWarning
                ) {
                    ProviderSubgroup(title = "淘宝") {
                        CredentialRow(
                            label = state.alliance.taobaoAppKey.label,
                            masked = state.alliance.taobaoAppKey.masked,
                            configured = state.alliance.taobaoAppKey.isConfigured,
                            onClick = { onStartEdit(SettingsField.TAOBAO_APPKEY) }
                        )
                        CredentialRow(
                            label = state.alliance.taobaoSecret.label,
                            masked = state.alliance.taobaoSecret.masked,
                            configured = state.alliance.taobaoSecret.isConfigured,
                            onClick = { onStartEdit(SettingsField.TAOBAO_SECRET) }
                        )
                    }
                    ProviderSubgroup(title = "京东") {
                        CredentialRow(
                            label = state.alliance.jdAppKey.label,
                            masked = state.alliance.jdAppKey.masked,
                            configured = state.alliance.jdAppKey.isConfigured,
                            onClick = { onStartEdit(SettingsField.JD_APPKEY) }
                        )
                        CredentialRow(
                            label = state.alliance.jdSecret.label,
                            masked = state.alliance.jdSecret.masked,
                            configured = state.alliance.jdSecret.isConfigured,
                            onClick = { onStartEdit(SettingsField.JD_SECRET) }
                        )
                    }
                    ProviderSubgroup(title = "拼多多") {
                        CredentialRow(
                            label = state.alliance.pddClientId.label,
                            masked = state.alliance.pddClientId.masked,
                            configured = state.alliance.pddClientId.isConfigured,
                            onClick = { onStartEdit(SettingsField.PDD_CLIENT_ID) }
                        )
                        CredentialRow(
                            label = state.alliance.pddClientSecret.label,
                            masked = state.alliance.pddClientSecret.masked,
                            configured = state.alliance.pddClientSecret.isConfigured,
                            onClick = { onStartEdit(SettingsField.PDD_CLIENT_SECRET) }
                        )
                    }
                }
            }

            // ===== 关于 =====
            item {
                SettingsGroupCard(
                    title = "关于",
                    icon = Icons.Filled.Info,
                    iconTint = SemanticInfo
                ) {
                    InfoRow(label = "版本号", value = state.about.versionName.ifBlank { "—" })
                    InfoRow(label = "构建时间", value = state.about.buildTime.ifBlank { "—" })
                    InfoRow(
                        label = "GitHub",
                        value = state.about.githubUrl,
                        trailing = {
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.about.githubUrl))
                                runCatching { context.startActivity(intent) }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "在浏览器打开",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }

            // ===== 缓存清理 =====
            item {
                SettingsGroupCard(
                    title = "缓存清理",
                    subtitle = "仅清理 cacheDir 与临时产物，不影响素材库已导入文件",
                    icon = Icons.Filled.CleaningServices,
                    iconTint = BrandRed
                ) {
                    InfoRow(
                        label = "当前缓存",
                        value = SettingsViewModel.formatCacheSize(state.cache.sizeBytes)
                    )
                    Spacer(Modifier.height(10.dp))
                    GradientButton(
                        text = "清理缓存",
                        onClick = onClearCache,
                        leadingIcon = Icons.Filled.Delete,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 底部安全区
            item {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }
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
}

// =============================================================================
// 子组件
// =============================================================================

/**
 * 设置分组卡片：标题 + 副标题 + 图标 + 自定义内容。
 */
@Composable
private fun SettingsGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = BrandRed,
    subtitleTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = subtitleTint
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * 联盟分组内子分组：组标题 + 内容。
 */
@Composable
private fun ProviderSubgroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        content()
    }
}

/**
 * 凭证项行：标签 + 当前状态 + 箭头。
 */
@Composable
private fun CredentialRow(
    label: String,
    masked: String,
    configured: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (configured) Icons.Filled.VerifiedUser else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (configured) SemanticSuccess else NeutralGray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (configured) "已配置：$masked" else "未配置，点击设置",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (configured) MaterialTheme.colorScheme.onSurfaceVariant else SemanticWarning
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 信息行：标签 + 值 + 可选尾部操作。
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(text = field.label, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

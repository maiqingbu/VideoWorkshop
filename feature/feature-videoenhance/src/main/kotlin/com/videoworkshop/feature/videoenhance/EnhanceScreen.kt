package com.videoworkshop.feature.videoenhance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.videoworkshop.core.designsystem.theme.BadgeShape
import com.videoworkshop.core.designsystem.theme.BrandRed
import com.videoworkshop.core.designsystem.theme.CardShape
import com.videoworkshop.core.designsystem.theme.NeutralGrayLight
import com.videoworkshop.core.designsystem.theme.SemanticSuccess
import com.videoworkshop.core.designsystem.theme.SemanticSuccessContainer
import com.videoworkshop.core.ui.components.GradientButton
import com.videoworkshop.core.ui.components.OutlineButton
import com.videoworkshop.core.ui.components.VWCard
import com.videoworkshop.core.ui.components.VWTopBar
import com.videoworkshop.domain.model.CopyResult
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.VoiceProfile
import java.io.File

/**
 * 带货包装页面入口。
 *
 * 合成完成后自动通过 [onCompleted] 导航至发布页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhanceScreen(
    onBack: () -> Unit,
    onCompleted: (outputPath: String) -> Unit,
    viewModel: EnhanceViewModel = hiltViewModel()
) {
    val goods by viewModel.goods.collectAsStateWithLifecycle()
    val copies by viewModel.copies.collectAsStateWithLifecycle()
    val selectedCopyIndex by viewModel.selectedCopyIndex.collectAsStateWithLifecycle()
    val selectedVoice by viewModel.selectedVoice.collectAsStateWithLifecycle()
    val subtitleEnabled by viewModel.subtitleEnabled.collectAsStateWithLifecycle()
    val subtitleStyle by viewModel.subtitleStyle.collectAsStateWithLifecycle()
    val selectedBgm by viewModel.selectedBgm.collectAsStateWithLifecycle()
    val selectedStickers by viewModel.selectedStickers.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val copyLoading by viewModel.copyLoading.collectAsStateWithLifecycle()
    val voiceGenerating by viewModel.voiceGenerating.collectAsStateWithLifecycle()
    val voicePath by viewModel.voicePath.collectAsStateWithLifecycle()
    val outputPath by viewModel.outputPath.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // 编辑文案对话框状态
    var showEditDialog by remember { mutableStateOf(false) }

    // 合成完成后自动导航
    LaunchedEffect(outputPath) {
        outputPath?.let { onCompleted(it) }
    }

    Scaffold(
        topBar = { VWTopBar(title = "带货包装", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            EnhanceBackgroundOverlay()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                VideoStatusCard(videoPath = viewModel.videoPath)

                Spacer(Modifier.height(16.dp))

                // 区域1 AI带货文案
                CopySection(
                    goods = goods,
                    copies = copies,
                    selectedIndex = selectedCopyIndex,
                    copyLoading = copyLoading,
                    onSelectCopy = viewModel::selectCopy,
                    onRegenerate = { goods?.let(viewModel::generateCopy) },
                    onEdit = { showEditDialog = true }
                )

                Spacer(Modifier.height(16.dp))

                // 区域2 配音
                VoiceSection(
                    selectedVoice = selectedVoice,
                    voiceGenerating = voiceGenerating,
                    voicePath = voicePath,
                    onSelectVoice = viewModel::selectVoice,
                    onGenerateVoice = viewModel::generateVoice
                )

                Spacer(Modifier.height(16.dp))

                // 区域3 添加字幕
                SubtitleSection(
                    subtitleEnabled = subtitleEnabled,
                    selectedStyle = subtitleStyle,
                    onToggleSubtitle = viewModel::toggleSubtitle,
                    onSelectStyle = viewModel::selectSubtitleStyle
                )

                Spacer(Modifier.height(16.dp))

                // 区域4 背景音乐
                BgmSection(
                    selectedBgm = selectedBgm,
                    onSelectBgm = viewModel::selectBgm
                )

                Spacer(Modifier.height(16.dp))

                // 区域5 带货贴纸
                StickerSection(
                    selectedStickers = selectedStickers,
                    onToggleSticker = viewModel::toggleSticker
                )

                Spacer(Modifier.height(24.dp))

                // 底部生成按钮
                GradientButton(
                    text = "生成带货视频",
                    onClick = viewModel::generateVideo,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Filled.AutoAwesome
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "AI 将根据所选配置合成最终带货视频",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // 处理中全屏蒙层
            AnimatedVisibility(
                visible = isProcessing,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                ProcessingOverlay(progress = progress)
            }

            // 错误提示（底部 Toast 样式）
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                ErrorToast(
                    message = errorMessage ?: "",
                    onDismiss = viewModel::clearError
                )
            }
        }
    }

    // 编辑文案对话框
    if (showEditDialog) {
        EditCopyDialog(
            copy = copies.getOrNull(selectedCopyIndex),
            onDismiss = { showEditDialog = false },
            onConfirm = { newBody ->
                viewModel.updateCopyBody(selectedCopyIndex, newBody)
                showEditDialog = false
            }
        )
    }
}

// =============================================================================
// 背景
// =============================================================================

/**
 * 顶部品牌红渐变光晕，营造电商活力氛围。
 */
@Composable
private fun EnhanceBackgroundOverlay() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BrandRed.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            )
    )
}

// =============================================================================
// 视频状态卡片
// =============================================================================

@Composable
private fun VideoStatusCard(videoPath: String) {
    VWCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 状态图标圆底
            Surface(
                color = SemanticSuccessContainer,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SemanticSuccess,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "去重完成",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SemanticSuccess
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = SemanticSuccess.copy(alpha = 0.12f),
                        shape = BadgeShape
                    ) {
                        Text(
                            text = "可包装",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = SemanticSuccess,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = File(videoPath).name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // 预览按钮
            Surface(
                color = BrandRed.copy(alpha = 0.10f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = BrandRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "预览",
                        color = BrandRed,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// =============================================================================
// 区域1 AI带货文案
// =============================================================================

@Composable
private fun CopySection(
    goods: Goods?,
    copies: List<CopyResult>,
    selectedIndex: Int,
    copyLoading: Boolean,
    onSelectCopy: (Int) -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit
) {
    SectionHeader(
        title = "AI带货文案",
        icon = Icons.Filled.AutoAwesome
    )
    Spacer(Modifier.height(10.dp))

    VWCard {
        // 商品信息小字
        if (goods != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = BrandRed.copy(alpha = 0.10f),
                    shape = BadgeShape
                ) {
                    Text(
                        text = "商品",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandRed,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${goods.name}  ·  ¥${goods.price}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // 文案展示区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(NeutralGrayLight.copy(alpha = 0.40f))
                .padding(14.dp)
        ) {
            if (copyLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = BrandRed
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AI 正在生成文案...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val current = copies.getOrNull(selectedIndex)
                Text(
                    text = current?.body ?: "暂无文案，点击重新生成",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 3版文案切换 Tab
        if (copies.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                copies.forEachIndexed { index, _ ->
                    CopyTab(
                        index = index,
                        selected = index == selectedIndex,
                        onClick = { onSelectCopy(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 重新生成 & 编辑 按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlineButton(
                text = "重新生成",
                onClick = onRegenerate,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Filled.Refresh
            )
            OutlineButton(
                text = "编辑",
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Filled.Edit
            )
        }
    }
}

@Composable
private fun CopyTab(
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) BrandRed else MaterialTheme.colorScheme.outlineVariant
    val background = if (selected) BrandRed.copy(alpha = 0.08f) else Color.Transparent
    val textColor = if (selected) BrandRed else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .height(34.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "版本 ${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// =============================================================================
// 区域2 配音
// =============================================================================

@Composable
private fun VoiceSection(
    selectedVoice: VoiceProfile?,
    voiceGenerating: Boolean,
    voicePath: String?,
    onSelectVoice: (VoiceProfile) -> Unit,
    onGenerateVoice: () -> Unit
) {
    SectionHeader(
        title = "配音（可选）",
        icon = Icons.Filled.GraphicEq
    )
    Spacer(Modifier.height(10.dp))

    VWCard {
        Text(
            text = "选择音色",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(10.dp))

        // 音色选择 Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VoiceProfile.entries.forEach { voice ->
                SelectableBadge(
                    label = voice.displayName,
                    selected = selectedVoice == voice,
                    onClick = { onSelectVoice(voice) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 生成配音按钮
        GradientButton(
            text = if (voiceGenerating) "生成中..." else "生成配音",
            onClick = onGenerateVoice,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedVoice != null && !voiceGenerating,
            loading = voiceGenerating,
            leadingIcon = Icons.Filled.GraphicEq
        )

        // 配音进度/结果
        if (voiceGenerating) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BrandRed,
                trackColor = NeutralGrayLight
            )
        } else if (voicePath != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SemanticSuccess,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "配音已生成",
                    style = MaterialTheme.typography.bodySmall,
                    color = SemanticSuccess,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SelectableBadge(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) BrandRed else MaterialTheme.colorScheme.outlineVariant
    val background = if (selected) BrandRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    val textColor = if (selected) BrandRed else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .height(36.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// =============================================================================
// 区域3 添加字幕
// =============================================================================

@Composable
private fun SubtitleSection(
    subtitleEnabled: Boolean,
    selectedStyle: SubtitleStyleOption,
    onToggleSubtitle: () -> Unit,
    onSelectStyle: (SubtitleStyleOption) -> Unit
) {
    SectionHeader(
        title = "添加字幕",
        icon = Icons.Filled.Subtitles
    )
    Spacer(Modifier.height(10.dp))

    VWCard {
        // 开关行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "自动生成字幕",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "根据文案自动生成并烧录字幕",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = subtitleEnabled,
                onCheckedChange = { onToggleSubtitle() }
            )
        }

        // 字幕样式选择
        AnimatedVisibility(visible = subtitleEnabled) {
            Column {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "字幕样式",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubtitleStyleOptions.forEach { option ->
                        SelectableBadge(
                            label = option.displayName,
                            selected = selectedStyle == option,
                            onClick = { onSelectStyle(option) }
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 区域4 背景音乐
// =============================================================================

@Composable
private fun BgmSection(
    selectedBgm: String?,
    onSelectBgm: (String) -> Unit
) {
    SectionHeader(
        title = "背景音乐",
        icon = Icons.Filled.MusicNote
    )
    Spacer(Modifier.height(10.dp))

    VWCard {
        Text(
            text = "内置音乐库",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(10.dp))

        // 横滚 Badge
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(BgmOptions) { option ->
                SelectableBadge(
                    label = option.displayName,
                    selected = selectedBgm == option.identifier,
                    onClick = { onSelectBgm(option.identifier) }
                )
            }
        }

        if (selectedBgm == null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "未选择则保留原音轨",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =============================================================================
// 区域5 带货贴纸
// =============================================================================

@Composable
private fun StickerSection(
    selectedStickers: List<String>,
    onToggleSticker: (String) -> Unit
) {
    SectionHeader(
        title = "带货贴纸",
        icon = Icons.Filled.AutoAwesome
    )
    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StickerOptions.forEach { option ->
            StickerCard(
                option = option,
                selected = option.identifier in selectedStickers,
                onClick = { onToggleSticker(option.identifier) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StickerCard(
    option: StickerOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) BrandRed else MaterialTheme.colorScheme.outlineVariant
    val background = if (selected) BrandRed.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = CardShape,
        color = background,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        shadowElevation = if (selected) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = option.emoji,
                fontSize = 28.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = option.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) BrandRed else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =============================================================================
// 处理中蒙层
// =============================================================================

@Composable
private fun ProcessingOverlay(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 圆形进度
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 5.dp,
                        color = BrandRed,
                        progress = { progress.coerceIn(0f, 1f) }
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandRed
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "正在合成...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "正在合成带货视频，请勿离开页面",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// =============================================================================
// 编辑文案对话框
// =============================================================================

@Composable
private fun EditCopyDialog(
    copy: CopyResult?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(copy?.body ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "编辑文案",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    placeholder = { Text("请输入文案内容") },
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(Modifier.width(8.dp))
                    GradientButton(
                        text = "保存",
                        onClick = { onConfirm(text) },
                        leadingIcon = Icons.Filled.Check
                    )
                }
            }
        }
    }
}

@Composable
private fun IconButton(onClick: () -> Unit) {
    Surface(
        color = NeutralGrayLight.copy(alpha = 0.5f),
        shape = CircleShape,
        modifier = Modifier
            .size(28.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =============================================================================
// 错误提示
// =============================================================================

@Composable
private fun ErrorToast(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.errorContainer,
            shadowElevation = 6.dp,
            modifier = Modifier.clickable(onClick = onDismiss)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// =============================================================================
// 通用组件
// =============================================================================

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = BrandRed.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

package com.videoworkshop.feature.dedup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.videoworkshop.core.common.VideoUtils
import com.videoworkshop.core.designsystem.theme.BadgeShape
import com.videoworkshop.core.designsystem.theme.BrandNavy
import com.videoworkshop.core.designsystem.theme.BrandRed
import com.videoworkshop.core.designsystem.theme.CardShape
import com.videoworkshop.core.designsystem.theme.NeutralGrayLight
import com.videoworkshop.core.designsystem.theme.SemanticError
import com.videoworkshop.core.designsystem.theme.SemanticErrorContainer
import com.videoworkshop.core.designsystem.theme.SemanticSuccess
import com.videoworkshop.core.designsystem.theme.SemanticSuccessContainer
import com.videoworkshop.core.ui.components.GradientButton
import com.videoworkshop.core.ui.components.OutlineButton
import com.videoworkshop.core.ui.components.VWCard
import com.videoworkshop.core.ui.components.VWTopBar
import com.videoworkshop.domain.model.DedupConfig
import com.videoworkshop.domain.model.DedupProgress
import com.videoworkshop.domain.model.DedupStrength
import com.videoworkshop.domain.model.VideoClip
import java.io.File

/**
 * 去重处理页面入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DedupScreen(
    onBack: () -> Unit,
    onNext: (outputPath: String) -> Unit,
    viewModel: DedupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val videoInfo by viewModel.videoInfo.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { VWTopBar(title = "去重处理", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DedupBackgroundOverlay()

            when (val state = uiState) {
                is DedupUiState.Idle -> IdleContent(
                    videoPath = viewModel.videoPath,
                    videoInfo = videoInfo,
                    config = config,
                    mode = mode,
                    onChangeVideo = onBack,
                    onModeSelected = viewModel::setMode,
                    onToggleItem = viewModel::toggleItem,
                    onStrengthSelected = viewModel::setStrength,
                    onStart = { viewModel.startDedup(viewModel.videoPath) }
                )

                is DedupUiState.Processing -> ProcessingContent(
                    videoPath = viewModel.videoPath,
                    videoInfo = videoInfo,
                    progress = progress
                )

                is DedupUiState.Completed -> CompletedContent(
                    outputPath = state.outputPath,
                    videoInfo = videoInfo,
                    onNext = { onNext(state.outputPath) },
                    onReset = viewModel::reset
                )

                is DedupUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.startDedup(viewModel.videoPath) },
                    onReset = viewModel::reset
                )
            }
        }
    }
}

// =============================================================================
// 背景
// =============================================================================

/**
 * 顶部品牌红渐变光晕，营造电商活力氛围。
 */
@Composable
private fun DedupBackgroundOverlay() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
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
// 空闲态
// =============================================================================

@Composable
private fun IdleContent(
    videoPath: String,
    videoInfo: VideoClip?,
    config: DedupConfig,
    mode: DedupMode,
    onChangeVideo: () -> Unit,
    onModeSelected: (DedupMode) -> Unit,
    onToggleItem: (Int) -> Unit,
    onStrengthSelected: (DedupStrength) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp)
    ) {
        VideoInfoCard(
            videoPath = videoPath,
            videoInfo = videoInfo,
            onChange = onChangeVideo
        )
        Spacer(Modifier.height(16.dp))

        SectionTitle("去重模式")
        Spacer(Modifier.height(10.dp))
        ModeSelector(mode = mode, onSelect = onModeSelected)
        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(
            visible = mode == DedupMode.CUSTOM,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                SectionTitle("去重项目")
                Spacer(Modifier.height(10.dp))
                DedupItemsList(config = config, onToggle = onToggleItem)
                Spacer(Modifier.height(16.dp))
            }
        }

        SectionTitle("去重强度")
        Spacer(Modifier.height(10.dp))
        StrengthSelector(strength = config.strength, onSelect = onStrengthSelected)
        Spacer(Modifier.height(28.dp))

        GradientButton(
            text = "开始去重",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Filled.AutoAwesome
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "共 8 项去重算法，预计耗时约 30 秒",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// =============================================================================
// 视频信息卡片
// =============================================================================

@Composable
private fun VideoInfoCard(
    videoPath: String,
    videoInfo: VideoClip?,
    onChange: () -> Unit
) {
    VWCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 缩略图渐变占位
            Box(
                modifier = Modifier
                    .size(width = 92.dp, height = 64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(BrandRed, BrandNavy)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = File(videoPath).name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetadataChip(
                        text = videoInfo?.let { VideoUtils.formatDuration(it.duration) } ?: "--:--"
                    )
                    MetadataChip(
                        text = videoInfo?.let { "${it.width}x${it.height}" } ?: "—"
                    )
                    MetadataChip(
                        text = videoInfo?.let { VideoUtils.formatFileSize(it.size) } ?: "—"
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            CompactChangeButton(onChange = onChange)
        }
    }
}

@Composable
private fun MetadataChip(text: String) {
    Surface(
        color = NeutralGrayLight.copy(alpha = 0.6f),
        shape = BadgeShape
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompactChangeButton(onChange: () -> Unit) {
    Surface(
        color = BrandRed.copy(alpha = 0.10f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onChange)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = null,
                tint = BrandRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "更换",
                color = BrandRed,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// =============================================================================
// 模式选择
// =============================================================================

@Composable
private fun ModeSelector(mode: DedupMode, onSelect: (DedupMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ModeCard(
            modifier = Modifier.weight(1f),
            title = "一键去重",
            subtitle = "8项全自动",
            icon = Icons.Filled.AutoAwesome,
            selected = mode == DedupMode.QUICK,
            recommended = true,
            onClick = { onSelect(DedupMode.QUICK) }
        )
        ModeCard(
            modifier = Modifier.weight(1f),
            title = "自定义",
            subtitle = "自由组合",
            icon = Icons.Filled.Tune,
            selected = mode == DedupMode.CUSTOM,
            recommended = false,
            onClick = { onSelect(DedupMode.CUSTOM) }
        )
    }
}

@Composable
private fun ModeCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    recommended: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) BrandRed else MaterialTheme.colorScheme.outlineVariant
    val background = if (selected) BrandRed.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
    Surface(
        modifier = modifier
            .height(118.dp)
            .clickable(onClick = onClick),
        shape = CardShape,
        color = background,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        shadowElevation = if (selected) 4.dp else 1.dp
    ) {
        Box {
            if (recommended) {
                Surface(
                    color = BrandRed,
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "推荐",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    color = if (selected) BrandRed.copy(alpha = 0.14f) else NeutralGrayLight.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) BrandRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) BrandRed else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =============================================================================
// 去重项目列表
// =============================================================================

private data class DedupItemDef(
    val index: Int,
    val title: String,
    val icon: ImageVector,
    val locked: Boolean,
    val description: String
)

private val DedupItemDefs: List<DedupItemDef> = listOf(
    DedupItemDef(0, "MD5指纹修改", Icons.Filled.Fingerprint, locked = true, description = "重封装，必选项"),
    DedupItemDef(1, "帧率调整", Icons.Filled.Speed, locked = false, description = "微调视频帧率"),
    DedupItemDef(2, "码率修改", Icons.Filled.HighQuality, locked = false, description = "调整编码码率"),
    DedupItemDef(3, "画面裁剪", Icons.Filled.Crop, locked = false, description = "边缘像素裁切"),
    DedupItemDef(4, "镜像翻转", Icons.Filled.Flip, locked = false, description = "水平/垂直翻转"),
    DedupItemDef(5, "色彩偏移", Icons.Filled.Palette, locked = false, description = "色相饱和度偏移"),
    DedupItemDef(6, "音频重组", Icons.Filled.GraphicEq, locked = false, description = "重塑音轨结构"),
    DedupItemDef(7, "元数据清洗", Icons.Filled.CleaningServices, locked = true, description = "清理元信息，必选")
)

@Composable
private fun DedupItemsList(config: DedupConfig, onToggle: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DedupItemDefs.forEach { def ->
            DedupItemRow(
                def = def,
                checked = config.isChecked(def.index),
                onToggle = { onToggle(def.index) }
            )
        }
    }
}

@Composable
private fun DedupItemRow(
    def: DedupItemDef,
    checked: Boolean,
    onToggle: () -> Unit
) {
    VWCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 图标底色圆
            Surface(
                color = if (def.locked) NeutralGrayLight.copy(alpha = 0.7f) else BrandRed.copy(alpha = 0.10f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = def.icon,
                        contentDescription = null,
                        tint = if (def.locked) MaterialTheme.colorScheme.onSurfaceVariant else BrandRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = def.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (def.locked) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = NeutralGrayLight.copy(alpha = 0.7f),
                            shape = BadgeShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "必选",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Text(
                    text = def.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                enabled = !def.locked
            )
        }
    }
}

// =============================================================================
// 强度选择
// =============================================================================

@Composable
private fun StrengthSelector(strength: DedupStrength, onSelect: (DedupStrength) -> Unit) {
    VWCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择去重强度档位",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = strength.label(),
                color = BrandRed,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = strength.sliderValue(),
            onValueChange = { onSelect(it.toStrength()) },
            valueRange = 0f..2f,
            steps = 1
        )
        Spacer(Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StrengthLabel(
                label = "轻度",
                active = strength == DedupStrength.LIGHT,
                onClick = { onSelect(DedupStrength.LIGHT) },
                modifier = Modifier.weight(1f)
            )
            StrengthLabel(
                label = "标准",
                active = strength == DedupStrength.STANDARD,
                onClick = { onSelect(DedupStrength.STANDARD) },
                modifier = Modifier.weight(1f)
            )
            StrengthLabel(
                label = "深度",
                active = strength == DedupStrength.DEEP,
                onClick = { onSelect(DedupStrength.DEEP) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StrengthLabel(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = if (active) BrandRed else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = modifier.clickable(onClick = onClick)
    )
}

// =============================================================================
// 处理中
// =============================================================================

@Composable
private fun ProcessingContent(
    videoPath: String,
    videoInfo: VideoClip?,
    progress: DedupProgress?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp)
    ) {
        VideoInfoCard(
            videoPath = videoPath,
            videoInfo = videoInfo,
            onChange = { }
        )
        Spacer(Modifier.height(20.dp))
        ProcessingCard(progress = progress)
    }
}

@Composable
private fun ProcessingCard(progress: DedupProgress?) {
    VWCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
                color = BrandRed
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "去重处理中",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (progress != null) {
                        "${progress.currentStep}  ${progress.stepIndex + 1}/${progress.totalSteps}"
                    } else {
                        "准备中"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${((progress?.progress ?: 0f) * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BrandRed
            )
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { (progress?.progress ?: 0f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = BrandRed,
            trackColor = NeutralGrayLight
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "正在执行 8 项去重算法，请勿离开页面",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =============================================================================
// 完成
// =============================================================================

@Composable
private fun CompletedContent(
    outputPath: String,
    videoInfo: VideoClip?,
    onNext: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Surface(
            color = SemanticSuccessContainer,
            shape = CircleShape,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SemanticSuccess,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "去重完成",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "已成功生成去重视频，可继续进行带货包装",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        VWCard {
            CompletedFileInfoRow(label = "文件名", value = File(outputPath).name)
            CompletedFileInfoRow(
                label = "时长",
                value = videoInfo?.let { VideoUtils.formatDuration(it.duration) } ?: "—"
            )
            CompletedFileInfoRow(
                label = "分辨率",
                value = videoInfo?.let { "${it.width}x${it.height}" } ?: "—"
            )
            CompletedFileInfoRow(
                label = "大小",
                value = videoInfo?.let { VideoUtils.formatFileSize(it.size) } ?: "—"
            )
        }

        Spacer(Modifier.height(28.dp))
        GradientButton(
            text = "下一步",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Filled.ArrowForward
        )
        Spacer(Modifier.height(12.dp))
        OutlineButton(
            text = "再处理一个",
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Filled.Refresh
        )
    }
}

@Composable
private fun CompletedFileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).padding(start = 16.dp)
        )
    }
}

// =============================================================================
// 错误
// =============================================================================

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = SemanticErrorContainer,
            shape = CircleShape,
            modifier = Modifier.size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = SemanticError,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "去重失败",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        GradientButton(
            text = "重试",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Filled.Refresh
        )
        Spacer(Modifier.height(12.dp))
        OutlineButton(
            text = "返回修改",
            onClick = onReset,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// =============================================================================
// 通用组件 & 扩展
// =============================================================================

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

private fun DedupConfig.isChecked(index: Int): Boolean = when (index) {
    0 -> md5Modify
    1 -> fpsAdjust
    2 -> bitrateModify
    3 -> cropTransform
    4 -> mirrorFlip
    5 -> colorShift
    6 -> audioReshape
    7 -> metadataClean
    else -> false
}

private fun DedupStrength.label(): String = when (this) {
    DedupStrength.LIGHT -> "轻度"
    DedupStrength.STANDARD -> "标准"
    DedupStrength.DEEP -> "深度"
}

private fun DedupStrength.sliderValue(): Float = when (this) {
    DedupStrength.LIGHT -> 0f
    DedupStrength.STANDARD -> 1f
    DedupStrength.DEEP -> 2f
}

private fun Float.toStrength(): DedupStrength = when {
    this < 0.5f -> DedupStrength.LIGHT
    this < 1.5f -> DedupStrength.STANDARD
    else -> DedupStrength.DEEP
}

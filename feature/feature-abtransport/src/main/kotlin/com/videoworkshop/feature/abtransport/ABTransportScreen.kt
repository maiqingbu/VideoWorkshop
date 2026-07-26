package com.videoworkshop.feature.abtransport

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.videoworkshop.core.common.VideoUtils
import com.videoworkshop.core.designsystem.theme.BadgeShape
import com.videoworkshop.core.designsystem.theme.BrandNavy
import com.videoworkshop.core.designsystem.theme.BrandRed
import com.videoworkshop.core.designsystem.theme.CardShape
import com.videoworkshop.core.designsystem.theme.NeutralGrayLight
import com.videoworkshop.core.designsystem.theme.SemanticError
import com.videoworkshop.core.designsystem.theme.SemanticErrorContainer
import com.videoworkshop.core.designsystem.theme.SemanticInfo
import com.videoworkshop.core.designsystem.theme.SemanticSuccess
import com.videoworkshop.core.designsystem.theme.SemanticSuccessContainer
import com.videoworkshop.core.designsystem.theme.SemanticWarning
import com.videoworkshop.core.ui.components.GradientButton
import com.videoworkshop.core.ui.components.OutlineButton
import com.videoworkshop.core.ui.components.VWCard
import com.videoworkshop.core.ui.components.VWTopBar
import com.videoworkshop.domain.model.ABTransportMode
import com.videoworkshop.domain.model.DurationStrategy
import com.videoworkshop.domain.model.VideoClip
import java.io.File

/**
 * AB 搬运页面入口。
 *
 * 按 [ABTransportUiState] 切换不同内容：
 * - [ABTransportUiState.Idle]：A/B 选择 + 合成配置 + 关键帧预览 + 开始合成按钮
 * - [ABTransportUiState.Processing]：进度条 + 取消按钮
 * - [ABTransportUiState.Completed]：结果页（预览产物 + 去重 / AI 包装两个入口）
 * - [ABTransportUiState.Error]：错误信息 + 重试
 *
 * 关键规则（spec 已确认）：
 * - A/B 视频**仅从素材库选择**：通过 [MaterialPickerDialog] 弹窗选择
 * - 合成完成后**自动保存产物到素材库**，结果页不显示「保存」按钮
 * - 结果页提供「去重」「AI 包装」两个跳转入口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ABTransportScreen(
    onBack: () -> Unit,
    onDedup: (String) -> Unit,
    onEnhance: (String) -> Unit,
    viewModel: ABTransportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val videoAPath by viewModel.videoAPath.collectAsStateWithLifecycle()
    val videoBPath by viewModel.videoBPath.collectAsStateWithLifecycle()
    val videoAInfo by viewModel.videoAInfo.collectAsStateWithLifecycle()
    val videoBInfo by viewModel.videoBInfo.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val durationStrategy by viewModel.durationStrategy.collectAsStateWithLifecycle()
    val volumeRatioA by viewModel.volumeRatioA.collectAsStateWithLifecycle()
    val volumeRatioB by viewModel.volumeRatioB.collectAsStateWithLifecycle()
    val keyframesA by viewModel.keyframesA.collectAsStateWithLifecycle()
    val keyframesB by viewModel.keyframesB.collectAsStateWithLifecycle()
    val audioMissing by viewModel.audioMissing.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // 错误提示（一次性消费，渲染到最上层）
    LaunchedEffect(error) {
        val msg = error
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeError()
        }
    }

    // A 视频无音轨提示（渲染到最上层，避免被弹窗阻挡）
    LaunchedEffect(audioMissing) {
        if (audioMissing) {
            snackbarHostState.showSnackbar("A 视频无音轨，无法作为音频源")
        }
    }

    // 自动入库结果提示
    LaunchedEffect(isSaved, uiState) {
        if (isSaved && uiState is ABTransportUiState.Completed) {
            snackbarHostState.showSnackbar("产物已自动保存到素材库")
        }
    }

    // 选择器状态：null / "A" / "B"
    var pickerTarget by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { VWTopBar(title = "AB 搬运", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ABTransportBackgroundOverlay()

            when (val state = uiState) {
                is ABTransportUiState.Idle -> IdleContent(
                    videoAPath = videoAPath,
                    videoBPath = videoBPath,
                    videoAInfo = videoAInfo,
                    videoBInfo = videoBInfo,
                    mode = mode,
                    durationStrategy = durationStrategy,
                    volumeRatioA = volumeRatioA,
                    volumeRatioB = volumeRatioB,
                    keyframesA = keyframesA,
                    keyframesB = keyframesB,
                    audioMissing = audioMissing,
                    onPickA = { pickerTarget = "A" },
                    onPickB = { pickerTarget = "B" },
                    onClearA = viewModel::clearVideoA,
                    onClearB = viewModel::clearVideoB,
                    onModeSelected = viewModel::setMode,
                    onDurationStrategySelected = viewModel::setDurationStrategy,
                    onVolumeRatioAChanged = viewModel::setVolumeRatioA,
                    onVolumeRatioBChanged = viewModel::setVolumeRatioB,
                    onStart = viewModel::startSynthesis
                )

                is ABTransportUiState.Processing -> ProcessingContent(
                    progressPercent = state.progressPercent,
                    currentMs = state.currentMs,
                    totalMs = state.totalMs,
                    onCancel = viewModel::cancelSynthesis
                )

                is ABTransportUiState.Completed -> CompletedContent(
                    outputPath = state.outputPath,
                    isSaved = isSaved,
                    onDedup = { onDedup(state.outputPath) },
                    onEnhance = { onEnhance(state.outputPath) },
                    onReset = viewModel::reset
                )

                is ABTransportUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = viewModel::startSynthesis,
                    onReset = viewModel::reset
                )
            }

            // 素材选择器（最上层弹窗）
            pickerTarget?.let { target ->
                MaterialPickerDialog(
                    title = if (target == "A") "选择 A 视频（音频源）" else "选择 B 视频（画面源）",
                    loadMaterials = { viewModel.loadVideoMaterials() },
                    onDismiss = { pickerTarget = null },
                    onSelected = { path ->
                        if (target == "A") viewModel.selectVideoA(path)
                        else viewModel.selectVideoB(path)
                        pickerTarget = null
                    }
                )
            }
        }
    }
}

// =============================================================================
// 背景光晕
// =============================================================================

@Composable
private fun ABTransportBackgroundOverlay() {
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
    videoAPath: String?,
    videoBPath: String?,
    videoAInfo: VideoClip?,
    videoBInfo: VideoClip?,
    mode: ABTransportMode,
    durationStrategy: DurationStrategy,
    volumeRatioA: Float,
    volumeRatioB: Float,
    keyframesA: List<String>,
    keyframesB: List<String>,
    audioMissing: Boolean,
    onPickA: () -> Unit,
    onPickB: () -> Unit,
    onClearA: () -> Unit,
    onClearB: () -> Unit,
    onModeSelected: (ABTransportMode) -> Unit,
    onDurationStrategySelected: (DurationStrategy) -> Unit,
    onVolumeRatioAChanged: (Float) -> Unit,
    onVolumeRatioBChanged: (Float) -> Unit,
    onStart: () -> Unit
) {
    val canStart = !videoAPath.isNullOrBlank() && !videoBPath.isNullOrBlank() && !audioMissing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp)
    ) {
        // 规则提示
        RulesBanner()
        Spacer(Modifier.height(12.dp))

        // A/B 视频选择卡片
        SectionTitle("选择视频")
        Spacer(Modifier.height(10.dp))
        VideoSelectionRow(
            videoAPath = videoAPath,
            videoBPath = videoBPath,
            videoAInfo = videoAInfo,
            videoBInfo = videoBInfo,
            audioMissing = audioMissing,
            onPickA = onPickA,
            onPickB = onPickB,
            onClearA = onClearA,
            onClearB = onClearB
        )
        Spacer(Modifier.height(16.dp))

        // 关键帧预览
        if (keyframesA.isNotEmpty() || keyframesB.isNotEmpty()) {
            SectionTitle("关键帧预览")
            Spacer(Modifier.height(10.dp))
            KeyframePreviewSection(
                keyframesA = keyframesA,
                keyframesB = keyframesB
            )
            Spacer(Modifier.height(16.dp))
        }

        // 合成模式
        SectionTitle("合成模式")
        Spacer(Modifier.height(10.dp))
        ModeSelector(mode = mode, onSelect = onModeSelected)
        Spacer(Modifier.height(16.dp))

        // 音量比例（仅 MIX 模式可见）
        AnimatedVisibility(
            visible = mode == ABTransportMode.MIX,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                SectionTitle("音量比例")
                Spacer(Modifier.height(10.dp))
                VolumeRatioSection(
                    volumeRatioA = volumeRatioA,
                    volumeRatioB = volumeRatioB,
                    onRatioAChanged = onVolumeRatioAChanged,
                    onRatioBChanged = onVolumeRatioBChanged
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        // 时长策略
        SectionTitle("时长对齐策略")
        Spacer(Modifier.height(10.dp))
        DurationStrategySelector(strategy = durationStrategy, onSelect = onDurationStrategySelected)
        Spacer(Modifier.height(28.dp))

        // 开始合成按钮
        GradientButton(
            text = "开始合成",
            onClick = onStart,
            enabled = canStart,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Filled.AutoAwesome
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = when {
                videoAPath.isNullOrBlank() || videoBPath.isNullOrBlank() ->
                    "请先选择 A/B 视频"
                audioMissing ->
                    "A 视频无音轨，无法作为音频源"
                else ->
                    "A 视频作为音频源，B 视频作为画面源，合成后自动保存到素材库"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (canStart) MaterialTheme.colorScheme.onSurfaceVariant else SemanticWarning,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// =============================================================================
// 规则提示横幅
// =============================================================================

@Composable
private fun RulesBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SemanticInfo.copy(alpha = 0.08f),
        shape = CardShape
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = SemanticInfo,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "A 视频提供音频 · B 视频提供画面",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "A/B 仅可从素材库选择，合成产物自动入库",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =============================================================================
// 视频选择卡片
// =============================================================================

@Composable
private fun VideoSelectionRow(
    videoAPath: String?,
    videoBPath: String?,
    videoAInfo: VideoClip?,
    videoBInfo: VideoClip?,
    audioMissing: Boolean,
    onPickA: () -> Unit,
    onPickB: () -> Unit,
    onClearA: () -> Unit,
    onClearB: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        VideoSlotCard(
            modifier = Modifier.weight(1f),
            label = "A 视频",
            subLabel = "音频源",
            icon = Icons.Filled.MusicNote,
            videoPath = videoAPath,
            videoInfo = videoAInfo,
            audioMissing = audioMissing,
            onPick = onPickA,
            onClear = onClearA
        )
        VideoSlotCard(
            modifier = Modifier.weight(1f),
            label = "B 视频",
            subLabel = "画面源",
            icon = Icons.Filled.Movie,
            videoPath = videoBPath,
            videoInfo = videoBInfo,
            audioMissing = false,
            onPick = onPickB,
            onClear = onClearB
        )
    }
}

@Composable
private fun VideoSlotCard(
    modifier: Modifier = Modifier,
    label: String,
    subLabel: String,
    icon: ImageVector,
    videoPath: String?,
    videoInfo: VideoClip?,
    audioMissing: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(180.dp)
            .clickable(enabled = videoPath == null, onClick = onPick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (videoPath != null) 2.dp else 1.dp,
            color = when {
                videoPath != null && audioMissing -> SemanticError
                videoPath != null -> BrandRed
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        shadowElevation = if (videoPath != null) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (videoPath == null) {
                // 未选择态
                Surface(
                    color = BrandRed.copy(alpha = 0.10f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = BrandRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = BrandRed.copy(alpha = 0.08f),
                    shape = BadgeShape
                ) {
                    Text(
                        text = "从素材库选择",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // 已选择态：缩略图 + 文件名 + 时长 + 更换/清除
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
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
                Spacer(Modifier.height(8.dp))
                Text(
                    text = File(videoPath).name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = videoInfo?.let { VideoUtils.formatDuration(it.duration) } ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (audioMissing) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "无音轨",
                        style = MaterialTheme.typography.labelSmall,
                        color = SemanticError,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = BrandRed.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable(onClick = onPick)
                    ) {
                        Text(
                            text = "更换",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Surface(
                        color = NeutralGrayLight.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable(onClick = onClear)
                    ) {
                        Text(
                            text = "清除",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 关键帧预览
// =============================================================================

@Composable
private fun KeyframePreviewSection(
    keyframesA: List<String>,
    keyframesB: List<String>
) {
    VWCard {
        if (keyframesA.isNotEmpty()) {
            KeyframeRow(label = "A 关键帧", frames = keyframesA)
        }
        if (keyframesA.isNotEmpty() && keyframesB.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
        }
        if (keyframesB.isNotEmpty()) {
            KeyframeRow(label = "B 关键帧", frames = keyframesB)
        }
    }
}

@Composable
private fun KeyframeRow(label: String, frames: List<String>) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(frames, key = { it }) { framePath ->
                KeyframeItem(framePath = framePath)
            }
        }
    }
}

@Composable
private fun KeyframeItem(framePath: String) {
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 54.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(NeutralGrayLight),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(framePath))
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // 加载失败时背景灰底自然透出
    }
}

// =============================================================================
// 合成模式选择
// =============================================================================

@Composable
private fun ModeSelector(mode: ABTransportMode, onSelect: (ABTransportMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ModeCard(
            modifier = Modifier.weight(1f),
            title = "纯替换",
            subtitle = "A 音轨 + B 画面",
            icon = Icons.Filled.SwapHoriz,
            selected = mode == ABTransportMode.PURE_REPLACE,
            recommended = true,
            onClick = { onSelect(ABTransportMode.PURE_REPLACE) }
        )
        ModeCard(
            modifier = Modifier.weight(1f),
            title = "混合",
            subtitle = "A + B 音轨混音",
            icon = Icons.Filled.GraphicEq,
            selected = mode == ABTransportMode.MIX,
            recommended = false,
            onClick = { onSelect(ABTransportMode.MIX) }
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
            .height(110.dp)
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
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    color = if (selected) BrandRed.copy(alpha = 0.14f) else NeutralGrayLight.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) BrandRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
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
// 音量比例
// =============================================================================

@Composable
private fun VolumeRatioSection(
    volumeRatioA: Float,
    volumeRatioB: Float,
    onRatioAChanged: (Float) -> Unit,
    onRatioBChanged: (Float) -> Unit
) {
    VWCard {
        VolumeSlider(
            label = "A 视频音量",
            icon = Icons.Filled.MusicNote,
            ratio = volumeRatioA,
            onRatioChanged = onRatioAChanged
        )
        Spacer(Modifier.height(16.dp))
        VolumeSlider(
            label = "B 视频原声音量",
            icon = Icons.Filled.Audiotrack,
            ratio = volumeRatioB,
            onRatioChanged = onRatioBChanged
        )
    }
}

@Composable
private fun VolumeSlider(
    label: String,
    icon: ImageVector,
    ratio: Float,
    onRatioChanged: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(ratio * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                color = BrandRed,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = ratio,
            onValueChange = onRatioChanged,
            valueRange = 0f..1f
        )
    }
}

// =============================================================================
// 时长策略
// =============================================================================

@Composable
private fun DurationStrategySelector(strategy: DurationStrategy, onSelect: (DurationStrategy) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DurationStrategyCard(
            modifier = Modifier.weight(1f),
            title = "截断",
            subtitle = "取较短时长",
            icon = Icons.Filled.ContentCut,
            selected = strategy == DurationStrategy.TRUNCATE,
            onClick = { onSelect(DurationStrategy.TRUNCATE) }
        )
        DurationStrategyCard(
            modifier = Modifier.weight(1f),
            title = "循环",
            subtitle = "补齐较长时长",
            icon = Icons.Filled.Loop,
            selected = strategy == DurationStrategy.LOOP,
            onClick = { onSelect(DurationStrategy.LOOP) }
        )
        DurationStrategyCard(
            modifier = Modifier.weight(1f),
            title = "自定义",
            subtitle = "指定起止点",
            icon = Icons.Filled.Tune,
            selected = strategy == DurationStrategy.CUSTOM,
            onClick = { onSelect(DurationStrategy.CUSTOM) }
        )
    }
}

@Composable
private fun DurationStrategyCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) BrandRed else MaterialTheme.colorScheme.outlineVariant
    val background = if (selected) BrandRed.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
    Surface(
        modifier = modifier
            .height(96.dp)
            .clickable(onClick = onClick),
        shape = CardShape,
        color = background,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        shadowElevation = if (selected) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) BrandRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) BrandRed else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =============================================================================
// 处理中
// =============================================================================

@Composable
private fun ProcessingContent(
    progressPercent: Float,
    currentMs: Long,
    totalMs: Long,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 32.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 5.dp,
            color = BrandRed
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "正在合成 AB 搬运视频",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${VideoUtils.formatDuration(currentMs)} / ${VideoUtils.formatDuration(totalMs)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        LinearProgressIndicator(
            progress = { progressPercent.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = BrandRed,
            trackColor = NeutralGrayLight
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${(progressPercent * 100).toInt()}%",
            style = MaterialTheme.typography.titleLarge,
            color = BrandRed,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(28.dp))
        OutlineButton(
            text = "取消合成",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Filled.Close
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "取消将立即停止 FFmpeg 进程",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// =============================================================================
// 完成态（结果页）
// =============================================================================

@Composable
private fun CompletedContent(
    outputPath: String,
    isSaved: Boolean,
    onDedup: () -> Unit,
    onEnhance: () -> Unit,
    onReset: () -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Surface(
            color = SemanticSuccessContainer,
            shape = CircleShape,
            modifier = Modifier.size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SemanticSuccess,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "AB 搬运完成",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isSaved) "产物已自动保存到素材库" else "合成完成，自动入库失败",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSaved) MaterialTheme.colorScheme.onSurfaceVariant else SemanticWarning
        )
        Spacer(Modifier.height(20.dp))

        // 产物预览（点击可播放）
        VWCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.778f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(BrandRed, BrandNavy)
                        )
                    )
                    .clickable { showPreview = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(56.dp)
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "点击播放",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = File(outputPath).name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(28.dp))

        // 两个跳转入口
        SectionTitle("继续处理")
        Spacer(Modifier.height(10.dp))
        GradientButton(
            text = "去重",
            onClick = onDedup,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Filled.CleaningServices
        )
        Spacer(Modifier.height(12.dp))
        GradientButton(
            text = "AI 包装",
            onClick = onEnhance,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Filled.AutoAwesome
        )
        Spacer(Modifier.height(20.dp))
        OutlineButton(
            text = "再处理一个",
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Filled.Refresh
        )
    }

    // 视频预览对话框（最上层）
    if (showPreview) {
        ABTransportVideoPreviewDialog(
            videoPath = outputPath,
            onDismiss = { showPreview = false }
        )
    }
}

// =============================================================================
// 错误态
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
            text = "合成失败",
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
// 通用组件
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

// =============================================================================
// 视频预览对话框（结果页使用）
// =============================================================================

/**
 * 视频预览对话框：基于 ExoPlayer 全屏播放合成产物。
 *
 * 关闭时通过 [DisposableEffect] 自动释放 [ExoPlayer]，避免内存泄漏。
 *
 * @param videoPath 视频本地路径
 * @param onDismiss 关闭回调
 */
@Composable
private fun ABTransportVideoPreviewDialog(
    videoPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(videoPath)))
            prepare()
            playWhenReady = true
        }
    }

    androidx.compose.runtime.DisposableEffect(videoPath) {
        onDispose {
            exoPlayer.release()
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .aspectRatio(1.778f),
                factory = { ctx ->
                    androidx.media3.ui.PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        controllerAutoShow = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                }
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }

            Text(
                text = videoPath.substringAfterLast('/').ifBlank { "AB 搬运产物" },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

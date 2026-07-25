package com.videoworkshop.feature.publish

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.videoworkshop.core.designsystem.theme.BrandRed
import com.videoworkshop.core.designsystem.theme.BrandRedLight
import com.videoworkshop.core.designsystem.theme.SemanticInfo
import com.videoworkshop.core.designsystem.theme.SemanticSuccess
import com.videoworkshop.core.ui.components.GradientButton
import com.videoworkshop.core.ui.components.OutlineButton
import com.videoworkshop.core.ui.components.VWCard
import com.videoworkshop.core.ui.components.VWTopBar
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.PublishTarget

/**
 * 发布流程主界面。
 *
 * 依次展示内容预览、关联商品、发布设置（标题/标签/封面）、平台选择与发布入口，
 * 发布完成后弹出底部引导 Sheet。整体采用电商活力红 (#E94560) 主题。
 *
 * @param type     内容形式（video / image）
 * @param filePath 文件路径或合成内容标识
 * @param goodsId  关联商品 ID
 * @param onBack   返回上一级
 * @param onDone   发布流程结束（如关闭引导后）返回
 * @param viewModel 发布 ViewModel
 */
@Composable
fun PublishScreen(
    type: String,
    filePath: String,
    goodsId: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: PublishViewModel = hiltViewModel()
) {
    val contentInfo by viewModel.contentInfo.collectAsStateWithLifecycle()
    val goods by viewModel.goods.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val coverReady by viewModel.coverReady.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedPlatform.collectAsStateWithLifecycle()
    val isPublishing by viewModel.isPublishing.collectAsStateWithLifecycle()
    val showGuide by viewModel.showGuide.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(type, filePath, goodsId) {
        viewModel.load(type, filePath, goodsId)
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = { VWTopBar(title = "发布", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            // 内容预览卡片
            ContentPreviewCard(
                info = contentInfo,
                goods = goods,
                onPreview = { /* 预览入口占位 */ }
            )

            // 区域1 关联商品
            SectionHeader(title = "关联商品")
            GoodsCard(goods = goods)

            // 区域2 发布设置
            SectionHeader(title = "发布设置")
            PublishSettingsCard(
                title = title,
                onTitleChange = viewModel::updateTitle,
                tags = tags,
                coverReady = coverReady,
                coverUrl = goods?.imageUrl
            )

            // 区域3 选择平台
            SectionHeader(title = "选择平台", hint = "三选一")
            PlatformSelector(
                selected = selectedPlatform,
                onSelect = viewModel::selectPlatform
            )

            // 发布按钮
            Spacer(Modifier.height(2.dp))
            GradientButton(
                text = publishButtonText(selectedPlatform),
                onClick = viewModel::publish,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedPlatform != null,
                loading = isPublishing,
                leadingIcon = Icons.Filled.PlayArrow
            )

            // 底部提示
            BottomPublishHint(platform = selectedPlatform)

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showGuide) {
        PublishGuideSheet(
            platform = selectedPlatform,
            onDismiss = {
                viewModel.dismissGuide()
                onDone()
            }
        )
    }
}

// =============================================================================
// 内容预览卡片
// =============================================================================

@Composable
private fun ContentPreviewCard(
    info: ContentInfo?,
    goods: Goods?,
    onPreview: () -> Unit
) {
    VWCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 状态图标 / 缩略图
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(SemanticSuccess, SemanticSuccess.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (info?.isVideo == true) "带货视频已生成" else "图文内容已生成",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = info?.displayName ?: "正在准备内容…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlineButton(
                text = "预览",
                onClick = onPreview,
                leadingIcon = Icons.Filled.PlayArrow,
                modifier = Modifier.width(92.dp)
            )
        }
    }
}

// =============================================================================
// 区域1：关联商品卡片
// =============================================================================

@Composable
private fun GoodsCard(goods: Goods?) {
    VWCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandRed.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (goods?.imageUrl != null) {
                        AsyncImage(
                            model = goods.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = BrandRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goods?.name ?: "正在加载商品…",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "¥${goods?.price?.let { "%.2f".format(it) } ?: "--"}",
                            color = BrandRed,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "佣金 ${goods?.let { (it.commissionRate * 100).toInt() } ?: 0}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusPill(text = "已关联", container = SemanticSuccess, content = Color.White)
            }

            // 链接已复制提示
            if (goods?.promoUrl != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SemanticInfo.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = SemanticInfo,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "商品链接已复制，发布时可粘贴挂载",
                        style = MaterialTheme.typography.labelMedium,
                        color = SemanticInfo
                    )
                }
            }
        }
    }
}

// =============================================================================
// 区域2：发布设置
// =============================================================================

@Composable
private fun PublishSettingsCard(
    title: String,
    onTitleChange: (String) -> Unit,
    tags: List<String>,
    coverReady: Boolean,
    coverUrl: String?
) {
    VWCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // 视频标题
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "视频标题",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    AiTag()
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("AI 已生成标题，可编辑") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = BrandRed,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true
                )
            }

            // 话题标签
            Column {
                Text(
                    text = "话题标签",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                if (tags.isEmpty()) {
                    Text(
                        text = "暂无标签",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    TagFlowRow(tags = tags)
                }
            }

            // 封面图
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandRed.copy(alpha = 0.06f))
                        .border(1.dp, BrandRed.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverUrl != null) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = BrandRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "封面图",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (coverReady) "已智能选取首帧/首图" else "正在选取封面…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (coverReady) {
                    StatusPill(text = "已就绪", container = SemanticSuccess, content = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TagFlowRow(tags: List<String>) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            Text(
                text = "#$tag",
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(BrandRed.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = BrandRed,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AiTag() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BrandRed.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = BrandRed,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = "AI",
            color = BrandRed,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// =============================================================================
// 区域3：平台选择
// =============================================================================

private data class PlatformOption(
    val target: PublishTarget,
    val emoji: String,
    val label: String
)

private val platformOptions = listOf(
    PlatformOption(PublishTarget.DOUYIN, "🎵", "抖音"),
    PlatformOption(PublishTarget.KUAISHOU, "📸", "快手"),
    PlatformOption(PublishTarget.XHS, "📕", "小红书")
)

@Composable
private fun PlatformSelector(
    selected: PublishTarget?,
    onSelect: (PublishTarget) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        platformOptions.forEach { option ->
            PlatformCell(
                option = option,
                selected = option.target == selected,
                onClick = { onSelect(option.target) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PlatformCell(
    option: PlatformOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) BrandRed else MaterialTheme.colorScheme.outlineVariant
    val background = if (selected) BrandRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = option.emoji, fontSize = 30.sp)
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(BrandRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Text(
            text = option.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) BrandRed else MaterialTheme.colorScheme.onSurface
        )
    }
}

// =============================================================================
// 底部提示
// =============================================================================

@Composable
private fun BottomPublishHint(platform: PublishTarget?) {
    val name = platform?.displayName ?: "所选平台"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.ContentCopy,
            contentDescription = null,
            tint = BrandRed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "将自动打开${name}发布页，商品链接已复制",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =============================================================================
// 发布后引导 Sheet
// =============================================================================

@Composable
private fun PublishGuideSheet(
    platform: PublishTarget?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部图标
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(listOf(BrandRed, BrandRedLight))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = "已为你打开${platform?.displayName.orEmpty()}发布页",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 步骤引导
            GuideStep(
                index = 1,
                text = "在平台发布页粘贴标题（标题已为你生成并复制）"
            )
            GuideStep(
                index = 2,
                text = "在平台内搜索商品并挂载，完成带货发布"
            )

            Spacer(Modifier.height(8.dp))
            GradientButton(
                text = "我知道了",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GuideStep(index: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BrandRed.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(50))
                .background(BrandRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// =============================================================================
// 通用：状态胶囊 / 标题
// =============================================================================

@Composable
private fun StatusPill(
    text: String,
    container: Color,
    content: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionHeader(title: String, hint: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .background(BrandRed, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (hint != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = hint,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 发布按钮文案。 */
private fun publishButtonText(platform: PublishTarget?): String =
    if (platform == null) "请选择发布平台" else "发布到${platform.displayName}"

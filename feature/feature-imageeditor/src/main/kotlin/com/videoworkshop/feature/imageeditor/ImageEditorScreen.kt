package com.videoworkshop.feature.imageeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.videoworkshop.core.designsystem.theme.BrandRed
import com.videoworkshop.core.designsystem.theme.BrandRedLight
import com.videoworkshop.core.designsystem.theme.SemanticSuccess
import com.videoworkshop.core.ui.components.GradientButton
import com.videoworkshop.core.ui.components.OutlineButton
import com.videoworkshop.core.ui.components.VWCard
import com.videoworkshop.core.ui.components.VWTopBar
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.ImageTemplate

/**
 * 图文带货编辑器主界面。
 *
 * 由模板选择、图片导入、AI 文案、图片处理与发布入口五大区域组成，
 * 整体采用电商活力红 (#E94560) 主题，配合卡片阴影、圆角与渐变按钮。
 *
 * @param goodsId  关联商品 ID
 * @param onBack   返回上一级
 * @param onPublish 进入发布流程
 * @param viewModel 编辑器 ViewModel
 */
@Composable
fun ImageEditorScreen(
    goodsId: String,
    onBack: () -> Unit,
    onPublish: () -> Unit,
    viewModel: ImageEditorViewModel = hiltViewModel()
) {
    val goods by viewModel.goods.collectAsStateWithLifecycle()
    val selectedTemplate by viewModel.selectedTemplate.collectAsStateWithLifecycle()
    val importedImages by viewModel.importedImages.collectAsStateWithLifecycle()
    val copyResult by viewModel.copyResult.collectAsStateWithLifecycle()
    val editMode by viewModel.editMode.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(goodsId) { viewModel.loadGoods(goodsId) }

    LaunchedEffect(Unit) {
        viewModel.publishEvent.collect { onPublish() }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = { VWTopBar(title = "图文编辑", onBack = onBack) },
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

            // 商品关联卡片
            GoodsAssociationCard(goods = goods)

            // 区域1 选择模板
            SectionHeader(title = "选择模板", hint = "四款带货模板")
            TemplateGrid(
                selected = selectedTemplate,
                onSelect = viewModel::selectTemplate
            )

            // 区域2 导入图片
            SectionHeader(
                title = "导入图片",
                hint = "${importedImages.size}/${selectedTemplate.maxImages} 张"
            )
            ImportImagesRow(
                images = importedImages,
                maxCount = selectedTemplate.maxImages,
                onAdd = { viewModel.addImage("imported_${System.currentTimeMillis()}") },
                onRemove = viewModel::removeImage
            )

            // 区域3 AI文案
            SectionHeader(title = "AI文案", hint = "智能生成")
            AiCopyCard(
                goods = goods,
                title = copyResult?.title,
                body = copyResult?.body,
                tags = copyResult?.tags.orEmpty(),
                isGenerating = isGenerating,
                editMode = editMode,
                onRegenerate = { goods?.let(viewModel::generateCopy) },
                onToggleEdit = viewModel::toggleEditMode,
                onTitleChange = viewModel::updateCopyTitle,
                onBodyChange = viewModel::updateCopyBody
            )

            // 区域4 图片处理
            SectionHeader(title = "图片处理", hint = "一键美化")
            ImageProcessBadges()

            // 区域5 生成图文并发布
            Spacer(Modifier.height(2.dp))
            GradientButton(
                text = "生成图文并发布",
                onClick = viewModel::generateAndPublish,
                modifier = Modifier.fillMaxWidth(),
                loading = isGenerating,
                leadingIcon = Icons.Filled.AutoAwesome
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// =============================================================================
// 商品关联卡片
// =============================================================================

@Composable
private fun GoodsAssociationCard(goods: Goods?) {
    VWCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 左侧红色渐变竖条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(listOf(BrandRed, BrandRedLight)),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(Modifier.width(12.dp))

            // 商品缩略图
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))

            // 名称 + 佣金
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goods?.name ?: "正在加载商品…",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "¥${goods?.price?.let { "%.2f".format(it) } ?: "--"}",
                        color = BrandRed,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "佣金 ${goods?.let { (it.commissionRate * 100).toInt() } ?: 0}%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(Modifier.width(8.dp))

            // 已关联徽章
            StatusPill(
                text = "已关联",
                container = SemanticSuccess,
                content = Color.White
            )
        }
    }
}

// =============================================================================
// 区域标题
// =============================================================================

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

// =============================================================================
// 区域1：模板四宫格
// =============================================================================

private data class TemplateOption(
    val template: ImageTemplate,
    val emoji: String,
    val label: String,
    val desc: String
)

private val templateOptions = listOf(
    TemplateOption(ImageTemplate.GOODS_RECOMMEND, "📋", "好物推荐", "突出单品卖点"),
    TemplateOption(ImageTemplate.REVIEW_SCORE, "⭐", "测评打分", "评分卡式对比"),
    TemplateOption(ImageTemplate.LIST_COLLECTION, "📝", "清单合集", "多商品好物清单"),
    TemplateOption(ImageTemplate.FLASH_SALE, "🔥", "限时特惠", "强促销氛围")
)

@Composable
private fun TemplateGrid(
    selected: ImageTemplate,
    onSelect: (ImageTemplate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        templateOptions.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { option ->
                    TemplateCell(
                        option = option,
                        selected = option.template == selected,
                        onClick = { onSelect(option.template) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // 该行仅一个元素时补占位，保持等宽对齐
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TemplateCell(
    option: TemplateOption,
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
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = option.emoji, fontSize = 26.sp)
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(50))
                        .background(BrandRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
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
        Text(
            text = option.desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =============================================================================
// 区域2：导入图片
// =============================================================================

@Composable
private fun ImportImagesRow(
    images: List<String>,
    maxCount: Int,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(images, key = { it }) { path ->
            ImportedImageThumb(path = path, onRemove = { onRemove(path) })
        }
        if (images.size < maxCount) {
            item(key = "add") { AddImageBox(onClick = onAdd) }
        }
    }
}

@Composable
private fun ImportedImageThumb(path: String, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(86.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = path.takeIf { it.startsWith("http") || it.startsWith("/") },
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        // 占位图标（非真实路径时）
        if (path.startsWith("imported_")) {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = null,
                tint = BrandRed.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        }
        // 移除按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(BrandRed)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "移除",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun AddImageBox(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(86.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BrandRed.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = BrandRed.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "添加图片",
            tint = BrandRed,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "添加",
            style = MaterialTheme.typography.labelSmall,
            color = BrandRed
        )
    }
}

// =============================================================================
// 区域3：AI 文案
// =============================================================================

@Composable
private fun AiCopyCard(
    goods: Goods?,
    title: String?,
    body: String?,
    tags: List<String>,
    isGenerating: Boolean,
    editMode: Boolean,
    onRegenerate: () -> Unit,
    onToggleEdit: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit
) {
    VWCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = BrandRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "AI 智能文案",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            when {
                isGenerating && title == null -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = BrandRed
                        )
                        Text(
                            text = "正在生成文案…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                title == null -> {
                    Text(
                        text = "点击下方「重新生成」获取带货文案",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                editMode -> {
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text("标题") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = body.orEmpty(),
                        onValueChange = onBodyChange,
                        label = { Text("正文") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                else -> {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = body.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tags.isNotEmpty()) {
                        WrapTags(tags = tags)
                    }
                }
            }

            // 操作按钮
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton(
                    text = "重新生成",
                    onClick = onRegenerate,
                    enabled = goods != null && !isGenerating,
                    leadingIcon = Icons.Filled.Refresh,
                    modifier = Modifier.weight(1f)
                )
                OutlineButton(
                    text = if (editMode) "完成" else "编辑",
                    onClick = onToggleEdit,
                    enabled = title != null,
                    leadingIcon = Icons.Filled.Edit,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WrapTags(tags: List<String>) {
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

// =============================================================================
// 区域4：图片处理 Badge 横排
// =============================================================================

private data class ProcessAction(
    val label: String,
    val icon: ImageVector
)

private val processActions = listOf(
    ProcessAction("滤镜", Icons.Filled.FilterVintage),
    ProcessAction("裁剪", Icons.Filled.Crop),
    ProcessAction("加文字", Icons.Filled.TextFields),
    ProcessAction("加贴纸", Icons.Filled.EmojiEmotions),
    ProcessAction("圆角", Icons.Filled.RoundedCorner)
)

@Composable
private fun ImageProcessBadges() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(processActions, key = { it.label }) { action ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(BrandRed.copy(alpha = 0.12f), BrandRedLight.copy(alpha = 0.12f))
                        )
                    )
                    .border(1.dp, BrandRed.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .clickable { }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = BrandRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = action.label,
                    color = BrandRed,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// =============================================================================
// 通用：状态胶囊
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

package com.videoworkshop.feature.material

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.videoworkshop.core.designsystem.theme.BrandRed
import com.videoworkshop.core.designsystem.theme.BrandRedLight
import com.videoworkshop.core.designsystem.theme.SemanticInfo
import com.videoworkshop.core.ui.components.EmptyState
import com.videoworkshop.core.ui.components.VWTopBar
import com.videoworkshop.domain.model.MaterialEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 素材库主界面。
 *
 * 顶部 FilterChip 标签栏（全部/视频/图片/已处理/未处理）切换过滤，
 * 2 列网格展示素材卡片（缩略图 + 文件名 + 来源标签 + 大小）。
 *
 * 交互：
 * - 点击卡片：弹出 [MaterialActionSheet] 操作菜单
 * - 长按卡片：进入多选模式，支持批量删除
 * - 右下角 FAB：拉起系统选择器导入新素材，content:// URI 持久化并复制到 App 私有目录
 *
 * @param onBack        返回上一级
 * @param onDedup       跳去重页，参数为素材本地路径
 * @param onABTransport 跳 AB 搬运页，参数为素材本地路径
 * @param onEnhance     跳带货包装页，参数为素材本地路径
 * @param viewModel     素材库 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialScreen(
    onBack: () -> Unit,
    onDedup: (String) -> Unit = {},
    onABTransport: (String) -> Unit = {},
    onEnhance: (String) -> Unit = {},
    viewModel: MaterialViewModel = hiltViewModel()
) {
    val materials by viewModel.materials.collectAsStateWithLifecycle()
    val filterTag by viewModel.filterTag.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 临时弹层状态
    var actionSheetFor by remember { mutableStateOf<MaterialEntity?>(null) }
    var editDialogFor by remember { mutableStateOf<MaterialEntity?>(null) }
    var videoPreviewPath by remember { mutableStateOf<String?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // 持久化 URI 读取权限，避免后续访问被拒
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            val type = when {
                mime == null -> "other"
                mime.startsWith("video", ignoreCase = true) -> "video"
                mime.startsWith("image", ignoreCase = true) -> "image"
                else -> "other"
            }
            // 仓库内部会把 content:// URI 复制到 App 私有目录，存储本地路径
            viewModel.saveImportedMaterial(path = uri.toString(), type = type)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.pickFileEvent.collect {
            pickLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = {
            if (isMultiSelectMode) {
                // 多选模式顶栏：选中数量 + 取消 + 删除
                VWTopBar(
                    title = "已选 ${selectedIds.size}",
                    onBack = null,
                    actions = {
                        IconButton(onClick = { viewModel.exitMultiSelect() }) {
                            Icon(Icons.Filled.Close, contentDescription = "退出多选")
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "批量删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            } else {
                VWTopBar(title = "素材库", onBack = onBack)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!isMultiSelectMode) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::importMaterial,
                    containerColor = BrandRed,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Add, contentDescription = "导入素材") },
                    text = { Text("导入", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 标签筛选栏
            MaterialFilterRow(
                selected = filterTag,
                onSelect = viewModel::selectFilterTag
            )

            if (materials.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.VideoLibrary,
                    title = "暂无素材，去导入吧",
                    message = "点击右下角「导入」按钮，添加视频或图片素材",
                    modifier = Modifier.padding(top = 48.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 12.dp,
                        bottom = 96.dp
                    )
                ) {
                    items(materials, key = { it.id }) { material ->
                        MaterialCard(
                            material = material,
                            isSelected = material.id in selectedIds,
                            isMultiSelectMode = isMultiSelectMode,
                            onClick = { clicked ->
                                if (isMultiSelectMode) {
                                    viewModel.toggleSelection(clicked.id)
                                } else {
                                    actionSheetFor = clicked
                                }
                            },
                            onLongClick = { longPressed ->
                                if (!isMultiSelectMode) {
                                    viewModel.enterMultiSelect(longPressed.id)
                                } else {
                                    viewModel.toggleSelection(longPressed.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ===== 弹层：操作菜单 =====
    actionSheetFor?.let { material ->
        MaterialActionSheet(
            onDismiss = { actionSheetFor = null },
            onPreview = {
                if (material.type.equals("video", ignoreCase = true)) {
                    videoPreviewPath = material.path
                }
            },
            onDedup = { onDedup(material.path) },
            onABTransport = { onABTransport(material.path) },
            onEnhance = { onEnhance(material.path) },
            onEdit = { editDialogFor = material },
            onRename = { editDialogFor = material },
            onDelete = { viewModel.deleteMaterial(material.id) }
        )
    }

    // ===== 弹层：编辑对话框 =====
    editDialogFor?.let { material ->
        MaterialEditDialog(
            material = material,
            onDismiss = { editDialogFor = null },
            onSave = { tags, note ->
                viewModel.updateMaterial(material.id, tags, note)
            }
        )
    }

    // ===== 弹层：视频预览 =====
    videoPreviewPath?.let { path ->
        VideoPreviewDialog(
            videoPath = path,
            onDismiss = { videoPreviewPath = null }
        )
    }
}

// =============================================================================
// 标签筛选栏
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialFilterRow(
    selected: MaterialFilterTag,
    onSelect: (MaterialFilterTag) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MaterialFilterTag.entries.forEach { tag ->
            FilterChip(
                selected = tag == selected,
                onClick = { onSelect(tag) },
                label = { Text(tag.label) }
            )
        }
    }
}

// =============================================================================
// 素材卡片
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialCard(
    material: MaterialEntity,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: (MaterialEntity) -> Unit,
    onLongClick: (MaterialEntity) -> Unit
) {
    val borderColor = if (isSelected) BrandRed else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = { onClick(material) },
                onLongClick = { onLongClick(material) }
            )
    ) {
        // 缩略图
        MaterialThumb(material = material, isSelected = isSelected, isMultiSelectMode = isMultiSelectMode)

        // 信息区
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = material.displayName(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SourceBadge(source = material.source)
                Text(
                    text = material.sizeText(LocalContext.current),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 缩略图：
 * - 视频类型：Coil + VideoFrameDecoder 自动提取首帧；加载失败回退到图标占位
 * - 图片类型：直接加载
 * - 其他：图标占位
 */
@Composable
private fun MaterialThumb(
    material: MaterialEntity,
    isSelected: Boolean,
    isMultiSelectMode: Boolean
) {
    val isVideo = material.type.equals("video", ignoreCase = true)
    val isImage = material.type.equals("image", ignoreCase = true)
    val (icon, gradient) = when {
        isVideo -> Icons.Filled.VideoLibrary to listOf(BrandRed, BrandRedLight)
        isImage -> Icons.Filled.Image to listOf(SemanticInfo, SemanticInfo.copy(alpha = 0.6f))
        else -> Icons.Filled.BrokenImage to listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD))
    }

    val model = remember(material.path) {
        runCatching { Uri.parse(material.path) }.getOrNull() ?: material.path
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Brush.verticalGradient(gradient)),
        contentAlignment = Alignment.Center
    ) {
        if (isVideo || isImage) {
            // Coil AsyncImage：对视频自动调用 VideoFrameDecoder 提取首帧
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 加载占位图标（AsyncImage 加载前显示，加载后被覆盖）
        if (!isVideo && !isImage) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(36.dp)
            )
        }

        // 视频类型追加播放徽标
        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // 多选模式下显示选中徽标
        if (isMultiSelectMode) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = if (isSelected) "已选中" else "未选中",
                tint = if (isSelected) BrandRed else Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
            )
        }
    }
}

@Composable
private fun SourceBadge(source: String) {
    val (text, color) = when (source) {
        "import" -> "导入" to BrandRed
        "generated" -> "生成" to SemanticInfo
        "official" -> "官方" to Color(0xFFFFA500)
        else -> source to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium
    )
}

// =============================================================================
// 工具扩展
// =============================================================================

/** 由路径推导展示文件名。 */
private fun MaterialEntity.displayName(): String = when {
    path.startsWith("content://") -> "素材 $id"
    path.contains("/") -> path.substringAfterLast('/')
    else -> path.ifBlank { "素材 $id" }
}

/**
 * 读取文件大小并格式化：
 * - content:// URI：通过 ContentResolver.openFileDescriptor 获取大小
 * - 本地路径：直接用 [File.length]
 * - 不可读时返回 "—"
 */
private fun MaterialEntity.sizeText(context: android.content.Context): String {
    val bytes = runCatching {
        val uri = Uri.parse(path)
        if (uri.scheme?.equals("content", ignoreCase = true) == true) {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } else if (uri.scheme?.equals("file", ignoreCase = true) == true) {
            uri.path?.let { File(it).length() } ?: 0L
        } else {
            File(path).length()
        }
    }.getOrDefault(0L)
    return formatFileSize(bytes)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var idx = 0
    while (size >= 1024 && idx < units.lastIndex) {
        size /= 1024
        idx++
    }
    return "${"%.1f".format(size)} ${units[idx]}"
}

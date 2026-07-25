package com.videoworkshop.feature.material

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.videoworkshop.core.designsystem.theme.BrandRed
import com.videoworkshop.core.designsystem.theme.BrandRedLight
import com.videoworkshop.core.designsystem.theme.SemanticInfo
import com.videoworkshop.core.ui.components.EmptyState
import com.videoworkshop.core.ui.components.VWTopBar
import com.videoworkshop.domain.model.MaterialEntity
import java.io.File

/**
 * 素材库主界面。
 *
 * 顶部 Tab（全部/视频/图片）切换过滤，2 列网格展示素材卡片
 * （缩略图 + 文件名 + 来源标签 + 大小），空状态引导导入，
 * 右下角 FAB 拉起系统选择器导入新素材，长按卡片删除。
 *
 * @param onBack    返回上一级
 * @param viewModel 素材库 ViewModel
 */
@Composable
fun MaterialScreen(
    onBack: () -> Unit,
    viewModel: MaterialViewModel = hiltViewModel()
) {
    val materials by viewModel.materials.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            val type = when {
                mime == null -> "other"
                mime.startsWith("video", ignoreCase = true) -> "video"
                mime.startsWith("image", ignoreCase = true) -> "image"
                else -> "other"
            }
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
        topBar = { VWTopBar(title = "素材库", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::importMaterial,
                containerColor = BrandRed,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = "导入素材") },
                text = { Text("导入", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab 切换
            MaterialTabRow(
                selected = selectedTab,
                onSelect = viewModel::selectTab
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
                            onLongClick = {
                                viewModel.deleteMaterial(material.id)
                                // 触发一次性提示
                            }
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// Tab 切换（分段控件）
// =============================================================================

@Composable
private fun MaterialTabRow(
    selected: TabType,
    onSelect: (TabType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabType.entries.forEach { tab ->
            val isSelected = tab == selected
            val bg = if (isSelected) {
                Brush.horizontalGradient(listOf(BrandRed, BrandRedLight))
            } else {
                Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(bg)
                    .combinedClickable { onSelect(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// =============================================================================
// 素材卡片
// =============================================================================

@Composable
private fun MaterialCard(
    material: MaterialEntity,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = { /* 预览占位 */ },
                onLongClick = onLongClick
            )
    ) {
        // 缩略图
        MaterialThumb(type = material.type)

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
                    text = material.sizeText(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MaterialThumb(type: String) {
    val isVideo = type.equals("video", ignoreCase = true)
    val isImage = type.equals("image", ignoreCase = true)
    val (icon, gradient) = when {
        isVideo -> Icons.Filled.VideoLibrary to
            listOf(BrandRed, BrandRedLight)
        isImage -> Icons.Filled.Image to
            listOf(SemanticInfo, SemanticInfo.copy(alpha = 0.6f))
        else -> Icons.Filled.BrokenImage to
            listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Brush.verticalGradient(gradient)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(36.dp)
        )
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
    path.contains("/") -> path.substringAfterLast("/")
    else -> path.ifBlank { "素材 $id" }
}

/** 读取文件大小并格式化，不可读时返回 "—"。 */
private fun MaterialEntity.sizeText(): String {
    val bytes = runCatching { File(path).length() }.getOrDefault(0L)
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

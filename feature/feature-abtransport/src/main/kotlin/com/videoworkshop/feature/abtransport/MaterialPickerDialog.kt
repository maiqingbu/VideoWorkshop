package com.videoworkshop.feature.abtransport

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.videoworkshop.core.designsystem.theme.BrandNavy
import com.videoworkshop.core.designsystem.theme.BrandRed
import com.videoworkshop.core.ui.components.EmptyState
import com.videoworkshop.domain.model.MaterialEntity

/**
 * 素材库视频选择器对话框。
 *
 * 用于 AB 搬运页选择 A/B 视频：仅显示视频类型素材，
 * 单击选中后通过 [onSelected] 回调返回路径并关闭。
 *
 * @param title       对话框标题（如「选择 A 视频（音频源）」）
 * @param loadMaterials 加载视频素材列表的挂起函数（由 ViewModel 提供）
 * @param onDismiss   关闭回调
 * @param onSelected  选中回调，参数为素材本地路径
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialPickerDialog(
    title: String,
    loadMaterials: suspend () -> List<MaterialEntity>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    var materials by remember { mutableStateOf<List<MaterialEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        isLoading = true
        materials = loadMaterials()
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭")
                    }
                }

                // 提示
                Text(
                    text = "仅显示视频类型素材",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center),
                                strokeWidth = 3.dp,
                                color = BrandRed
                            )
                        }

                        materials.isEmpty() -> {
                            EmptyState(
                                icon = Icons.Filled.VideoLibrary,
                                title = "暂无视频素材",
                                message = "请先在素材库中导入视频",
                                modifier = Modifier.padding(top = 48.dp)
                            )
                        }

                        else -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    top = 8.dp,
                                    bottom = 24.dp
                                )
                            ) {
                                items(materials, key = { it.id }) { material ->
                                    PickerMaterialCard(
                                        material = material,
                                        onClick = {
                                            onSelected(material.path)
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 选择器内的素材卡片：缩略图 + 文件名 + 时长。
 */
@Composable
private fun PickerMaterialCard(
    material: MaterialEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column {
            // 缩略图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(BrandRed, BrandNavy)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val thumb = material.thumbnail
                if (thumb.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumb)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 文件名
            Text(
                text = material.path.substringAfterLast('/'),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

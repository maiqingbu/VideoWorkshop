package com.videoworkshop.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videoworkshop.core.designsystem.theme.BrandNavy
import com.videoworkshop.core.designsystem.theme.BrandRed
import com.videoworkshop.core.designsystem.theme.BrandRedDark
import com.videoworkshop.core.designsystem.theme.NeutralGray
import com.videoworkshop.core.designsystem.theme.NeutralGrayLight
import com.videoworkshop.core.designsystem.theme.SemanticInfo
import com.videoworkshop.domain.model.Draft
import android.widget.Toast

/**
 * 快捷功能入口类型。
 */
enum class QuickAction { MATERIAL, PUBLISH_RECORDS }

/**
 * 首页 —— 视频工坊创作中心。
 *
 * 顶部为品牌红渐变标题区，下方为「视频带货 / 图文带货 / AB 搬运 / 二创工厂」
 * 四张主入口卡片（二创工厂为灰态「敬请期待」），以及最近草稿横向列表与
 * 「素材库 / 发布记录」两个快捷入口。整体使用 [LazyColumn] 并支持下拉刷新。
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onVideoMode: () -> Unit,
    onImageMode: () -> Unit,
    onABTransport: () -> Unit,
    onRefresh: () -> Unit,
    onQuickAction: (QuickAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { HomeHeader(draftCount = uiState.recentDrafts.size) }

            // 4 主入口卡片：2x2 网格
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeCard(
                        modifier = Modifier.weight(1f),
                        gradient = Brush.linearGradient(listOf(BrandRed, BrandRedDark)),
                        icon = Icons.Filled.Movie,
                        title = "视频带货",
                        description = "去重 + 配音 + 发布",
                        onClick = onVideoMode
                    )
                    ModeCard(
                        modifier = Modifier.weight(1f),
                        gradient = Brush.linearGradient(listOf(SemanticInfo, BrandNavy)),
                        icon = Icons.Filled.Image,
                        title = "图文带货",
                        description = "模板 + 文案 + 发布",
                        onClick = onImageMode
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeCard(
                        modifier = Modifier.weight(1f),
                        gradient = Brush.linearGradient(listOf(BrandRed, BrandRedDark)),
                        icon = Icons.Filled.SwapHoriz,
                        title = "AB 搬运",
                        description = "音轨替换 / 混合 / 对齐",
                        onClick = onABTransport
                    )
                    ModeCard(
                        modifier = Modifier.weight(1f),
                        gradient = Brush.linearGradient(listOf(NeutralGrayLight, NeutralGray)),
                        icon = Icons.Filled.AutoAwesome,
                        title = "二创工厂",
                        description = "敬请期待",
                        titleColor = Color.White,
                        descriptionColor = Color.White.copy(alpha = 0.85f),
                        onClick = {
                            Toast.makeText(context, "敬请期待", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item { SectionHeader(title = "最近草稿") }

            item {
                if (uiState.recentDrafts.isEmpty()) {
                    EmptyDraftCard()
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.recentDrafts, key = { it.id }) { draft ->
                            DraftCard(draft = draft)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader(title = "快捷功能")
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Collections,
                        label = "素材库",
                        onClick = { onQuickAction(QuickAction.MATERIAL) }
                    )
                    QuickItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.History,
                        label = "发布记录",
                        onClick = { onQuickAction(QuickAction.PUBLISH_RECORDS) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

/**
 * 顶部品牌渐变标题区。
 */
@Composable
private fun HomeHeader(draftCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BrandRed, BrandRedDark)))
    ) {
        // 装饰：右上角半透明圆，增加层次感
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 40.dp)
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "视频工坊",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "一站式带货创作工具",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(value = draftCount.toString(), label = "草稿")
                StatItem(value = "0", label = "已发布")
                StatItem(value = "0", label = "收藏商品")
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp
        )
    }
}

/**
 * 创作模式大卡片：渐变背景 + 图标 + 标题 + 描述 + 「开始制作」引导。
 */
@Composable
private fun ModeCard(
    modifier: Modifier = Modifier,
    gradient: Brush,
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    titleColor: Color = Color.White,
    descriptionColor: Color = Color.White.copy(alpha = 0.88f)
) {
    Box(
        modifier = modifier
            .height(168.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    color = descriptionColor,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "开始制作",
                        color = titleColor.copy(alpha = 0.95f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = titleColor.copy(alpha = 0.95f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
    )
}

/**
 * 草稿为空时的占位卡片。
 */
@Composable
private fun EmptyDraftCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(92.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = NeutralGray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "暂无草稿，快去创作吧",
                color = NeutralGray,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 单条草稿横向卡片（草稿列表非空时展示）。
 */
@Composable
private fun DraftCard(draft: Draft) {
    Surface(
        modifier = Modifier
            .width(220.dp)
            .height(110.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (draft.type == com.videoworkshop.domain.model.ContentType.VIDEO) {
                            Icons.Filled.Movie
                        } else {
                            Icons.Filled.Image
                        },
                        contentDescription = null,
                        tint = BrandRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (draft.type == com.videoworkshop.domain.model.ContentType.VIDEO) "视频草稿" else "图文草稿",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = draft.content.ifBlank { "未填写文案" },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 快捷功能网格项。
 */
@Composable
private fun QuickItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(96.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandRed,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

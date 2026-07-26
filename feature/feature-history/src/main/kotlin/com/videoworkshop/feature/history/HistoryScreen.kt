package com.videoworkshop.feature.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.videoworkshop.core.designsystem.theme.SemanticSuccess
import com.videoworkshop.core.ui.components.EmptyState
import com.videoworkshop.core.ui.components.ErrorState
import com.videoworkshop.core.ui.components.GradientButton
import com.videoworkshop.core.ui.components.LoadingState
import com.videoworkshop.core.ui.components.VWCard
import com.videoworkshop.core.ui.components.VWTopBar
import com.videoworkshop.domain.model.ContentType
import com.videoworkshop.domain.model.Draft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 发布记录主界面。
 *
 * 顶部使用 [VWTopBar]（标题「发布记录」），列表以 [LazyColumn] 渲染已发布草稿，
 * 每项展示标题/平台/时间/状态，点击后展开详情区域并提供「重新发布」入口。
 * 加载中、空数据、错误分别使用 [LoadingState]/[EmptyState]/[ErrorState] 占位。
 *
 * @param state      UI 状态
 * @param onBack     返回上一级
 * @param onRefresh  下拉/重试刷新
 * @param onItemClick         点击列表项（展开/收起详情）
 * @param onRepublish 点击详情区域「重新发布」按钮，参数为草稿 ID
 * @param onErrorConsume      清除一次性错误提示
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onItemClick: (Long) -> Unit,
    onRepublish: (Long) -> Unit,
    onErrorConsume: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorConsume()
        }
    }

    Scaffold(
        topBar = {
            VWTopBar(
                title = "发布记录",
                onBack = onBack,
                actions = {
                    androidx.compose.material3.IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "刷新"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.loading -> LoadingState(message = "正在加载发布记录…")
                state.drafts.isEmpty() -> EmptyState(
                    icon = Icons.Filled.History,
                    title = "暂无发布记录",
                    message = "完成发布后这里会显示历史记录"
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.drafts,
                        key = { it.id }
                    ) { draft ->
                        HistoryItem(
                            draft = draft,
                            expanded = state.expandedId == draft.id,
                            onClick = { onItemClick(draft.id) },
                            onRepublish = { onRepublish(draft.id) }
                        )
                    }
                }
            }

            if (state.error != null && state.drafts.isEmpty()) {
                ErrorState(
                    message = state.error,
                    onRetry = onRefresh
                )
            }
        }
    }
}

// =============================================================================
// 列表项 + 展开详情
// =============================================================================

/**
 * 单条发布记录卡片。点击展开/收起详情区域。
 */
@Composable
private fun HistoryItem(
    draft: Draft,
    expanded: Boolean,
    onClick: () -> Unit,
    onRepublish: () -> Unit
) {
    VWCard(onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 左侧状态图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SemanticSuccess.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SemanticSuccess,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))

                // 中间标题 + 元信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draft.content.ifBlank { "未命名草稿" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MetaChip(text = contentTypeLabel(draft.type))
                        Spacer(Modifier.width(8.dp))
                        MetaChip(text = formatTime(draft.createdAt))
                    }
                }

                // 右侧展开箭头
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 状态条
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(text = "已发布", container = SemanticSuccess, content = Color.White)
                Spacer(Modifier.width(8.dp))
                MetaChip(text = "平台 —") // TODO: Draft 模型暂无 platform 字段
                Spacer(Modifier.weight(1f))
                Text(
                    text = "ID ${draft.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 展开详情区域
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                DetailSection(
                    draft = draft,
                    onRepublish = onRepublish
                )
            }
        }
    }
}

/**
 * 展开后的详情区域：完整内容 + 媒体数量 + 「重新发布」按钮。
 */
@Composable
private fun DetailSection(
    draft: Draft,
    onRepublish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 完整文案
        Text(
            text = "内容",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = draft.content.ifBlank { "（无文案）" },
            style = MaterialTheme.typography.bodyMedium
        )

        // 关联商品 + 媒体数量
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "创建于 ${formatTime(draft.createdAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "关联商品 ID：${draft.goodsId.ifBlank { "—" }}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "媒体文件：${draft.mediaPaths.size} 个",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 重新发布按钮
        GradientButton(
            text = "重新发布",
            onClick = onRepublish,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Filled.PlayArrow
        )
    }
}

// =============================================================================
// 通用辅助组件
// =============================================================================

/**
 * 元信息小标签：浅灰文字 + 可选前置图标。
 */
@Composable
private fun MetaChip(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 状态胶囊：彩色背景 + 白色文字。
 */
@Composable
private fun StatusBadge(
    text: String,
    container: Color,
    content: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// =============================================================================
// 工具函数
// =============================================================================

/** 时间戳格式化为 yyyy-MM-dd HH:mm。 */
private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0L) return "—"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/** 内容形式转中文标签。 */
private fun contentTypeLabel(type: ContentType): String = when (type) {
    ContentType.VIDEO -> "视频"
    ContentType.IMAGE -> "图文"
}

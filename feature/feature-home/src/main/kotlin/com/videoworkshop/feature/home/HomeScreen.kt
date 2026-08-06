package com.videoworkshop.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.videoworkshop.core.designsystem.theme.InkSecondary
import com.videoworkshop.core.designsystem.theme.InkTertiary
import com.videoworkshop.core.designsystem.theme.PinePrimary
import com.videoworkshop.core.designsystem.theme.VWRadius
import com.videoworkshop.core.designsystem.theme.VWSpacing
import com.videoworkshop.core.designsystem.theme.VWTypeScale
import com.videoworkshop.core.ui.components.VWPrimaryButton
import com.videoworkshop.core.ui.components.VWProjectRow
import com.videoworkshop.domain.model.Project
import com.videoworkshop.domain.model.ProjectStatus
import com.videoworkshop.domain.model.ProjectType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 快捷功能入口类型。
 */
enum class QuickAction { MATERIAL, PROJECTS }

/**
 * 常用工具入口。
 */
enum class WorkbenchTool(val label: String, val icon: ImageVector) {
    AB_RECOMPOSE("音画重组", Icons.Filled.SwapHoriz),
    MATERIAL_PROCESS("素材处理", Icons.Filled.SyncAlt),
    IMAGE_MAKE("图文制作", Icons.Filled.Image),
    GOODS_LIBRARY("商品库", Icons.Filled.Collections)
}

/**
 * 工作台 —— 视频工坊创作中心。
 *
 * 顶部仅保留应用名与任务入口，删除品牌渐变头图、Logo 方块、装饰圆与假统计。
 * 「新建项目」是本页唯一大主按钮；最近项目改为纵向列表；常用工具使用白底小面板。
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onQuickAction: (QuickAction) -> Unit,
    onCreateProject: () -> Unit,
    onProjectClick: (String) -> Unit,
    onToolSelected: (WorkbenchTool) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item { WorkbenchHeader() }

            // 新建项目：本页唯一大主按钮
            item {
                VWPrimaryButton(
                    text = "新建项目",
                    onClick = onCreateProject,
                    modifier = Modifier.padding(horizontal = VWSpacing.lg, vertical = VWSpacing.md),
                    leadingIcon = Icons.Filled.Add
                )
            }

            // 最近项目
            item {
                SectionHeader(
                    title = "最近项目",
                    trailing = if (uiState.recentProjects.isNotEmpty()) {
                        {
                            Text(
                                text = "全部",
                                style = VWTypeScale.caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { onQuickAction(QuickAction.PROJECTS) }
                            )
                        }
                    } else null
                )
            }

            if (uiState.recentProjects.isEmpty()) {
                item {
                    EmptyProjectCard(onCreate = onCreateProject)
                }
            } else {
                items(uiState.recentProjects, key = { it.id }) { project ->
                    VWProjectRow(
                        title = project.title.ifBlank { "未命名项目" },
                        typeLabel = project.type.typeLabel(),
                        status = project.status.statusLabel(),
                        timeLabel = project.updatedAt.toRelativeLabel(),
                        onClick = { onProjectClick(project.id) },
                        thumbnailIcon = project.type.typeIcon()
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = VWSpacing.lg),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            // 常用工具
            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader(title = "常用工具")
            }

            item {
                ToolGrid(onTool = onToolSelected)
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 顶部：应用名 + 任务入口，删除品牌渐变头图。
 */
@Composable
private fun WorkbenchHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VWSpacing.lg, vertical = VWSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "视频工坊",
            style = VWTypeScale.pageTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        // 任务入口（占位，后续接入真实任务）
        Icon(
            imageVector = Icons.Filled.Upcoming,
            contentDescription = "任务（暂无进行中任务）",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VWSpacing.lg, vertical = VWSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = VWTypeScale.sectionTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * 项目为空时的占位卡片。
 */
@Composable
private fun EmptyProjectCard(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VWSpacing.lg)
            .clip(VWRadius.card)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onCreate)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = InkTertiary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "还没有项目，点击新建项目开始创作",
            style = VWTypeScale.caption,
            color = InkSecondary
        )
    }
}

/**
 * 常用工具：白底小面板，不使用彩色渐变。
 */
@Composable
private fun ToolGrid(onTool: (WorkbenchTool) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VWSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(VWSpacing.md)
    ) {
        listOf(
            WorkbenchTool.AB_RECOMPOSE,
            WorkbenchTool.MATERIAL_PROCESS,
            WorkbenchTool.IMAGE_MAKE,
            WorkbenchTool.GOODS_LIBRARY
        ).chunked(2).forEach { rowTools ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VWSpacing.md)
            ) {
                rowTools.forEach { tool ->
                    ToolItem(tool = tool, onClick = { onTool(tool) })
                }
            }
        }
    }
}

@Composable
private fun ToolItem(tool: WorkbenchTool, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(VWRadius.card)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(VWSpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(VWRadius.thumbnail)
                .background(PinePrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = PinePrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(VWSpacing.md))
        Text(
            text = tool.label,
            style = VWTypeScale.listTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun ProjectType.typeIcon(): ImageVector = when (this) {
    ProjectType.VIDEO_COMMERCE -> Icons.Filled.Movie
    ProjectType.IMAGE_COMMERCE -> Icons.Filled.Image
    ProjectType.AB_RECOMPOSE -> Icons.Filled.SwapHoriz
    ProjectType.VIDEO_REWORK -> Icons.Filled.ContentCut
    ProjectType.LONG_VIDEO_CLIP -> Icons.Filled.AudioFile
}

private fun ProjectType.typeLabel(): String = when (this) {
    ProjectType.VIDEO_COMMERCE -> "视频项目"
    ProjectType.IMAGE_COMMERCE -> "图文项目"
    ProjectType.AB_RECOMPOSE -> "音画项目"
    ProjectType.VIDEO_REWORK -> "二创项目"
    ProjectType.LONG_VIDEO_CLIP -> "切片项目"
}

internal fun ProjectStatus.statusLabel(): String = when (this) {
    ProjectStatus.DRAFT -> "草稿"
    ProjectStatus.PREPARING -> "素材准备中"
    ProjectStatus.PROCESSING -> "处理中"
    ProjectStatus.READY_TO_PUBLISH -> "待发布"
    ProjectStatus.PUBLISHED -> "已发布"
    ProjectStatus.FAILED -> "失败"
    ProjectStatus.ARCHIVED -> "已归档"
}

private fun Long.toRelativeLabel(): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(this))
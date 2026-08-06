package com.videoworkshop.feature.project

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
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.videoworkshop.core.designsystem.theme.InkSecondary
import com.videoworkshop.core.designsystem.theme.InkTertiary
import com.videoworkshop.core.designsystem.theme.PineContainer
import com.videoworkshop.core.designsystem.theme.PinePrimary
import com.videoworkshop.core.designsystem.theme.VWRadius
import com.videoworkshop.core.designsystem.theme.VWSpacing
import com.videoworkshop.core.designsystem.theme.VWTypeScale
import com.videoworkshop.core.ui.components.VWBottomSheet
import com.videoworkshop.core.ui.components.VWListRow
import com.videoworkshop.core.ui.components.VWProjectRow
import com.videoworkshop.core.ui.components.VWTopAppBar
import com.videoworkshop.domain.model.Project
import com.videoworkshop.domain.model.ProjectStatus
import com.videoworkshop.domain.model.ProjectType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 项目筛选枚举。
 */
enum class ProjectFilter(val label: String) {
    ALL("全部"),
    PREPARING("制作中"),
    READY_TO_PUBLISH("待发布"),
    PUBLISHED("已发布"),
    ARCHIVED("归档")
}

/**
 * 项目列表页（素简工坊）。
 *
 * 纵向列表 + 顶部筛选 + 右上角"＋"新建（底部弹层选类型 → 输入名称 → 创建）。
 *
 * @param selectedFilter 当前筛选条件（由 ViewModel 驱动，保证筛选真实生效）
 * @param initialOpenCreate 是否在进入页面时直接打开新建弹层（工作台主按钮进入时传 true）
 * @param createUiState 新建项目的 UI 状态（名称、加载、错误）
 */
@Composable
fun ProjectListScreen(
    uiState: ProjectListUiState,
    selectedFilter: ProjectFilter,
    onFilterChange: (ProjectFilter) -> Unit,
    onProjectClick: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    initialOpenCreate: Boolean = false,
    createUiState: ProjectCreateUiState = ProjectCreateUiState(),
    onTitleChange: (String) -> Unit = {},
    onCreate: (ProjectType) -> Unit = {},
) {
    var showCreateSheet by remember { mutableStateOf(initialOpenCreate) }
    var pendingType by remember { mutableStateOf<ProjectType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        VWTopAppBar(
            title = "项目",
            onBack = onBack,
            actions = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "搜索项目",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = VWSpacing.sm)
                        .size(24.dp)
                        .clickable { /* 搜索功能后续接入 */ }
                )
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "新建项目",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = VWSpacing.sm)
                        .size(24.dp)
                        .clickable { showCreateSheet = true }
                )
            }
        )

        FilterBar(selected = selectedFilter, onFilterChange = onFilterChange)

        when {
            uiState.isLoading -> LoadingState()
            uiState.projects.isEmpty() -> EmptyProjectState(onCreate = { showCreateSheet = true })
            else -> ProjectList(
                projects = uiState.projects,
                onProjectClick = onProjectClick
            )
        }
    }

    if (showCreateSheet) {
        CreateProjectSheet(
            onDismiss = { showCreateSheet = false },
            onTypeSelected = { type ->
                showCreateSheet = false
                pendingType = type
            }
        )
    }

    pendingType?.let { type ->
        CreateNameDialog(
            type = type,
            uiState = createUiState,
            onTitleChange = onTitleChange,
            onConfirm = {
                onCreate(type)
            },
            onDismiss = { pendingType = null }
        )
    }
}

@Composable
private fun FilterBar(
    selected: ProjectFilter,
    onFilterChange: (ProjectFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VWSpacing.lg, vertical = VWSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(VWSpacing.sm)
    ) {
        ProjectFilter.entries.forEach { filter ->
            val isSelected = selected == filter
            Text(
                text = filter.label,
                style = VWTypeScale.label,
                color = if (isSelected) PinePrimary else InkSecondary,
                modifier = Modifier
                    .clip(VWRadius.badge)
                    .background(
                        if (isSelected) PineContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onFilterChange(filter) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ProjectList(
    projects: List<Project>,
    onProjectClick: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(projects, key = { it.id }) { project ->
            VWProjectRow(
                title = project.title.ifBlank { "未命名项目" },
                typeLabel = project.type.typeLabel(),
                status = project.status.statusLabel(),
                timeLabel = project.updatedAt.toTimeLabel(),
                onClick = { onProjectClick(project.id) },
                thumbnailIcon = project.type.typeIcon()
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = VWSpacing.lg),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun EmptyProjectState(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = InkTertiary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "还没有项目",
            style = VWTypeScale.listTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "点击右上角“＋”，或新建一个项目开始创作",
            style = VWTypeScale.caption,
            color = InkSecondary
        )
        Spacer(Modifier.height(16.dp))
        com.videoworkshop.core.ui.components.VWPrimaryButton(
            text = "新建项目",
            onClick = onCreate
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * 新建项目底部弹层：选择项目类型，不使用背景渐变。
 */
@Composable
private fun CreateProjectSheet(
    onDismiss: () -> Unit,
    onTypeSelected: (ProjectType) -> Unit
) {
    VWBottomSheet(
        title = "新建项目",
        onDismiss = onDismiss
    ) {
        ProjectType.entries.forEach { type ->
            VWListRow(
                title = type.typeLabel(),
                subtitle = type.typeDescription(),
                leading = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(VWRadius.thumbnail)
                            .background(PinePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = type.typeIcon(),
                            contentDescription = null,
                            tint = PinePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                showTrailingArrow = true,
                onClick = { onTypeSelected(type) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = VWSpacing.lg),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * 新建项目名称输入对话框：选择类型后弹出，名称留空将自动生成默认名称。
 */
@Composable
private fun CreateNameDialog(
    type: ProjectType,
    uiState: ProjectCreateUiState,
    onTitleChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!uiState.isCreating) onDismiss() },
        title = { Text(type.typeLabel()) },
        text = {
            Column {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("请输入项目名称，留空自动生成") },
                    singleLine = true,
                    enabled = !uiState.isCreating
                )
                uiState.error?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = VWTypeScale.caption,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !uiState.isCreating
            ) {
                if (uiState.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("创建")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isCreating) {
                Text("取消")
            }
        }
    )
}

private fun ProjectType.typeIcon(): ImageVector = when (this) {
    ProjectType.VIDEO_COMMERCE -> Icons.Filled.Movie
    ProjectType.IMAGE_COMMERCE -> Icons.Filled.Image
    ProjectType.AB_RECOMPOSE -> Icons.Filled.SwapHoriz
    ProjectType.VIDEO_REWORK -> Icons.Filled.ContentCut
    ProjectType.LONG_VIDEO_CLIP -> Icons.Filled.AudioFile
}

private fun ProjectType.typeLabel(): String = when (this) {
    ProjectType.VIDEO_COMMERCE -> "视频带货"
    ProjectType.IMAGE_COMMERCE -> "图文带货"
    ProjectType.AB_RECOMPOSE -> "音画重组"
    ProjectType.VIDEO_REWORK -> "视频二创"
    ProjectType.LONG_VIDEO_CLIP -> "长视频切片"
}

private fun ProjectType.typeDescription(): String = when (this) {
    ProjectType.VIDEO_COMMERCE -> "使用视频素材制作商品内容"
    ProjectType.IMAGE_COMMERCE -> "使用图片和文案制作多页图文"
    ProjectType.AB_RECOMPOSE -> "组合不同视频的声音和画面"
    ProjectType.VIDEO_REWORK -> "调整已有视频并生成新版本"
    ProjectType.LONG_VIDEO_CLIP -> "从长视频中切片再利用"
}

private fun ProjectStatus.statusLabel(): String = when (this) {
    ProjectStatus.DRAFT -> "草稿"
    ProjectStatus.PREPARING -> "制作中"
    ProjectStatus.PROCESSING -> "处理中"
    ProjectStatus.READY_TO_PUBLISH -> "待发布"
    ProjectStatus.PUBLISHED -> "已发布"
    ProjectStatus.FAILED -> "失败"
    ProjectStatus.ARCHIVED -> "已归档"
}

private fun Long.toTimeLabel(): String =
    SimpleDateFormat("M 月 d 日 HH:mm", Locale.getDefault()).format(Date(this))
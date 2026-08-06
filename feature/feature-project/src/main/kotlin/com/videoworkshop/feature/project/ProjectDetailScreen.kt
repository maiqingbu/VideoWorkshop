package com.videoworkshop.feature.project

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.videoworkshop.core.designsystem.theme.ErrorRed
import com.videoworkshop.core.designsystem.theme.InkSecondary
import com.videoworkshop.core.designsystem.theme.InkTertiary
import com.videoworkshop.core.designsystem.theme.PinePrimary
import com.videoworkshop.core.designsystem.theme.PineOnPrimary
import com.videoworkshop.core.designsystem.theme.VWRadius
import com.videoworkshop.core.designsystem.theme.VWSpacing
import com.videoworkshop.core.designsystem.theme.VWTypeScale
import com.videoworkshop.core.ui.components.VWPrimaryButton
import com.videoworkshop.core.ui.components.VWStatusTag
import com.videoworkshop.core.ui.components.VWStatusType
import com.videoworkshop.core.ui.components.VWTopAppBar
import com.videoworkshop.domain.model.Project
import com.videoworkshop.domain.model.ProjectStatus
import com.videoworkshop.domain.model.ProjectType

/**
 * 项目详情页（素简工坊）。
 *
 * 结构：状态 + 继续制作 + 制作进度 + 内容区。
 * - 一个项目只有一个主操作：「继续制作」。
 * - 制作进度用步骤和文字，未完成步骤空心、当前步骤实心。
 * - 危险操作（重命名 / 归档 / 删除）收进右上角菜单。
 */
@Composable
fun ProjectDetailScreen(
    uiState: ProjectDetailUiState,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    uiState.actionMessage?.let { message ->
        androidx.compose.runtime.LaunchedEffect(message) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        VWTopAppBar(
            title = uiState.project?.title?.ifBlank { "未命名项目" } ?: "项目详情",
            onBack = onBack,
            actions = {
                if (uiState.project != null) {
                    ProjectMenu(
                        onRename = { showRenameDialog = true },
                        onArchive = onArchive,
                        onDelete = { showDeleteDialog = true }
                    )
                }
            }
        )

        when {
            uiState.isLoading -> LoadingState()
            uiState.project == null -> EmptyState(uiState.error)
            else -> DetailContent(
                project = uiState.project,
                onRename = { showRenameDialog = true },
                onArchive = onArchive,
                onDelete = { showDeleteDialog = true }
            )
        }
    }

    if (showRenameDialog && uiState.project != null) {
        RenameDialog(
            initialTitle = uiState.project.title,
            onConfirm = { newTitle ->
                onRename(newTitle)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除项目") },
            text = { Text("确定要删除「${uiState.project?.title ?: "该项目"}」吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("删除", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/** 右上角操作菜单：重命名、归档、删除。 */
@Composable
private fun ProjectMenu(
    onRename: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "更多操作",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("重命名") },
                leadingIcon = {
                    Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text("归档") },
                leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                onClick = {
                    expanded = false
                    onArchive()
                }
            )
            DropdownMenuItem(
                text = { Text("删除", color = ErrorRed) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = ErrorRed) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
private fun DetailContent(
    project: Project,
    onRename: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VWSpacing.lg, vertical = VWSpacing.md),
        verticalArrangement = Arrangement.spacedBy(VWSpacing.lg)
    ) {
        StatusSection(project)
        ContinueButton(project)
        ProgressSection(project)
        CurrentGoodsSection(project)
        ProjectAssetsSection(project)
        RecentOutputSection(project)
        ProjectRecordSection(project)
        Spacer(Modifier.height(VWSpacing.sm))
    }
}

/** 状态标签 + 上次编辑时间。 */
@Composable
private fun StatusSection(project: Project) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VWSpacing.sm)
    ) {
        VWStatusTag(
            text = project.status.statusLabel(),
            type = project.status.statusType()
        )
        Text(
            text = "上次编辑：${project.updatedAt.relativeTimeLabel()}",
            style = VWTypeScale.caption,
            color = InkSecondary
        )
    }
}

/** 唯一主操作：继续制作。归档项目不展示主操作。制作流程后续批次接入，当前为禁用态。 */
@Composable
private fun ContinueButton(project: Project) {
    if (project.status == ProjectStatus.ARCHIVED) return
    val label = if (project.status == ProjectStatus.FAILED) "重新制作" else "继续制作"
    Column {
        VWPrimaryButton(
            text = label,
            onClick = { /* 制作流程在后续批次接入 */ },
            enabled = false
        )
        Spacer(Modifier.height(VWSpacing.sm))
        Text(
            text = "制作流程将在后续版本开放",
            style = VWTypeScale.caption,
            color = InkTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

/** 制作进度：商品 → 素材 → 文案 → 成片 → 发布。 */
@Composable
private fun ProgressSection(project: Project) {
    Column {
        Text(
            text = "制作进度",
            style = VWTypeScale.sectionTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(VWSpacing.md))

        val (done, current) = project.status.progressSteps()
        val steps = listOf("商品", "素材", "文案", "成片", "发布")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, label ->
                val state = when {
                    index < done -> StepState.DONE
                    index == current -> StepState.CURRENT
                    else -> StepState.TODO
                }
                StepItem(label = label, state = state)
            }
        }
    }
}

/** 单个进度步骤：空心（未完成）/ 实心（当前）。 */
@Composable
private fun StepItem(label: String, state: StepState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (state) {
            StepState.DONE -> Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(PinePrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = PineOnPrimary,
                    style = VWTypeScale.label
                )
            }

            StepState.CURRENT -> Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(PinePrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "•",
                    color = PineOnPrimary,
                    style = VWTypeScale.label
                )
            }

            StepState.TODO -> Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
        Text(
            text = label,
            style = VWTypeScale.label,
            color = if (state == StepState.TODO) InkTertiary else PinePrimary
        )
    }
}

/** 当前商品：无关联商品时显示空状态。 */
@Composable
private fun CurrentGoodsSection(project: Project) {
    SectionHeader(title = "当前商品")
    if (project.goodsSnapshotId == null) {
        EmptySectionRow(
            text = "尚未关联商品",
            actionLabel = "选择商品",
            onClick = { /* 商品选择在后续批次接入 */ },
            actionEnabled = false
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(VWRadius.card)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { /* 商品详情在后续批次接入 */ }
                .padding(VWSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(VWRadius.thumbnail)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingBag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(VWSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "已关联商品",
                    style = VWTypeScale.listTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "查看详情",
                    style = VWTypeScale.caption,
                    color = PinePrimary
                )
            }
        }
    }
}

/** 项目素材：暂无真实数据链路时显示空状态。 */
@Composable
private fun ProjectAssetsSection(project: Project) {
    SectionHeader(title = "项目素材")
    EmptySectionRow(
        text = "暂无素材",
        actionLabel = "导入素材",
        onClick = { /* 素材导入在后续批次接入 */ },
        actionEnabled = false
    )
}

/** 最近产物：暂无真实产物时显示空状态。 */
@Composable
private fun RecentOutputSection(project: Project) {
    SectionHeader(title = "最近产物")
    EmptySectionRow(
        text = "暂无产物",
        actionLabel = null,
        onClick = null
    )
}

/** 项目记录：只展示真实存在的记录。 */
@Composable
private fun ProjectRecordSection(project: Project) {
    SectionHeader(title = "项目记录")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(VWRadius.card)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        RecordRow(
            icon = Icons.Filled.Create,
            label = "创建项目",
            time = project.createdAt.relativeTimeLabel()
        )
        if (project.updatedAt != project.createdAt) {
            HorizontalDivider(
                modifier = Modifier.padding(start = VWSpacing.lg),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            RecordRow(
                icon = Icons.Filled.Edit,
                label = "编辑项目",
                time = project.updatedAt.relativeTimeLabel()
            )
        }
    }
}

@Composable
private fun RecordRow(icon: ImageVector, label: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VWSpacing.lg, vertical = VWSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(VWSpacing.md))
        Text(
            text = label,
            style = VWTypeScale.body,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = time,
            style = VWTypeScale.label,
            color = InkTertiary
        )
    }
}

/** 分区标题。 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = VWTypeScale.sectionTitle,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** 空状态行：文本 + 可选的操作链接。actionEnabled 为 false 时操作置灰不可点击。 */
@Composable
private fun EmptySectionRow(
    text: String,
    actionLabel: String?,
    onClick: (() -> Unit)?,
    actionEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(VWRadius.card)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = VWSpacing.lg, vertical = VWSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = VWTypeScale.body,
            color = InkSecondary,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onClick != null) {
            Text(
                text = actionLabel,
                style = VWTypeScale.body,
                color = if (actionEnabled) PinePrimary else InkTertiary,
                modifier = if (actionEnabled) Modifier.clickable(onClick = onClick) else Modifier
            )
        }
    }
}

@Composable
private fun RenameDialog(
    initialTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名项目") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                label = { Text("项目名称") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim()) },
                enabled = title.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(message: String?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message ?: "项目不存在或已被删除",
            color = InkSecondary,
            style = VWTypeScale.body
        )
    }
}

/** 进度步骤状态。 */
private enum class StepState { DONE, CURRENT, TODO }

/**
 * 由项目状态推导「已完成步骤数 + 当前步骤索引」。
 * 返回 Pair(done, current)，current 为 -1 表示没有进行中的步骤。
 */
private fun ProjectStatus.progressSteps(): Pair<Int, Int> = when (this) {
    ProjectStatus.DRAFT -> 0 to 0
    ProjectStatus.PREPARING -> 1 to 1
    ProjectStatus.PROCESSING -> 2 to 2
    ProjectStatus.READY_TO_PUBLISH -> 4 to 4
    ProjectStatus.PUBLISHED -> 5 to -1
    ProjectStatus.FAILED -> 0 to 0
    ProjectStatus.ARCHIVED -> 0 to -1
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

private fun ProjectStatus.statusType(): VWStatusType = when (this) {
    ProjectStatus.DRAFT, ProjectStatus.ARCHIVED -> VWStatusType.NEUTRAL
    ProjectStatus.PREPARING, ProjectStatus.PROCESSING -> VWStatusType.PRIMARY
    ProjectStatus.READY_TO_PUBLISH -> VWStatusType.WARNING
    ProjectStatus.PUBLISHED -> VWStatusType.SUCCESS
    ProjectStatus.FAILED -> VWStatusType.ERROR
}

private fun ProjectType.typeLabel(): String = when (this) {
    ProjectType.VIDEO_COMMERCE -> "视频带货"
    ProjectType.IMAGE_COMMERCE -> "图文带货"
    ProjectType.AB_RECOMPOSE -> "音画重组"
    ProjectType.VIDEO_REWORK -> "视频二创"
    ProjectType.LONG_VIDEO_CLIP -> "长视频切片"
}

/** 相对时间：刚刚 / n 分钟前 / n 小时前 / n 天前 / 日期。 */
private fun Long.relativeTimeLabel(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    if (diff < 0) return "刚刚"
    val minutes = diff / 60_000
    if (minutes < 1) return "刚刚"
    if (minutes < 60) return "${minutes} 分钟前"
    val hours = minutes / 60
    if (hours < 24) return "${hours} 小时前"
    val days = hours / 24
    if (days < 7) return "${days} 天前"
    val date = java.text.SimpleDateFormat("M 月 d 日", java.util.Locale.getDefault())
    return date.format(java.util.Date(this))
}
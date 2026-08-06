package com.videoworkshop.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.domain.model.Project
import com.videoworkshop.domain.usecase.project.ArchiveProjectUseCase
import com.videoworkshop.domain.usecase.project.DeleteProjectUseCase
import com.videoworkshop.domain.usecase.project.GetProjectUseCase
import com.videoworkshop.domain.usecase.project.UpdateProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 项目详情 UI 状态。
 */
data class ProjectDetailUiState(
    val project: Project? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val actionMessage: String? = null
)

/**
 * 项目详情 ViewModel。
 *
 * 通过路由中的 projectId 恢复项目状态，支持重命名、归档、删除。
 */
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val getProjectUseCase: GetProjectUseCase,
    private val updateProjectUseCase: UpdateProjectUseCase,
    private val archiveProjectUseCase: ArchiveProjectUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    fun load(projectId: String) {
        viewModelScope.launch {
            getProjectUseCase.observe(projectId).collect { project ->
                _uiState.value = _uiState.value.copy(
                    project = project,
                    isLoading = false,
                    error = if (project == null) "项目不存在或已被删除" else null
                )
            }
        }
    }

    fun renameProject(newTitle: String) {
        val project = _uiState.value.project ?: return
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            runCatching {
                updateProjectUseCase(project.copy(title = newTitle))
            }.onSuccess {
                _uiState.value = _uiState.value.copy(actionMessage = "已重命名")
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message ?: "重命名失败")
            }
        }
    }

    fun archiveProject() {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            runCatching { archiveProjectUseCase(project.id) }
                .onSuccess { _uiState.value = _uiState.value.copy(actionMessage = "已归档") }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "归档失败") }
        }
    }

    fun deleteProject() {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            runCatching { deleteProjectUseCase(project.id) }
                .onSuccess { _uiState.value = _uiState.value.copy(actionMessage = "已删除") }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "删除失败") }
        }
    }

    fun consumeActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }
}
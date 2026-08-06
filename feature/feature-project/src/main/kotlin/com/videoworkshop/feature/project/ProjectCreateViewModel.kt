package com.videoworkshop.feature.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.domain.model.ProjectType
import com.videoworkshop.domain.usecase.project.CreateProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 新建项目 UI 状态。
 */
data class ProjectCreateUiState(
    val title: String = "",
    val isCreating: Boolean = false,
    val error: String? = null,
    val createdProjectId: String? = null
)

/**
 * 新建项目 ViewModel。
 */
@HiltViewModel
class ProjectCreateViewModel @Inject constructor(
    private val createProjectUseCase: CreateProjectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectCreateUiState())
    val uiState: StateFlow<ProjectCreateUiState> = _uiState.asStateFlow()

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title, error = null)
    }

    fun create(type: ProjectType) {
        val state = _uiState.value
        if (state.isCreating) return

        _uiState.value = state.copy(isCreating = true, error = null)
        viewModelScope.launch {
            runCatching { createProjectUseCase.invoke(title = state.title, type = type) }
                .onSuccess { projectId ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        createdProjectId = projectId
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = e.message ?: "创建项目失败"
                    )
                }
        }
    }

    fun consumeCreated() {
        _uiState.value = _uiState.value.copy(createdProjectId = null)
    }
}
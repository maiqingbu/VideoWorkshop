package com.videoworkshop.feature.material

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.domain.model.MaterialEntity
import com.videoworkshop.domain.repository.MaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 素材库 Tab 类型。
 *
 * @param label   展示名称
 * @param apiName 对应 [MaterialEntity.type] 的过滤值，null 表示不过滤
 */
enum class TabType(val label: String, val apiName: String?) {
    ALL("全部", null),
    VIDEO("视频", "video"),
    IMAGE("图片", "image")
}

/**
 * 素材库 ViewModel。
 *
 * 维护当前 Tab、素材列表（按 Tab 过滤）以及导入/删除能力。
 * 导入通过 [importMaterial] 发出一次性事件，由 UI 层拉起系统选择器，
 * 选择结果回传 [saveImportedMaterial] 持久化。
 *
 * @param materialRepository 素材仓库
 * @param dispatchers        协程调度器
 */
@HiltViewModel
class MaterialViewModel @Inject constructor(
    private val materialRepository: MaterialRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _allMaterials = MutableStateFlow<List<MaterialEntity>>(emptyList())

    private val _selectedTab = MutableStateFlow(TabType.ALL)
    val selectedTab: StateFlow<TabType> = _selectedTab.asStateFlow()

    /** 按 [selectedTab] 过滤后的素材列表。 */
    val materials: StateFlow<List<MaterialEntity>> =
        combine(_allMaterials, _selectedTab) { all, tab ->
            if (tab.apiName == null) all
            else all.filter { it.type.equals(tab.apiName, ignoreCase = true) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 请求拉起系统选择器的一次性事件。
     */
    private val _pickFileEvent = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val pickFileEvent: SharedFlow<Unit> = _pickFileEvent.asSharedFlow()

    init {
        loadMaterials()
    }

    /** 加载全部素材。 */
    private fun loadMaterials() {
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) { materialRepository.getMaterials() }
            }.onSuccess { _allMaterials.value = it }
                .onFailure { _error.value = "素材加载失败" }
        }
    }

    /** 切换 Tab。 */
    fun selectTab(tab: TabType) {
        _selectedTab.value = tab
    }

    /** FAB 点击：请求 UI 拉起系统选择器。 */
    fun importMaterial() {
        _pickFileEvent.tryEmit(Unit)
    }

    /**
     * UI 选择文件后回传，持久化到素材库并刷新列表。
     *
     * @param path 文件路径或内容 URI 字符串
     * @param type 素材类型（video / image / other）
     */
    fun saveImportedMaterial(path: String, type: String) {
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) {
                    materialRepository.saveMaterial(path = path, source = "import", type = type)
                }
            }.onSuccess { loadMaterials() }
                .onFailure { _error.value = "导入失败" }
        }
    }

    /** 删除指定 ID 的素材。 */
    fun deleteMaterial(id: Long) {
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) { materialRepository.deleteMaterial(id) }
            }.onSuccess {
                // 乐观本地更新，避免等待数据库刷新
                _allMaterials.value = _allMaterials.value.filterNot { it.id == id }
            }.onFailure {
                _error.value = "删除失败"
            }
        }
    }

    /** 清除一次性错误提示。 */
    fun consumeError() {
        _error.value = null
    }
}

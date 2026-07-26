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
 * 素材库筛选标签。
 *
 * @param label   展示名称
 * @param predicate 列表过滤谓词；null 表示不过滤
 */
enum class MaterialFilterTag(val label: String) {
    ALL("全部"),
    VIDEO("视频"),
    IMAGE("图片"),
    PROCESSED("已处理"),
    UNPROCESSED("未处理");

    /** 判断素材是否匹配当前标签。 */
    fun matches(material: MaterialEntity): Boolean = when (this) {
        ALL -> true
        VIDEO -> material.type.equals("video", ignoreCase = true)
        IMAGE -> material.type.equals("image", ignoreCase = true)
        PROCESSED -> material.source.equals("generated", ignoreCase = true) ||
            material.source.equals("official", ignoreCase = true)
        UNPROCESSED -> material.source.equals("import", ignoreCase = true)
    }
}


/**
 * 素材库 ViewModel。
 *
 * 维护筛选标签、素材列表（按标签过滤）以及导入/删除/编辑/多选能力。
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

    private val _filterTag = MutableStateFlow(MaterialFilterTag.ALL)
    val filterTag: StateFlow<MaterialFilterTag> = _filterTag.asStateFlow()

    /** 按 [filterTag] 过滤后的素材列表。 */
    val materials: StateFlow<List<MaterialEntity>> =
        combine(_allMaterials, _filterTag) { all, tag ->
            all.filter(tag::matches)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ===== 多选状态 =====
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

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

    /** 切换筛选标签。 */
    fun selectFilterTag(tag: MaterialFilterTag) {
        _filterTag.value = tag
    }


    /** FAB 点击：请求 UI 拉起系统选择器。 */
    fun importMaterial() {
        _pickFileEvent.tryEmit(Unit)
    }

    /**
     * UI 选择文件后回传，持久化到素材库并刷新列表。
     *
     * 仓库层会处理 content:// URI 复制与持久化，UI 层只需把原始 URI 字符串传入。
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

    /**
     * 更新素材的编辑字段（标签 + 备注）。
     */
    fun updateMaterial(id: Long, tags: List<String>, note: String) {
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) {
                    materialRepository.updateMaterial(id = id, tags = tags, note = note)
                }
            }.onSuccess {
                // 乐观更新本地列表
                _allMaterials.value = _allMaterials.value.map { entity ->
                    if (entity.id == id) entity.copy(tags = tags, note = note) else entity
                }
            }.onFailure { _error.value = "保存失败" }
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
                // 同步移除选中
                _selectedIds.value = _selectedIds.value - id
            }.onFailure {
                _error.value = "删除失败"
            }
        }
    }

    // ===== 多选模式 =====

    /** 进入多选模式并预选中指定素材。 */
    fun enterMultiSelect(initialId: Long) {
        _isMultiSelectMode.value = true
        _selectedIds.value = setOf(initialId)
    }

    /** 退出多选模式并清空选中。 */
    fun exitMultiSelect() {
        _isMultiSelectMode.value = false
        _selectedIds.value = emptySet()
    }

    /** 在多选模式下切换选中状态；若全部取消则自动退出。 */
    fun toggleSelection(id: Long) {
        if (!_isMultiSelectMode.value) return
        val current = _selectedIds.value
        _selectedIds.value = if (id in current) current - id else current + id
        if (_selectedIds.value.isEmpty()) {
            _isMultiSelectMode.value = false
        }
    }

    /** 批量删除当前选中的素材，删除完成后自动退出多选模式。 */
    fun deleteSelected() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) { materialRepository.deleteMaterials(ids) }
            }.onSuccess {
                _allMaterials.value = _allMaterials.value.filterNot { it.id in ids }
                _selectedIds.value = emptySet()
                _isMultiSelectMode.value = false
            }.onFailure {
                _error.value = "批量删除失败"
            }
        }
    }

    /** 清除一次性错误提示。 */
    fun consumeError() {
        _error.value = null
    }
}

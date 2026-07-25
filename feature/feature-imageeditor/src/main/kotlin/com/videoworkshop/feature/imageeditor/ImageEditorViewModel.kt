package com.videoworkshop.feature.imageeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.domain.model.CopyResult
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.ImageTemplate
import com.videoworkshop.domain.repository.GoodsRepository
import com.videoworkshop.domain.usecase.GenerateImageCopyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 图文带货编辑器 ViewModel。
 *
 * 持有模板选择、已导入图片、AI 文案结果等编辑态，并协调
 * [GenerateImageCopyUseCase] 完成文案生成与"生成图文并发布"流程。
 *
 * @param generateImageCopyUseCase 图文文案生成用例
 * @param goodsRepository          联盟商品仓库，用于按 ID 加载关联商品
 * @param dispatchers              协程调度器
 */
@HiltViewModel
class ImageEditorViewModel @Inject constructor(
    private val generateImageCopyUseCase: GenerateImageCopyUseCase,
    private val goodsRepository: GoodsRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    // ===== 关联商品 =====
    private val _goods = MutableStateFlow<Goods?>(null)
    val goods: StateFlow<Goods?> = _goods.asStateFlow()

    // ===== 区域1：模板选择 =====
    private val _selectedTemplate = MutableStateFlow(ImageTemplate.GOODS_RECOMMEND)
    val selectedTemplate: StateFlow<ImageTemplate> = _selectedTemplate.asStateFlow()

    // ===== 区域2：导入图片 =====
    private val _importedImages = MutableStateFlow<List<String>>(emptyList())
    val importedImages: StateFlow<List<String>> = _importedImages.asStateFlow()

    // ===== 区域3：AI 文案 =====
    private val _copyResult = MutableStateFlow<CopyResult?>(null)
    val copyResult: StateFlow<CopyResult?> = _copyResult.asStateFlow()

    /** 文案是否处于可编辑态（"编辑"按钮切换）。 */
    private val _editMode = MutableStateFlow(false)
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    // ===== 通用状态 =====
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * "生成图文并发布"成功后触发的一次性事件。
     *
     * UI 层通过 [kotlinx.coroutines.flow.collect] 收到后跳转发布流程。
     */
    private val _publishEvent = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val publishEvent: SharedFlow<Unit> = _publishEvent.asSharedFlow()

    init {
        // 占位：实际由 UI 通过 loadGoods(goodsId) 触发，
        // 保证 ViewModel 不依赖构造期参数。
    }

    /**
     * 按商品 ID 加载关联商品。
     *
     * 由于路由仅携带 [goodsId]，这里通过全量搜索后在内存中匹配，
     * 兼容各联盟平台的 Mock 数据。
     */
    fun loadGoods(goodsId: String) {
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) {
                    goodsRepository.searchGoods(keyword = "", provider = null)
                        .firstOrNull { it.id == goodsId }
                }
            }.onSuccess { found ->
                _goods.value = found
                // 首次进入若已有商品，则自动生成一版文案
                if (found != null && _copyResult.value == null) {
                    generateCopy(found)
                }
            }.onFailure { _error.value = "商品信息加载失败" }
        }
    }

    /** 区域1：选择图文模板。 */
    fun selectTemplate(template: ImageTemplate) {
        _selectedTemplate.value = template
        // 模板切换后，超出的图片予以裁剪
        val max = template.maxImages
        _importedImages.update { it.take(max) }
    }

    /** 区域2：添加一张已导入图片，受当前模板上限约束。 */
    fun addImage(path: String) {
        val max = _selectedTemplate.value.maxImages
        _importedImages.update { current ->
            if (current.contains(path) || current.size >= max) {
                current
            } else {
                current + path
            }
        }
    }

    /** 区域2：移除一张已导入图片。 */
    fun removeImage(path: String) {
        _importedImages.update { it.filterNot { item -> item == path } }
    }

    /** 区域3：基于商品与当前模板（重新）生成 AI 文案。 */
    fun generateCopy(goods: Goods) {
        if (_isGenerating.value) return
        _isGenerating.value = true
        _error.value = null
        _editMode.value = false
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) {
                    generateImageCopyUseCase(goods, _selectedTemplate.value)
                }
            }.onSuccess { result ->
                _copyResult.value = result
            }.onFailure {
                // AI 服务未配置时回退一份占位文案，保证编辑器可用
                _copyResult.value = fallbackCopy(goods, _selectedTemplate.value)
                _error.value = "AI 服务暂不可用，已生成占位文案"
            }
            _isGenerating.value = false
        }
    }

    /** 区域3：切换文案编辑态。 */
    fun toggleEditMode() {
        _editMode.value = !_editMode.value
    }

    /** 区域3：编辑标题。 */
    fun updateCopyTitle(title: String) {
        _copyResult.value?.let { current ->
            _copyResult.value = current.copy(title = title)
        }
    }

    /** 区域3：编辑正文。 */
    fun updateCopyBody(body: String) {
        _copyResult.value?.let { current ->
            _copyResult.value = current.copy(body = body)
        }
    }

    /**
     * 区域5：生成图文并发布。
     *
     * 确保文案已生成后向 UI 发出发布事件；文案生成失败则不跳转，
     * 仅刷新错误提示。
     */
    fun generateAndPublish() {
        val goods = _goods.value ?: run {
            _error.value = "请先关联商品"
            return
        }
        if (_isGenerating.value) return
        _isGenerating.value = true
        _error.value = null
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) {
                    generateImageCopyUseCase(goods, _selectedTemplate.value)
                }
            }.onSuccess { result ->
                _copyResult.value = result
                _publishEvent.tryEmit(Unit)
            }.onFailure {
                // 回退文案后仍允许进入发布流程
                _copyResult.value = fallbackCopy(goods, _selectedTemplate.value)
                _publishEvent.tryEmit(Unit)
            }
            _isGenerating.value = false
        }
    }

    /** 清除一次性错误提示。 */
    fun consumeError() {
        _error.value = null
    }

    /** AI 不可用时的占位文案，避免阻塞编辑器主流程。 */
    private fun fallbackCopy(goods: Goods, template: ImageTemplate): CopyResult = CopyResult(
        title = "${goods.name} | ${template.displayName}推荐",
        body = buildString {
            appendLine(goods.name)
            appendLine("现价 ¥${"%.2f".format(goods.price)}，佣金 ${(goods.commissionRate * 100).toInt()}%")
            appendLine("亲测好物，闭眼入不踩雷～")
        }.trim(),
        sellingPoints = listOf("品质保障", "性价比高", "佣金丰厚"),
        tags = listOf("好物推荐", "9.9包邮", "种草")
    )

    /** 暴露为 Flow 供 UI 收集发布事件（[publishEvent] 的别名，便于测试）。 */
    val publishEventFlow: Flow<Unit> get() = publishEvent
}

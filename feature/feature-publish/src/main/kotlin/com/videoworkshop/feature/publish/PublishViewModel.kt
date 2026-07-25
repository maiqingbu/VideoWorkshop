package com.videoworkshop.feature.publish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.domain.model.ContentType
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.PublishTarget
import com.videoworkshop.domain.repository.AiRepository
import com.videoworkshop.domain.repository.GoodsRepository
import com.videoworkshop.domain.usecase.PublishContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 待发布内容的基本信息。
 *
 * @param type        内容形式原始字符串（video / image）
 * @param filePath    文件路径或合成内容标识
 * @param displayName 展示用文件名
 * @param isVideo     是否为视频内容
 */
data class ContentInfo(
    val type: String,
    val filePath: String,
    val displayName: String,
    val isVideo: Boolean
)

/**
 * 发布流程 ViewModel。
 *
 * 负责加载关联商品、生成 AI 标题与话题标签、维护平台选择与发布态，
 * 并在发布完成后弹出平台操作引导。
 *
 * @param publishContentUseCase 发布用例
 * @param goodsRepository       联盟商品仓库，按 ID 加载关联商品
 * @param aiRepository          AI 仓库，用于生成标题与标签
 * @param dispatchers           协程调度器
 */
@HiltViewModel
class PublishViewModel @Inject constructor(
    private val publishContentUseCase: PublishContentUseCase,
    private val goodsRepository: GoodsRepository,
    private val aiRepository: AiRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _contentInfo = MutableStateFlow<ContentInfo?>(null)
    val contentInfo: StateFlow<ContentInfo?> = _contentInfo.asStateFlow()

    private val _goods = MutableStateFlow<Goods?>(null)
    val goods: StateFlow<Goods?> = _goods.asStateFlow()

    // ===== 区域2：发布设置 =====
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _coverReady = MutableStateFlow(false)
    val coverReady: StateFlow<Boolean> = _coverReady.asStateFlow()

    // ===== 区域3：平台选择 =====
    private val _selectedPlatform = MutableStateFlow<PublishTarget?>(null)
    val selectedPlatform: StateFlow<PublishTarget?> = _selectedPlatform.asStateFlow()

    // ===== 发布态 =====
    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _showGuide = MutableStateFlow(false)
    val showGuide: StateFlow<Boolean> = _showGuide.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 初始化发布上下文：记录内容信息、加载商品、生成标题与标签、智能选取封面。
     */
    fun load(type: String, filePath: String, goodsId: String) {
        val isVideo = type.equals("video", ignoreCase = true)
        _contentInfo.value = ContentInfo(
            type = type,
            filePath = filePath,
            displayName = displayNameOf(filePath, isVideo),
            isVideo = isVideo
        )
        _coverReady.value = true // 智能选取封面（首帧/首图），此处标记为就绪

        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) {
                    goodsRepository.searchGoods(keyword = "", provider = null)
                        .firstOrNull { it.id == goodsId }
                }
            }.onSuccess { found ->
                _goods.value = found
                found?.let { generateTitleAndTags(it) }
            }.onFailure {
                _error.value = "商品信息加载失败"
                // 即使无商品也提供默认标题
                ensureDefaultTitle(isVideo)
            }
        }
    }

    /** 区域2：编辑视频标题。 */
    fun updateTitle(title: String) {
        _title.value = title
    }

    /** 区域3：选择发布平台。 */
    fun selectPlatform(target: PublishTarget) {
        _selectedPlatform.value = target
    }

    /** "发布到XX"：调用发布用例，成功后弹出引导。 */
    fun publish() {
        val info = _contentInfo.value
        val target = _selectedPlatform.value
        if (info == null) {
            _error.value = "内容信息缺失"
            return
        }
        if (target == null) {
            _error.value = "请先选择发布平台"
            return
        }
        if (_isPublishing.value) return
        _isPublishing.value = true
        _error.value = null

        val contentType = if (info.isVideo) ContentType.VIDEO else ContentType.IMAGE
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) {
                    publishContentUseCase(
                        filePath = info.filePath,
                        type = contentType,
                        target = target,
                        title = _title.value.ifBlank { info.displayName },
                        goodsLink = _goods.value?.promoUrl
                    )
                }
            }.onSuccess { ok ->
                // 仓库已复制链接并尝试拉起平台；无论是否成功拉起均展示引导
                if (ok) {
                    _showGuide.value = true
                } else {
                    _error.value = "未检测到${target.displayName}，请手动粘贴发布"
                    _showGuide.value = true
                }
            }.onFailure {
                _error.value = "发布失败：${it.message ?: "未知错误"}"
                _showGuide.value = true
            }
            _isPublishing.value = false
        }
    }

    /** 关闭发布引导 Sheet。 */
    fun dismissGuide() {
        _showGuide.value = false
    }

    /** 清除一次性错误提示。 */
    fun consumeError() {
        _error.value = null
    }

    /** 通过 AI 生成标题与话题标签，失败则回退。 */
    private fun generateTitleAndTags(goods: Goods) {
        viewModelScope.launch {
            runCatching {
                withContext(dispatchers.io) {
                    aiRepository.generateTitleAndTags(
                        content = goods.name,
                        goodsName = goods.name
                    )
                }
            }.onSuccess { (title, tags) ->
                _title.value = title
                _tags.value = tags
            }.onFailure {
                val (title, tags) = fallbackTitleAndTags(goods)
                _title.value = title
                _tags.value = tags
            }
        }
    }

    /** 无商品时的默认标题。 */
    private fun ensureDefaultTitle(isVideo: Boolean) {
        if (_title.value.isBlank()) {
            _title.value = if (isVideo) "好物种草视频，赶紧来看看" else "好物种草图文，赶紧来看看"
            _tags.value = listOf("好物推荐", "9.9包邮", "种草")
        }
    }

    /** AI 不可用时的标题/标签回退。 */
    private fun fallbackTitleAndTags(goods: Goods): Pair<String, List<String>> {
        val title = "${goods.name.take(18)}… 值得入手的好物！"
        val tags = listOf("好物推荐", "9.9包邮", "种草", goods.provider.name)
        return title to tags
    }

    /** 由文件路径推导展示名。 */
    private fun displayNameOf(filePath: String, isVideo: Boolean): String {
        return when {
            filePath.startsWith("image_content_") -> "图文内容"
            filePath.contains("/") -> filePath.substringAfterLast("/")
            else -> if (isVideo) "带货视频.mp4" else "图文内容"
        }
    }
}

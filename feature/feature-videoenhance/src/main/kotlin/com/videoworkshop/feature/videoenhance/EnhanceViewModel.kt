package com.videoworkshop.feature.videoenhance

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.domain.model.AllianceProvider
import com.videoworkshop.domain.model.CopyResult
import com.videoworkshop.domain.model.EnhanceConfig
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.VoiceProfile
import com.videoworkshop.domain.usecase.EnhanceVideoUseCase
import com.videoworkshop.domain.usecase.GenerateVideoCopyUseCase
import com.videoworkshop.domain.usecase.SynthesizeVoiceUseCase
import com.videoworkshop.domain.usecase.TranscribeSubtitleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// =============================================================================
// 选项模型（供 UI 渲染选项列表）
// =============================================================================

/**
 * 字幕样式选项。
 */
data class SubtitleStyleOption(
    val displayName: String,
    val style: String
)

/** 内置字幕样式列表。 */
val SubtitleStyleOptions = listOf(
    SubtitleStyleOption(
        displayName = "黑体白底",
        style = "FontName=SimHei,FontSize=28,PrimaryColour=&H00FFFFFF," +
            "OutlineColour=&H00000000,BorderStyle=1,Outline=2,Shadow=0"
    ),
    SubtitleStyleOption(
        displayName = "楷体黄底",
        style = "FontName=KaiTi,FontSize=28,PrimaryColour=&H0000FFFF," +
            "OutlineColour=&H00000000,BorderStyle=1,Outline=2,Shadow=0"
    ),
    SubtitleStyleOption(
        displayName = "宋体黑底",
        style = "FontName=SimSun,FontSize=26,PrimaryColour=&H00FFFFFF," +
            "OutlineColour=&H00000000,BorderStyle=3,Outline=1,Shadow=0"
    ),
    SubtitleStyleOption(
        displayName = "圆体白底",
        style = "FontName=Arial,FontSize=26,PrimaryColour=&H00FFFFFF," +
            "OutlineColour=&H00000000,BorderStyle=1,Outline=2,Shadow=0"
    )
)

/**
 * 背景音乐选项。
 */
data class BgmOption(
    val displayName: String,
    val identifier: String
)

/** 内置背景音乐库。 */
val BgmOptions = listOf(
    BgmOption("轻快", "bgm_light"),
    BgmOption("舒缓", "bgm_soft"),
    BgmOption("节奏", "bgm_beat")
)

/**
 * 带货贴纸选项。
 */
data class StickerOption(
    val emoji: String,
    val label: String,
    val identifier: String
)

/** 内置带货贴纸。 */
val StickerOptions = listOf(
    StickerOption("🛒", "点击购物车", "sticker_cart"),
    StickerOption("🔥", "热卖中", "sticker_hot"),
    StickerOption("💰", "9.9包邮", "sticker_discount")
)

// =============================================================================
// ViewModel
// =============================================================================

/**
 * 带货包装页面视图模型。
 *
 * 编排 AI 文案生成、配音合成、字幕、背景音乐、贴纸选择与最终视频合成。
 *
 * @param generateVideoCopyUseCase 生成带货文案
 * @param synthesizeVoiceUseCase    合成配音
 * @param transcribeSubtitleUseCase 字幕转写
 * @param enhanceVideoUseCase       视频增强合成
 * @param savedStateHandle          导航参数：videoPath、goodsId
 */
@HiltViewModel
class EnhanceViewModel @Inject constructor(
    private val generateVideoCopyUseCase: GenerateVideoCopyUseCase,
    private val synthesizeVoiceUseCase: SynthesizeVoiceUseCase,
    private val transcribeSubtitleUseCase: TranscribeSubtitleUseCase,
    private val enhanceVideoUseCase: EnhanceVideoUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 已去重的视频文件路径（已解码）。 */
    val videoPath: String = Uri.decode(savedStateHandle.get<String>("videoPath").orEmpty())

    private val goodsId: String = Uri.decode(savedStateHandle.get<String>("goodsId").orEmpty())

    /** 当前关联商品（基于 goodsId 构造，用于文案生成与展示）。 */
    private val _goods = MutableStateFlow(buildPlaceholderGoods())
    val goods: StateFlow<Goods?> = _goods.asStateFlow()

    private val _copies = MutableStateFlow<List<CopyResult>>(emptyList())
    /** AI 生成的多版文案。 */
    val copies: StateFlow<List<CopyResult>> = _copies.asStateFlow()

    private val _selectedCopyIndex = MutableStateFlow(0)
    /** 当前选中的文案索引。 */
    val selectedCopyIndex: StateFlow<Int> = _selectedCopyIndex.asStateFlow()

    private val _selectedVoice = MutableStateFlow<VoiceProfile?>(null)
    /** 选中的配音音色，未选为 null。 */
    val selectedVoice: StateFlow<VoiceProfile?> = _selectedVoice.asStateFlow()

    private val _subtitleEnabled = MutableStateFlow(false)
    /** 是否生成字幕。 */
    val subtitleEnabled: StateFlow<Boolean> = _subtitleEnabled.asStateFlow()

    private val _subtitleStyle = MutableStateFlow(SubtitleStyleOptions.first())
    /** 当前字幕样式。 */
    val subtitleStyle: StateFlow<SubtitleStyleOption> = _subtitleStyle.asStateFlow()

    private val _selectedBgm = MutableStateFlow<String?>(null)
    /** 选中的背景音乐标识，未选为 null。 */
    val selectedBgm: StateFlow<String?> = _selectedBgm.asStateFlow()

    private val _selectedStickers = MutableStateFlow<List<String>>(emptyList())
    /** 选中的贴纸标识列表。 */
    val selectedStickers: StateFlow<List<String>> = _selectedStickers.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    /** 是否正在合成视频。 */
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    /** 合成进度（0.0 ~ 1.0）。 */
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _copyLoading = MutableStateFlow(false)
    /** 文案生成中。 */
    val copyLoading: StateFlow<Boolean> = _copyLoading.asStateFlow()

    private val _voiceGenerating = MutableStateFlow(false)
    /** 配音生成中。 */
    val voiceGenerating: StateFlow<Boolean> = _voiceGenerating.asStateFlow()

    private val _voicePath = MutableStateFlow<String?>(null)
    /** 已生成的配音音频路径。 */
    val voicePath: StateFlow<String?> = _voicePath.asStateFlow()

    private val _outputPath = MutableStateFlow<String?>(null)
    /** 合成产物路径，完成后非空。 */
    val outputPath: StateFlow<String?> = _outputPath.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    /** 错误信息。 */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        generateCopy(_goods.value)
    }

    /**
     * 生成带货文案。
     */
    fun generateCopy(goods: Goods) {
        viewModelScope.launch {
            _copyLoading.value = true
            _errorMessage.value = null
            try {
                val results = generateVideoCopyUseCase(goods.name, goods.price)
                _copies.value = results
                _selectedCopyIndex.value = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _errorMessage.value = e.message ?: "文案生成失败"
            } finally {
                _copyLoading.value = false
            }
        }
    }

    /**
     * 切换选中的文案版本。
     */
    fun selectCopy(index: Int) {
        if (index in _copies.value.indices) _selectedCopyIndex.value = index
    }

    /**
     * 更新指定版本文案的正文内容（用户手动编辑）。
     */
    fun updateCopyBody(index: Int, newBody: String) {
        if (index !in _copies.value.indices) return
        val current = _copies.value[index]
        _copies.value = _copies.value.toMutableList().apply {
            set(index, current.copy(body = newBody))
        }
    }

    /**
     * 选择配音音色，再次点击同一项可取消。
     */
    fun selectVoice(voice: VoiceProfile) {
        _selectedVoice.value = if (_selectedVoice.value == voice) null else voice
    }

    /**
     * 切换字幕开关。
     */
    fun toggleSubtitle() {
        _subtitleEnabled.value = !_subtitleEnabled.value
    }

    /**
     * 选择字幕样式。
     */
    fun selectSubtitleStyle(option: SubtitleStyleOption) {
        _subtitleStyle.value = option
    }

    /**
     * 选择背景音乐，再次点击同一项可取消。
     */
    fun selectBgm(bgm: String) {
        _selectedBgm.value = if (_selectedBgm.value == bgm) null else bgm
    }

    /**
     * 切换贴纸选中状态。
     */
    fun toggleSticker(sticker: String) {
        val current = _selectedStickers.value.toMutableList()
        if (sticker in current) current.remove(sticker) else current.add(sticker)
        _selectedStickers.value = current
    }

    /**
     * 生成配音音频。
     *
     * 需要先选中音色与至少一版文案。
     */
    fun generateVoice() {
        val voice = _selectedVoice.value ?: run {
            _errorMessage.value = "请先选择配音音色"
            return
        }
        val body = _copies.value.getOrNull(_selectedCopyIndex.value)?.body
        if (body.isNullOrBlank()) {
            _errorMessage.value = "暂无文案可配音"
            return
        }
        if (_voiceGenerating.value) return
        viewModelScope.launch {
            _voiceGenerating.value = true
            _errorMessage.value = null
            try {
                _voicePath.value = synthesizeVoiceUseCase(body, voice)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _errorMessage.value = e.message ?: "配音生成失败"
            } finally {
                _voiceGenerating.value = false
            }
        }
    }

    /**
     * 生成带货视频。
     */
    fun generateVideo() {
        if (_isProcessing.value) return
        _isProcessing.value = true
        _errorMessage.value = null
        _progress.value = 0f
        _outputPath.value = null
        viewModelScope.launch {
            try {
                val output = buildOutputPath()
                val copyBody = _copies.value
                    .getOrNull(_selectedCopyIndex.value)
                    ?.body
                val config = EnhanceConfig(
                    copy = copyBody,
                    voice = _selectedVoice.value,
                    subtitle = _subtitleEnabled.value,
                    subtitleStyle = _subtitleStyle.value.style
                        .takeIf { _subtitleEnabled.value },
                    bgm = _selectedBgm.value,
                    stickers = _selectedStickers.value
                )
                val flow = enhanceVideoUseCase(videoPath, output, config)
                flow.catch { e ->
                    _isProcessing.value = false
                    _errorMessage.value = e.message ?: "视频合成失败"
                }.collect { p ->
                    _progress.value = p
                    if (p >= 1f) {
                        _outputPath.value = output
                        _isProcessing.value = false
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _isProcessing.value = false
                _errorMessage.value = e.message ?: "视频合成失败"
            }
        }
    }

    /**
     * 清除错误信息。
     */
    fun clearError() {
        _errorMessage.value = null
    }

    private fun buildPlaceholderGoods(): Goods = Goods(
        id = goodsId,
        provider = AllianceProvider.PDD,
        name = if (goodsId.isBlank() || goodsId == "none") "精选好物" else "商品 ${goodsId.takeLast(8)}",
        price = 29.9,
        originalPrice = 59.9,
        commissionRate = 0.20,
        promoUrl = null,
        imageUrl = null,
        videoSources = emptyList()
    )

    private fun buildOutputPath(): String {
        val source = File(videoPath)
        val name = source.nameWithoutExtension
        val ext = source.extension.ifBlank { "mp4" }
        val parent = source.parentFile ?: File(System.getProperty("java.io.tmpdir"))
        return File(parent, "${name}_enhance_${System.currentTimeMillis()}.$ext").absolutePath
    }
}

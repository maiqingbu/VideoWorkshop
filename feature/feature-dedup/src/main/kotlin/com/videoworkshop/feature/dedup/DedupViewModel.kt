package com.videoworkshop.feature.dedup

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.domain.model.DedupConfig
import com.videoworkshop.domain.model.DedupProgress
import com.videoworkshop.domain.model.DedupStrength
import com.videoworkshop.domain.model.VideoClip
import com.videoworkshop.domain.repository.DedupRepository
import com.videoworkshop.domain.usecase.DedupVideoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 去重页面视图模型。
 *
 * 持有去重配置、处理进度与产物路径等状态，并封装去重执行流程。
 *
 * @param dedupVideoUseCase 执行去重的用例（核心依赖）
 * @param dedupRepository   用于解析视频元信息以展示文件卡片
 * @param savedStateHandle  导航参数，携带待去重的视频路径
 */
@HiltViewModel
class DedupViewModel @Inject constructor(
    private val dedupVideoUseCase: DedupVideoUseCase,
    private val dedupRepository: DedupRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 待去重的视频文件路径（已解码）。 */
    val videoPath: String = Uri.decode(savedStateHandle.get<String>("videoPath").orEmpty())

    private val _videoInfo = MutableStateFlow<VideoClip?>(null)
    /** 视频元信息，用于信息卡片展示（时长/分辨率/大小）。 */
    val videoInfo: StateFlow<VideoClip?> = _videoInfo.asStateFlow()

    private val _config = MutableStateFlow(DedupConfig())
    /** 当前去重配置。 */
    val config: StateFlow<DedupConfig> = _config.asStateFlow()

    private val _mode = MutableStateFlow(DedupMode.QUICK)
    /** 当前去重模式。 */
    val mode: StateFlow<DedupMode> = _mode.asStateFlow()

    private val _progress = MutableStateFlow<DedupProgress?>(null)
    /** 去重进度，空闲时为 null。 */
    val progress: StateFlow<DedupProgress?> = _progress.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    /** 是否正在处理。 */
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _outputPath = MutableStateFlow<String?>(null)
    /** 去重产物路径，完成前为 null。 */
    val outputPath: StateFlow<String?> = _outputPath.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    /** 错误信息，无错误时为 null。 */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 页面整体 UI 状态，由 [isProcessing]、[progress]、[outputPath]、[errorMessage] 派生。
     */
    val uiState: StateFlow<DedupUiState> = combine(
        _isProcessing,
        _progress,
        _outputPath,
        _errorMessage
    ) { processing, progress, output, error ->
        when {
            error != null -> DedupUiState.Error(error)
            output != null -> DedupUiState.Completed(output)
            processing -> DedupUiState.Processing(
                progress = progress?.progress ?: 0f,
                currentStep = progress?.currentStep ?: "准备中"
            )
            else -> DedupUiState.Idle
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DedupUiState.Idle
    )

    init {
        loadVideoInfo()
    }

    /**
     * 切换指定去重项的启用状态。
     *
     * 索引与 [DedupConfig] 字段对应：
     * 0/7 为锁定项（MD5、元数据），始终启用，不可关闭。
     */
    fun toggleItem(index: Int) {
        if (_isProcessing.value) return
        val current = _config.value
        _config.value = when (index) {
            0 -> current.copy(md5Modify = true)        // 锁定：必选
            1 -> current.copy(fpsAdjust = !current.fpsAdjust)
            2 -> current.copy(bitrateModify = !current.bitrateModify)
            3 -> current.copy(cropTransform = !current.cropTransform)
            4 -> current.copy(mirrorFlip = !current.mirrorFlip)
            5 -> current.copy(colorShift = !current.colorShift)
            6 -> current.copy(audioReshape = !current.audioReshape)
            7 -> current.copy(metadataClean = true)    // 锁定：必选
            else -> current
        }
    }

    /**
     * 设置去重强度档位。
     */
    fun setStrength(strength: DedupStrength) {
        if (_isProcessing.value) return
        _config.value = _config.value.copy(strength = strength)
    }

    /**
     * 切换去重模式。切到一键模式时自动启用全部 8 项。
     */
    fun setMode(mode: DedupMode) {
        if (_isProcessing.value) return
        _mode.value = mode
        if (mode == DedupMode.QUICK) {
            _config.value = _config.value.copy(
                md5Modify = true,
                fpsAdjust = true,
                bitrateModify = true,
                cropTransform = true,
                mirrorFlip = true,
                colorShift = true,
                audioReshape = true,
                metadataClean = true
            )
        }
    }

    /**
     * 开始执行去重。
     *
     * @param inputPath 输入视频路径
     */
    fun startDedup(inputPath: String) {
        if (_isProcessing.value || inputPath.isBlank()) return
        val outputPath = buildOutputPath(inputPath)
        _isProcessing.value = true
        _errorMessage.value = null
        _outputPath.value = null
        _progress.value = DedupProgress(
            currentStep = "准备中",
            stepIndex = 0,
            totalSteps = TOTAL_STEPS,
            progress = 0f
        )
        viewModelScope.launch {
            val flow = try {
                dedupVideoUseCase(inputPath, outputPath, _config.value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _isProcessing.value = false
                _errorMessage.value = e.message ?: "启动去重失败"
                return@launch
            }
            flow.catch { e ->
                _isProcessing.value = false
                _errorMessage.value = e.message ?: "去重失败，请重试"
            }.collect { p ->
                _progress.value = p
                if (p.progress >= 1f) {
                    _outputPath.value = outputPath
                    _isProcessing.value = false
                }
            }
        }
    }

    /**
     * 重置为初始状态，便于重新处理。
     */
    fun reset() {
        _isProcessing.value = false
        _progress.value = null
        _outputPath.value = null
        _errorMessage.value = null
        _config.value = DedupConfig()
        _mode.value = DedupMode.QUICK
    }

    private fun loadVideoInfo() {
        if (videoPath.isBlank()) return
        viewModelScope.launch {
            try {
                _videoInfo.value = dedupRepository.getVideoInfo(videoPath)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // 解析失败时保留 null，UI 退化为仅展示文件名
            }
        }
    }

    private fun buildOutputPath(inputPath: String): String {
        val source = File(inputPath)
        val name = source.nameWithoutExtension
        val ext = source.extension.ifBlank { "mp4" }
        val parent = source.parentFile ?: File(System.getProperty("java.io.tmpdir"))
        return File(parent, "${name}_dedup_${System.currentTimeMillis()}.$ext").absolutePath
    }

    private companion object {
        const val TOTAL_STEPS = 8
    }
}

package com.videoworkshop.feature.abtransport

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.domain.model.ABTransportConfig
import com.videoworkshop.domain.model.ABTransportMode
import com.videoworkshop.domain.model.ABTransportProgress
import com.videoworkshop.domain.model.DurationStrategy
import com.videoworkshop.domain.model.MaterialEntity
import com.videoworkshop.domain.model.TimelineSegment
import com.videoworkshop.domain.model.VideoClip
import com.videoworkshop.domain.repository.DedupRepository
import com.videoworkshop.domain.repository.MaterialRepository
import com.videoworkshop.domain.usecase.ABTransportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * AB 搬运页面视图模型。
 *
 * 持有 A/B 视频选择、合成配置、处理进度与产物路径等状态，
 * 封装关键帧预览、合成执行、取消与自动入库流程。
 *
 * 核心规则（spec 已确认）：
 * - A/B 视频**仅从素材库选择**，由 UI 弹出素材选择器
 * - 合成完成后**自动保存产物到素材库**（type=video，标签「AB搬运」）
 * - A 视频无音轨时禁止开始合成，提示用户
 * - 取消合成立即停止 FFmpeg，无残留进程（由 AVStreamSwapper 的 awaitClose 保证）
 *
 * @param abTransportUseCase 执行 AB 搬运的用例
 * @param dedupRepository    用于视频信息解析、音轨检测、关键帧提取
 * @param materialRepository 用于素材库选择列表与产物自动入库
 * @param dispatchers        协程调度器
 * @param savedStateHandle   导航参数，可选预填 A 视频路径
 */
@HiltViewModel
class ABTransportViewModel @Inject constructor(
    private val abTransportUseCase: ABTransportUseCase,
    private val dedupRepository: DedupRepository,
    private val materialRepository: MaterialRepository,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 预填的 A 视频路径（已解码），来自素材库「AB 搬运」菜单入口。 */
    private val prefilledVideoA: String =
        Uri.decode(savedStateHandle.get<String>("videoA").orEmpty())

    // ===== A/B 视频选择状态 =====

    private val _videoAPath = MutableStateFlow<String?>(null)
    val videoAPath: StateFlow<String?> = _videoAPath.asStateFlow()

    private val _videoBPath = MutableStateFlow<String?>(null)
    val videoBPath: StateFlow<String?> = _videoBPath.asStateFlow()

    private val _videoAInfo = MutableStateFlow<VideoClip?>(null)
    val videoAInfo: StateFlow<VideoClip?> = _videoAInfo.asStateFlow()

    private val _videoBInfo = MutableStateFlow<VideoClip?>(null)
    val videoBInfo: StateFlow<VideoClip?> = _videoBInfo.asStateFlow()

    // ===== 合成配置 =====

    private val _mode = MutableStateFlow(ABTransportMode.PURE_REPLACE)
    val mode: StateFlow<ABTransportMode> = _mode.asStateFlow()

    private val _durationStrategy = MutableStateFlow(DurationStrategy.TRUNCATE)
    val durationStrategy: StateFlow<DurationStrategy> = _durationStrategy.asStateFlow()

    private val _volumeRatioA = MutableStateFlow(1.0f)
    val volumeRatioA: StateFlow<Float> = _volumeRatioA.asStateFlow()

    private val _volumeRatioB = MutableStateFlow(0.5f)
    val volumeRatioB: StateFlow<Float> = _volumeRatioB.asStateFlow()

    /** A 自定义起止点（毫秒），null 表示使用全长。 */
    private val _segmentA = MutableStateFlow<TimelineSegment?>(null)
    val segmentA: StateFlow<TimelineSegment?> = _segmentA.asStateFlow()

    /** B 自定义起止点（毫秒），null 表示使用全长。 */
    private val _segmentB = MutableStateFlow<TimelineSegment?>(null)
    val segmentB: StateFlow<TimelineSegment?> = _segmentB.asStateFlow()

    // ===== 关键帧预览 =====

    private val _keyframesA = MutableStateFlow<List<String>>(emptyList())
    val keyframesA: StateFlow<List<String>> = _keyframesA.asStateFlow()

    private val _keyframesB = MutableStateFlow<List<String>>(emptyList())
    val keyframesB: StateFlow<List<String>> = _keyframesB.asStateFlow()

    // ===== 处理状态 =====

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow<ABTransportProgress?>(null)
    val progress: StateFlow<ABTransportProgress?> = _progress.asStateFlow()

    private val _outputPath = MutableStateFlow<String?>(null)
    val outputPath: StateFlow<String?> = _outputPath.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** A 视频无音轨标志，用于禁止开始合成并提示用户。 */
    private val _audioMissing = MutableStateFlow(false)
    val audioMissing: StateFlow<Boolean> = _audioMissing.asStateFlow()

    /** 产物是否已自动保存到素材库。 */
    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    /** 当前合成任务，用于取消。 */
    private var synthesisJob: Job? = null

    /** 临时关键帧输出目录，ViewModel 销毁时由系统清理。 */
    private val keyframeDir: File by lazy {
        File(System.getProperty("java.io.tmpdir"), "ab_keyframes_${System.currentTimeMillis()}")
            .apply { mkdirs() }
    }

    /**
     * 页面整体 UI 状态，由各子状态派生。
     */
    val uiState: StateFlow<ABTransportUiState> = combine(
        _isProcessing,
        _progress,
        _outputPath,
        _error
    ) { processing, progress, output, error ->
        when {
            error != null -> ABTransportUiState.Error(error)
            output != null -> ABTransportUiState.Completed(output)
            processing -> ABTransportUiState.Processing(
                progressPercent = progress?.progress ?: 0f,
                currentMs = progress?.currentMs ?: 0L,
                totalMs = progress?.totalMs ?: 0L
            )
            else -> ABTransportUiState.Idle
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ABTransportUiState.Idle
    )

    init {
        // 从素材库入口预填 A 视频
        if (prefilledVideoA.isNotBlank()) {
            selectVideoA(prefilledVideoA)
        }
    }

    // =============================================================================
    // A/B 视频选择
    // =============================================================================

    /**
     * 设置 A 视频（音频源）。
     *
     * 同时：解析视频信息、检测音轨、提取关键帧。
     */
    fun selectVideoA(path: String) {
        if (_isProcessing.value) return
        _videoAPath.value = path
        _videoAInfo.value = null
        _keyframesA.value = emptyList()
        _audioMissing.value = false
        loadVideoInfo(path, isVideoA = true)
        checkAudioTrack(path)
        extractKeyframes(path, isVideoA = true)
    }

    /**
     * 设置 B 视频（画面源）。
     *
     * 同时：解析视频信息、提取关键帧。
     */
    fun selectVideoB(path: String) {
        if (_isProcessing.value) return
        _videoBPath.value = path
        _videoBInfo.value = null
        _keyframesB.value = emptyList()
        loadVideoInfo(path, isVideoB = true)
        extractKeyframes(path, isVideoB = true)
    }

    /** 清除 A 视频选择。 */
    fun clearVideoA() {
        if (_isProcessing.value) return
        _videoAPath.value = null
        _videoAInfo.value = null
        _keyframesA.value = emptyList()
        _audioMissing.value = false
        _segmentA.value = null
    }

    /** 清除 B 视频选择。 */
    fun clearVideoB() {
        if (_isProcessing.value) return
        _videoBPath.value = null
        _videoBInfo.value = null
        _keyframesB.value = emptyList()
        _segmentB.value = null
    }

    // =============================================================================
    // 配置变更
    // =============================================================================

    /** 切换合成模式。 */
    fun setMode(mode: ABTransportMode) {
        if (_isProcessing.value) return
        _mode.value = mode
    }

    /** 切换时长策略。 */
    fun setDurationStrategy(strategy: DurationStrategy) {
        if (_isProcessing.value) return
        _durationStrategy.value = strategy
        // 切换到非自定义时清除片段
        if (strategy != DurationStrategy.CUSTOM) {
            _segmentA.value = null
            _segmentB.value = null
        }
    }

    /** 设置 A 音量比例（0.0 ~ 1.0）。 */
    fun setVolumeRatioA(ratio: Float) {
        if (_isProcessing.value) return
        _volumeRatioA.value = ratio.coerceIn(0f, 1f)
    }

    /** 设置 B 原声音量比例（0.0 ~ 1.0）。 */
    fun setVolumeRatioB(ratio: Float) {
        if (_isProcessing.value) return
        _volumeRatioB.value = ratio.coerceIn(0f, 1f)
    }

    /** 设置 A 自定义起止点（毫秒）。 */
    fun setSegmentA(startMs: Long, endMs: Long) {
        if (_isProcessing.value) return
        if (endMs <= startMs || startMs < 0) return
        _segmentA.value = TimelineSegment(startMs = startMs, endMs = endMs)
    }

    /** 设置 B 自定义起止点（毫秒）。 */
    fun setSegmentB(startMs: Long, endMs: Long) {
        if (_isProcessing.value) return
        if (endMs <= startMs || startMs < 0) return
        _segmentB.value = TimelineSegment(startMs = startMs, endMs = endMs)
    }

    // =============================================================================
    // 合成流程
    // =============================================================================

    /**
     * 开始合成。
     *
     * 前置校验：
     * - A/B 视频均已选择
     * - A 视频包含音轨
     *
     * 成功完成后自动保存产物到素材库（标签「AB搬运」）。
     */
    fun startSynthesis() {
        if (_isProcessing.value) return
        val pathA = _videoAPath.value
        val pathB = _videoBPath.value
        if (pathA.isNullOrBlank() || pathB.isNullOrBlank()) return
        if (_audioMissing.value) return

        val outputPath = buildOutputPath(pathA, pathB)
        _isProcessing.value = true
        _error.value = null
        _outputPath.value = null
        _isSaved.value = false
        _progress.value = ABTransportProgress(
            progress = 0f,
            currentMs = 0L,
            totalMs = 0L
        )

        val config = ABTransportConfig(
            videoAPath = pathA,
            videoBPath = pathB,
            mode = _mode.value,
            durationStrategy = _durationStrategy.value,
            volumeRatioA = _volumeRatioA.value,
            volumeRatioB = _volumeRatioB.value,
            segmentA = _segmentA.value,
            segmentB = _segmentB.value,
            outputPath = outputPath
        )

        synthesisJob = viewModelScope.launch {
            val flow = try {
                abTransportUseCase(config)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _isProcessing.value = false
                _error.value = e.message ?: "启动合成失败"
                return@launch
            }

            try {
                flow.collect { p ->
                    _progress.value = p
                    if (p.isCompleted) {
                        _outputPath.value = p.outputPath
                        _isProcessing.value = false
                        // 自动保存产物到素材库
                        saveResultToMaterial(p.outputPath!!)
                    } else if (p.isFailed) {
                        _isProcessing.value = false
                        _error.value = p.error ?: "合成失败"
                    }
                }
            } catch (e: CancellationException) {
                // 用户主动取消，不视为错误
                _isProcessing.value = false
                throw e
            } catch (e: Throwable) {
                _isProcessing.value = false
                _error.value = e.message ?: "合成过程异常"
            }
        }
    }

    /**
     * 取消合成。
     *
     * 取消 [synthesisJob] 会触发 Flow 收集取消，
     * AVStreamSwapper 的 callbackFlow 通过 awaitClose 调用 FfmpegHandle.cancel()，
     * 立即停止 FFmpeg 会话，无残留进程。
     */
    fun cancelSynthesis() {
        synthesisJob?.cancel()
        synthesisJob = null
        _isProcessing.value = false
        _progress.value = null
    }

    /**
     * 重置为初始状态，便于重新合成。
     */
    fun reset() {
        cancelSynthesis()
        _outputPath.value = null
        _error.value = null
        _progress.value = null
        _isSaved.value = false
    }

    /**
     * 清除一次性错误提示。
     */
    fun consumeError() {
        _error.value = null
    }

    // =============================================================================
    // 素材库选择列表
    // =============================================================================

    /**
     * 加载素材库中所有视频类型素材，供 A/B 选择器使用。
     */
    suspend fun loadVideoMaterials(): List<MaterialEntity> = withContext(dispatchers.io) {
        runCatching { materialRepository.getMaterials() }
            .getOrDefault(emptyList())
            .filter { it.type.equals("video", ignoreCase = true) }
    }

    // =============================================================================
    // 内部：视频信息 / 音轨检测 / 关键帧
    // =============================================================================

    private fun loadVideoInfo(path: String, isVideoA: Boolean = false, isVideoB: Boolean = false) {
        viewModelScope.launch {
            try {
                val info = withContext(dispatchers.io) { dedupRepository.getVideoInfo(path) }
                if (isVideoA) _videoAInfo.value = info
                if (isVideoB) _videoBInfo.value = info
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // 解析失败保留 null，UI 退化为仅展示文件名
            }
        }
    }

    private fun checkAudioTrack(path: String) {
        viewModelScope.launch {
            try {
                val hasAudio = withContext(dispatchers.io) { dedupRepository.hasAudioTrack(path) }
                _audioMissing.value = !hasAudio
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // 检测失败时不阻止合成，让 FFmpeg 自行报错
                _audioMissing.value = false
            }
        }
    }

    private fun extractKeyframes(path: String, isVideoA: Boolean = false, isVideoB: Boolean = false) {
        viewModelScope.launch {
            try {
                val frames = withContext(dispatchers.io) {
                    dedupRepository.extractKeyframes(path, KEYFRAME_COUNT, keyframeDir.absolutePath)
                }
                if (isVideoA) _keyframesA.value = frames
                if (isVideoB) _keyframesB.value = frames
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // 关键帧提取失败不影响主流程
            }
        }
    }

    // =============================================================================
    // 内部：产物自动入库
    // =============================================================================

    /**
     * 将合成产物自动保存到素材库。
     *
     * - type = "video"
     * - source = "generated"（标记为已处理）
     * - 标签 = ["AB搬运"]（便于素材库筛选）
     *
     * 保存失败不阻塞 UI，用户仍可在结果页操作产物。
     */
    private fun saveResultToMaterial(outputPath: String) {
        viewModelScope.launch {
            try {
                val saved = withContext(dispatchers.io) {
                    materialRepository.saveMaterial(
                        path = outputPath,
                        source = "generated",
                        type = "video",
                        thumbnail = null
                    )
                }
                // 设置标签「AB搬运」
                withContext(dispatchers.io) {
                    materialRepository.updateMaterial(
                        id = saved.id,
                        tags = listOf(TAG_AB_TRANSPORT),
                        note = "AB 搬运合成产物"
                    )
                }
                _isSaved.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // 保存失败不阻塞用户操作产物
                _isSaved.value = false
            }
        }
    }

    // =============================================================================
    // 内部：输出路径生成
    // =============================================================================

    private fun buildOutputPath(pathA: String, pathB: String): String {
        val nameA = File(pathA).nameWithoutExtension
        val parent = File(pathA).parentFile ?: File(System.getProperty("java.io.tmpdir"))
        if (!parent.exists()) parent.mkdirs()
        return File(parent, "${nameA}_ab_${System.currentTimeMillis()}.mp4").absolutePath
    }

    private companion object {
        /** 关键帧提取数量（spec：3-5 帧）。 */
        const val KEYFRAME_COUNT = 4

        /** AB 搬运产物标签。 */
        const val TAG_AB_TRANSPORT = "AB搬运"
    }
}

/**
 * AB 搬运页面整体 UI 状态。
 */
sealed interface ABTransportUiState {
    /** 空闲态：可编辑配置。 */
    data object Idle : ABTransportUiState

    /** 处理中：展示进度条与取消按钮。 */
    data class Processing(
        val progressPercent: Float,
        val currentMs: Long,
        val totalMs: Long
    ) : ABTransportUiState

    /** 完成态：展示结果页与跳转入口。 */
    data class Completed(val outputPath: String) : ABTransportUiState

    /** 错误态：展示错误信息与重试。 */
    data class Error(val message: String) : ABTransportUiState
}

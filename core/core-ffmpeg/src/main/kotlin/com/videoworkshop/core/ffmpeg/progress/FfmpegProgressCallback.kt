package com.videoworkshop.core.ffmpeg.progress

import com.arthenica.ffmpegkit.Statistics
import com.arthenica.ffmpegkit.StatisticsCallback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FFmpeg 执行进度回调。
 *
 * 实现 [StatisticsCallback]，将 FFmpegKit 上报的 [Statistics] 按已处理时间
 * 占总时长的比例换算为百分比进度（0f..1f），并通过 [progressFlow] 以
 * [Flow] 的形式对外暴露，便于协程消费端订阅。
 *
 * 使用方式：
 * ```
 * val callback = FfmpegProgressCallback(totalDurationMs = 10_000)
 * FFmpegKitConfig.enableStatisticsCallback(callback)
 * // ... 执行 FFmpeg 命令 ...
 * callback.progressFlow.collect { progress -> /* 更新 UI */ }
 * ```
 *
 * @param totalDurationMs 视频总时长（毫秒），用于换算百分比。
 */
class FfmpegProgressCallback(
    private val totalDurationMs: Long
) : StatisticsCallback {

    private val _progress = MutableStateFlow(0f)

    /** 进度流，发射 0f..1f 的进度值。 */
    val progressFlow: Flow<Float> = _progress.asStateFlow()

    /** 当前进度值（0f..1f）。 */
    val currentProgress: Float
        get() = _progress.value

    /**
     * FFmpegKit 统计回调。
     *
     * [Statistics.getTime] 返回已处理时间（毫秒），除以 [totalDurationMs]
     * 得到百分比，并裁剪到 [0f, 1f] 区间。
     */
    override fun apply(statistics: Statistics) {
        val progress = if (totalDurationMs > 0L) {
            val currentTime = statistics.time // 毫秒 (Double)
            (currentTime / totalDurationMs.toDouble()).toFloat().coerceIn(MIN_PROGRESS, MAX_PROGRESS)
        } else {
            0f
        }
        _progress.value = progress
    }

    /** 重置进度为 0。 */
    fun reset() {
        _progress.value = 0f
    }

    /** 标记为完成（进度 = 1f）。 */
    fun complete() {
        _progress.value = MAX_PROGRESS
    }

    private companion object {
        const val MIN_PROGRESS = 0f
        const val MAX_PROGRESS = 1f
    }
}

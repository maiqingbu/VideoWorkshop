package com.videoworkshop.core.ffmpeg

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.LogCallback
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Session
import com.arthenica.ffmpegkit.Statistics
import com.arthenica.ffmpegkit.StatisticsCallback
import java.util.concurrent.atomic.AtomicReference

/**
 * FFmpeg 执行引擎（单例）。
 *
 * 封装 [FFmpegKit] 的同步/异步执行、取消及版本查询能力。
 * 所有命令均为单条 FFmpeg 命令行字符串，引擎不做参数校验，
 * 仅负责调度与回调通知。
 *
 * 线程安全：[currentSession] 使用 [AtomicReference] 保证并发安全。
 */
object FfmpegEngine {

    /** 当前正在执行的会话，用于取消操作。 */
    private val currentSession = AtomicReference<Session?>(null)

    /**
     * 同步执行一条 FFmpeg 命令。
     *
     * 调用线程会被阻塞直到命令执行完毕，适用于 WorkManager 等后台线程场景。
     *
     * @param command 完整的 FFmpeg 命令（不含 `ffmpeg` 前缀）。
     * @return `true` 表示执行成功（ReturnCode == 0）。
     */
    fun execute(command: String): Boolean {
        val session = FFmpegKit.execute(command)
        currentSession.set(session)
        val returnCode = session.returnCode
        return ReturnCode.isSuccess(returnCode)
    }

    /**
     * 异步执行一条 FFmpeg 命令，带进度回调。
     *
     * 通过 [FFmpegKit.executeAsync] 在内部线程池中执行，
     * [onProgress] 在统计回调触发时调用，[onComplete] 在会话结束时调用。
     *
     * @param command    完整的 FFmpeg 命令（不含 `ffmpeg` 前缀）。
     * @param onProgress 进度回调，参数为 0f..1f 的百分比。需要传入 [durationMs] 才能换算。
     * @param onComplete 完成回调，参数为 `true` 表示成功。
     * @param durationMs 视频总时长（毫秒），用于将 Statistics.time 换算为百分比。
     *                   传 0 时 [onProgress] 始终收到 0f。
     * @return 当前会话 [Session]，可用于手动取消。
     */
    fun executeAsync(
        command: String,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit,
        durationMs: Long = 0L
    ): Session {
        val statisticsCallback = StatisticsCallback { statistics: Statistics ->
            val progress = computeProgress(statistics, durationMs)
            onProgress(progress)
        }

        val executeCallback = FFmpegSessionCompleteCallback { session ->
            val success = ReturnCode.isSuccess(session.returnCode)
            onComplete(success)
        }

        val logCallback = LogCallback { /* 静默日志，可按需扩展 */ }

        val session = FFmpegKit.executeAsync(
            command,
            executeCallback,
            logCallback,
            statisticsCallback
        )
        currentSession.set(session)
        return session
    }

    /**
     * 取消当前正在执行的会话。
     *
     * @return `true` 表示存在活动会话并已发出取消请求。
     */
    fun cancel(): Boolean {
        val session = currentSession.getAndSet(null) ?: return false
        FFmpegKit.cancel(session.sessionId)
        return true
    }

    /**
     * 获取 FFmpegKit 库版本号。
     *
     * @return 版本字符串，例如 `"6.0-2"`。
     */
    fun getVersion(): String = FFmpegKitConfig.getVersion()

    /**
     * 根据 Statistics 的 time 字段和总时长换算进度百分比。
     *
     * @param statistics FFmpegKit 统计数据。
     * @param durationMs 视频总时长（毫秒）。
     * @return 0f..1f 的进度值。
     */
    private fun computeProgress(statistics: Statistics, durationMs: Long): Float {
        if (durationMs <= 0L) return 0f
        val currentTime = statistics.time // 毫秒 (Double)
        val progress = (currentTime / durationMs.toDouble()).toFloat()
        return progress.coerceIn(0f, 1f)
    }
}

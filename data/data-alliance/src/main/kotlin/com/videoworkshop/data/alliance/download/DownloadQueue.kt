package com.videoworkshop.data.alliance.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 基于 WorkManager 的下载队列：把视频下载任务投递到后台执行，并暴露进度 Flow。
 *
 * - [enqueue]：创建一次性 [VideoDownloadWorker] 并入队，返回任务 ID；
 * - [observeProgress]：把 [WorkInfo] 流映射为 [DownloadState] 有限状态机。
 */
class DownloadQueue @Inject constructor(
    private val workManager: WorkManager,
) {

    /**
     * 入队一个下载任务。
     *
     * @param url      视频下载地址
     * @param destPath 目标本地绝对路径
     * @param referer  可选 Referer 头（京东视频必填）
     * @return WorkManager 任务 ID，可用于 [observeProgress]。
     */
    fun enqueue(url: String, destPath: String, referer: String? = null): UUID {
        val inputData = Data.Builder()
            .putString(KEY_URL, url)
            .putString(KEY_DEST_PATH, destPath)
            .apply { referer?.takeIf { it.isNotBlank() }?.let { putString(KEY_REFERER, it) } }
            .build()

        val request = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueue(request)
        return request.id
    }

    /**
     * 观察指定任务的下载状态。
     */
    fun observeProgress(id: UUID): Flow<DownloadState> =
        workManager.getWorkInfoByIdFlow(id).map { info ->
            when (info?.state) {
                null,
                WorkInfo.State.ENQUEUED,
                WorkInfo.State.BLOCKED -> DownloadState.Queued

                WorkInfo.State.RUNNING -> {
                    val percent = info.progress.getInt(KEY_PROGRESS, 0)
                    DownloadState.Downloading(percent / PERCENT_SCALE.toFloat())
                }

                WorkInfo.State.SUCCEEDED ->
                    DownloadState.Completed(info.outputData.getString(KEY_PATH).orEmpty())

                WorkInfo.State.FAILED ->
                    DownloadState.Failed(info.outputData.getString(KEY_ERROR) ?: "下载失败")

                WorkInfo.State.CANCELLED ->
                    DownloadState.Failed("下载已取消")
            }
        }

    internal companion object {
        const val KEY_URL = "url"
        const val KEY_DEST_PATH = "dest_path"
        const val KEY_REFERER = "referer"
        const val KEY_PROGRESS = "progress"     // 0 ~ 100
        const val KEY_PATH = "path"             // 成功后的本地路径
        const val KEY_ERROR = "error"           // 失败原因

        private const val PERCENT_SCALE = 100
    }
}

/**
 * 视频下载 Worker：在 WorkManager 后台线程执行 [VideoDownloader]，并通过
 * `setProgress` 上报进度，便于 [DownloadQueue.observeProgress] 感知。
 *
 * 由于 [VideoDownloader] 的进度回调是非挂起的 `(Float) -> Unit`，这里用一个
 * Conflated Channel 把进度转发到一个独立协程中调用挂起的 `setProgress`，
 * 既保证进度上报，又不阻塞下载读取循环。
 */
@HiltWorker
class VideoDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: androidx.work.WorkerParameters,
    private val downloader: VideoDownloader,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(DownloadQueue.KEY_URL)
            ?: return fail("缺少下载地址")
        val destPath = inputData.getString(DownloadQueue.KEY_DEST_PATH)
            ?: return fail("缺少目标路径")
        val referer = inputData.getString(DownloadQueue.KEY_REFERER)

        return coroutineScope {
            val progressChannel = Channel<Float>(Channel.CONFLATED)
            val reporter = launch {
                for (p in progressChannel) {
                    setProgress(
                        workDataOf(
                            DownloadQueue.KEY_PROGRESS to (p * PERCENT_SCALE).toInt().coerceIn(0, PERCENT_SCALE)
                        )
                    )
                }
            }

            val result = downloader.download(url, destPath, referer) { p ->
                progressChannel.trySend(p)
            }

            progressChannel.close()
            reporter.cancel()

            if (result.isSuccess) {
                Result.success(
                    workDataOf(DownloadQueue.KEY_PATH to result.getOrNull())
                )
            } else {
                fail(result.exceptionOrNull()?.message ?: "下载失败")
            }
        }
    }

    private fun fail(message: String): Result =
        Result.failure(workDataOf(DownloadQueue.KEY_ERROR to message))

    private companion object {
        private const val PERCENT_SCALE = 100
    }
}

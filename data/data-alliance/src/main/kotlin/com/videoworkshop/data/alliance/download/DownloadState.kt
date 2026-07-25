package com.videoworkshop.data.alliance.download

/**
 * 下载任务状态。
 *
 * 用于 [DownloadQueue.observeProgress] 向上层暴露的有限状态机。
 */
sealed class DownloadState {

    /** 已入队，尚未开始下载。 */
    data object Queued : DownloadState()

    /**
     * 下载中。
     *
     * @param progress 已下载比例，取值范围 0.0 ~ 1.0。
     */
    data class Downloading(val progress: Float) : DownloadState()

    /**
     * 下载完成。
     *
     * @param path 已下载文件的本地绝对路径。
     */
    data class Completed(val path: String) : DownloadState()

    /**
     * 下载失败。
     *
     * @param error 失败原因描述。
     */
    data class Failed(val error: String) : DownloadState()
}

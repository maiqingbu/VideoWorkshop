package com.videoworkshop.domain.usecase

import com.videoworkshop.domain.model.ABTransportConfig
import com.videoworkshop.domain.model.ABTransportProgress
import com.videoworkshop.domain.repository.DedupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 执行 AB 搬运：以 A 视频作为音频源、B 视频作为画面源合成输出。
 *
 * 作为 [DedupRepository.abTransport] 的薄封装，对外提供「调用即用」的入口，
 * 与 [DedupVideoUseCase] 风格保持一致。
 *
 * 流发射序列：
 * - 若干条进行中 [ABTransportProgress]（progress 0f..1f，outputPath = null）
 * - 最终一条：成功时 progress = 1f 且 outputPath 非空；失败时 error 非空
 *
 * 调用方应在协程作用域中收集返回的 [Flow]，并根据 [ABTransportProgress.isCompleted]
 * 与 [ABTransportProgress.isFailed] 判定终态。
 *
 * 示例：
 * ```
 * abTransportUseCase(config).collect { progress ->
 *     updateUi(progress)
 *     if (progress.isCompleted) showToast("搬运完成: ${progress.outputPath}")
 *     if (progress.isFailed)    showError(progress.error!!)
 * }
 * ```
 */
class ABTransportUseCase @Inject constructor(
    private val dedupRepository: DedupRepository
) {
    /**
     * 触发 AB 搬运。
     *
     * @param config AB 搬运配置。
     * @return 进度流。
     */
    suspend operator fun invoke(config: ABTransportConfig): Flow<ABTransportProgress> {
        return dedupRepository.abTransport(config)
    }
}

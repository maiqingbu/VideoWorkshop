package com.videoworkshop.domain.model

/**
 * AB 搬运进度。
 *
 * 由 [com.videoworkshop.domain.repository.DedupRepository.abTransport] 推送，
 * 描述一次 AB 搬运任务的进度与终态。
 *
 * 流发射序列约定：
 * 1. 若干条进行中状态（[outputPath] = null，[error] = null，[progress] 在 0..1 之间递增）
 * 2. 最终一条：成功时 [progress] = 1f 且 [outputPath] 非空；失败时 [error] 非空
 *
 * 与 [DedupProgress] 的差异：
 * - [DedupProgress] 面向「分步骤」去重流程，含 stepIndex/totalSteps
 * - [ABTransportProgress] 面向「单条 FFmpeg 命令」的 AB 搬运，含 currentMs/totalMs 时间轴进度
 *
 * 错误以 [String] 形式表达（而非 [Throwable]），保持 domain 层不暴露底层异常类型，
 * 由 data 层负责将 [Throwable] 的 message 投射到 [error] 字段。
 *
 * @param progress   总体进度（0.0 ~ 1.0），完成时为 1.0
 * @param currentMs  已处理时长（毫秒）
 * @param totalMs    总时长（毫秒），用于换算百分比
 * @param outputPath 输出路径，仅完成态非空
 * @param error      失败原因，仅失败态非空
 */
data class ABTransportProgress(
    val progress: Float,
    val currentMs: Long,
    val totalMs: Long,
    val outputPath: String? = null,
    val error: String? = null
) {
    /** 是否为完成态（outputPath 非空且无错误）。 */
    val isCompleted: Boolean get() = outputPath != null && error == null

    /** 是否为失败态。 */
    val isFailed: Boolean get() = error != null
}

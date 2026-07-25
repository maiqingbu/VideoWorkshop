package com.videoworkshop.domain.model

/**
 * 一次去重任务的持久化记录。
 *
 * @param id         任务 ID
 * @param inputPath  输入视频路径
 * @param outputPath 输出视频路径
 * @param config     去重配置
 * @param state      当前状态
 * @param progress   进度（0.0 ~ 1.0）
 * @param error      失败时的错误信息
 * @param createdAt  创建时间戳（毫秒）
 */
data class DedupTask(
    val id: String,
    val inputPath: String,
    val outputPath: String,
    val config: DedupConfig,
    val state: TaskState,
    val progress: Float,
    val error: String?,
    val createdAt: Long
)

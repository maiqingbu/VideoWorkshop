package com.videoworkshop.domain.repository

import com.videoworkshop.domain.model.ABTransportConfig
import com.videoworkshop.domain.model.ABTransportProgress
import com.videoworkshop.domain.model.DedupConfig
import com.videoworkshop.domain.model.DedupProgress
import com.videoworkshop.domain.model.EnhanceConfig
import com.videoworkshop.domain.model.VideoClip
import kotlinx.coroutines.flow.Flow

/**
 * 视频处理仓库，负责去重、视频信息解析、增强与 AB 搬运。
 */
interface DedupRepository {

    /**
     * 执行视频去重，通过 [Flow] 推送去重进度。
     */
    suspend fun dedupVideo(inputPath: String, outputPath: String, config: DedupConfig): Flow<DedupProgress>

    /**
     * 解析本地视频信息。
     */
    suspend fun getVideoInfo(path: String): VideoClip

    /**
     * 检测指定视频文件是否包含音轨。
     *
     * 用于 AB 搬运前置校验：A 视频作为音频源必须包含音轨，否则无法开始合成。
     *
     * @param path 视频文件路径。
     * @return `true` 表示至少存在一条音频流。
     */
    suspend fun hasAudioTrack(path: String): Boolean

    /**
     * 从视频中提取多帧缩略图，用于关键帧预览。
     *
     * 在 [0, duration] 区间内均匀采样 [count] 帧，输出到 [outputDir] 目录。
     * 用于 AB 搬运合成前预览 A/B 视频内容。
     *
     * @param videoPath 视频文件路径。
     * @param count     提取帧数（推荐 3-5）。
     * @param outputDir 输出目录（需已存在）。
     * @return 提取成功的缩略图绝对路径列表；若视频不可读则返回空列表。
     */
    suspend fun extractKeyframes(videoPath: String, count: Int, outputDir: String): List<String>

    /**
     * 执行视频增强，通过 [Flow] 推送总体进度（0.0 ~ 1.0）。
     */
    suspend fun enhanceVideo(inputPath: String, outputPath: String, config: EnhanceConfig): Flow<Float>

    /**
     * 执行 AB 搬运：以 A 视频作为音频源、B 视频作为画面源合成输出。
     *
     * 流发射序列：
     * - 若干条进行中 [ABTransportProgress]（progress 0f..1f，outputPath = null，error = null）
     * - 最终一条：成功时 progress = 1f 且 outputPath 非空；失败时 error 非空
     *
     * 失败场景（在挂起阶段或冷流收集阶段均会以 error 进度形式发射，不抛异常）：
     * - A/B 视频文件不存在或不可读
     * - A 视频无音轨（无法作为音频源）
     * - FFmpeg 命令执行失败（返回码非 0）
     *
     * @param config AB 搬运配置。
     * @return 进度流。
     */
    suspend fun abTransport(config: ABTransportConfig): Flow<ABTransportProgress>
}

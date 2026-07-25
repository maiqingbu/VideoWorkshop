package com.videoworkshop.domain.repository

import com.videoworkshop.domain.model.DedupConfig
import com.videoworkshop.domain.model.DedupProgress
import com.videoworkshop.domain.model.EnhanceConfig
import com.videoworkshop.domain.model.VideoClip
import kotlinx.coroutines.flow.Flow

/**
 * 视频处理仓库，负责去重、视频信息解析与增强。
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
     * 执行视频增强，通过 [Flow] 推送总体进度（0.0 ~ 1.0）。
     */
    suspend fun enhanceVideo(inputPath: String, outputPath: String, config: EnhanceConfig): Flow<Float>
}

package com.videoworkshop.domain.usecase

import com.videoworkshop.domain.model.EnhanceConfig
import com.videoworkshop.domain.repository.DedupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 执行视频增强，返回总体进度流（0.0 ~ 1.0）。
 */
class EnhanceVideoUseCase @Inject constructor(
    private val dedupRepo: DedupRepository
) {
    suspend operator fun invoke(
        inputPath: String,
        outputPath: String,
        config: EnhanceConfig
    ): Flow<Float> {
        return dedupRepo.enhanceVideo(inputPath, outputPath, config)
    }
}

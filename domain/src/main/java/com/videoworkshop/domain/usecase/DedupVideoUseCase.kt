package com.videoworkshop.domain.usecase

import com.videoworkshop.domain.model.DedupConfig
import com.videoworkshop.domain.model.DedupProgress
import com.videoworkshop.domain.repository.DedupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 执行视频去重。
 */
class DedupVideoUseCase @Inject constructor(
    private val dedupRepo: DedupRepository
) {
    suspend operator fun invoke(
        inputPath: String,
        outputPath: String,
        config: DedupConfig
    ): Flow<DedupProgress> {
        return dedupRepo.dedupVideo(inputPath, outputPath, config)
    }
}

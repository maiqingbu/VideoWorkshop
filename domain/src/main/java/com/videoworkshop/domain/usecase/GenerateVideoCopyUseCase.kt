package com.videoworkshop.domain.usecase

import com.videoworkshop.domain.model.CopyResult
import com.videoworkshop.domain.repository.AiRepository
import javax.inject.Inject

/**
 * 生成视频带货文案候选。
 */
class GenerateVideoCopyUseCase @Inject constructor(
    private val aiRepo: AiRepository
) {
    suspend operator fun invoke(
        goodsName: String,
        price: Double,
        keywords: String? = null
    ): List<CopyResult> {
        return aiRepo.generateVideoCopy(goodsName, price, keywords)
    }
}

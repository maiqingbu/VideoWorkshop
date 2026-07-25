package com.videoworkshop.domain.usecase

import com.videoworkshop.domain.model.CopyResult
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.ImageTemplate
import com.videoworkshop.domain.repository.AiRepository
import javax.inject.Inject

/**
 * 基于商品与图文模板生成图文文案。
 */
class GenerateImageCopyUseCase @Inject constructor(
    private val aiRepo: AiRepository
) {
    suspend operator fun invoke(goods: Goods, template: ImageTemplate): CopyResult {
        return aiRepo.generateImageCopy(goods, template)
    }
}

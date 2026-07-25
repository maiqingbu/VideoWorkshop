package com.videoworkshop.domain.usecase

import com.videoworkshop.domain.model.ContentType
import com.videoworkshop.domain.model.PublishTarget
import com.videoworkshop.domain.repository.PublishRepository
import javax.inject.Inject

/**
 * 发布内容到目标平台。
 */
class PublishContentUseCase @Inject constructor(
    private val publishRepo: PublishRepository
) {
    suspend operator fun invoke(
        filePath: String,
        type: ContentType,
        target: PublishTarget,
        title: String,
        goodsLink: String? = null
    ): Boolean {
        return publishRepo.publish(filePath, type, target, title, goodsLink)
    }
}

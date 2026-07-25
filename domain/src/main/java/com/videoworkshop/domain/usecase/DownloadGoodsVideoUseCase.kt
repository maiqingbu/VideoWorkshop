package com.videoworkshop.domain.usecase

import com.videoworkshop.domain.model.AllianceProvider
import com.videoworkshop.domain.model.VideoSource
import com.videoworkshop.domain.repository.GoodsRepository
import javax.inject.Inject

/**
 * 获取商品关联的视频源列表。
 */
class DownloadGoodsVideoUseCase @Inject constructor(
    private val goodsRepo: GoodsRepository
) {
    suspend operator fun invoke(
        goodsId: String,
        provider: AllianceProvider
    ): List<VideoSource> {
        return goodsRepo.getGoodsVideo(goodsId, provider)
    }
}

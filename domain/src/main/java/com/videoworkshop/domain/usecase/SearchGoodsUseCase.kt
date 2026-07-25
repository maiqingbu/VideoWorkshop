package com.videoworkshop.domain.usecase

import com.videoworkshop.domain.model.AllianceProvider
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.repository.GoodsRepository
import javax.inject.Inject

/**
 * 按关键词搜索联盟商品。
 */
class SearchGoodsUseCase @Inject constructor(
    private val goodsRepo: GoodsRepository
) {
    suspend operator fun invoke(
        keyword: String,
        provider: AllianceProvider? = null
    ): List<Goods> {
        return goodsRepo.searchGoods(keyword, provider)
    }
}

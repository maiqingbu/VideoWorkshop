package com.videoworkshop.domain.repository

import com.videoworkshop.domain.model.AllianceProvider
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.VideoSource

/**
 * 联盟商品数据仓库。
 */
interface GoodsRepository {

    /**
     * 按关键词搜索商品，可指定联盟平台。
     */
    suspend fun searchGoods(keyword: String, provider: AllianceProvider?): List<Goods>

    /**
     * 获取商品详情。
     */
    suspend fun getGoodsDetail(goodsId: String, provider: AllianceProvider): Goods

    /**
     * 获取商品关联的视频源列表。
     */
    suspend fun getGoodsVideo(goodsId: String, provider: AllianceProvider): List<VideoSource>
}

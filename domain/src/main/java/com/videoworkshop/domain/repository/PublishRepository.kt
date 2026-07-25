package com.videoworkshop.domain.repository

import com.videoworkshop.domain.model.ContentType
import com.videoworkshop.domain.model.PublishTarget

/**
 * 内容发布仓库。
 */
interface PublishRepository {

    /**
     * 发布内容到目标平台。
     *
     * @param filePath  待发布文件路径
     * @param type      内容形式
     * @param target    目标平台
     * @param title     标题
     * @param goodsLink 商品推广链接，可空
     * @return 是否成功发起发布
     */
    suspend fun publish(
        filePath: String,
        type: ContentType,
        target: PublishTarget,
        title: String,
        goodsLink: String?
    ): Boolean
}

package com.videoworkshop.domain.model

/**
 * 图文内容模板。
 *
 * @param displayName 展示名称
 * @param icon        模板图标资源标识
 * @param desc        模板描述
 * @param minImages   最少所需图片数
 * @param maxImages   最多支持图片数
 */
enum class ImageTemplate(
    val displayName: String,
    val icon: String,
    val desc: String,
    val minImages: Int,
    val maxImages: Int
) {
    GOODS_RECOMMEND(
        displayName = "商品推荐",
        icon = "ic_goods_recommend",
        desc = "突出单品卖点，适合种草带货",
        minImages = 1,
        maxImages = 3
    ),
    REVIEW_SCORE(
        displayName = "测评打分",
        icon = "ic_review_score",
        desc = "评分卡式测评，展示优劣对比",
        minImages = 3,
        maxImages = 6
    ),
    LIST_COLLECTION(
        displayName = "清单合集",
        icon = "ic_list_collection",
        desc = "多商品合集，适合好物清单",
        minImages = 4,
        maxImages = 9
    ),
    FLASH_SALE(
        displayName = "促销秒杀",
        icon = "ic_flash_sale",
        desc = "强促销氛围，突出价格与限时",
        minImages = 1,
        maxImages = 4
    )
}

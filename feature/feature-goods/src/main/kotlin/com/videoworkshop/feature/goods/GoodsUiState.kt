package com.videoworkshop.feature.goods

import com.videoworkshop.domain.model.AllianceProvider
import com.videoworkshop.domain.model.ContentType
import com.videoworkshop.domain.model.Goods

/**
 * 商品搜索页 UI 状态。
 *
 * @param query         当前搜索关键词。
 * @param provider      当前选中的联盟平台。
 * @param results       搜索/推荐结果列表（关键词为空时展示热门推荐）。
 * @param isSearching   是否正在搜索。
 * @param selectedGoods 已选中的商品，路由侧观察其变化触发跳转后清空。
 * @param mode          本次创作的模式（视频 / 图文），由导航参数决定后续跳转去向。
 * @param isLibrary     是否为「商品库」入口（独立路由 goods-library），影响标题展示。
 * @param error         一次性错误提示（如链接解析失败），UI 显示后需调用 [GoodsViewModel.consumeError] 清空。
 */
data class GoodsUiState(
    val query: String = "",
    val provider: AllianceProvider = AllianceProvider.TAOBAO,
    val results: List<Goods> = emptyList(),
    val isSearching: Boolean = false,
    val selectedGoods: Goods? = null,
    val mode: ContentType = ContentType.VIDEO,
    val isLibrary: Boolean = false,
    val error: String? = null
)

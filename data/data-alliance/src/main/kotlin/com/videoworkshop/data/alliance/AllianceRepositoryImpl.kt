package com.videoworkshop.data.alliance

import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.data.alliance.jd.JdUnionClient
import com.videoworkshop.data.alliance.jd.toVideoSources
import com.videoworkshop.data.alliance.mock.MockGoodsData
import com.videoworkshop.data.alliance.pdd.PddDdkClient
import com.videoworkshop.data.alliance.pdd.toGoods as pddToGoods
import com.videoworkshop.data.alliance.taobao.TaobaoTopClient
import com.videoworkshop.data.alliance.taobao.toGoods as taobaoToGoods
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.VideoSource
import com.videoworkshop.domain.repository.GoodsRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 联盟商品仓库实现。
 *
 * 通过 [AllianceProvider] 把请求路由到各平台客户端（淘宝 TOP / 京东联盟 / 拼多多 DDK）。
 *
 * 当前三方联盟 API 资质均未申请，各客户端 [isConfigured] 返回 false，
 * 因此所有方法当前都返回 [MockGoodsData] 中的 Mock 数据。资质申请到位后，
 * 只需在客户端构造处注入真实 appKey/appSecret（或 client_id/client_secret），
 * 即可自动切换到真实接口，无需改动本仓库。
 *
 * @param taobaoClient 淘宝 TOP 客户端
 * @param jdClient     京东联盟客户端
 * @param pddClient    拼多多 DDK 客户端
 * @param dispatchers  协程调度器
 */
class AllianceRepositoryImpl @Inject constructor(
    private val taobaoClient: TaobaoTopClient,
    private val jdClient: JdUnionClient,
    private val pddClient: PddDdkClient,
    private val dispatchers: DispatcherProvider,
) : GoodsRepository {

    override suspend fun searchGoods(
        keyword: String,
        provider: AllianceProvider?,
    ): List<Goods> = withContext(dispatchers.io) {
        // 资质未申请：直接返回 Mock 列表，按 keyword / provider 过滤。
        //
        // 真实搜索示意（资质申请后在各 provider 内实现）：
        //   TAOBAO -> taobao.tbk.item.get（关键词搜索）
        //   JD     -> jd.union.open.goods.query
        //   PDD    -> pdd.ddk.goods.search
        val byProvider = provider
            ?.let { p -> MockGoodsData.mockGoodsList.filter { it.provider == p } }
            ?: MockGoodsData.mockGoodsList

        if (keyword.isBlank()) {
            byProvider
        } else {
            byProvider.filter { it.name.contains(keyword, ignoreCase = true) }
        }
    }

    override suspend fun getGoodsDetail(
        goodsId: String,
        provider: AllianceProvider,
    ): Goods = withContext(dispatchers.io) {
        when (provider) {
            AllianceProvider.TAOBAO -> {
                // 真实调用（资质申请后生效）：
                if (taobaoClient.isConfigured()) {
                    taobaoClient.getItemDetail(platformId(goodsId)).taobaoToGoods()
                } else {
                    mockDetail(goodsId, provider)
                }
            }

            AllianceProvider.JD -> {
                // 京东联盟商品基础信息需走 jd.union.open.goods.query，
                // 本模块当前仅实现媒体查询（视频），故详情暂回退 Mock。
                mockDetail(goodsId, provider)
            }

            AllianceProvider.PDD -> {
                // 真实调用（资质申请后生效）：
                if (pddClient.isConfigured()) {
                    pddClient.getGoodsDetail(platformId(goodsId)).pddToGoods(goodsId)
                } else {
                    mockDetail(goodsId, provider)
                }
            }
        }
    }

    override suspend fun getGoodsVideo(
        goodsId: String,
        provider: AllianceProvider,
    ): List<VideoSource> = withContext(dispatchers.io) {
        when (provider) {
            AllianceProvider.TAOBAO -> {
                // 真实调用：商品详情中的主图视频
                if (taobaoClient.isConfigured()) {
                    taobaoClient.getItemDetail(platformId(goodsId)).taobaoToGoods().videoSources
                } else {
                    MockGoodsData.videoSourcesOf(goodsId)
                }
            }

            AllianceProvider.JD -> {
                // 真实调用：京东联盟媒体查询（受 QPS≤5 令牌桶限流）
                if (jdClient.isConfigured()) {
                    jdClient.queryGoodsMedia(platformId(goodsId)).toVideoSources()
                } else {
                    MockGoodsData.videoSourcesOf(goodsId)
                }
            }

            AllianceProvider.PDD -> {
                // 真实调用：商品详情中的 video_urls
                if (pddClient.isConfigured()) {
                    pddClient.getGoodsDetail(platformId(goodsId)).pddToGoods(goodsId).videoSources
                } else {
                    MockGoodsData.videoSourcesOf(goodsId)
                }
            }
        }
    }

    /** 取 Mock 详情，未命中则抛出明确异常。 */
    private fun mockDetail(goodsId: String, provider: AllianceProvider): Goods =
        MockGoodsData.findGoods(goodsId, provider)
            ?: error(
                "Mock 中未找到 $provider 商品 $goodsId，且该联盟资质未配置，" +
                    "请在申请到资质后启用真实调用"
            )

    /**
     * 从商品 ID 中提取平台侧数字 ID。
     *
     * Mock ID 形如 `tb_683321458901`，去掉前缀后取数字部分；若已是纯数字则直接解析。
     */
    private fun platformId(goodsId: String): Long =
        goodsId.toLongOrNull()
            ?: goodsId.substringAfter('_').toLongOrNull()
            ?: 0L
}

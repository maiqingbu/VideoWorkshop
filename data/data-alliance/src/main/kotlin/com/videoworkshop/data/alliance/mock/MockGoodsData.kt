package com.videoworkshop.data.alliance.mock

import com.videoworkshop.data.alliance.AllianceProvider
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.VideoSource

/**
 * 联盟商品 Mock 数据。
 *
 * 联盟 API（淘宝 TOP / 京东联盟 / 拼多多 DDK）资质尚未申请期间，
 * [com.videoworkshop.data.alliance.AllianceRepositoryImpl] 在未配置凭证时
 * 统一返回此处的 Mock 数据，保证搜索 / 详情 / 视频源链路可联调。
 *
 * 数据覆盖 3 个平台、6 个商品（每平台 2 个），视频源使用各平台公开的
 * CDN 测试 URL 格式（如 `https://cloud.video.taobao.com/play/u/xxx.mp4`），
 * 每个商品 1 ~ 2 个视频地址。
 */
object MockGoodsData {

    // ===== 淘宝（TAOBAO）=====

    /** 1. 零食大礼包 —— 2 个视频 */
    private const val TB_SNACK_ID = "tb_683321458901"

    /** 2. 保湿面霜 —— 1 个视频 */
    private const val TB_CREAM_ID = "tb_671298843122"

    // ===== 京东（JD）=====

    /** 3. 蓝牙耳机 —— 2 个视频 */
    private const val JD_EARBUD_ID = "jd_100880766293"

    /** 4. 运动水杯 —— 1 个视频 */
    private const val JD_BOTTLE_ID = "jd_100993455718"

    // ===== 拼多多（PDD）=====

    /** 5. 手机支架 —— 2 个视频 */
    private const val PDD_STAND_ID = "pdd_2094831726"

    /** 6. 收纳盒 —— 1 个视频 */
    private const val PDD_BOX_ID = "pdd_2097113885"

    /**
     * 每个商品 ID 对应的视频源列表。
     *
     * 视频地址采用各平台公开测试 CDN 格式，京东视频源标注需要 Referer 头
     * （由 [com.videoworkshop.data.alliance.download.VideoDownloader] 处理）。
     */
    val mockVideoSources: Map<String, List<VideoSource>> = buildMap {
        // 淘宝云视频：cloud.video.taobao.com/play/u/{userId}/p/2/e/6/t/1/{videoId}.mp4
        put(
            TB_SNACK_ID,
            listOf(
                vs(
                    url = "https://cloud.video.taobao.com/play/u/2553075807/p/2/e/6/t/1/50078072377.mp4",
                    cover = "https://img.alicdn.com/imgextra/i4/2200758493821/O1CN01snack01_!!2200758493821.jpg",
                    durationMs = 28_400L,
                ),
                vs(
                    url = "https://cloud.video.taobao.com/play/u/2553075807/p/2/e/6/t/1/50078072378.mp4",
                    cover = "https://img.alicdn.com/imgextra/i3/2200758493821/O1CN01snack02_!!2200758493821.jpg",
                    durationMs = 16_800L,
                ),
            ),
        )
        put(
            TB_CREAM_ID,
            listOf(
                vs(
                    url = "https://cloud.video.taobao.com/play/u/2810084392/p/2/e/6/t/1/50124088302.mp4",
                    cover = "https://img.alicdn.com/imgextra/i2/2200849301276/O1CN01cream01_!!2200849301276.jpg",
                    durationMs = 35_200L,
                ),
            ),
        )

        // 京东视频 CDN：vod.300hu.com（下载时需带 Referer: https://www.jd.com）
        put(
            JD_EARBUD_ID,
            listOf(
                vs(
                    url = "https://vod.300hu.com/4c1f2e3a8b9c6d7e0f1a2b3c4d5e6f7a/gu/bt-earbuds-01.mp4",
                    cover = "https://img14.360buyimg.com/n0/jfs/t1/180882/17/12876/120678/611a2b3cEarbuds01/abc.jpg",
                    durationMs = 41_600L,
                ),
                vs(
                    url = "https://vod.300hu.com/5d2e3f4b9c0d1e2f3a4b5c6d7e8f9a0b/gu/bt-earbuds-02.mp4",
                    cover = "https://img14.360buyimg.com/n0/jfs/t1/200104/22/14012/135521/611a2b3cEarbuds02/def.jpg",
                    durationMs = 22_000L,
                ),
            ),
        )
        put(
            JD_BOTTLE_ID,
            listOf(
                vs(
                    url = "https://vod.300hu.com/6e3f4a5c0d1e2f3a4b5c6d7e8f9a0b1c/gu/sport-bottle-01.mp4",
                    cover = "https://img14.360buyimg.com/n0/jfs/t1/155012/9/11023/88421/612b3c4dBottle01/ghi.jpg",
                    durationMs = 19_500L,
                ),
            ),
        )

        // 拼多多视频 CDN：omsproductionimg.yangkeduo.com
        put(
            PDD_STAND_ID,
            listOf(
                vs(
                    url = "https://omsproductionimg.yangkeduo.com/goods/images/2022-03-12/phone-stand-01.mp4",
                    cover = "https://t00img.yangkeduo.com/goods/images/2022-03-12/phone-stand-cover-01.jpeg",
                    durationMs = 24_000L,
                ),
                vs(
                    url = "https://omsproductionimg.yangkeduo.com/goods/images/2022-03-12/phone-stand-02.mp4",
                    cover = "https://t00img.yangkeduo.com/goods/images/2022-03-12/phone-stand-cover-02.jpeg",
                    durationMs = 12_800L,
                ),
            ),
        )
        put(
            PDD_BOX_ID,
            listOf(
                vs(
                    url = "https://omsproductionimg.yangkeduo.com/goods/images/2022-04-05/storage-box-01.mp4",
                    cover = "https://t00img.yangkeduo.com/goods/images/2022-04-05/storage-box-cover-01.jpeg",
                    durationMs = 31_200L,
                ),
            ),
        )
    }

    /**
     * Mock 商品列表（6 个，覆盖 3 个平台）。
     */
    val mockGoodsList: List<Goods> = listOf(
        // ===== 淘宝 =====
        Goods(
            id = TB_SNACK_ID,
            provider = AllianceProvider.TAOBAO,
            name = "三只松鼠坚果零食大礼包 1.5kg 混合干果每日坚果",
            price = 39.9,
            originalPrice = 59.9,
            commissionRate = 0.20,
            promoUrl = "https://s.click.taobao.com/t?e=m%3D2%26s%3Dtb_snack_683321458901",
            imageUrl = "https://img.alicdn.com/imgextra/i4/2200758493821/O1CN01snack01_!!2200758493821.jpg",
            videoSources = mockVideoSources[TB_SNACK_ID].orEmpty(),
        ),
        Goods(
            id = TB_CREAM_ID,
            provider = AllianceProvider.TAOBAO,
            name = "珀莱雅红宝石面霜 保湿补水抗皱紧致 滋润修护 50g",
            price = 128.0,
            originalPrice = 199.0,
            commissionRate = 0.15,
            promoUrl = "https://s.click.taobao.com/t?e=m%3D2%26s%3Dtb_cream_671298843122",
            imageUrl = "https://img.alicdn.com/imgextra/i2/2200849301276/O1CN01cream01_!!2200849301276.jpg",
            videoSources = mockVideoSources[TB_CREAM_ID].orEmpty(),
        ),

        // ===== 京东 =====
        Goods(
            id = JD_EARBUD_ID,
            provider = AllianceProvider.JD,
            name = "小米 Redmi Buds 5 真无线蓝牙耳机 降噪长续航 通用手机",
            price = 199.0,
            originalPrice = 299.0,
            commissionRate = 0.10,
            promoUrl = "https://u.jd.com/earbud_100880766293",
            imageUrl = "https://img14.360buyimg.com/n0/jfs/t1/180882/17/12876/120678/611a2b3cEarbuds01/abc.jpg",
            videoSources = mockVideoSources[JD_EARBUD_ID].orEmpty(),
        ),
        Goods(
            id = JD_BOTTLE_ID,
            provider = AllianceProvider.JD,
            name = "富光 Tritan 运动水杯 大容量便携男女学生健身户外水壶 1L",
            price = 49.9,
            originalPrice = 79.9,
            commissionRate = 0.12,
            promoUrl = "https://u.jd.com/bottle_100993455718",
            imageUrl = "https://img14.360buyimg.com/n0/jfs/t1/155012/9/11023/88421/612b3c4dBottle01/ghi.jpg",
            videoSources = mockVideoSources[JD_BOTTLE_ID].orEmpty(),
        ),

        // ===== 拼多多 =====
        Goods(
            id = PDD_STAND_ID,
            provider = AllianceProvider.PDD,
            name = "桌面手机支架折叠落地直播通用平板床头追剧神器",
            price = 19.9,
            originalPrice = 35.0,
            commissionRate = 0.25,
            promoUrl = "https://mobile.yangkeduo.com/goods.html?goods_id=2094831726",
            imageUrl = "https://t00img.yangkeduo.com/goods/images/2022-03-12/phone-stand-cover-01.jpeg",
            videoSources = mockVideoSources[PDD_STAND_ID].orEmpty(),
        ),
        Goods(
            id = PDD_BOX_ID,
            provider = AllianceProvider.PDD,
            name = "桌面透明收纳盒 多功能化妆品杂物整理盒 大容量分格储物盒",
            price = 29.9,
            originalPrice = 49.9,
            commissionRate = 0.18,
            promoUrl = "https://mobile.yangkeduo.com/goods.html?goods_id=2097113885",
            imageUrl = "https://t00img.yangkeduo.com/goods/images/2022-04-05/storage-box-cover-01.jpeg",
            videoSources = mockVideoSources[PDD_BOX_ID].orEmpty(),
        ),
    )

    /** 按商品 ID 与平台查找 Mock 商品。 */
    fun findGoods(goodsId: String, provider: AllianceProvider): Goods? =
        mockGoodsList.firstOrNull { it.id == goodsId && it.provider == provider }

    /** 按商品 ID 查找 Mock 视频源列表。 */
    fun videoSourcesOf(goodsId: String): List<VideoSource> =
        mockVideoSources[goodsId].orEmpty()

    /** 构造视频源。 */
    private fun vs(
        url: String,
        cover: String,
        durationMs: Long,
        format: String = "mp4",
    ): VideoSource = VideoSource(
        url = url,
        coverUrl = cover,
        duration = durationMs,
        format = format,
    )
}

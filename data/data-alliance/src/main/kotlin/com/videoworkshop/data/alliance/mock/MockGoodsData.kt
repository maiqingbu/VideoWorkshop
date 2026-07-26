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
 * FIX-05：每平台扩量至 10 条，覆盖服饰 / 数码 / 家居 / 美妆 / 食品 / 母婴 /
 * 运动 / 图书 / 虚拟 / 汽车用品 等主流带货品类。
 * 视频源使用各平台公开的 CDN 测试 URL 格式（如 `https://cloud.video.taobao.com/play/u/xxx.mp4`），
 * 仅原 6 个老商品保留视频源；新增商品 videoSources 留空（与真实联盟一致——并非所有商品都有素材视频）。
 */
object MockGoodsData {

    // ===== 淘宝（TAOBAO）=====

    /** 1. 零食大礼包 —— 2 个视频 */
    private const val TB_SNACK_ID = "tb_683321458901"

    /** 2. 保湿面霜 —— 1 个视频 */
    private const val TB_CREAM_ID = "tb_671298843122"

    /** 3. 男士T恤（服饰） */
    private const val TB_TSHIRT_ID = "tb_692014587301"

    /** 4. 智能手表（数码） */
    private const val TB_WATCH_ID = "tb_680123477890"

    /** 5. 加湿器（家居） */
    private const val TB_HUMIDIFIER_ID = "tb_675098123456"

    /** 6. 婴儿纸尿裤（母婴） */
    private const val TB_DIAPER_ID = "tb_669876543210"

    /** 7. 瑜伽垫（运动） */
    private const val TB_YOGA_ID = "tb_688234567890"

    /** 8. 畅销小说（图书） */
    private const val TB_BOOK_ID = "tb_694567890123"

    /** 9. 视频会员充值（虚拟） */
    private const val TB_VIP_ID = "tb_697890123456"

    /** 10. 车载手机支架（汽车用品） */
    private const val TB_CAR_MOUNT_ID = "tb_699123456789"

    // ===== 京东（JD）=====

    /** 1. 蓝牙耳机 —— 2 个视频 */
    private const val JD_EARBUD_ID = "jd_100880766293"

    /** 2. 运动水杯 —— 1 个视频 */
    private const val JD_BOTTLE_ID = "jd_100993455718"

    /** 3. 牛仔裤（服饰） */
    private const val JD_JEANS_ID = "jd_101123456789"

    /** 4. 机械键盘（数码） */
    private const val JD_KEYBOARD_ID = "jd_101234567890"

    /** 5. 收纳衣柜（家居） */
    private const val JD_WARDROBE_ID = "jd_101345678901"

    /** 6. 口红套装（美妆） */
    private const val JD_LIPSTICK_ID = "jd_101456789012"

    /** 7. 坚果礼盒（食品） */
    private const val JD_NUT_ID = "jd_101567890123"

    /** 8. 婴儿推车（母婴） */
    private const val JD_STROLLER_ID = "jd_101678901234"

    /** 9. 畅销书（图书） */
    private const val JD_BOOK_ID = "jd_101789012345"

    /** 10. 游戏点卡（虚拟） */
    private const val JD_GAMECARD_ID = "jd_101890123456"

    // ===== 拼多多（PDD）=====

    /** 1. 手机支架 —— 2 个视频 */
    private const val PDD_STAND_ID = "pdd_2094831726"

    /** 2. 收纳盒 —— 1 个视频 */
    private const val PDD_BOX_ID = "pdd_2097113885"

    /** 3. 卫衣（服饰） */
    private const val PDD_HOODIE_ID = "pdd_2101234567"

    /** 4. 充电宝（数码） */
    private const val PDD_POWERBANK_ID = "pdd_2102345678"

    /** 5. 抱枕（家居） */
    private const val PDD_PILLOW_ID = "pdd_2103456789"

    /** 6. 面膜（美妆） */
    private const val PDD_MASK_ID = "pdd_2104567890"

    /** 7. 水果（食品） */
    private const val PDD_FRUIT_ID = "pdd_2105678901"

    /** 8. 奶粉（母婴） */
    private const val PDD_MILK_ID = "pdd_2106789012"

    /** 9. 跳绳（运动） */
    private const val PDD_ROPE_ID = "pdd_2107890123"

    /** 10. 汽车脚垫（汽车用品） */
    private const val PDD_MAT_ID = "pdd_2108901234"

    /**
     * 每个商品 ID 对应的视频源列表。
     *
     * 视频地址采用各平台公开测试 CDN 格式，京东视频源标注需要 Referer 头
     * （由 [com.videoworkshop.data.alliance.download.VideoDownloader] 处理）。
     * 仅原 6 个老商品保留视频源；新增商品无视频源（与真实联盟一致）。
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
     * Mock 商品列表（30 个，覆盖 3 个平台 × 10 个品类）。
     *
     * 新增商品（每平台 8 个）使用 picsum.photos 占位图，与原 6 个老商品的真实 CDN 封面区分；
     * 视频源仅老商品保留，新商品留空。
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
        Goods(
            id = TB_TSHIRT_ID,
            provider = AllianceProvider.TAOBAO,
            name = "男士短袖T恤夏季纯棉修身翻领半袖POLO衫 男装上衣",
            price = 79.0,
            originalPrice = 129.0,
            commissionRate = 0.18,
            promoUrl = "https://s.click.taobao.com/t?e=m%3D2%26s%3Dtb_tshirt_692014587301",
            imageUrl = "https://picsum.photos/seed/tb-tshirt/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = TB_WATCH_ID,
            provider = AllianceProvider.TAOBAO,
            name = "智能手表蓝牙通话运动计步心率监测男女多功能手环",
            price = 199.0,
            originalPrice = 399.0,
            commissionRate = 0.12,
            promoUrl = "https://s.click.taobao.com/t?e=m%3D2%26s%3Dtb_watch_680123477890",
            imageUrl = "https://picsum.photos/seed/tb-watch/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = TB_HUMIDIFIER_ID,
            provider = AllianceProvider.TAOBAO,
            name = "加湿器家用静音卧室大容量雾量办公室空调房补水净化",
            price = 89.0,
            originalPrice = 159.0,
            commissionRate = 0.22,
            promoUrl = "https://s.click.taobao.com/t?e=m%3D2%26s%3Dtb_humidifier_675098123456",
            imageUrl = "https://picsum.photos/seed/tb-humidifier/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = TB_DIAPER_ID,
            provider = AllianceProvider.TAOBAO,
            name = "婴儿纸尿裤超薄透气新生儿拉拉裤尺码 S/M/L/XL 大包装",
            price = 109.0,
            originalPrice = 169.0,
            commissionRate = 0.16,
            promoUrl = "https://s.click.taobao.com/t?e=m%3D2%26s%3Dtb_diaper_669876543210",
            imageUrl = "https://picsum.photos/seed/tb-diaper/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = TB_YOGA_ID,
            provider = AllianceProvider.TAOBAO,
            name = "瑜伽垫加厚防滑女初学者健身垫运动地垫男减肥垫子",
            price = 49.0,
            originalPrice = 99.0,
            commissionRate = 0.25,
            promoUrl = "https://s.click.taobao.com/t?e=m%3D2%26s%3Dtb_yoga_688234567890",
            imageUrl = "https://picsum.photos/seed/tb-yoga/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = TB_BOOK_ID,
            provider = AllianceProvider.TAOBAO,
            name = "活着 余华 当代文学小说畅销书籍正版包邮",
            price = 25.0,
            originalPrice = 39.0,
            commissionRate = 0.30,
            promoUrl = "https://s.click.taobao.com/t?e=m%3D2%26s%3Dtb_book_694567890123",
            imageUrl = "https://picsum.photos/seed/tb-book/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = TB_VIP_ID,
            provider = AllianceProvider.TAOBAO,
            name = "腾讯视频VIP会员12个月年卡官方直充手机话费充值",
            price = 178.0,
            originalPrice = 253.0,
            commissionRate = 0.08,
            promoUrl = "https://s.click.taobao.com/t?e=m%3D2%26s%3Dtb_vip_697890123456",
            imageUrl = "https://picsum.photos/seed/tb-vip/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = TB_CAR_MOUNT_ID,
            provider = AllianceProvider.TAOBAO,
            name = "车载手机支架车用导航仪吸盘式通用出风口固定架汽车用品",
            price = 29.9,
            originalPrice = 59.0,
            commissionRate = 0.20,
            promoUrl = "https://s.click.taobao.com/t?e=m%3D2%26s%3Dtb_carmount_699123456789",
            imageUrl = "https://picsum.photos/seed/tb-carmount/400/400",
            videoSources = emptyList(),
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
        Goods(
            id = JD_JEANS_ID,
            provider = AllianceProvider.JD,
            name = " Levi's李维斯男士牛仔裤直筒宽松秋冬休闲长裤 505经典款",
            price = 299.0,
            originalPrice = 499.0,
            commissionRate = 0.10,
            promoUrl = "https://u.jd.com/jeans_101123456789",
            imageUrl = "https://picsum.photos/seed/jd-jeans/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = JD_KEYBOARD_ID,
            provider = AllianceProvider.JD,
            name = "罗技 K845 机械键盘 有线背光游戏键盘 104键全键无冲",
            price = 299.0,
            originalPrice = 399.0,
            commissionRate = 0.10,
            promoUrl = "https://u.jd.com/keyboard_101234567890",
            imageUrl = "https://picsum.photos/seed/jd-keyboard/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = JD_WARDROBE_ID,
            provider = AllianceProvider.JD,
            name = "简易布衣柜租房用收纳衣柜大容量防尘组合式衣柜加固",
            price = 119.0,
            originalPrice = 199.0,
            commissionRate = 0.15,
            promoUrl = "https://u.jd.com/wardrobe_101345678901",
            imageUrl = "https://picsum.photos/seed/jd-wardrobe/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = JD_LIPSTICK_ID,
            provider = AllianceProvider.JD,
            name = "完美日记小细管口红唇釉哑光雾面持久显白女生日礼物套装",
            price = 99.0,
            originalPrice = 159.0,
            commissionRate = 0.18,
            promoUrl = "https://u.jd.com/lipstick_101456789012",
            imageUrl = "https://picsum.photos/seed/jd-lipstick/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = JD_NUT_ID,
            provider = AllianceProvider.JD,
            name = "百草味每日坚果750g 30小包混合干果仁零食大礼包",
            price = 69.9,
            originalPrice = 99.9,
            commissionRate = 0.12,
            promoUrl = "https://u.jd.com/nut_101567890123",
            imageUrl = "https://picsum.photos/seed/jd-nut/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = JD_STROLLER_ID,
            provider = AllianceProvider.JD,
            name = "好孩子婴儿推车可坐可躺轻便折叠宝宝双向高景观遛娃神器",
            price = 599.0,
            originalPrice = 899.0,
            commissionRate = 0.10,
            promoUrl = "https://u.jd.com/stroller_101678901234",
            imageUrl = "https://picsum.photos/seed/jd-stroller/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = JD_BOOK_ID,
            provider = AllianceProvider.JD,
            name = "三体三部曲 刘慈欣科幻小说全集地球往事正版包邮",
            price = 89.0,
            originalPrice = 138.0,
            commissionRate = 0.20,
            promoUrl = "https://u.jd.com/book_101789012345",
            imageUrl = "https://picsum.photos/seed/jd-book/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = JD_GAMECARD_ID,
            provider = AllianceProvider.JD,
            name = "Steam钱包充值卡 100元 PC游戏点卡 数字版自动发货",
            price = 95.0,
            originalPrice = 100.0,
            commissionRate = 0.05,
            promoUrl = "https://u.jd.com/gamecard_101890123456",
            imageUrl = "https://picsum.photos/seed/jd-gamecard/400/400",
            videoSources = emptyList(),
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
        Goods(
            id = PDD_HOODIE_ID,
            provider = AllianceProvider.PDD,
            name = "卫衣女宽松外套秋季韩版宽松短款上衣春秋休闲时尚",
            price = 59.0,
            originalPrice = 99.0,
            commissionRate = 0.22,
            promoUrl = "https://mobile.yangkeduo.com/goods.html?goods_id=2101234567",
            imageUrl = "https://picsum.photos/seed/pdd-hoodie/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = PDD_POWERBANK_ID,
            provider = AllianceProvider.PDD,
            name = "充电宝20000毫安超薄便携式快充移动电源适用苹果安卓",
            price = 69.0,
            originalPrice = 129.0,
            commissionRate = 0.15,
            promoUrl = "https://mobile.yangkeduo.com/goods.html?goods_id=2102345678",
            imageUrl = "https://picsum.photos/seed/pdd-powerbank/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = PDD_PILLOW_ID,
            provider = AllianceProvider.PDD,
            name = "抱枕沙发靠垫床头大号可爱卡通床上靠枕客厅靠背垫子",
            price = 25.9,
            originalPrice = 49.0,
            commissionRate = 0.20,
            promoUrl = "https://mobile.yangkeduo.com/goods.html?goods_id=2103456789",
            imageUrl = "https://picsum.photos/seed/pdd-pillow/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = PDD_MASK_ID,
            provider = AllianceProvider.PDD,
            name = "面膜补水保湿女20片装紧致毛孔淡纹精华深层滋润",
            price = 39.9,
            originalPrice = 89.0,
            commissionRate = 0.25,
            promoUrl = "https://mobile.yangkeduo.com/goods.html?goods_id=2104567890",
            imageUrl = "https://picsum.photos/seed/pdd-mask/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = PDD_FRUIT_ID,
            provider = AllianceProvider.PDD,
            name = "广西沃柑5斤新鲜水果当季整箱蜜桔甜橙柑橘包邮",
            price = 29.9,
            originalPrice = 49.9,
            commissionRate = 0.18,
            promoUrl = "https://mobile.yangkeduo.com/goods.html?goods_id=2105678901",
            imageUrl = "https://picsum.photos/seed/pdd-fruit/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = PDD_MILK_ID,
            provider = AllianceProvider.PDD,
            name = "婴儿奶粉1段0-6个月新生儿配方羊奶粉800g罐装正品",
            price = 199.0,
            originalPrice = 298.0,
            commissionRate = 0.12,
            promoUrl = "https://mobile.yangkeduo.com/goods.html?goods_id=2106789012",
            imageUrl = "https://picsum.photos/seed/pdd-milk/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = PDD_ROPE_ID,
            provider = AllianceProvider.PDD,
            name = "跳绳成人专业减肥健身无线负重计数钢丝运动绳学生中考",
            price = 15.9,
            originalPrice = 29.9,
            commissionRate = 0.30,
            promoUrl = "https://mobile.yangkeduo.com/goods.html?goods_id=2107890123",
            imageUrl = "https://picsum.photos/seed/pdd-rope/400/400",
            videoSources = emptyList(),
        ),
        Goods(
            id = PDD_MAT_ID,
            provider = AllianceProvider.PDD,
            name = "汽车脚垫全包围加厚丝圈车用内饰定制尾箱后备箱垫",
            price = 89.0,
            originalPrice = 199.0,
            commissionRate = 0.15,
            promoUrl = "https://mobile.yangkeduo.com/goods.html?goods_id=2108901234",
            imageUrl = "https://picsum.photos/seed/pdd-mat/400/400",
            videoSources = emptyList(),
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

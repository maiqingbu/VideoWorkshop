package com.videoworkshop.data.alliance.pdd

import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.data.alliance.pdd.model.PddGoodsDetailResponse
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Locale

/**
 * 拼多多联盟（DDK）客户端：负责公共参数组装、MD5 签名与接口调用。
 *
 * 拼多多 DDK 签名算法（官方）：
 * 1. 把所有请求参数（含公共参数与业务参数，不含 `sign`）按参数名 ASCII 升序排序；
 * 2. 按排序结果无缝拼接 `key1value1key2value2...` 形成基础字符串；
 * 3. 在基础字符串头尾各拼接一次 `client_secret`：
 *    `client_secret + 基础字符串 + client_secret`；
 * 4. 对最终字符串做 MD5，结果转大写，即 `sign`。
 *
 * 当前拼多多联盟 API 资质尚未申请，[isConfigured] 返回 false，
 * 仓库层据此回退到 Mock 数据。
 *
 * @param clientId     拼多多 DDK client_id（资质申请后注入）
 * @param clientSecret 拼多多 DDK client_secret（资质申请后注入）
 * @param api          拼多多 DDK Retrofit 接口
 * @param dispatchers  协程调度器
 */
class PddDdkClient(
    private val clientId: String = "",
    private val clientSecret: String = "",
    private val api: PddDdkApi,
    private val dispatchers: DispatcherProvider,
) {

    /** 是否已配置可用凭证。 */
    fun isConfigured(): Boolean = clientId.isNotBlank() && clientSecret.isNotBlank()

    /**
     * 构造公共参数（不含 `sign`）。时间戳为秒级。
     */
    fun buildCommonParams(
        type: String,
        timestamp: Long = System.currentTimeMillis() / 1000,
    ): Map<String, String> = buildMap {
        put("type", type)
        put("client_id", clientId)
        put("timestamp", timestamp.toString())
        put("data_type", "JSON")
        put("version", "1.0")
    }

    /**
     * 组装公共 + 业务参数并计算签名，返回可直接发起请求的完整参数集。
     *
     * @param type        接口方法名，如 `pdd.ddk.goods.detail`
     * @param bizParams   业务参数（会被 JSON 序列化后作为字符串值参与签名）
     */
    fun buildSignedParams(
        type: String,
        bizParams: Map<String, String>,
    ): Map<String, String> {
        val all = buildCommonParams(type) + bizParams
        val sign = sign(all, clientSecret)
        return all + ("sign" to sign)
    }

    /**
     * 拼多多 MD5 签名：`MD5(clientSecret + sorted(key+value) + clientSecret)`，结果大写。
     */
    fun sign(params: Map<String, String>, clientSecret: String): String {
        val sb = StringBuilder(clientSecret)
        params.toSortedMap().forEach { (k, v) ->
            sb.append(k).append(v)
        }
        sb.append(clientSecret)
        return md5Hex(sb.toString()).uppercase(Locale.US)
    }

    /**
     * 调用 `pdd.ddk.goods.detail` 获取商品详情。
     *
     * 资质未申请时抛出 [IllegalStateException]，调用方应优先返回 Mock。
     */
    suspend fun getGoodsDetail(goodsId: Long): PddGoodsDetailResponse =
        withContext(dispatchers.io) {
            check(isConfigured()) {
                "拼多多 DDK 资质未配置，请在申请到 client_id/client_secret 后启用真实调用"
            }
            val params = buildSignedParams(
                type = "pdd.ddk.goods.detail",
                bizParams = mapOf(
                    "goods_id" to goodsId.toString(),
                    // 拼多多部分业务参数需以 JSON 字符串形式提交
                    "pid" to "",
                ),
            )
            api.getGoodsDetail(params)
        }

    private fun md5Hex(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(HEX[(b.toInt() ushr 4) and 0x0F])
            sb.append(HEX[b.toInt() and 0x0F])
        }
        return sb.toString()
    }

    private companion object {
        private val HEX = "0123456789abcdef".toCharArray()
    }
}

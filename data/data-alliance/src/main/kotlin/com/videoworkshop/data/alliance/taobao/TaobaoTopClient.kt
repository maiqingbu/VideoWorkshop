package com.videoworkshop.data.alliance.taobao

import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.data.alliance.taobao.model.TaobaoItemDetailResponse
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 淘宝开放平台（TOP）客户端：负责系统参数组装、TOP 签名与接口调用。
 *
 * TOP 签名算法（`sign_method = md5`）：
 * 1. 把除 `sign` 外的所有请求参数按参数名 ASCII 升序排序；
 * 2. 拼接为 `key1value1key2value2...`（不使用任何分隔符）；
 * 3. 在拼接串前后各加上 `appSecret`；
 * 4. 对最终字符串做 MD5，结果转大写，即 `sign`。
 *
 * 当前淘宝联盟 API 资质尚未申请，`appKey` / `appSecret` 为占位空串，
 * [isConfigured] 返回 false，仓库层据此回退到 Mock 数据。
 *
 * @param appKey    TOP 应用 App Key（资质申请后注入）
 * @param appSecret TOP 应用 App Secret（资质申请后注入）
 * @param api       TOP Retrofit 接口
 * @param dispatchers 协程调度器
 */
class TaobaoTopClient(
    private val appKey: String = "",
    private val appSecret: String = "",
    private val api: TaobaoTbkApi,
    private val dispatchers: DispatcherProvider,
) {

    /** 是否已配置可用凭证。 */
    fun isConfigured(): Boolean = appKey.isNotBlank() && appSecret.isNotBlank()

    /**
     * 构造 TOP 系统级参数（不含 `sign`）。
     *
     * @param method    TOP 方法名，如 `taobao.tbk.item.info.get`
     * @param timestamp 时间戳，格式 `yyyy-MM-dd HH:mm:ss`，东八区
     */
    fun buildSystemParams(
        method: String,
        timestamp: String = formatTimestamp(System.currentTimeMillis()),
    ): Map<String, String> = buildMap {
        put("method", method)
        put("app_key", appKey)
        put("timestamp", timestamp)
        put("format", "json")
        put("v", "2.0")
        put("sign_method", "md5")
        put("partner_id", "videoworkshop")
    }

    /**
     * 组装系统级 + 业务级参数并计算签名，返回可直接发起请求的完整参数集。
     */
    fun buildSignedParams(
        method: String,
        bizParams: Map<String, String>,
    ): Map<String, String> {
        val all = buildSystemParams(method) + bizParams
        val sign = sign(all, appSecret)
        return all + ("sign" to sign)
    }

    /**
     * TOP MD5 签名：`MD5(appSecret + sorted(key+value) + appSecret)`，结果大写。
     */
    fun sign(params: Map<String, String>, appSecret: String): String {
        val sb = StringBuilder(appSecret)
        params.toSortedMap().forEach { (k, v) ->
            sb.append(k).append(v)
        }
        sb.append(appSecret)
        return md5Hex(sb.toString()).uppercase(Locale.US)
    }

    /**
     * 调用 `taobao.tbk.item.info.get` 获取商品详情。
     *
     * 资质未申请时抛出 [IllegalStateException]，调用方应优先返回 Mock。
     */
    suspend fun getItemDetail(numIid: Long): TaobaoItemDetailResponse =
        withContext(dispatchers.io) {
            check(isConfigured()) {
                "淘宝 TOP 资质未配置，请在申请到 appKey/appSecret 后启用真实调用"
            }
            val params = buildSignedParams(
                method = "taobao.tbk.item.info.get",
                bizParams = mapOf(
                    "num_iid" to numIid.toString(),
                    "fields" to "num_iid,title,pict_url,price,video",
                ),
            )
            api.getItemDetails(params)
        }

    private fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        return sdf.format(Date(millis))
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

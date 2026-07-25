package com.videoworkshop.data.alliance.jd

import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.data.alliance.jd.model.JdMediaResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Locale

/**
 * 京东联盟客户端：负责系统参数组装、SHA256 签名、QPS 限流与接口调用。
 *
 * 京东联盟签名算法（`sign_method = sha256`）：
 * 1. 把除 `sign` 外的所有请求参数按参数名 ASCII 升序排序；
 * 2. 拼接为 `key1value1key2value2...`；
 * 3. 在拼接串前后各加上 `appSecret`；
 * 4. 对最终字符串做 SHA-256，结果转大写，即 `sign`。
 *
 * 时间戳统一使用 13 位毫秒值（[System.currentTimeMillis]）。
 *
 * QPS 限制：京东联盟媒体查询接口默认 QPS ≤ 5，使用令牌桶
 * （容量 5，每秒补充 5）在客户端侧做限流，避免触发网关 429。
 *
 * 当前京东联盟 API 资质尚未申请，[isConfigured] 返回 false，
 * 仓库层据此回退到 Mock 数据。
 *
 * @param appKey    京东联盟 App Key（资质申请后注入）
 * @param appSecret 京东联盟 App Secret（资质申请后注入）
 * @param api       京东联盟 Retrofit 接口
 * @param dispatchers 协程调度器
 */
class JdUnionClient(
    private val appKey: String = "",
    private val appSecret: String = "",
    private val api: JdUnionApi,
    private val dispatchers: DispatcherProvider,
) {

    /** 京东联盟媒体查询 QPS 上限。 */
    private val rateLimiter = TokenBucket(capacity = 5L, refillPerSecond = 5.0)

    /** 是否已配置可用凭证。 */
    fun isConfigured(): Boolean = appKey.isNotBlank() && appSecret.isNotBlank()

    /**
     * 构造系统级参数（不含 `sign`）。时间戳为 13 位毫秒值。
     */
    fun buildSystemParams(timestamp: Long = System.currentTimeMillis()): Map<String, String> =
        buildMap {
            put("app_key", appKey)
            put("timestamp", timestamp.toString())
            put("format", "json")
            put("v", "1.0")
            put("sign_method", "sha256")
        }

    /**
     * 组装系统级 + 业务级参数并计算签名，返回可直接发起请求的完整参数集。
     */
    fun buildSignedParams(
        method: String,
        bizParams: Map<String, String>,
        timestamp: Long = System.currentTimeMillis(),
    ): Map<String, String> {
        val all = buildSystemParams(timestamp) + ("method" to method) + bizParams
        val sign = sign(all, appSecret)
        return all + ("sign" to sign)
    }

    /**
     * 京东 SHA256 签名：`SHA256(appSecret + sorted(key+value) + appSecret)`，结果大写。
     */
    fun sign(params: Map<String, String>, appSecret: String): String {
        val sb = StringBuilder(appSecret)
        params.toSortedMap().forEach { (k, v) ->
            sb.append(k).append(v)
        }
        sb.append(appSecret)
        return sha256Hex(sb.toString()).uppercase(Locale.US)
    }

    /**
     * 查询商品媒体（视频）信息。受令牌桶限流保护（QPS ≤ 5）。
     *
     * 资质未申请时抛出 [IllegalStateException]，调用方应优先返回 Mock。
     */
    suspend fun queryGoodsMedia(skuId: Long): JdMediaResponse =
        withContext(dispatchers.io) {
            check(isConfigured()) {
                "京东联盟资质未配置，请在申请到 appKey/appSecret 后启用真实调用"
            }
            rateLimiter.acquire()
            val params = buildSignedParams(
                method = "jd.union.open.goods.media.query",
                bizParams = mapOf(
                    "skuIds" to skuId.toString(),
                    "siteId" to "",
                ),
            )
            api.queryMedia(params)
        }

    private fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
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

/**
 * 令牌桶限流器：容量 [capacity]，每秒补充 [refillPerSecond] 个令牌。
 *
 * 用于在客户端侧控制 QPS（如京东联盟 ≤ 5 QPS）。`acquire` 为挂起函数，
 * 令牌不足时按需等待，避免空转忙等。
 */
private class TokenBucket(
    private val capacity: Long,
    private val refillPerSecond: Double,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private var tokens: Double = capacity.toDouble()
    private var lastRefillMs: Long = nowMs()
    private val lock = Any()

    suspend fun acquire() {
        while (true) {
            val waitMs = synchronized(lock) {
                refill()
                if (tokens >= 1.0) {
                    tokens -= 1.0
                    0L
                } else {
                    val need = 1.0 - tokens
                    (need / refillPerSecond * 1000.0).toLong().coerceAtLeast(1L)
                }
            }
            if (waitMs == 0L) return
            delay(waitMs)
        }
    }

    private fun refill() {
        val now = nowMs()
        val elapsed = now - lastRefillMs
        if (elapsed > 0) {
            tokens = (tokens + elapsed / 1000.0 * refillPerSecond)
                .coerceAtMost(capacity.toDouble())
            lastRefillMs = now
        }
    }
}

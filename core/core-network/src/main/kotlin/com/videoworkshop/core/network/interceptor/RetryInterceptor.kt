package com.videoworkshop.core.network.interceptor

import com.videoworkshop.core.common.AppConstants
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 重试拦截器：对网络异常与可重试状态码（429/5xx）按指数退避重试，最多 [maxRetry] 次。
 *
 * 总尝试次数 = [maxRetry] + 1（首次 + 最多 [maxRetry] 次重试）。
 * 默认 [maxRetry] 取自 [AppConstants.NETWORK_RETRY_COUNT]（3 次）。
 */
class RetryInterceptor(
    private val maxRetry: Int = AppConstants.NETWORK_RETRY_COUNT,
    private val baseDelayMs: Long = AppConstants.NETWORK_RETRY_DELAY_MS,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var lastException: IOException? = null

        repeat(maxRetry + 1) { attempt ->
            val isLastAttempt = attempt == maxRetry
            try {
                val response = chain.proceed(chain.request())
                // 非可重试状态，或已达最后一次尝试，直接返回
                if (response.code !in RETRYABLE_STATUSES || isLastAttempt) {
                    return response
                }
                // 可重试状态：关闭响应体后退避重试
                response.close()
                sleepBackoff(attempt)
            } catch (e: IOException) {
                if (isLastAttempt) throw e
                lastException = e
                sleepBackoff(attempt)
            }
        }
        throw lastException ?: IOException("Request failed after $maxRetry retries")
    }

    /**
     * 指数退避：baseDelayMs * 2^attempt。
     */
    private fun sleepBackoff(attempt: Int) {
        val delay = baseDelayMs * (1L shl attempt)
        try {
            Thread.sleep(delay)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    companion object {
        /** 触发重试的 HTTP 状态码集合。 */
        private val RETRYABLE_STATUSES = setOf(429, 500, 502, 503, 504)
    }
}

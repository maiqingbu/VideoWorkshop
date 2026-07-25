package com.videoworkshop.core.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 自定义日志拦截器：以统一 TAG 打印请求方法、URL、关键 Header 与响应耗时/状态码。
 *
 * 仅在 debug 构建生效时打印 body 摘要由上层控制；此处保持轻量，避免读取大 body。
 */
class LoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNanos = System.nanoTime()

        Log.i(TAG, "--> ${request.method} ${request.url}")
        request.headers.forEach { (key, value) ->
            // 脱敏：Authorization / 含 key/secret 的头只打印键名
            if (key.equals("Authorization", ignoreCase = true) || key.contains("secret", ignoreCase = true)) {
                Log.i(TAG, "  $key: ******")
            } else {
                Log.i(TAG, "  $key: $value")
            }
        }

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            val elapsedMs = (System.nanoTime() - startNanos) / NANOS_PER_MS
            Log.w(TAG, "<-- FAIL ${request.method} ${request.url} (${elapsedMs}ms) ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }

        val elapsedMs = (System.nanoTime() - startNanos) / NANOS_PER_MS
        Log.i(
            TAG,
            "<-- ${response.code} ${response.message} ${request.url} (${elapsedMs}ms, ${response.body?.contentLength() ?: -1}B)"
        )
        return response
    }

    companion object {
        private const val TAG = "VW-Net"
        private const val NANOS_PER_MS = 1_000_000L
    }
}

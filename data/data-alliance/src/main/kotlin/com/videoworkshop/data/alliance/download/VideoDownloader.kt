package com.videoworkshop.data.alliance.download

import com.videoworkshop.core.common.DispatcherProvider
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 基于 OkHttp 的视频下载器：支持分片（Range）请求与断点续传。
 *
 * 下载流程：
 * 1. 以 `<destPath>.part` 作为临时文件，若已存在则视为已下载部分，发起带
 *    `Range: bytes=<已下载>-` 的续传请求；
 * 2. 若服务器返回 206，则追加写入临时文件；若返回 200（不支持续传或忽略 Range），
 *    则从 0 重新下载并覆盖临时文件；
 * 3. 通过 [onProgress] 回调上报进度（0.0 ~ 1.0），按字节量节流避免回调过频；
 * 4. 下载完成并校验完整后，把临时文件重命名为目标路径。
 *
 * 京东视频 CDN（vod.300hu.com）会校验 `Referer` 头，需通过 [referer] 传入
 * `https://www.jd.com`，否则返回 403。
 *
 * @param client     共享 OkHttp 客户端（来自 core-network）
 * @param dispatchers 协程调度器
 */
class VideoDownloader @Inject constructor(
    private val client: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) {

    /**
     * 下载视频到 [destPath]。
     *
     * @param url        视频下载地址
     * @param destPath   目标本地绝对路径
     * @param referer    可选 Referer 头（京东视频必填）
     * @param onProgress 进度回调，参数为已下载比例（0.0 ~ 1.0）
     * @return 成功返回文件绝对路径，失败返回异常。
     */
    suspend fun download(
        url: String,
        destPath: String,
        referer: String? = null,
        onProgress: (Float) -> Unit = {},
    ): kotlin.Result<String> = withContext(dispatchers.io) {
        val destFile = File(destPath)
        destFile.parentFile?.mkdirs()
        val tempFile = File("$destPath.part")

        try {
            var downloaded = if (tempFile.exists()) tempFile.length() else 0L

            val request = Request.Builder().url(url).apply {
                if (downloaded > 0L) header("Range", "bytes=$downloaded-")
                referer?.takeIf { it.isNotBlank() }?.let { header("Referer", it) }
                header("User-Agent", USER_AGENT)
                header("Accept", "*/*")
                header("Connection", "keep-alive")
            }.build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                val code = resp.code
                if (!resp.isSuccessful && code != HTTP_PARTIAL_CONTENT) {
                    return@use kotlin.Result.failure(IOException("下载失败：HTTP $code"))
                }

                // 206 表示服务器接受续传；若服务器忽略 Range 返回 200，则从头覆盖
                val resumeEffective = code == HTTP_PARTIAL_CONTENT && downloaded > 0L
                if (!resumeEffective) downloaded = 0L

                val body = resp.body
                    ?: return@use kotlin.Result.failure(IOException("响应体为空"))

                val contentLength = body.contentLength()
                val total: Long = when {
                    code == HTTP_PARTIAL_CONTENT ->
                        parseTotalFromContentRange(resp.header("Content-Range"))
                            ?: (contentLength + downloaded).takeIf { it > 0 } ?: -1L
                    contentLength > 0 -> contentLength
                    else -> -1L
                }

                body.byteStream().use { input ->
                    FileOutputStream(tempFile, resumeEffective).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesSinceReport = 0L
                        var lastReportedBytes = -1L
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            bytesSinceReport += read
                            if (bytesSinceReport >= REPORT_INTERVAL_BYTES) {
                                bytesSinceReport = 0L
                                if (downloaded != lastReportedBytes) {
                                    lastReportedBytes = downloaded
                                    val p = if (total > 0) {
                                        (downloaded.toFloat() / total).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                    onProgress(p)
                                }
                            }
                        }
                        output.fd.sync()
                    }
                }

                // 完整性校验
                if (total > 0 && downloaded < total) {
                    return@use kotlin.Result.failure(
                        IOException("下载数据不完整：$downloaded / $total bytes")
                    )
                }

                if (destFile.exists()) destFile.delete()
                if (!tempFile.renameTo(destFile)) {
                    tempFile.copyTo(destFile, overwrite = true)
                    tempFile.delete()
                }
                onProgress(1f)
                kotlin.Result.success(destFile.absolutePath)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            kotlin.Result.failure(e)
        }
    }

    /** 解析 `Content-Range: bytes <start>-<end>/<total>` 中的 total。 */
    private fun parseTotalFromContentRange(contentRange: String?): Long? {
        if (contentRange.isNullOrBlank()) return null
        val slash = contentRange.lastIndexOf('/')
        if (slash < 0 || slash == contentRange.length - 1) return null
        return contentRange.substring(slash + 1).trim().toLongOrNull()
    }

    private companion object {
        const val HTTP_PARTIAL_CONTENT = 206
        const val BUFFER_SIZE = 8 * 1024
        const val REPORT_INTERVAL_BYTES = 256 * 1024L
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 VideoWorkshop/1.0"
    }
}

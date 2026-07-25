package com.videoworkshop.data.publish

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import com.videoworkshop.domain.model.ContentType
import java.io.File

/**
 * 发布回退策略。
 *
 * 当目标平台 App 未安装或分享 Intent 拉起失败时，
 * 使用 [MediaScannerConnection] 将文件扫描到系统相册，
 * 确保用户产出至少保存在本地。
 *
 * 这是一个纯静态工具对象，不依赖注入。
 */
object PublishFallback {

    /**
     * 将文件扫描到系统相册。
     *
     * 通过 [MediaScannerConnection.scanFile] 通知 MediaScanner 扫描指定文件，
     * 使其出现在系统相册 / 文件管理器中。
     *
     * @param context  应用上下文
     * @param filePath 本地文件绝对路径
     * @param type      内容形式（决定 MIME 类型）
     * @return `true` 表示扫描请求已成功发出
     */
    fun saveToGallery(context: Context, filePath: String, type: ContentType): Boolean {
        val file = File(filePath)
        if (!file.exists()) {
            return false
        }

        val mimeType = when (type) {
            ContentType.VIDEO -> MIME_VIDEO
            ContentType.IMAGE -> MIME_IMAGE
        }

        return try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(filePath),
                arrayOf(mimeType)
            ) { _: String?, _: Uri? ->
                // 扫描完成回调，无需处理
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private const val MIME_VIDEO = "video/*"
    private const val MIME_IMAGE = "image/*"
}

package com.videoworkshop.data.publish

import android.content.Intent
import android.net.Uri
import com.videoworkshop.domain.model.ContentType

/**
 * 分享 Intent 构建器。
 *
 * 根据内容 URI、目标包名、标题和内容类型构造一个 [Intent.ACTION_SEND] Intent，
 * 用于定向拉起目标平台（抖音 / 快手 / 小红书）的发布流程。
 *
 * 构建的 Intent 特征：
 * - action = [Intent.ACTION_SEND]
 * - type = "video/&#42;" 或 "image/&#42;"（由 [ContentType] 决定）
 * - [Intent.EXTRA_STREAM] = 待分享文件 URI
 * - [Intent.EXTRA_TEXT] = 标题文本
 * - setPackage(targetPackage) 定向到目标 App
 * - [Intent.FLAG_GRANT_READ_URI_PERMISSION] 授予目标 App 读取 URI 的权限
 */
object ShareIntentBuilder {

    /**
     * 构建分享 Intent。
     *
     * @param uri            待分享文件的 content:// URI（由 FileProvider 生成）
     * @param targetPackage  目标平台包名
     * @param title          标题文本，可为空
     * @param type           内容形式（视频或图文）
     * @return 配置完毕的 [Intent.ACTION_SEND] Intent
     */
    fun buildShareIntent(
        uri: Uri,
        targetPackage: String,
        title: String?,
        type: ContentType
    ): Intent {
        val mimeType = when (type) {
            ContentType.VIDEO -> MIME_VIDEO
            ContentType.IMAGE -> MIME_IMAGE
        }

        return Intent(Intent.ACTION_SEND).apply {
            this.type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            if (!title.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, title)
            }
            setPackage(targetPackage)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private const val MIME_VIDEO = "video/*"
    private const val MIME_IMAGE = "image/*"
}

package com.videoworkshop.data.publish

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.videoworkshop.domain.model.ContentType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * FileProvider URI 生成工具。
 *
 * 通过 [FileProvider.getUriForFile] 将本地文件路径转换为 `content://` URI，
 * 以便安全地授予目标平台 App 读取文件内容的临时权限。
 *
 * FileProvider 的 authority 约定为 `${packageName}.fileprovider`，
 * 对应 app 模块 AndroidManifest.xml 中声明的 FileProvider 配置。
 *
 * @param context 应用上下文，通过 Hilt @ApplicationContext 注入
 */
class FileProviderHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 将本地文件路径转换为可分享的 content:// URI。
     *
     * 流程：
     * 1. 检查文件是否存在，不存在则抛出 [PublishException]
     * 2. 调用 [FileProvider.getUriForFile] 生成 URI
     * 3. 根据文件扩展名与 [ContentType] 确认 MIME 类型
     *
     * @param filePath 本地文件绝对路径
     * @return 可分享的 content:// URI
     * @throws PublishException 文件不存在时抛出
     */
    fun getShareUri(filePath: String): Uri {
        val file = File(filePath)
        if (!file.exists()) {
            throw PublishException("待发布文件不存在: $filePath")
        }

        val authority = "${context.packageName}$FILE_PROVIDER_SUFFIX"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * 根据内容类型获取对应的 MIME 类型字符串。
     *
     * @param type 内容形式
     * @return "video/&#42;" 或 "image/&#42;"
     */
    fun getMimeType(type: ContentType): String = when (type) {
        ContentType.VIDEO -> MIME_VIDEO
        ContentType.IMAGE -> MIME_IMAGE
    }

    private companion object {
        const val FILE_PROVIDER_SUFFIX = ".fileprovider"
        const val MIME_VIDEO = "video/*"
        const val MIME_IMAGE = "image/*"
    }
}

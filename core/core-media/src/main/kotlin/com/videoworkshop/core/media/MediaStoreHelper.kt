package com.videoworkshop.core.media

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 媒体导入与元信息读取工具。
 *
 * 提供 [ActivityResultContracts] 适配的选图/选视频契约，以及基于
 * [MediaMetadataRetriever] 与 [BitmapFactory] 的文件信息解析能力。
 */
object MediaStoreHelper {

    // ===== 选择契约 =====

    /** 单视频选择请求（Photo Picker，API 兼容回退由系统处理）。 */
    fun videoPickRequest(): PickVisualMediaRequest =
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)

    /** 单图片选择请求。 */
    fun imagePickRequest(): PickVisualMediaRequest =
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    /** 视频/图片通用选择请求。 */
    fun mediaPickRequest(): PickVisualMediaRequest =
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)

    /** GetContent 契约（兼容低版本设备，mime 任意）。 */
    fun getContentContract(): ActivityResultContracts.GetContent =
        ActivityResultContracts.GetContent()

    // ===== 视频信息 =====

    /**
     * 从 [uri] 解析视频信息（时长/分辨率/大小/MIME）。
     *
     * 解析失败返回 null。
     */
    fun getVideoInfo(context: Context, uri: Uri): VideoInfo? {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "video/*"
        val size = getFileSize(context, uri)
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val width = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val height = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            VideoInfo(
                path = uri.toString(),
                duration = durationMs,
                width = width,
                height = height,
                size = size,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    // ===== 图片信息 =====

    /**
     * 从 [uri] 解析图片信息（分辨率/大小/MIME）。
     *
     * 解析失败返回 null。
     */
    fun getImageInfo(context: Context, uri: Uri): ImageInfo? {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "image/*"
        val size = getFileSize(context, uri)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) return null
        return ImageInfo(
            path = uri.toString(),
            width = width,
            height = height,
            size = size,
            mimeType = mimeType
        )
    }

    // ===== 通用文件信息 =====

    /** 通过 ContentResolver 查询文件大小（字节）。 */
    fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
        }
        return size
    }

    /** 通过 ContentResolver 查询文件显示名。 */
    fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
        }
        return name
    }
}

/**
 * 图片文件元信息。
 */
data class ImageInfo(
    val path: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val mimeType: String,
)

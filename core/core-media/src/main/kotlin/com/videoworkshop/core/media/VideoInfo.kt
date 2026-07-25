package com.videoworkshop.core.media

import com.videoworkshop.core.database.entity.MaterialEntity
import java.util.UUID

/**
 * 视频文件元信息。
 *
 * @param path     文件路径或 Uri 字符串。
 * @param duration 时长（毫秒）。
 * @param width    宽度（像素）。
 * @param height   高度（像素）。
 * @param size     文件大小（字节）。
 * @param mimeType MIME 类型，如 `video/mp4`。
 */
data class VideoInfo(
    val path: String,
    val duration: Long,
    val width: Int,
    val height: Int,
    val size: Long,
    val mimeType: String,
) {

    /**
     * 转换为素材实体，便于入库索引。
     *
     * @param thumbnail 缩略图路径。
     * @param source    来源，默认 `import`。
     */
    fun toMaterialEntity(
        thumbnail: String? = null,
        source: String = "import",
    ): MaterialEntity = MaterialEntity(
        id = UUID.randomUUID().toString(),
        localPath = path,
        source = source,
        type = "video",
        thumbnail = thumbnail,
        createdAt = System.currentTimeMillis(),
    )
}

package com.videoworkshop.core.ffmpeg

import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformation
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.StreamInformation
import com.videoworkshop.domain.model.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * FFprobe 媒体信息提取工具。
 *
 * 通过 [FFprobeKit.getMediaInformation] 解析视频文件的元信息，
 * 转换为领域模型 [VideoClip]。
 *
 * 所有方法均为挂起函数，内部在 [Dispatchers.IO] 上执行。
 */
object FfprobeHelper {

    /**
     * 提取视频文件的元信息。
     *
     * @param path 视频文件绝对路径。
     * @return 包含 fps、bitrate、duration、width、height、size、mimeType 的 [VideoClip]。
     *         若解析失败，对应字段回退为默认值（0 / 0f / "video/mp4"）。
     */
    suspend fun getVideoInfo(path: String): VideoClip = withContext(Dispatchers.IO) {
        val session = FFprobeKit.getMediaInformation(path)
        val mediaInfo = session.mediaInformation
        val returnCode = session.returnCode

        if (mediaInfo == null || !ReturnCode.isSuccess(returnCode)) {
            // 解析失败时回退到文件系统信息
            return@withContext fallbackVideoClip(path)
        }

        val videoStream = findVideoStream(mediaInfo)

        val durationMs = parseDurationMs(mediaInfo.duration)
        val bitrate = mediaInfo.bitrate?.toLongOrNull() ?: 0L
        val width = videoStream?.width?.toInt() ?: 0
        val height = videoStream?.height?.toInt() ?: 0
        val fps = parseFrameRate(videoStream?.averageFrameRate ?: videoStream?.realFrameRate)
        val size = getFileSize(path)
        val mimeType = getMimeType(path)

        VideoClip(
            path = path,
            duration = durationMs,
            width = width,
            height = height,
            size = size,
            fps = fps,
            bitrate = bitrate,
            mimeType = mimeType
        )
    }

    /**
     * 从媒体信息中查找视频流。
     */
    private fun findVideoStream(mediaInfo: MediaInformation): StreamInformation? {
        val streams = mediaInfo.streams ?: return null
        return streams.firstOrNull { stream ->
            stream.type == STREAM_TYPE_VIDEO
        }
    }

    /**
     * 将 ffprobe 返回的时长字符串（秒，如 "10.500000"）转换为毫秒。
     */
    private fun parseDurationMs(durationStr: String?): Long {
        if (durationStr.isNullOrEmpty()) return 0L
        return try {
            (durationStr.toDouble() * 1000).toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }

    /**
     * 解析帧率字符串。
     *
     * ffprobe 返回的帧率可能是分数形式（如 "30000/1001"）或小数（如 "29.97"）。
     *
     * @param frameRateStr 帧率字符串。
     * @return 解析后的帧率，失败时回退为 30f。
     */
    private fun parseFrameRate(frameRateStr: String?): Float {
        if (frameRateStr.isNullOrEmpty()) return DEFAULT_FPS
        return try {
            if (frameRateStr.contains("/")) {
                val parts = frameRateStr.split("/")
                val numerator = parts[0].toDouble()
                val denominator = parts[1].toDouble()
                if (denominator == 0.0) DEFAULT_FPS else (numerator / denominator).toFloat()
            } else {
                frameRateStr.toFloat()
            }
        } catch (e: Exception) {
            DEFAULT_FPS
        }
    }

    /**
     * 获取文件大小（字节）。
     */
    private fun getFileSize(path: String): Long {
        return try {
            val file = File(path)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 根据文件扩展名推断 MIME 类型。
     */
    private fun getMimeType(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "mkv" -> "video/x-matroska"
            "flv" -> "video/x-flv"
            "wmv" -> "video/x-ms-wmv"
            "webm" -> "video/webm"
            "3gp" -> "video/3gpp"
            "ts" -> "video/mp2t"
            "m4v" -> "video/x-m4v"
            else -> "video/mp4"
        }
    }

    /**
     * 解析失败时的回退 [VideoClip]。
     */
    private fun fallbackVideoClip(path: String): VideoClip {
        return VideoClip(
            path = path,
            duration = 0L,
            width = 0,
            height = 0,
            size = getFileSize(path),
            fps = DEFAULT_FPS,
            bitrate = 0L,
            mimeType = getMimeType(path)
        )
    }

    private const val STREAM_TYPE_VIDEO = "video"
    private const val DEFAULT_FPS = 30f
}

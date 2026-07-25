package com.videoworkshop.core.common

import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

/**
 * 视频与文件相关的通用格式化工具。
 */
object VideoUtils {

    /**
     * 将毫秒时长格式化为可读字符串。
     *
     * - 不足 1 小时：`mm:ss`，例如 `03:05`
     * - 超过 1 小时：`hh:mm:ss`，例如 `01:03:05`
     */
    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * 将字节数格式化为带单位的文件大小字符串。
     *
     * 例如：`1.5 MB`、`800.0 KB`、`2.0 GB`。
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
            .coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    /**
     * 将秒数转为毫秒。
     */
    fun secondsToMillis(seconds: Int): Long = seconds.toLong() * 1000L

    /**
     * 判断分辨率是否超过允许的最大值。
     */
    fun isResolutionValid(width: Int, height: Int): Boolean {
        val shortSide = minOf(width, height)
        return shortSide <= AppConstants.MAX_RESOLUTION
    }

    /**
     * 判断视频体积（字节）是否在限制范围内。
     */
    fun isSizeValid(bytes: Long): Boolean {
        val maxBytes = AppConstants.MAX_VIDEO_SIZE_MB.toLong() * 1024L * 1024L
        return bytes <= maxBytes
    }

    /**
     * 判断视频时长（毫秒）是否在限制范围内。
     */
    fun isDurationValid(ms: Long): Boolean {
        val maxMs = AppConstants.MAX_VIDEO_DURATION_SEC.toLong() * 1000L
        return ms <= maxMs
    }
}

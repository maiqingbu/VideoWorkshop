package com.videoworkshop.core.ffmpeg.algorithms

import com.videoworkshop.core.ffmpeg.pipeline.DedupStrength

/**
 * 码率修改器。
 *
 * 根据去重强度对应的码率系数，将原始码率缩放为新的目标码率。
 * 改变码率会直接改变编码后的比特流，从而影响文件哈希和感知质量。
 *
 * 注意：码率系数 [DedupStrength.bitrateFactor] 小于 1 时会降低码率，
 * 大于 1 时会提高码率。
 */
object BitrateModifier {

    /**
     * 根据原始码率和强度系数计算目标码率。
     *
     * @param originalBitrate 原始视频码率（bps）。
     * @param strength        去重强度档位。
     * @return 目标码率字符串（bps），例如 `1700000`。
     */
    fun buildParam(originalBitrate: Long, strength: DedupStrength): String {
        val targetBitrate = (originalBitrate * strength.bitrateFactor).toLong()
        // 确保码率不低于最低值，避免输出质量过差
        val safeBitrate = targetBitrate.coerceAtLeast(MIN_BITRATE)
        return safeBitrate.toString()
    }

    private const val MIN_BITRATE = 200_000L
}

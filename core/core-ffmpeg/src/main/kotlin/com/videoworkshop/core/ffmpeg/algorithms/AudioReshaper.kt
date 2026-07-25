package com.videoworkshop.core.ffmpeg.algorithms

import com.videoworkshop.core.ffmpeg.pipeline.DedupStrength

/**
 * 音轨重塑器。
 *
 * 对音频流施加均衡器（equalizer）调整，并叠加微量粉噪声（anoisesrc），
 * 改变音频指纹以辅助视频去重。
 *
 * 由于 `anoisesrc` 是音频源滤镜，不能直接在 `-af` 单输入链中使用，
 * 因此本方法返回 `-filter_complex` 兼容的完整音频滤镜图：
 *
 * ```
 * [0:a]equalizer=...[a0];
 * anoisesrc=d=DURATION:c=pink:a=AMP[noise];
 * [a0][noise]amix=inputs=2:duration=first:dropout=0[aout]
 * ```
 *
 * 调用方应将此字符串放入 `-filter_complex` 参数中，并 `-map "[aout]"`。
 *
 * 强度参数：
 * - [DedupStrength.eqGain]：均衡器增益（dB）。
 * - [DedupStrength.noiseAmplitude]：噪声幅度（0.0 ~ 1.0）。
 */
object AudioReshaper {

    /** 均衡器中心频率（Hz）。 */
    private const val EQUALIZER_FREQUENCY = 1000.0

    /** 均衡器滤波类型。 */
    private const val EQUALIZER_FILTER_TYPE = "q"

    /** 均衡器带宽（倍频程）。 */
    private const val EQUALIZER_BANDWIDTH = 1.0

    /**
     * 构建 filter_complex 兼容的音频滤镜图。
     *
     * @param strength     去重强度档位。
     * @param durationSec  视频时长（秒），用于 anoisesrc 的 duration 参数。
     * @return filter_complex 音频片段字符串，输出标签为 `[aout]`。
     */
    fun buildFilter(strength: DedupStrength, durationSec: Double): String {
        val safeDuration = if (durationSec > 0) durationSec else 1.0
        return buildString {
            // 1. 对输入音频施加均衡器
            append("[0:a]equalizer=f=")
            append(EQUALIZER_FREQUENCY)
            append(":t=")
            append(EQUALIZER_FILTER_TYPE)
            append(":w=")
            append(EQUALIZER_BANDWIDTH)
            append(":g=")
            append(strength.eqGain)
            append("[a0];")

            // 2. 生成粉噪声
            append("anoisesrc=d=")
            append(formatNumber(safeDuration))
            append(":c=pink:a=")
            append(formatNumber(strength.noiseAmplitude))
            append("[noise];")

            // 3. 混合原始音频（EQ 后）与噪声
            append("[a0][noise]amix=inputs=2:duration=first:dropout=0[aout]")
        }
    }

    /**
     * 格式化数字，去掉多余的小数尾零。
     */
    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    /**
     * 格式化浮点数。
     */
    private fun formatNumber(value: Float): String {
        return formatNumber(value.toDouble())
    }
}

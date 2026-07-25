package com.videoworkshop.core.ffmpeg.algorithms

import com.videoworkshop.core.ffmpeg.pipeline.DedupStrength

/**
 * 色彩偏移器。
 *
 * 使用 FFmpeg 的 `hue` 滤镜同时调整色相（h）和饱和度（s），
 * 改变画面的色彩分布，从而影响视频感知哈希。
 *
 * - 色相偏移由 [DedupStrength.hueShift] 决定（单位：度）。
 * - 饱和度系数由 [DedupStrength.saturationFactor] 决定（1.0 = 不变）。
 */
object ColorShifter {

    /**
     * 构建 hue 滤镜参数。
     *
     * 输出格式：`hue=h={hueShift}:s={saturationFactor}`
     *
     * @param strength 去重强度档位。
     * @return 滤镜字符串。
     */
    fun buildFilter(strength: DedupStrength): String {
        return "hue=h=${strength.hueShift}:s=${strength.saturationFactor}"
    }
}

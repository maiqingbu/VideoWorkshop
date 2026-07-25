package com.videoworkshop.core.ffmpeg.algorithms

import com.videoworkshop.core.ffmpeg.pipeline.DedupStrength

/**
 * 裁剪变换器。
 *
 * 使用 FFmpeg 的 `crop` 滤镜从视频四边各裁去一定像素，
 * 改变画面尺寸和构图，从而影响视频感知哈希。
 *
 * 裁剪像素数由 [DedupStrength.cropPixels] 决定，强度越高裁剪越多。
 * 裁剪量始终为偶数（`2 * cropPixels`），以保证编码器要求的偶数宽高。
 */
object CropTransformer {

    /**
     * 构建 crop 滤镜参数。
     *
     * 输出格式：`crop=iw-{2*crop}:ih-{2*crop}:{crop}:{crop}`
     *
     * @param strength 去重强度档位。
     * @return 滤镜字符串。
     */
    fun buildFilter(strength: DedupStrength): String {
        val crop = strength.cropPixels
        val totalCrop = 2 * crop // 保证偶数
        return "crop=iw-$totalCrop:ih-$totalCrop:$crop:$crop"
    }
}

package com.videoworkshop.core.ffmpeg.algorithms

import com.videoworkshop.core.ffmpeg.pipeline.DedupStrength

/**
 * 帧率调整器。
 *
 * 通过 FFmpeg 的 `fps` 滤镜将视频重采样到目标帧率，
 * 改变帧序列的时序分布，从而影响视频指纹。
 *
 * 强度越高，目标帧率偏离原始帧率越多。
 */
object FpsAdjuster {

    /**
     * 构建 fps 滤镜参数。
     *
     * @param strength 去重强度档位。
     * @return 滤镜字符串，例如 `fps=30`。
     */
    fun buildFilter(strength: DedupStrength): String {
        return "fps=${formatFps(strength.fps)}"
    }

    /**
     * 格式化帧率：整数去掉小数尾零（29.0 → 29），小数保留原值（29.97 → 29.97）。
     */
    private fun formatFps(fps: Double): String {
        return if (fps == fps.toLong().toDouble()) {
            fps.toLong().toString()
        } else {
            fps.toString()
        }
    }
}

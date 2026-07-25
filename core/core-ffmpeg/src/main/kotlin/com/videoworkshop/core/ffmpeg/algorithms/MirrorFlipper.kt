package com.videoworkshop.core.ffmpeg.algorithms

/**
 * 镜像翻转器。
 *
 * 使用 FFmpeg 的 `hflip` 滤镜对视频进行水平镜像翻转，
 * 改变画面像素排列，从而影响视频感知哈希。
 *
 * 镜像翻转不依赖强度档位，始终执行完整的水平翻转。
 */
object MirrorFlipper {

    /**
     * 构建 hflip 滤镜参数。
     *
     * @return 滤镜字符串 `hflip`。
     */
    fun buildFilter(): String = "hflip"
}

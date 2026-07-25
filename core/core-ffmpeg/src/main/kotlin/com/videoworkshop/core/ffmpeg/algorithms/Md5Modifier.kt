package com.videoworkshop.core.ffmpeg.algorithms

/**
 * MD5 修改器。
 *
 * 视频去重的核心目标之一是改变文件 MD5 哈希，使其不被平台判定为重复。
 * 在整个去重流水线中，视频会被重新编码，MD5 自然会发生变化。
 *
 * 本修改器额外添加 `-fflags +bitexact` 参数，移除编码器写入的确定性元数据
 * 和填充字节，进一步确保输出文件的唯一性。
 */
object Md5Modifier {

    /**
     * 构建 MD5 修改相关的 FFmpeg 参数。
     *
     * @return FFmpeg 参数字符串，例如 `-fflags +bitexact`。
     */
    fun buildParam(): String = "-fflags +bitexact"
}

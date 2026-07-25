package com.videoworkshop.core.ffmpeg.algorithms

/**
 * 元数据清理器。
 *
 * 移除视频文件中的全局元数据和章节信息，
 * 包括创建时间、编码器信息、GPS 数据等，
 * 防止平台通过元数据比对判定为重复。
 */
object MetadataCleaner {

    /**
     * 构建元数据清理相关的 FFmpeg 参数。
     *
     * - `-map_metadata -1`：清除全局元数据。
     * - `-map_chapters -1`：清除章节信息。
     *
     * @return FFmpeg 参数字符串。
     */
    fun buildParam(): String = "-map_metadata -1 -map_chapters -1"
}

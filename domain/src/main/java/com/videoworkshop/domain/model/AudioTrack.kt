package com.videoworkshop.domain.model

/**
 * 音轨元信息。
 *
 * @param index       音轨索引（从 0 开始）
 * @param codec       编解码器名称，例如 "aac"
 * @param sampleRate  采样率（Hz）
 * @param channels    声道数
 * @param durationMs  时长（毫秒）
 * @param bitrate     码率（bps）
 */
data class AudioTrack(
    val index: Int,
    val codec: String,
    val sampleRate: Int,
    val channels: Int,
    val durationMs: Long,
    val bitrate: Int
)

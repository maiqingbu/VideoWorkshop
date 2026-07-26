package com.videoworkshop.core.ffmpeg.operators

import com.videoworkshop.core.ffmpeg.pipeline.MultiInputCommandBuilder

/**
 * 跨视频音频混合器。
 *
 * 与 [com.videoworkshop.core.ffmpeg.enhance.AudioMixer] 的差异：
 * - [AudioMixer] 接收「视频路径 + 独立音频文件」（如配音、BGM），输入是异构的。
 * - 本混合器接收「A 视频路径 + B 视频路径」，需从 B 视频抽取原声后再与 A 音频按比例混合，
 *   输入是同构的两个视频，对应 AB 搬运的 MIX 模式（B 画面 + A 音频 + B 原声）。
 *
 * 输出策略：
 * - 视频：直接复制 B 视频流（`-map 1:v:0 -c:v copy`），保留 B 的画面质量。
 * - 音频：A、B 各自调整音量后通过 `amix` 混合为一路，编码为 AAC。
 *
 * filter_complex 结构：
 * ```
 * [0:a]volume={A}[a0];
 * [1:a]volume={B}[a1];
 * [a0][a1]amix=inputs=2:duration=first[aout]
 * ```
 *
 * 假设前提：
 * - A 视频含音轨；若 A 无音轨应由上层（如 AVStreamSwapper）预先检测并改走 PURE_REPLACE 路径。
 * - B 视频含视频流和音轨。
 *
 * 与 [AudioMixer] 的风格保持一致（滤镜输出标签同为 `[aout]`、视频流直接复制、音频编码为 aac），
 * 但通过 [MultiInputCommandBuilder] 返回参数列表（不含 `ffmpeg` 前缀），便于与流水线其它环节拼装。
 */
class CrossVideoAudioMixer {

    /**
     * 构建跨视频音频混合命令。
     *
     * @param videoAPath   A 视频路径（音频源）。
     * @param videoBPath   B 视频路径（画面源 + 原声源）。
     * @param volumeRatioA A 音量比例（0.0 ~ 1.0），超出范围将被截断到该区间。
     * @param volumeRatioB B 原声音量比例（0.0 ~ 1.0），超出范围将被截断到该区间。
     * @param outputPath   输出视频路径。
     * @return FFmpeg 命令参数列表（不含 `ffmpeg` 前缀），每个元素为一个独立 token。
     */
    fun buildCommand(
        videoAPath: String,
        videoBPath: String,
        volumeRatioA: Float,
        volumeRatioB: Float,
        outputPath: String
    ): List<String> {
        // 音量比例限制在 [0.0, 1.0] 范围内，避免负值或过大值损坏音轨
        val safeVolumeA = volumeRatioA.coerceIn(MIN_VOLUME, MAX_VOLUME)
        val safeVolumeB = volumeRatioB.coerceIn(MIN_VOLUME, MAX_VOLUME)

        // filter_complex：
        //   [0:a]volume={A}[a0]                       —— 调整 A 音量
        //   [1:a]volume={B}[a1]                       —— 调整 B 原声音量
        //   [a0][a1]amix=inputs=2:duration=first[aout] —— 两路混合，时长跟随首路（A）
        val filterComplex = buildString {
            append("[0:a]volume=")
            append(formatVolume(safeVolumeA))
            append("[a0];")
            append("[1:a]volume=")
            append(formatVolume(safeVolumeB))
            append("[a1];")
            append("[a0][a1]amix=inputs=2:duration=first")
            append(LABEL_AOUT)
        }

        return MultiInputCommandBuilder()
            .addInput(videoAPath)
            .addInput(videoBPath)
            .filterComplex(filterComplex)
            .addMap("1:v:0")
            .addMap(LABEL_AOUT)
            .videoCodec("copy")
            .audioCodec("aac")
            .output(outputPath)
            .build()
    }

    /**
     * 格式化音量值，去掉多余的小数尾零。
     *
     * 例如：1.0 -> "1"，0.5 -> "0.5"，0.0 -> "0"。
     * 与 [com.videoworkshop.core.ffmpeg.algorithms.AudioReshaper] 的数字格式化风格保持一致。
     */
    private fun formatVolume(value: Float): String {
        val doubleValue = value.toDouble()
        return if (doubleValue == doubleValue.toLong().toDouble()) {
            doubleValue.toLong().toString()
        } else {
            value.toString()
        }
    }

    companion object {
        /** 音量最小值。 */
        private const val MIN_VOLUME = 0.0f

        /** 音量最大值。 */
        private const val MAX_VOLUME = 1.0f

        /** 混合输出流标签，与 [com.videoworkshop.core.ffmpeg.enhance.AudioMixer] 保持一致。 */
        private const val LABEL_AOUT = "[aout]"
    }
}

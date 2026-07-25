package com.videoworkshop.core.ffmpeg.enhance

/**
 * 音频混音器。
 *
 * 将视频原声、配音和可选的背景音乐（BGM）混合为一条音轨，
 * 用于带货视频的音频包装。
 *
 * 使用 FFmpeg 的 `amix` 滤镜进行多路音频混合：
 * - 无 BGM 时混合 2 路（原声 + 配音）
 * - 有 BGM 时混合 3 路（原声 + 配音 + BGM）
 *
 * 视频流直接复制（`-c:v copy`），仅替换音频。
 */
object AudioMixer {

    /**
     * 构建音频混合命令。
     *
     * @param videoPath  原始视频路径（提供视频流和原声）。
     * @param voicePath  配音音频路径。
     * @param bgmPath    背景音乐路径，为 `null` 时不加 BGM。
     * @param outputPath 输出视频路径。
     * @return 完整的 FFmpeg 命令字符串。
     */
    fun buildMixCommand(
        videoPath: String,
        voicePath: String,
        bgmPath: String?,
        outputPath: String
    ): String {
        val cmd = StringBuilder()
        cmd.append("ffmpeg")
        cmd.append(" -i \"$videoPath\"")
        cmd.append(" -i \"$voicePath\"")

        if (bgmPath != null) {
            cmd.append(" -i \"$bgmPath\"")
            // 三路混音：原声 + 配音 + BGM
            cmd.append(" -filter_complex \"[0:a][1:a][2:a]amix=inputs=3:duration=longest:dropout=0$LABEL_AOUT\"")
        } else {
            // 两路混音：原声 + 配音
            cmd.append(" -filter_complex \"[0:a][1:a]amix=inputs=2:duration=longest:dropout=0$LABEL_AOUT\"")
        }

        // 映射：视频直接复制，音频取混合结果
        cmd.append(" -map 0:v")
        cmd.append(" -map $LABEL_AOUT")
        cmd.append(" -c:v copy")
        cmd.append(" -c:a aac")
        cmd.append(" -y \"$outputPath\"")

        return cmd.toString()
    }

    private const val LABEL_AOUT = "[aout]"
}

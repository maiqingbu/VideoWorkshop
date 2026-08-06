package com.videoworkshop.core.ffmpeg.pipeline

import com.videoworkshop.domain.model.VideoClip
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [DedupCommandBuilder] 的单元测试。
 *
 * 在 JVM 单元测试环境下，[com.videoworkshop.core.ffmpeg.hw.HardwareEncoderDetector]
 * 内部调用的 [android.media.MediaCodecList] 是 Android 桩类，调用即抛异常并被捕获，
 * 因此 [com.videoworkshop.core.ffmpeg.hw.HardwareEncoderDetector.getBestEncoder] 始终
 * 回退到软件编码器 `libx264`。本测试类所有用例均期望命令中出现 `libx264`。
 */
class DedupCommandBuilderTest {

    /** 测试用输入视频路径。 */
    private val inputPath = "/input.mp4"

    /** 测试用输出视频路径。 */
    private val outputPath = "/output.mp4"

    /** 默认测试视频片段：时长 10 秒、码率 2Mbps、30fps、1920x1080。 */
    private val videoClip = VideoClip(
        path = inputPath,
        duration = 10_000L,
        width = 1920,
        height = 1080,
        size = 5_000_000L,
        fps = 30f,
        bitrate = 2_000_000L,
        mimeType = "video/mp4"
    )

    // ===== 用例 1：全开配置（audioReshape=true）应使用 -filter_complex 并映射 [vout]/[aout] =====
    @Test
    fun build_withFullConfig_usesFilterComplexAndMapsVoutAout() {
        // DedupConfig 默认全部开关开启、强度 STANDARD
        val config = DedupConfig()

        val cmd = DedupCommandBuilder.build(inputPath, outputPath, videoClip, config)

        // 全开配置应走 -filter_complex，而非 -vf
        assertTrue("全开配置应使用 -filter_complex", cmd.contains("-filter_complex"))
        assertFalse("全开配置不应使用 -vf", cmd.contains("-vf"))

        // 视频与音频输出标签应存在
        assertTrue("应包含 [vout] 标签", cmd.contains("[vout]"))
        assertTrue("应包含 [aout] 标签", cmd.contains("[aout]"))

        // 映射应分别指向 [vout] 与 [aout]
        assertTrue("应映射 -map [vout]", cmd.contains("-map [vout]"))
        assertTrue("应映射 -map [aout]", cmd.contains("-map [aout]"))
    }

    // ===== 用例 2：audioReshape=false 应使用 -vf 并做简单映射 =====
    @Test
    fun build_withAudioReshapeDisabled_usesVfAndSimpleMap() {
        val config = DedupConfig(audioReshape = false)

        val cmd = DedupCommandBuilder.build(inputPath, outputPath, videoClip, config)

        // 应使用 -vf 简单滤镜，而非 -filter_complex
        // 注意：FFmpegKit 不解析 shell 引号，命令字符串中不应包含双引号字符
        assertTrue("应使用 -vf", cmd.contains("-vf "))
        assertFalse("命令不应包含双引号字符（FFmpegKit 不解析引号）", cmd.contains("\""))
        assertFalse("audioReshape 关闭时不应使用 -filter_complex", cmd.contains("-filter_complex"))

        // 应使用简单映射 0:v / 0:a?
        assertTrue("应包含 -map 0:v -map 0:a?", cmd.contains("-map 0:v -map 0:a?"))
    }

    // ===== 用例 3：无视频滤镜但 audioReshape=true 应 -map 0:v，且 -filter_complex 仅含音频段 =====
    @Test
    fun build_withNoVideoFiltersButAudioReshape_maps0vAndFilterComplexAudioOnly() {
        // 全部视频滤镜开关关闭，仅保留音频重塑
        val config = DedupConfig(
            fpsAdjust = false,
            cropTransform = false,
            mirrorFlip = false,
            colorShift = false,
            audioReshape = true
        )

        val cmd = DedupCommandBuilder.build(inputPath, outputPath, videoClip, config)

        // 仍应使用 -filter_complex（音频段需要）
        assertTrue("audioReshape 开启时应使用 -filter_complex", cmd.contains("-filter_complex"))

        // 无视频滤镜时视频应直接 -map 0:v，而非 -map [vout]
        assertTrue("应映射 -map 0:v", cmd.contains("-map 0:v"))
        assertFalse("无视频滤镜时不应出现 -map [vout]", cmd.contains("-map [vout]"))
        assertFalse("无视频滤镜时不应出现 [vout] 标签", cmd.contains("[vout]"))

        // -filter_complex 仅含音频段：应包含 [0:a]，不应包含 [0:v]
        assertTrue("应包含音频输入标签 [0:a]", cmd.contains("[0:a]"))
        assertFalse("无视频滤镜时不应包含视频输入标签 [0:v]", cmd.contains("[0:v]"))
        assertTrue("应映射 -map [aout]", cmd.contains("-map [aout]"))
    }

    // ===== 用例 4：无视频滤镜且 audioReshape=false 应无 -vf，仅简单映射 =====
    @Test
    fun build_withNoVideoFiltersAndNoAudioReshape_hasNoVf() {
        val config = DedupConfig(
            fpsAdjust = false,
            cropTransform = false,
            mirrorFlip = false,
            colorShift = false,
            audioReshape = false
        )

        val cmd = DedupCommandBuilder.build(inputPath, outputPath, videoClip, config)

        // 无任何滤镜时不应出现 -vf / -filter_complex
        assertFalse("无视频滤镜时不应出现 -vf", cmd.contains("-vf"))
        assertFalse("audioReshape 关闭时不应出现 -filter_complex", cmd.contains("-filter_complex"))

        // 仅做简单映射
        assertTrue("应包含 -map 0:v -map 0:a?", cmd.contains("-map 0:v -map 0:a?"))
    }

    // ===== 用例 5：bitrateModify 关闭时不应出现 -b:v =====
    @Test
    fun build_withBitrateModifyOff_hasNoBitrateFlag() {
        val config = DedupConfig(bitrateModify = false)

        val cmd = DedupCommandBuilder.build(inputPath, outputPath, videoClip, config)

        assertFalse("码率修改关闭时不应出现 -b:v", cmd.contains("-b:v"))
    }

    // ===== 用例 6：bitrateModify 开启但原始码率为 0 时不应出现 -b:v =====
    @Test
    fun build_withBitrateZero_hasNoBitrateFlag() {
        // 码率修改开启，但视频码率为 0
        val zeroBitrateClip = videoClip.copy(bitrate = 0L)
        val config = DedupConfig(bitrateModify = true)

        val cmd = DedupCommandBuilder.build(inputPath, outputPath, zeroBitrateClip, config)

        assertFalse("原始码率为 0 时不应出现 -b:v", cmd.contains("-b:v"))
    }

    // ===== 用例 7：metadataClean 关闭时不应出现 -map_metadata =====
    @Test
    fun build_withMetadataCleanOff_hasNoMapMetadata() {
        val config = DedupConfig(metadataClean = false)

        val cmd = DedupCommandBuilder.build(inputPath, outputPath, videoClip, config)

        assertFalse("元数据清理关闭时不应出现 -map_metadata", cmd.contains("-map_metadata"))
    }

    // ===== 用例 8：md5Modify 关闭时不应出现 -fflags =====
    @Test
    fun build_withMd5ModifyOff_hasNoFflags() {
        val config = DedupConfig(md5Modify = false)

        val cmd = DedupCommandBuilder.build(inputPath, outputPath, videoClip, config)

        assertFalse("MD5 修改关闭时不应出现 -fflags", cmd.contains("-fflags"))
    }

    // ===== 用例 9：命令应以 "ffmpeg -i" 开头，并以 -y outputPath 结尾（无引号） =====
    @Test
    fun build_startsAndEndsWithExpectedTokens() {
        val config = DedupConfig()

        val cmd = DedupCommandBuilder.build(inputPath, outputPath, videoClip, config)

        assertTrue("命令应以 ffmpeg -i 开头", cmd.startsWith("ffmpeg -i "))
        // FFmpegKit 按空格切分命令字符串且不解析 shell 引号，因此路径以裸字符串形式拼接
        assertTrue(
            "命令应以 -y $outputPath 结尾（不含引号）",
            cmd.endsWith("-y $outputPath")
        )
        assertFalse("命令不应包含任何双引号字符", cmd.contains("\""))
    }

    // ===== 用例 10：JVM 环境下编码器应为 libx264 =====
    @Test
    fun build_usesLibx264EncoderInJvm() {
        val config = DedupConfig()

        val cmd = DedupCommandBuilder.build(inputPath, outputPath, videoClip, config)

        // JVM 单测中 MediaCodecList 不可用，应回退到 libx264
        assertTrue("JVM 环境下应使用 libx264 编码器", cmd.contains("-c:v libx264"))
    }

    // ===== 用例 11：视频滤镜链顺序应为 fps → crop → hflip → hue =====
    @Test
    fun build_videoFilterChainOrderIsFpsCropHflipHue() {
        // 关闭音频重塑以走 -vf 简单滤镜路径，便于直接校验滤镜链顺序
        val config = DedupConfig(audioReshape = false)

        val cmd = DedupCommandBuilder.build(inputPath, outputPath, videoClip, config)

        val fpsIdx = cmd.indexOf("fps=")
        val cropIdx = cmd.indexOf("crop=")
        val hflipIdx = cmd.indexOf("hflip")
        val hueIdx = cmd.indexOf("hue=")

        // 各滤镜均应存在
        assertTrue("应包含 fps 滤镜", fpsIdx >= 0)
        assertTrue("应包含 crop 滤镜", cropIdx >= 0)
        assertTrue("应包含 hflip 滤镜", hflipIdx >= 0)
        assertTrue("应包含 hue 滤镜", hueIdx >= 0)

        // 顺序校验：fps → crop → hflip → hue
        assertTrue("fps 应在 crop 之前", fpsIdx < cropIdx)
        assertTrue("crop 应在 hflip 之前", cropIdx < hflipIdx)
        assertTrue("hflip 应在 hue 之前", hflipIdx < hueIdx)
    }
}

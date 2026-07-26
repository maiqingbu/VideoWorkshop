package com.videoworkshop.core.ffmpeg.pipeline

import com.videoworkshop.domain.model.ABTransportConfig
import com.videoworkshop.domain.model.ABTransportMode
import com.videoworkshop.domain.model.DurationStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AVSwapCommandBuilder] 的单元测试。
 *
 * 覆盖 PURE_REPLACE / MIX 两种模式的命令构建，包括流映射、滤镜图、权重、
 * 编码器选择与输出路径等场景。
 *
 * 在 JVM 单元测试环境下，[com.videoworkshop.core.ffmpeg.hw.HardwareEncoderDetector]
 * 内部调用的 [android.media.MediaCodecList] 是 Android 桩类，调用即抛异常并被捕获，
 * 因此 [com.videoworkshop.core.ffmpeg.hw.HardwareEncoderDetector.getBestEncoder] 始终
 * 回退到软件编码器 `libx264`。
 */
class AVSwapCommandBuilderTest {

    /** 测试用 A 视频（音频源）路径。 */
    private val videoAPath = "/A.mp4"

    /** 测试用 B 视频（画面源）路径。 */
    private val videoBPath = "/B.mp4"

    /** 测试用输出路径。 */
    private val outputPath = "/out.mp4"

    /**
     * 构造 PURE_REPLACE 模式的默认配置。
     */
    private fun pureReplaceConfig(
        volumeRatioA: Float = 1.0f,
        volumeRatioB: Float = 0.5f
    ) = ABTransportConfig(
        videoAPath = videoAPath,
        videoBPath = videoBPath,
        mode = ABTransportMode.PURE_REPLACE,
        durationStrategy = DurationStrategy.TRUNCATE,
        volumeRatioA = volumeRatioA,
        volumeRatioB = volumeRatioB,
        outputPath = outputPath
    )

    /**
     * 构造 MIX 模式的默认配置。
     */
    private fun mixConfig(
        volumeRatioA: Float = 1.0f,
        volumeRatioB: Float = 0.5f
    ) = ABTransportConfig(
        videoAPath = videoAPath,
        videoBPath = videoBPath,
        mode = ABTransportMode.MIX,
        durationStrategy = DurationStrategy.TRUNCATE,
        volumeRatioA = volumeRatioA,
        volumeRatioB = volumeRatioB,
        outputPath = outputPath
    )

    // ===== 用例 1：PURE_REPLACE 模式基础场景（含 -i B -i A -map 0:v:0 -map 1:a:0）=====
    @Test
    fun build_pureReplace_containsExpectedInputsAndMaps() {
        val args = AVSwapCommandBuilder().build(pureReplaceConfig())

        // 校验输入：-i B -i A（B 为输入 0，A 为输入 1，与 AVStreamSwapper 保持一致）
        val inputAIdx = args.indexOf(videoAPath)
        val inputBIdx = args.indexOf(videoBPath)
        assertTrue("应包含 -i A", inputAIdx > 0 && args[inputAIdx - 1] == "-i")
        assertTrue("应包含 -i B", inputBIdx > 0 && args[inputBIdx - 1] == "-i")
        assertTrue("B 应在 A 之前（B 为输入 0）", inputBIdx < inputAIdx)

        // 校验流映射：-map 0:v:0（B 画面） -map 1:a:0（A 音频）
        val mapVideoIdx = args.indexOfSequence("-map", "0:v:0")
        val mapAudioIdx = args.indexOfSequence("-map", "1:a:0")
        assertTrue("应包含 -map 0:v:0（取 B 画面）", mapVideoIdx >= 0)
        assertTrue("应包含 -map 1:a:0（取 A 音频）", mapAudioIdx >= 0)
    }

    // ===== 用例 2：PURE_REPLACE 模式不含 filter_complex =====
    @Test
    fun build_pureReplace_hasNoFilterComplex() {
        val args = AVSwapCommandBuilder().build(pureReplaceConfig())

        assertFalse(
            "PURE_REPLACE 模式不应包含 -filter_complex",
            args.contains("-filter_complex")
        )
    }

    // ===== 用例 3：MIX 模式含 amix filter_complex =====
    @Test
    fun build_mix_containsAmixFilterComplex() {
        val args = AVSwapCommandBuilder().build(mixConfig())

        val filterIdx = args.indexOf("-filter_complex")
        assertTrue("MIX 模式应包含 -filter_complex", filterIdx >= 0)

        // 校验滤镜内容包含 amix
        val filterValue = args[filterIdx + 1]
        assertTrue(
            "filter_complex 应包含 amix=inputs=2",
            filterValue.contains("amix=inputs=2")
        )
        // 输入 0 = B（原声），输入 1 = A（音频源）；amix 输入顺序为 [1:a][0:a]（A 先 B 后，与 weights 一致）
        assertTrue(
            "amix 应使用 A 与 B 的音频输入 [1:a][0:a]",
            filterValue.contains("[1:a][0:a]")
        )
        assertTrue(
            "amix 输出应标注 [aout]",
            filterValue.contains("[aout]")
        )

        // -map 应包含 0:v:0 与 [aout]（B 为输入 0，提供视频流）
        assertTrue(
            "应包含 -map 0:v:0",
            args.indexOfSequence("-map", "0:v:0") >= 0
        )
        assertTrue(
            "应包含 -map [aout]",
            args.indexOfSequence("-map", "[aout]") >= 0
        )
        // MIX 模式音频来自混音结果，不应直接映射 A 的音频
        assertFalse(
            "MIX 模式不应直接映射 -map 1:a:0",
            args.indexOfSequence("-map", "1:a:0") >= 0
        )
    }

    // ===== 用例 4：MIX 模式 weights 反映 volumeRatioA/B =====
    @Test
    fun build_mix_weightsReflectVolumeRatios() {
        val args = AVSwapCommandBuilder().build(
            mixConfig(volumeRatioA = 0.8f, volumeRatioB = 0.3f)
        )

        val filterIdx = args.indexOf("-filter_complex")
        assertTrue(filterIdx >= 0)
        val filterValue = args[filterIdx + 1]

        assertTrue(
            "amix weights 应反映 volumeRatioA=0.8 与 volumeRatioB=0.3",
            filterValue.contains("weights='0.8 0.3'")
        )
    }

    // ===== 用例 5：输出路径正确（位于参数列表末尾）=====
    @Test
    fun build_outputPathPlacedAtEnd() {
        val args = AVSwapCommandBuilder().build(pureReplaceConfig())

        assertEquals(
            "输出路径应位于参数列表末尾",
            outputPath,
            args.last()
        )
    }

    // ===== 用例 6：视频编码器选择（默认 copy；启用重编码时由 HardwareEncoderDetector 选择）=====
    @Test
    fun build_defaultVideoCodecIsCopy() {
        val args = AVSwapCommandBuilder().build(pureReplaceConfig())

        // 默认应使用 -c:v copy（AB 搬运不改变画面）
        assertTrue(
            "默认应使用 -c:v copy",
            args.indexOfSequence("-c:v", "copy") >= 0
        )
    }

    @Test
    fun build_reencodeVideoUsesHardwareEncoderDetector() {
        // 启用重编码：JVM 环境下 HardwareEncoderDetector 回退到 libx264
        val args = AVSwapCommandBuilder()
            .reencodeVideo(true)
            .build(pureReplaceConfig())

        assertTrue(
            "启用重编码时 JVM 环境下应使用 libx264",
            args.indexOfSequence("-c:v", "libx264") >= 0
        )
        assertFalse(
            "启用重编码时不应使用 copy",
            args.indexOfSequence("-c:v", "copy") >= 0
        )
    }

    // ===== 用例 7：音频编码器为 aac =====
    @Test
    fun build_audioCodecIsAac() {
        val args = AVSwapCommandBuilder().build(pureReplaceConfig())

        assertTrue(
            "音频编码器应为 aac",
            args.indexOfSequence("-c:a", "aac") >= 0
        )
    }

    /**
     * 辅助：在参数列表中查找连续出现的两个 token，返回首 token 的索引，未找到返回 -1。
     */
    private fun List<String>.indexOfSequence(first: String, second: String): Int {
        for (i in 0 until size - 1) {
            if (this[i] == first && this[i + 1] == second) return i
        }
        return -1
    }
}

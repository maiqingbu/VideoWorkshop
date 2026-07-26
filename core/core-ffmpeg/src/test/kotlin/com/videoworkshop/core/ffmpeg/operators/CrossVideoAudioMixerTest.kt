package com.videoworkshop.core.ffmpeg.operators

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [CrossVideoAudioMixer] 的单元测试。
 *
 * 验证跨视频音频混合命令的构建，覆盖：
 * - 双输入（`-i A -i B`）顺序
 * - `filter_complex` 含 `volume`（A、B 分别调音量）与 `amix` 滤镜
 * - 流映射（`-map 1:v:0 -map [aout]`）
 * - 编码器（`-c:v copy -c:a aac`）
 * - 音量比例参数与输出路径
 * - 音量越界截断
 *
 * 所有用例均校验 [CrossVideoAudioMixer.buildCommand] 返回的参数列表（不含 `ffmpeg` 前缀）。
 */
class CrossVideoAudioMixerTest {

    private val mixer = CrossVideoAudioMixer()

    private val videoA = "/input/A.mp4"
    private val videoB = "/input/B.mp4"
    private val output = "/output/out.mp4"

    // ===== 用例 1：基础混合命令含 -i A -i B（且顺序为 A 先 B 后）=====
    @Test
    fun buildCommand_包含双输入且顺序为A先B后() {
        val args = mixer.buildCommand(videoA, videoB, 1.0f, 0.5f, output)

        val inputAIdx = args.indexOf(videoA)
        val inputBIdx = args.indexOf(videoB)
        assertTrue("应包含 A 视频输入路径", inputAIdx >= 0)
        assertTrue("应包含 B 视频输入路径", inputBIdx >= 0)
        assertTrue("A 输入应在 B 输入之前", inputAIdx < inputBIdx)

        // -i 应紧邻各自输入路径之前
        assertEquals("-i 应位于 A 路径之前", "-i", args[inputAIdx - 1])
        assertEquals("-i 应位于 B 路径之前", "-i", args[inputBIdx - 1])
    }

    // ===== 用例 2：filter_complex 含 volume 滤镜（A 和 B 分别调音量）=====
    @Test
    fun buildCommand_filterComplex含两路volume滤镜() {
        val args = mixer.buildCommand(videoA, videoB, 0.8f, 0.3f, output)

        val filterIdx = args.indexOf("-filter_complex")
        assertTrue("应包含 -filter_complex 参数", filterIdx >= 0)
        val filter = args[filterIdx + 1]

        // A 路音量：[0:a]volume=0.8[a0]
        assertTrue(
            "filter_complex 应含 A 路 volume 滤镜 [0:a]",
            filter.contains("[0:a]volume=")
        )
        assertTrue(
            "A 路 volume 输出标签应为 [a0]",
            filter.contains("[a0]")
        )

        // B 路音量：[1:a]volume=0.3[a1]
        assertTrue(
            "filter_complex 应含 B 路 volume 滤镜 [1:a]",
            filter.contains("[1:a]volume=")
        )
        assertTrue(
            "B 路 volume 输出标签应为 [a1]",
            filter.contains("[a1]")
        )

        // A 路应在 B 路之前（保持输入顺序）
        assertTrue(
            "A 路 volume 应在 B 路 volume 之前",
            filter.indexOf("[0:a]volume=") < filter.indexOf("[1:a]volume=")
        )
    }

    // ===== 用例 3：filter_complex 含 amix 滤镜 =====
    @Test
    fun buildCommand_filterComplex含amix滤镜() {
        val args = mixer.buildCommand(videoA, videoB, 1.0f, 1.0f, output)

        val filterIdx = args.indexOf("-filter_complex")
        assertTrue("应包含 -filter_complex 参数", filterIdx >= 0)
        val filter = args[filterIdx + 1]

        assertTrue(
            "filter_complex 应含 amix 滤镜（inputs=2, duration=first）",
            filter.contains("amix=inputs=2:duration=first")
        )
        assertTrue(
            "amix 输入应为 [a0] 与 [a1]",
            filter.contains("[a0][a1]amix")
        )
        assertTrue(
            "amix 输出标签应为 [aout]",
            filter.contains("[aout]")
        )
    }

    // ===== 用例 4：-map 1:v:0 -map [aout] =====
    @Test
    fun buildCommand_流映射为B画面与混合音频() {
        val args = mixer.buildCommand(videoA, videoB, 1.0f, 0.5f, output)

        val firstMapIdx = args.indexOf("-map")
        assertTrue("应包含 -map 参数", firstMapIdx >= 0)

        // 第一个 -map 1:v:0（B 画面）
        assertEquals(
            "第一个 -map 应为 1:v:0（取 B 视频流）",
            "1:v:0",
            args[firstMapIdx + 1]
        )

        // 第二个 -map [aout]（混合音频）
        val secondMapIdx = args.indexOfFrom("-map", firstMapIdx + 1)
        assertTrue("应包含第二个 -map", secondMapIdx > firstMapIdx)
        assertEquals(
            "第二个 -map 应为 [aout]（取混合后音频）",
            "[aout]",
            args[secondMapIdx + 1]
        )
    }

    // ===== 用例 5：-c:v copy -c:a aac =====
    @Test
    fun buildCommand_视频流复制音频编码为aac() {
        val args = mixer.buildCommand(videoA, videoB, 1.0f, 0.5f, output)

        val videoCodecIdx = args.indexOf("-c:v")
        val audioCodecIdx = args.indexOf("-c:a")
        assertTrue("应包含 -c:v 参数", videoCodecIdx >= 0)
        assertTrue("应包含 -c:a 参数", audioCodecIdx >= 0)

        assertEquals(
            "视频编码器应为 copy（直接复制 B 画面）",
            "copy",
            args[videoCodecIdx + 1]
        )
        assertEquals(
            "音频编码器应为 aac",
            "aac",
            args[audioCodecIdx + 1]
        )
    }

    // ===== 用例 6：volumeRatioA=1.0, volumeRatioB=0.5 时 filter_complex 含正确权重 =====
    @Test
    fun buildCommand_A1B0p5_filterComplex含正确音量权重() {
        val args = mixer.buildCommand(videoA, videoB, 1.0f, 0.5f, output)

        val filterIdx = args.indexOf("-filter_complex")
        val filter = args[filterIdx + 1]

        // A 路音量为 1（1.0 格式化为 "1"）
        assertTrue(
            "A 路应含 volume=1 片段",
            filter.contains("[0:a]volume=1[a0]")
        )
        // B 路音量为 0.5
        assertTrue(
            "B 路应含 volume=0.5 片段",
            filter.contains("[1:a]volume=0.5[a1]")
        )
    }

    // ===== 用例 7：输出路径正确（位于参数列表末尾）=====
    @Test
    fun buildCommand_输出路径位于参数列表末尾() {
        val args = mixer.buildCommand(videoA, videoB, 1.0f, 0.5f, output)

        assertTrue("参数列表应包含输出路径", args.contains(output))
        assertEquals(
            "输出路径应位于参数列表末尾",
            output,
            args.last()
        )
    }

    // ===== 用例 8：音量比例越界时被截断到 [0.0, 1.0] =====
    @Test
    fun buildCommand_音量越界时截断到合法区间() {
        // A 超出上限（1.5 -> 1.0），B 低于下限（-0.3 -> 0.0）
        val args = mixer.buildCommand(videoA, videoB, 1.5f, -0.3f, output)

        val filterIdx = args.indexOf("-filter_complex")
        val filter = args[filterIdx + 1]

        // A 应被截断为 1.0 -> 格式化为 "1"
        assertTrue(
            "A 音量越界应截断为 1",
            filter.contains("[0:a]volume=1[a0]")
        )
        // B 应被截断为 0.0 -> 格式化为 "0"
        assertTrue(
            "B 音量越界应截断为 0",
            filter.contains("[1:a]volume=0[a1]")
        )
    }

    // ===== 用例 9：完整命令结构整体校验（顺序、关键 token）=====
    @Test
    fun buildCommand_完整命令结构符合预期() {
        val args = mixer.buildCommand(videoA, videoB, 1.0f, 0.5f, output)

        // 关键 token 的相对顺序校验
        val inputAIdx = args.indexOf("-i")
        val filterIdx = args.indexOf("-filter_complex")
        val firstMapIdx = args.indexOf("-map")
        val videoCodecIdx = args.indexOf("-c:v")
        val audioCodecIdx = args.indexOf("-c:a")
        val outputIdx = args.indexOf(output)

        assertTrue("-i 应位于 -filter_complex 之前", inputAIdx < filterIdx)
        assertTrue("-filter_complex 应位于 -map 之前", filterIdx < firstMapIdx)
        assertTrue("-map 应位于 -c:v 之前", firstMapIdx < videoCodecIdx)
        assertTrue("-c:v 应位于 -c:a 之前", videoCodecIdx < audioCodecIdx)
        assertTrue("-c:a 应位于输出路径之前", audioCodecIdx < outputIdx)
    }

    /**
     * 辅助：从 [startIndex] 开始查找 [element] 在列表中的索引，未找到返回 -1。
     *
     * 用于查找同一 token（如 `-map`）在参数列表中的第二次出现位置。
     */
    private fun List<String>.indexOfFrom(element: String, startIndex: Int): Int {
        for (i in startIndex until size) {
            if (this[i] == element) return i
        }
        return -1
    }
}

package com.videoworkshop.core.ffmpeg.operators

import com.videoworkshop.core.ffmpeg.pipeline.MultiInputCommandBuilder
import com.videoworkshop.domain.model.TimelineSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [TimelineAligner] 的单元测试。
 *
 * 覆盖三种时长对齐策略（TRUNCATE / LOOP / CUSTOM）的核心命令生成逻辑，
 * 校验 [MultiInputCommandBuilder.build] 输出的参数列表中关键 token 的存在性、
 * 取值与相对顺序。
 *
 * 所有用例均不依赖 FFmpeg 实际执行，仅做命令参数级别的断言。
 */
class TimelineAlignerTest {

    /** 被测对象（无状态，可复用）。 */
    private val aligner = TimelineAligner()

    /** 测试用 A 视频路径。 */
    private val pathA = "/A.mp4"

    /** 测试用 B 视频路径。 */
    private val pathB = "/B.mp4"

    // ===== 用例 1：TRUNCATE min(30s, 45s) = 30s =====
    @Test
    fun applyTruncate_aShorter_addsTWithMinDuration() {
        val builder = MultiInputCommandBuilder()
            .addInput(pathA)
            .addInput(pathB)
        aligner.applyTruncate(builder, 30_000L, 45_000L)
        val args = builder.build()

        val tIdx = args.indexOf("-t")
        assertTrue("TRUNCATE 应追加 -t 参数", tIdx >= 0)
        assertEquals("min(30s, 45s) 应为 30s", "30", args[tIdx + 1])
    }

    // ===== 用例 2：TRUNCATE min(45s, 30s) = 30s =====
    @Test
    fun applyTruncate_bShorter_addsTWithMinDuration() {
        val builder = MultiInputCommandBuilder()
            .addInput(pathA)
            .addInput(pathB)
        aligner.applyTruncate(builder, 45_000L, 30_000L)
        val args = builder.build()

        val tIdx = args.indexOf("-t")
        assertTrue("TRUNCATE 应追加 -t 参数", tIdx >= 0)
        assertEquals("min(45s, 30s) 应为 30s", "30", args[tIdx + 1])
    }

    // ===== 用例 3：LOOP max(30s, 45s) = 45s，短视频(A)含 -stream_loop -1 =====
    @Test
    fun applyLoop_aShorter_addsStreamLoopToAAndTWithMaxDuration() {
        val builder = MultiInputCommandBuilder()
        aligner.applyLoop(builder, pathA, pathB, 30_000L, 45_000L)
        val args = builder.build()

        // 期望命令：-stream_loop -1 -i /A.mp4 -i /B.mp4 -t 45
        val loopIdx = args.indexOf("-stream_loop")
        assertTrue("LOOP 应为短视频追加 -stream_loop", loopIdx >= 0)
        assertEquals("-stream_loop 的值应为 -1（无限循环）", "-1", args[loopIdx + 1])

        // -stream_loop -1 应紧接在 -i A.mp4 之前（input option 必须位于 -i 之前）
        assertTrue(
            "-stream_loop -1 应位于 -i $pathA 之前",
            loopIdx + 3 < args.size &&
                args[loopIdx + 2] == "-i" &&
                args[loopIdx + 3] == pathA
        )

        // 仅 A 被循环：-stream_loop 在整个命令中应只出现 1 次
        assertEquals(
            "-stream_loop 应只出现 1 次（仅作用于短视频 A）",
            1,
            args.count { it == "-stream_loop" }
        )

        // 输出端 -t 应为 max(30s, 45s) = 45s
        val tIdx = args.indexOf("-t")
        assertTrue("LOOP 应追加 -t 限制输出时长", tIdx >= 0)
        assertEquals("max(30s, 45s) 应为 45s", "45", args[tIdx + 1])
    }

    // ===== 用例 4：CUSTOM A 裁剪 5s-20s，B 裁剪 0s-15s，命令含对应 -ss/-t =====
    @Test
    fun applyCustomSegment_addsInputSeekingOptionsForBothInputs() {
        val segA = TimelineSegment(startMs = 5_000L, endMs = 20_000L) // 5s-20s, duration 15s
        val segB = TimelineSegment(startMs = 0L, endMs = 15_000L)     // 0s-15s, duration 15s
        val builder = MultiInputCommandBuilder()
        aligner.applyCustomSegment(builder, pathA, pathB, segA, segB)
        val args = builder.build()

        // 期望命令：-ss 5 -t 15 -i /A.mp4 -ss 0 -t 15 -i /B.mp4
        // A: -ss 5 -t 15 -i /A.mp4
        val ssAIdx = args.indexOf("-ss")
        assertTrue("A 的 -ss 应存在", ssAIdx >= 0)
        assertEquals("A startMs=5s", "5", args[ssAIdx + 1])
        assertEquals("A -t 标记", "-t", args[ssAIdx + 2])
        assertEquals("A durationMs=15s", "15", args[ssAIdx + 3])
        assertEquals("A -i 标记", "-i", args[ssAIdx + 4])
        assertEquals("A path", pathA, args[ssAIdx + 5])

        // B: -ss 0 -t 15 -i /B.mp4（从 ssAIdx+1 之后查找下一个 -ss）
        val ssBIdx = (ssAIdx + 1 until args.size).firstOrNull { args[it] == "-ss" } ?: -1
        assertTrue("B 的 -ss 应存在", ssBIdx >= 0)
        assertEquals("B startMs=0s", "0", args[ssBIdx + 1])
        assertEquals("B -t 标记", "-t", args[ssBIdx + 2])
        assertEquals("B durationMs=15s", "15", args[ssBIdx + 3])
        assertEquals("B -i 标记", "-i", args[ssBIdx + 4])
        assertEquals("B path", pathB, args[ssBIdx + 5])
    }

    // ===== 用例 5：自定义片段的 durationMs 计算正确（含非整数秒格式化） =====
    @Test
    fun applyCustomSegment_nonIntegerSeconds_formattedWithoutTrailingZero() {
        // 非整数秒：start=5.5s, end=20.25s, duration=14.75s
        val segA = TimelineSegment(startMs = 5_500L, endMs = 20_250L)
        val segB = TimelineSegment(startMs = 0L, endMs = 1_000L)
        val builder = MultiInputCommandBuilder()
        aligner.applyCustomSegment(builder, pathA, pathB, segA, segB)
        val args = builder.build()

        // 直接校验 TimelineSegment.durationMs 计算（领域模型保证）
        assertEquals(
            "TimelineSegment.durationMs 应为 endMs - startMs",
            14_750L,
            segA.durationMs
        )

        // 校验命令中 -ss / -t 的格式化结果
        val ssAIdx = args.indexOf("-ss")
        assertEquals("5.5s 应格式化为 5.5", "5.5", args[ssAIdx + 1])
        // -ss 后紧跟 -t，-t 后是 duration
        assertEquals("14.75s 应格式化为 14.75", "14.75", args[ssAIdx + 3])
    }

    // ===== 用例 6：边界场景 A=B 时长，TRUNCATE 添加 -t 等于该时长 =====
    @Test
    fun applyTruncate_equalDurations_addsTWithSameDuration() {
        val builder = MultiInputCommandBuilder()
            .addInput(pathA)
            .addInput(pathB)
        aligner.applyTruncate(builder, 30_000L, 30_000L)
        val args = builder.build()

        val tIdx = args.indexOf("-t")
        assertTrue("A=B 时长时仍应追加 -t（行为可预测）", tIdx >= 0)
        assertEquals("A=B=30s 时 -t 应为 30s", "30", args[tIdx + 1])
    }

    // ===== 补充用例：LOOP 当 A=B 时长，不应对任何输入加 -stream_loop =====
    @Test
    fun applyLoop_equalDurations_addsNoStreamLoop() {
        val builder = MultiInputCommandBuilder()
        aligner.applyLoop(builder, pathA, pathB, 30_000L, 30_000L)
        val args = builder.build()

        assertFalse("A=B 时长时不应有 -stream_loop", args.contains("-stream_loop"))

        val tIdx = args.indexOf("-t")
        assertTrue("LOOP 仍应追加 -t 限制输出时长", tIdx >= 0)
        assertEquals("max(30s, 30s) 应为 30s", "30", args[tIdx + 1])
    }

    // ===== 补充用例：LOOP 当 B 比 A 短，-stream_loop 应作用于 B =====
    @Test
    fun applyLoop_bShorter_addsStreamLoopToBOnly() {
        val builder = MultiInputCommandBuilder()
        aligner.applyLoop(builder, pathA, pathB, 45_000L, 30_000L)
        val args = builder.build()

        // 期望命令：-i /A.mp4 -stream_loop -1 -i /B.mp4 -t 45
        val loopIdx = args.indexOf("-stream_loop")
        assertTrue("LOOP 应为短视频 B 追加 -stream_loop", loopIdx >= 0)
        assertEquals("-stream_loop 的值应为 -1", "-1", args[loopIdx + 1])
        assertTrue(
            "-stream_loop -1 应位于 -i $pathB 之前",
            args[loopIdx + 2] == "-i" && args[loopIdx + 3] == pathB
        )

        // 仅 B 被循环：-stream_loop 在整个命令中应只出现 1 次
        assertEquals(
            "-stream_loop 应只出现 1 次（仅作用于短视频 B）",
            1,
            args.count { it == "-stream_loop" }
        )

        // 输出端 -t 应为 max(45s, 30s) = 45s
        val tIdx = args.indexOf("-t")
        assertTrue("LOOP 应追加 -t 限制输出时长", tIdx >= 0)
        assertEquals("max(45s, 30s) 应为 45s", "45", args[tIdx + 1])
    }

    // ===== 补充用例：applyTruncate 返回同一 builder 实例（支持链式） =====
    @Test
    fun applyTruncate_returnsSameBuilderInstance() {
        val builder = MultiInputCommandBuilder()
        val result = aligner.applyTruncate(builder, 30_000L, 45_000L)
        assertTrue("applyTruncate 应返回同一 builder 实例", result === builder)
    }

    // ===== 补充用例：applyLoop 返回同一 builder 实例（支持链式） =====
    @Test
    fun applyLoop_returnsSameBuilderInstance() {
        val builder = MultiInputCommandBuilder()
        val result = aligner.applyLoop(builder, pathA, pathB, 30_000L, 45_000L)
        assertTrue("applyLoop 应返回同一 builder 实例", result === builder)
    }

    // ===== 补充用例：applyCustomSegment 返回同一 builder 实例（支持链式） =====
    @Test
    fun applyCustomSegment_returnsSameBuilderInstance() {
        val builder = MultiInputCommandBuilder()
        val segA = TimelineSegment(0L, 1_000L)
        val segB = TimelineSegment(0L, 1_000L)
        val result = aligner.applyCustomSegment(builder, pathA, pathB, segA, segB)
        assertTrue("applyCustomSegment 应返回同一 builder 实例", result === builder)
    }
}

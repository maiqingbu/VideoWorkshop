package com.videoworkshop.core.ffmpeg.operators

import com.videoworkshop.domain.model.ABTransportConfig
import com.videoworkshop.domain.model.ABTransportMode
import com.videoworkshop.domain.model.DurationStrategy
import com.videoworkshop.domain.model.TimelineSegment
import com.videoworkshop.domain.model.VideoClip
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.cancelAndJoin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AVStreamSwapper] 的单元测试。
 *
 * 由于 [com.videoworkshop.core.ffmpeg.FfmpegEngine] 是单例 `object`，且 FFmpegKit
 * 在 core-ffmpeg 模块中为 `compileOnly`（不在测试 classpath 上），无法直接 mock。
 * 故通过 [FfmpegExecutor] 抽象注入 [FakeFfmpegExecutor]，并注入伪 probe 函数，
 * 实现完全隔离的纯 JVM 单元测试。
 *
 * 覆盖用例：
 * 1. swap 调用 FfmpegExecutor.executeAsync
 * 2. 进度回调正确转换（StatisticsCallback time → progress 0-1 → currentMs）
 * 3. 取消收集时调用 FfmpegHandle.cancel
 * 4. 失败时发射 error 状态
 * 5. 成功时发射完成状态（progress=1.0, outputPath 非空）
 * 另含命令构建（PURE_REPLACE / MIX / TRUNCATE / CUSTOM）的覆盖。
 */
class AVStreamSwapperTest {

    /** A 视频（音频源）：8 秒。 */
    private val clipA = VideoClip(
        path = "/audio_source.mp4",
        duration = 8_000L,
        width = 1920,
        height = 1080,
        size = 4_000_000L,
        fps = 30f,
        bitrate = 2_000_000L,
        mimeType = "video/mp4"
    )

    /** B 视频（画面源）：10 秒。 */
    private val clipB = VideoClip(
        path = "/video_source.mp4",
        duration = 10_000L,
        width = 1080,
        height = 1920,
        size = 5_000_000L,
        fps = 30f,
        bitrate = 2_500_000L,
        mimeType = "video/mp4"
    )

    /** PURE_REPLACE + TRUNCATE 默认配置。 */
    private val baseConfig = ABTransportConfig(
        videoAPath = clipA.path,
        videoBPath = clipB.path,
        mode = ABTransportMode.PURE_REPLACE,
        durationStrategy = DurationStrategy.TRUNCATE,
        outputPath = "/output.mp4"
    )

    /** 伪 probe 函数：按路径返回预设的 [VideoClip]。 */
    private val fakeProbe: suspend (String) -> VideoClip = { path ->
        when (path) {
            clipA.path -> clipA
            clipB.path -> clipB
            else -> throw IllegalArgumentException("unexpected path: $path")
        }
    }

    // ===== 用例 1：swap 调用 FfmpegExecutor.executeAsync =====

    @Test
    fun swap_callsExecutorExecuteAsyncExactlyOnce() = runTest {
        val fake = FakeFfmpegExecutor()
        val swapper = AVStreamSwapper(fake, probeVideo = fakeProbe)

        swapper.swap(baseConfig).toList()

        assertEquals("swap 应调用 executeAsync 恰好一次", 1, fake.executeCallCount)
        assertNotNull("应将命令传给 executor", fake.lastCommand)
        assertTrue("命令应以 ffmpeg 开头", fake.lastCommand!!.startsWith("ffmpeg"))
    }

    // ===== 用例 2：进度回调正确转换（progress 0-1 → currentMs）=====
    // TRUNCATE 策略下 totalMs = min(8000, 10000) = 8000
    // 0.25 * 8000 = 2000ms；0.5 * 8000 = 4000ms；0.75 * 8000 = 6000ms

    @Test
    fun swap_progressCallback_convertsToAbTransportProgress() = runTest {
        val fake = FakeFfmpegExecutor().apply {
            scriptedProgress = listOf(0.25f, 0.5f, 0.75f)
        }
        val swapper = AVStreamSwapper(fake, probeVideo = fakeProbe)

        val results = swapper.swap(baseConfig).toList()

        // 前 3 条为进行中的进度
        assertEquals("进行中进度条数应等于脚本进度数", 3, results.size - 1)

        assertEquals(0.25f, results[0].progress, 0.0001f)
        assertEquals(2000L, results[0].currentMs)
        assertEquals(8000L, results[0].totalMs)
        assertNull("进行中不应有 outputPath", results[0].outputPath)
        assertNull("进行中不应有 error", results[0].error)
        assertFalse("进行中不应为完成态", results[0].isCompleted)

        assertEquals(0.5f, results[1].progress, 0.0001f)
        assertEquals(4000L, results[1].currentMs)

        assertEquals(0.75f, results[2].progress, 0.0001f)
        assertEquals(6000L, results[2].currentMs)
    }

    // ===== 用例 3：取消收集时调用 FfmpegHandle.cancel =====

    @Test
    fun swap_whenCollectedAndCancelled_callsHandleCancel() = runTest {
        // autoComplete=false：executor 不主动调用 onComplete，保持 flow 开启
        val fake = FakeFfmpegExecutor().apply { autoComplete = false }
        val swapper = AVStreamSwapper(fake, probeVideo = fakeProbe)

        val flow = swapper.swap(baseConfig)
        val job = launch { flow.collect { } }

        // 运行 producer：注册 executeAsync 与 awaitClose（autoComplete=false 时 flow 保持开启）
        testScheduler.advanceUntilIdle()

        // 取消收集 → 触发 awaitClose { handle.cancel() }
        job.cancelAndJoin()
        // 确保 awaitClose 的清理回调已执行
        testScheduler.advanceUntilIdle()

        assertEquals("取消收集后应调用 handle.cancel 一次", 1, fake.cancelCount)
    }

    // ===== 用例 4：失败时发射 error 状态 =====

    @Test
    fun swap_whenExecutionFails_emitsFailedProgress() = runTest {
        val fake = FakeFfmpegExecutor().apply { succeed = false }
        val swapper = AVStreamSwapper(fake, probeVideo = fakeProbe)

        val results = swapper.swap(baseConfig).toList()

        val last = results.last()
        assertTrue("失败时末条应为失败态", last.isFailed)
        assertNotNull("失败时应携带 error", last.error)
        assertTrue("error 应为 FfmpegSwapException", last.error is FfmpegSwapException)
        assertNull("失败时不应有 outputPath", last.outputPath)
        assertFalse("失败时不应为完成态", last.isCompleted)
    }

    // ===== 用例 5：成功时发射完成状态（progress=1.0, outputPath 非空）=====

    @Test
    fun swap_whenExecutionSucceeds_emitsCompletedProgress() = runTest {
        val fake = FakeFfmpegExecutor() // 默认 succeed=true
        val swapper = AVStreamSwapper(fake, probeVideo = fakeProbe)

        val results = swapper.swap(baseConfig).toList()

        val last = results.last()
        assertTrue("成功时末条应为完成态", last.isCompleted)
        assertEquals("完成时 progress 应为 1.0", 1f, last.progress, 0.0001f)
        assertEquals("完成时 outputPath 应为配置的输出路径", baseConfig.outputPath, last.outputPath)
        assertNull("完成时不应有 error", last.error)
        assertEquals("完成时 currentMs 应等于 totalMs", 8000L, last.currentMs)
        assertEquals("完成时 totalMs 应为 8000", 8000L, last.totalMs)
    }

    // ===== 命令构建：PURE_REPLACE 使用 -map 0:v:0 -map 1:a:0 =====

    @Test
    fun buildCommand_pureReplace_mapsBVideoAndAAudio() = runTest {
        val fake = FakeFfmpegExecutor()
        val swapper = AVStreamSwapper(fake, probeVideo = fakeProbe)

        swapper.swap(baseConfig).toList()

        val cmd = fake.lastCommand!!
        assertTrue("应映射 B 的视频流 0:v:0", cmd.contains("-map 0:v:0"))
        assertTrue("应映射 A 的音频流 1:a:0", cmd.contains("-map 1:a:0"))
        assertFalse("PURE_REPLACE 不应使用 filter_complex", cmd.contains("-filter_complex"))
        // 输入顺序：B 在前（input 0），A 在后（input 1）
        assertTrue("B 应先于 A 出现", cmd.indexOf(clipB.path) < cmd.indexOf(clipA.path))
    }

    // ===== 命令构建：MIX 使用 filter_complex + amix =====

    @Test
    fun buildCommand_mix_usesFilterComplexWithAmix() = runTest {
        val config = baseConfig.copy(
            mode = ABTransportMode.MIX,
            volumeRatioA = 1.0f,
            volumeRatioB = 0.5f
        )
        val fake = FakeFfmpegExecutor()
        val swapper = AVStreamSwapper(fake, probeVideo = fakeProbe)

        swapper.swap(config).toList()

        val cmd = fake.lastCommand!!
        assertTrue("MIX 应使用 -filter_complex", cmd.contains("-filter_complex"))
        assertTrue("应包含 amix 滤镜", cmd.contains("amix=inputs=2"))
        assertTrue("应映射 [aout]", cmd.contains("-map [aout]"))
        assertTrue("A 音量应为 1.00", cmd.contains("volume=1.00"))
        assertTrue("B 音量应为 0.50", cmd.contains("volume=0.50"))
    }

    // ===== 命令构建：TRUNCATE 追加 -shortest =====

    @Test
    fun buildCommand_truncate_addsShortestFlag() = runTest {
        val fake = FakeFfmpegExecutor()
        val swapper = AVStreamSwapper(fake, probeVideo = fakeProbe)

        swapper.swap(baseConfig).toList() // baseConfig 即 TRUNCATE

        assertTrue("TRUNCATE 应追加 -shortest", fake.lastCommand!!.contains("-shortest"))
    }

    // ===== 时长选择：TRUNCATE=min / LOOP=max / CUSTOM=segmentB =====

    @Test
    fun chooseTotalMs_truncate_returnsMinDuration() {
        val swapper = AVStreamSwapper(FakeFfmpegExecutor(), probeVideo = fakeProbe)
        val config = baseConfig.copy(durationStrategy = DurationStrategy.TRUNCATE)

        val total = swapper.chooseTotalMs(clipA, clipB, config)

        assertEquals("TRUNCATE 应取 min(8000, 10000) = 8000", 8000L, total)
    }

    @Test
    fun chooseTotalMs_loop_returnsMaxDuration() {
        val swapper = AVStreamSwapper(FakeFfmpegExecutor(), probeVideo = fakeProbe)
        val config = baseConfig.copy(durationStrategy = DurationStrategy.LOOP)

        val total = swapper.chooseTotalMs(clipA, clipB, config)

        assertEquals("LOOP 应取 max(8000, 10000) = 10000", 10_000L, total)
    }

    @Test
    fun chooseTotalMs_custom_returnsSegmentBDuration() {
        val swapper = AVStreamSwapper(FakeFfmpegExecutor(), probeVideo = fakeProbe)
        val segB = TimelineSegment(startMs = 1_000, endMs = 4_000) // 时长 3000ms
        val config = baseConfig.copy(
            durationStrategy = DurationStrategy.CUSTOM,
            segmentB = segB
        )

        val total = swapper.chooseTotalMs(clipA, clipB, config)

        assertEquals("CUSTOM 应取 segmentB.durationMs = 3000", 3_000L, total)
    }

    // ===== 命令构建：CUSTOM 片段生成 -ss/-to =====

    @Test
    fun buildCommand_custom_emitsSeekOptionsForSegmentB() = runTest {
        val segB = TimelineSegment(startMs = 1_000, endMs = 4_000)
        val config = baseConfig.copy(
            durationStrategy = DurationStrategy.CUSTOM,
            segmentB = segB
        )
        val fake = FakeFfmpegExecutor()
        val swapper = AVStreamSwapper(fake, probeVideo = fakeProbe)

        swapper.swap(config).toList()

        val cmd = fake.lastCommand!!
        assertTrue("应包含 -ss 1.000（segmentB 起点）", cmd.contains("-ss 1.000"))
        assertTrue("应包含 -to 4.000（segmentB 终点）", cmd.contains("-to 4.000"))
    }

    // ===== Fake 实现 =====

    /**
     * 假 FFmpeg 执行器。
     *
     * 同步模拟 FFmpegKit 的异步回调：收集 flow 时立即按 [scriptedProgress] 顺序
     * 触发 [onProgress]，并根据 [autoComplete]/[succeed] 决定是否触发 [onComplete]。
     *
     * 通过 [cancelCount] 记录 [FfmpegHandle.cancel] 调用次数，用于验证取消语义。
     */
    private class FakeFfmpegExecutor : FfmpegExecutor {
        var lastCommand: String? = null
            private set
        var lastDurationMs: Long = 0L
            private set
        var executeCallCount: Int = 0
            private set
        var cancelCount: Int = 0
            private set

        /** 按顺序触发的进度值。 */
        var scriptedProgress: List<Float> = emptyList()

        /** onComplete 的成功与否。 */
        var succeed: Boolean = true

        /** 是否自动触发 onComplete（关闭 flow）；为 false 时 flow 保持开启以便测试取消。 */
        var autoComplete: Boolean = true

        override fun executeAsync(
            command: String,
            durationMs: Long,
            onProgress: (Float) -> Unit,
            onComplete: (Boolean) -> Unit
        ): FfmpegHandle {
            executeCallCount++
            lastCommand = command
            lastDurationMs = durationMs

            for (p in scriptedProgress) {
                onProgress(p)
            }
            if (autoComplete) {
                onComplete(succeed)
            }
            return FfmpegHandle { cancelCount++ }
        }
    }
}

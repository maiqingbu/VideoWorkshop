package com.videoworkshop.domain.usecase

import com.videoworkshop.domain.model.ABTransportConfig
import com.videoworkshop.domain.model.ABTransportMode
import com.videoworkshop.domain.model.ABTransportProgress
import com.videoworkshop.domain.model.DedupConfig
import com.videoworkshop.domain.model.DedupProgress
import com.videoworkshop.domain.model.DurationStrategy
import com.videoworkshop.domain.model.EnhanceConfig
import com.videoworkshop.domain.model.TimelineSegment
import com.videoworkshop.domain.model.VideoClip
import com.videoworkshop.domain.repository.DedupRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ABTransportUseCase] 的单元测试。
 *
 * domain 模块未引入 mockk，故通过 [FakeDedupRepository] 注入预设的进度序列，
 * 实现「无外部依赖、纯 JVM」的单元测试。覆盖用例：
 *
 * 1. 调用 UseCase 返回 [Flow]
 * 2. [Flow] 发射进度（进行中状态）
 * 3. [Flow] 发射完成（progress = 1f，outputPath 非空）
 * 4. [Flow] 发射错误（error 非空）
 * 5. 配置正确传递给 Repository（FakeDedupRepository 捕获最后一次调用的 config，含 PURE_REPLACE 场景）
 * 6. 音轨混合模式（MIX）配置传递
 * 7. 循环对齐策略（LOOP）配置传递
 * 8. 自定义起止点策略（CUSTOM）与 segmentA/segmentB 配置传递
 * 9. 取消收集时 UseCase 正确传播取消（不抛额外异常）
 * 10. A 视频无音轨异常由 Repository 转为 error 进度后，UseCase 正确转发
 */
class ABTransportUseCaseTest {

    // ===== 测试夹具 =====

    /** 默认 AB 搬运配置（PURE_REPLACE + TRUNCATE）。 */
    private val baseConfig = ABTransportConfig(
        videoAPath = "/audio_source.mp4",
        videoBPath = "/video_source.mp4",
        mode = ABTransportMode.PURE_REPLACE,
        durationStrategy = DurationStrategy.TRUNCATE,
        outputPath = "/output.mp4"
    )

    /** 完成态进度（progress = 1f, outputPath 非空）。 */
    private val completedProgress = ABTransportProgress(
        progress = 1f,
        currentMs = 8_000L,
        totalMs = 8_000L,
        outputPath = "/output.mp4"
    )

    /** 失败态进度（error 非空）。 */
    private val errorProgress = ABTransportProgress(
        progress = 0f,
        currentMs = 0L,
        totalMs = 8_000L,
        error = "FFmpeg 执行失败"
    )

    // ===== 用例 1：调用 UseCase 返回 Flow =====

    @Test
    fun invoke_returnsFlowFromRepository() = runTest {
        val fake = FakeDedupRepository(flowOf(completedProgress))
        val useCase = ABTransportUseCase(fake)

        val result = useCase(baseConfig)

        // 收集到至少一条进度，证明返回的是有效 Flow
        val emissions = result.toList()
        assertTrue("UseCase 应返回非空 Flow", emissions.isNotEmpty())
    }

    // ===== 用例 2：Flow 发射进度（进行中状态）=====

    @Test
    fun invoke_emitsInProgressProgressFromRepository() = runTest {
        val inProgress = ABTransportProgress(
            progress = 0.5f,
            currentMs = 4_000L,
            totalMs = 8_000L
        )
        val fake = FakeDedupRepository(flowOf(inProgress, completedProgress))
        val useCase = ABTransportUseCase(fake)

        val emissions = useCase(baseConfig).toList()

        assertEquals("应发射两条进度", 2, emissions.size)
        assertEquals("第一条应为进行中进度", inProgress, emissions[0])
        assertNull("进行中进度不应有 outputPath", emissions[0].outputPath)
        assertNull("进行中进度不应有 error", emissions[0].error)
        assertEquals("进行中进度 progress 应为 0.5f", 0.5f, emissions[0].progress, 0.0001f)
        assertFalse("进行中进度不应为完成态", emissions[0].isCompleted)
        assertFalse("进行中进度不应为失败态", emissions[0].isFailed)
    }

    // ===== 用例 3：Flow 发射完成 =====

    @Test
    fun invoke_emitsCompletedProgressFromRepository() = runTest {
        val fake = FakeDedupRepository(flowOf(completedProgress))
        val useCase = ABTransportUseCase(fake)

        val emissions = useCase(baseConfig).toList()

        assertEquals("应发射一条完成进度", 1, emissions.size)
        val last = emissions.last()
        assertTrue("完成态应标记为 isCompleted", last.isCompleted)
        assertNotNull("完成态 outputPath 应非空", last.outputPath)
        assertEquals("outputPath 应为配置的输出路径", baseConfig.outputPath, last.outputPath)
        assertNull("完成态 error 应为 null", last.error)
        assertFalse("完成态不应为失败态", last.isFailed)
    }

    // ===== 用例 4：Flow 发射错误 =====

    @Test
    fun invoke_emitsErrorProgressFromRepository() = runTest {
        val fake = FakeDedupRepository(flowOf(errorProgress))
        val useCase = ABTransportUseCase(fake)

        val emissions = useCase(baseConfig).toList()

        assertEquals("应发射一条错误进度", 1, emissions.size)
        val last = emissions.last()
        assertTrue("失败态应标记为 isFailed", last.isFailed)
        assertNotNull("失败态 error 应非空", last.error)
        assertEquals("error 内容应与 Fake 设置一致", "FFmpeg 执行失败", last.error)
        assertFalse("失败态不应为完成态", last.isCompleted)
        assertNull("失败态 outputPath 应为 null", last.outputPath)
    }

    // ===== 用例 5：配置正确传递给 Repository =====

    @Test
    fun invoke_passesConfigToRepository() = runTest {
        val fake = FakeDedupRepository(flowOf(completedProgress))
        val useCase = ABTransportUseCase(fake)

        useCase(baseConfig).toList()

        val captured = fake.lastAbTransportConfig
        assertNotNull("Repository 应被调用一次，config 应被捕获", captured)
        assertEquals("传递给 Repository 的 config 应与 UseCase 入参一致", baseConfig, captured)
        assertEquals("videoAPath 应正确传递", baseConfig.videoAPath, captured!!.videoAPath)
        assertEquals("videoBPath 应正确传递", baseConfig.videoBPath, captured.videoBPath)
        assertEquals("outputPath 应正确传递", baseConfig.outputPath, captured.outputPath)
        assertEquals("mode 应正确传递", baseConfig.mode, captured.mode)
        assertEquals("durationStrategy 应正确传递", baseConfig.durationStrategy, captured.durationStrategy)
    }

    // ===== 用例 6：MIX 模式（音轨混合）配置正确传递 =====

    @Test
    fun invoke_withMixMode_passesMixConfigToRepository() = runTest {
        val mixConfig = baseConfig.copy(
            mode = ABTransportMode.MIX,
            volumeRatioA = 0.8f,
            volumeRatioB = 0.4f
        )
        val fake = FakeDedupRepository(flowOf(completedProgress))
        val useCase = ABTransportUseCase(fake)

        useCase(mixConfig).toList()

        val captured = fake.lastAbTransportConfig
        assertNotNull("MIX 模式 config 应被捕获", captured)
        assertEquals("mode 应为 MIX", ABTransportMode.MIX, captured!!.mode)
        assertEquals("volumeRatioA 应正确传递", 0.8f, captured.volumeRatioA, 0.0001f)
        assertEquals("volumeRatioB 应正确传递", 0.4f, captured.volumeRatioB, 0.0001f)
    }

    // ===== 用例 7：LOOP 时长策略配置正确传递 =====

    @Test
    fun invoke_withLoopStrategy_passesLoopConfigToRepository() = runTest {
        val loopConfig = baseConfig.copy(durationStrategy = DurationStrategy.LOOP)
        val fake = FakeDedupRepository(flowOf(completedProgress))
        val useCase = ABTransportUseCase(fake)

        useCase(loopConfig).toList()

        val captured = fake.lastAbTransportConfig
        assertNotNull("LOOP 策略 config 应被捕获", captured)
        assertEquals("durationStrategy 应为 LOOP", DurationStrategy.LOOP, captured!!.durationStrategy)
    }

    // ===== 用例 8：CUSTOM 时长策略与 segmentA/segmentB 配置正确传递 =====

    @Test
    fun invoke_withCustomStrategy_passesCustomConfigToRepository() = runTest {
        val segA = TimelineSegment(startMs = 1_000L, endMs = 5_000L)
        val segB = TimelineSegment(startMs = 500L, endMs = 4_500L)
        val customConfig = baseConfig.copy(
            durationStrategy = DurationStrategy.CUSTOM,
            segmentA = segA,
            segmentB = segB
        )
        val fake = FakeDedupRepository(flowOf(completedProgress))
        val useCase = ABTransportUseCase(fake)

        useCase(customConfig).toList()

        val captured = fake.lastAbTransportConfig
        assertNotNull("CUSTOM 策略 config 应被捕获", captured)
        assertEquals("durationStrategy 应为 CUSTOM", DurationStrategy.CUSTOM, captured!!.durationStrategy)
        assertEquals("segmentA 应正确传递", segA, captured.segmentA)
        assertEquals("segmentB 应正确传递", segB, captured.segmentB)
    }

    // ===== 用例 9：取消收集时，UseCase 正确传播取消（不抛 CancellationException 之外异常）=====

    @Test
    fun invoke_whenCollectorCancels_propagatesCancellationWithoutExtraErrors() = runTest {
        // Fake 返回一个永不完成的 Flow：发射一条进行中进度后挂起，直到被取消
        val neverCompletingFlow: Flow<ABTransportProgress> = flow {
            emit(ABTransportProgress(progress = 0.3f, currentMs = 2_400L, totalMs = 8_000L))
            // 模拟 Repository 内部 FFmpeg 仍在执行：挂起直到被取消
            awaitCancellation()
        }
        val fake = FakeDedupRepository(neverCompletingFlow)
        val useCase = ABTransportUseCase(fake)

        coroutineScope {
            val collected = mutableListOf<ABTransportProgress>()
            val job: Job = launch {
                useCase(baseConfig).collect { collected += it }
            }
            // 让 producer 发射一条进度（runTest 中 awaitCancellation 会保持挂起）
            testScheduler.advanceUntilIdle()
            // 取消收集
            job.cancelAndJoin()

            // 应至少收到一条进行中进度，且取消不应产生额外异常
            assertTrue("取消前应至少收到一条进度", collected.isNotEmpty())
            assertEquals("第一条进度应为进行中状态", 0.3f, collected[0].progress, 0.0001f)
            assertFalse("进行中进度不应为完成态", collected[0].isCompleted)
        }
    }

    // ===== 用例 10：Repository 发射「A 视频无音轨」错误时，UseCase 正确转发 error 进度 =====

    @Test
    fun invoke_whenRepositoryEmitsNoAudioTrackError_propagatesErrorProgress() = runTest {
        // 模拟 Repository 检测到 A 视频无音轨，发射 error 进度
        val noAudioError = ABTransportProgress(
            progress = 0f,
            currentMs = 0L,
            totalMs = 0L,
            error = "A 视频无音轨，无法作为音频源"
        )
        val fake = FakeDedupRepository(flowOf(noAudioError))
        val useCase = ABTransportUseCase(fake)

        val emissions = useCase(baseConfig).toList()

        assertEquals("应发射一条 error 进度", 1, emissions.size)
        val last = emissions.last()
        assertTrue("无音轨错误应为失败态", last.isFailed)
        assertNotNull("失败态 error 应非空", last.error)
        assertEquals(
            "error 内容应为「A 视频无音轨，无法作为音频源」",
            "A 视频无音轨，无法作为音频源",
            last.error
        )
        assertFalse("失败态不应为完成态", last.isCompleted)
        assertNull("失败态 outputPath 应为 null", last.outputPath)
    }

    // ===== Fake 实现 =====

    /**
     * [DedupRepository] 的伪实现，用于隔离测试 [ABTransportUseCase]。
     *
     * 仅关心 [abTransport]：捕获最后一次调用的 [ABTransportConfig]，
     * 并发射构造时预设的进度流。其余方法返回未支持的占位值，
     * 因为 [ABTransportUseCase] 不会调用它们。
     */
    private class FakeDedupRepository(
        private val abTransportProgress: Flow<ABTransportProgress>
    ) : DedupRepository {

        /** 最后一次 [abTransport] 调用传入的配置，便于断言。 */
        var lastAbTransportConfig: ABTransportConfig? = null
            private set

        override suspend fun abTransport(config: ABTransportConfig): Flow<ABTransportProgress> {
            lastAbTransportConfig = config
            return abTransportProgress
        }

        // 以下方法在 ABTransportUseCase 测试中不会被调用，提供占位实现

        override suspend fun dedupVideo(
            inputPath: String,
            outputPath: String,
            config: DedupConfig
        ): Flow<DedupProgress> = flow {
            throw UnsupportedOperationException("FakeDedupRepository 不支持 dedupVideo")
        }

        override suspend fun getVideoInfo(path: String): VideoClip =
            throw UnsupportedOperationException("FakeDedupRepository 不支持 getVideoInfo")

        override suspend fun hasAudioTrack(path: String): Boolean =
            throw UnsupportedOperationException("FakeDedupRepository 不支持 hasAudioTrack")

        override suspend fun extractKeyframes(videoPath: String, count: Int, outputDir: String): List<String> =
            throw UnsupportedOperationException("FakeDedupRepository 不支持 extractKeyframes")

        override suspend fun enhanceVideo(
            inputPath: String,
            outputPath: String,
            config: EnhanceConfig
        ): Flow<Float> = flow {
            throw UnsupportedOperationException("FakeDedupRepository 不支持 enhanceVideo")
        }
    }
}

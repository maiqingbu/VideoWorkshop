package com.videoworkshop.data.ai.llm

import com.videoworkshop.data.ai.llm.model.ChatMessage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/**
 * [LlmProviderRouter] 的单元测试。
 *
 * 通过自定义的 [FakeLlmProvider]（非 mockk）控制每个 Provider 的成功/失败行为，
 * 覆盖「首个成功直接返回」「失败降级」「全部失败聚合异常」「空列表校验」
 * 以及「调用顺序」「入参透传」等场景。
 */
class LlmProviderRouterTest {

    /** 测试用 API Key。 */
    private val apiKey = "sk-test-key"

    /** 测试用对话上下文。 */
    private val messages = listOf(
        ChatMessage(role = "system", content = "你是一个助手"),
        ChatMessage(role = "user", content = "你好")
    )

    // ===== 用例 1：首个 Provider 成功 → 立即返回其结果，不调用后续 Provider =====
    @Test
    fun chat_firstProviderSucceeds_returnsItsResultImmediately() = runTest {
        val callLog = mutableListOf<String>()
        val argLog = mutableListOf<Pair<List<ChatMessage>, String>>()
        val first = FakeLlmProvider("DeepSeek", callLog, argLog, result = "deepseek-result")
        val second = FakeLlmProvider("Qwen", callLog, argLog, result = "qwen-result")
        val router = LlmProviderRouter(listOf(first, second))

        val result = router.chat(messages, apiKey)

        assertEquals("应返回首个 Provider 的结果", "deepseek-result", result)
        // 首个成功后应立即返回，第二个 Provider 不应被调用
        assertEquals("仅首个 Provider 应被调用", listOf("DeepSeek"), callLog)
    }

    // ===== 用例 2：首个 Provider 抛异常、第二个成功 → 降级返回第二个结果 =====
    @Test
    fun chat_firstThrowsSecondSucceeds_returnsSecondResult() = runTest {
        val callLog = mutableListOf<String>()
        val argLog = mutableListOf<Pair<List<ChatMessage>, String>>()
        val first = FakeLlmProvider(
            name = "DeepSeek",
            callLog = callLog,
            argLog = argLog,
            error = RuntimeException("deepseek-down")
        )
        val second = FakeLlmProvider("Qwen", callLog, argLog, result = "qwen-result")
        val router = LlmProviderRouter(listOf(first, second))

        val result = router.chat(messages, apiKey)

        assertEquals("失败降级后应返回第二个 Provider 的结果", "qwen-result", result)
        // 两个 Provider 都应被尝试（先失败后成功）
        assertEquals("应依次调用 DeepSeek 与 Qwen", listOf("DeepSeek", "Qwen"), callLog)
    }

    // ===== 用例 3：全部 Provider 失败 → 抛出带聚合信息的 IOException =====
    @Test
    fun chat_allProvidersFail_throwsIOExceptionWithAggregatedMessage() = runTest {
        val callLog = mutableListOf<String>()
        val argLog = mutableListOf<Pair<List<ChatMessage>, String>>()
        val first = FakeLlmProvider(
            name = "DeepSeek",
            callLog = callLog,
            argLog = argLog,
            error = RuntimeException("deepseek-down")
        )
        val second = FakeLlmProvider(
            name = "Qwen",
            callLog = callLog,
            argLog = argLog,
            error = IllegalStateException("qwen-bad")
        )
        val router = LlmProviderRouter(listOf(first, second))

        try {
            router.chat(messages, apiKey)
            fail("全部 Provider 失败时应抛出 IOException")
        } catch (e: IOException) {
            // 聚合错误信息应包含提示文案与最近一次错误的消息
            assertTrue(
                "异常信息应包含聚合提示文案",
                e.message!!.contains("全部 LLM Provider 均调用失败")
            )
            assertTrue(
                "异常信息应包含最近一次错误的消息",
                e.message!!.contains("qwen-bad")
            )
        }
        // 全部 Provider 均被尝试
        assertEquals("应依次调用全部 Provider", listOf("DeepSeek", "Qwen"), callLog)
    }

    // ===== 用例 4：Provider 列表为空 → 抛出 IllegalArgumentException =====
    @Test
    fun chat_emptyProviders_throwsIllegalArgumentException() = runTest {
        val router = LlmProviderRouter(emptyList())

        try {
            router.chat(messages, apiKey)
            fail("空 Provider 列表应抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "异常信息应包含未配置提示",
                e.message!!.contains("未配置任何 LLM Provider")
            )
        }
    }

    // ===== 用例 5：应按列表顺序依次尝试 Provider =====
    @Test
    fun chat_respectsProviderOrder_triesFirstBeforeSecond() = runTest {
        val callLog = mutableListOf<String>()
        val argLog = mutableListOf<Pair<List<ChatMessage>, String>>()
        val first = FakeLlmProvider(
            name = "DeepSeek",
            callLog = callLog,
            argLog = argLog,
            error = RuntimeException("fail-1")
        )
        val second = FakeLlmProvider("Qwen", callLog, argLog, result = "qwen-result")
        val third = FakeLlmProvider("Ernie", callLog, argLog, result = "ernie-result")
        val router = LlmProviderRouter(listOf(first, second, third))

        val result = router.chat(messages, apiKey)

        assertEquals("应由第二个 Provider 提供结果", "qwen-result", result)
        // 应按列表顺序依次尝试：先 DeepSeek 失败，再 Qwen 成功；Ernie 不应被调用
        assertEquals(
            "调用顺序应与列表一致，且成功后不再继续",
            listOf("DeepSeek", "Qwen"),
            callLog
        )
    }

    // ===== 用例 6：apiKey 应透传给 Provider =====
    @Test
    fun chat_passesApiKeyToProvider() = runTest {
        val callLog = mutableListOf<String>()
        val argLog = mutableListOf<Pair<List<ChatMessage>, String>>()
        val provider = FakeLlmProvider("DeepSeek", callLog, argLog, result = "ok")
        val router = LlmProviderRouter(listOf(provider))
        val expectedApiKey = "sk-secret-12345"

        router.chat(messages, expectedApiKey)

        assertEquals("apiKey 应原样透传给 Provider", expectedApiKey, argLog.first().second)
    }

    // ===== 用例 7：messages 应透传给 Provider =====
    @Test
    fun chat_passesMessagesToProvider() = runTest {
        val callLog = mutableListOf<String>()
        val argLog = mutableListOf<Pair<List<ChatMessage>, String>>()
        val provider = FakeLlmProvider("DeepSeek", callLog, argLog, result = "ok")
        val router = LlmProviderRouter(listOf(provider))

        router.chat(messages, apiKey)

        assertEquals("messages 应原样透传给 Provider", messages, argLog.first().first)
    }

    /**
     * 测试用假 [LlmProvider]。
     *
     * - [result]：成功时 [chat] 返回的文本（默认 `"ok-$name"`）。
     * - [error]：非 null 时 [chat] 抛出该异常（优先级高于 [result]）。
     * - [callLog]：共享的调用顺序日志，每次 [chat] 被调用时追加自身 [name]。
     * - [argLog]：共享的入参日志，每次 [chat] 被调用时追加 `(messages, apiKey)`。
     *
     * 通过共享 [callLog] / [argLog] 即可断言调用顺序与入参透传。
     *
     * 注意：构造参数顺序将必填的 [callLog] / [argLog] 置于 [result] / [error] 之前，
     * 以便在调用处以位置参数传入日志、以命名参数指定成功/失败行为。
     */
    private class FakeLlmProvider(
        override val name: String,
        private val callLog: MutableList<String>,
        private val argLog: MutableList<Pair<List<ChatMessage>, String>>,
        private val result: String = "ok-$name",
        private val error: Throwable? = null
    ) : LlmProvider {

        override val baseUrl: String = "https://example.test/v1"

        override suspend fun chat(messages: List<ChatMessage>, apiKey: String): String {
            callLog.add(name)
            argLog.add(messages to apiKey)
            error?.let { throw it }
            return result
        }
    }
}

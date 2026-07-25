package com.videoworkshop.data.alliance.download

import com.videoworkshop.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/**
 * 测试用 [DispatcherProvider]：所有调度器统一返回同一个 [UnconfinedTestDispatcher]，
 * 让 `withContext(io)` 在测试线程内同步执行，避免引入真实 IO/Default 调度器造成时序不确定性。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private class TestDispatcherProvider(
    private val dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
}

/**
 * [VideoDownloader] 单元测试。
 *
 * - [MockWebServer] 模拟 HTTP 响应（200 / 206 / 403 等）；
 * - [UnconfinedTestDispatcher] 让 `withContext(io)` 在测试线程内同步执行；
 * - [TemporaryFolder] 管理下载目标路径与 `.part` 临时文件。
 *
 * 覆盖：正常下载、Referer 头、断点续传、HTTP 错误、临时文件清理、User-Agent 头。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VideoDownloaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var downloader: VideoDownloader

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        downloader = VideoDownloader(
            client = OkHttpClient.Builder().build(),
            dispatchers = TestDispatcherProvider(UnconfinedTestDispatcher()),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /** 拼接位于临时目录下的目标路径。 */
    private fun destPath(name: String = "video.mp4"): String =
        tempFolder.root.absolutePath + File.separator + name

    @Test
    fun `成功下载_http200_写入正确内容并最终回调1f`() = runTest {
        val content = "hello-videoworkshop"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(content),
        )

        val target = destPath()
        val progress = mutableListOf<Float>()
        val result = downloader.download(
            url = server.url("/video.mp4").toString(),
            destPath = target,
            onProgress = { progress += it },
        )

        assertTrue(result.isSuccess)
        assertEquals(target, result.getOrNull())
        val saved = File(target)
        assertTrue(saved.exists())
        assertEquals(content, saved.readText())
        // 小文件不足 256KB 节流阈值，下载过程中不回调，仅在结束时回调 1f
        assertEquals(listOf(1f), progress)
    }

    @Test
    fun `下载时携带Referer请求头`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("ok"),
        )

        downloader.download(
            url = server.url("/video.mp4").toString(),
            destPath = destPath(),
            referer = "https://www.jd.com",
        )

        val request = server.takeRequest()
        assertEquals("https://www.jd.com", request.getHeader("Referer"))
    }

    @Test
    fun `断点续传_http206_追加写入并校验完整`() = runTest {
        val target = destPath()
        val partFile = File("$target.part")
        val partial = "HELLO"
        partFile.writeBytes(partial.toByteArray()) // 已下载 5 字节
        val remaining = "WORLD"
        val total = partial.length + remaining.length // 10

        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setHeader("Content-Range", "bytes ${partial.length}-${total - 1}/$total")
                .setBody(remaining),
        )

        val result = downloader.download(
            url = server.url("/video.mp4").toString(),
            destPath = target,
        )

        assertTrue(result.isSuccess)
        val saved = File(target)
        assertTrue(saved.exists())
        // 旧分片 + 续传分片
        assertEquals(partial + remaining, saved.readText())
        // 续传成功后 .part 临时文件应被重命名为目标文件
        assertFalse(File("$target.part").exists())
        // 应携带 Range 头请求剩余字节
        val request = server.takeRequest()
        assertEquals("bytes=${partial.length}-", request.getHeader("Range"))
    }

    @Test
    fun `http403_返回失败结果`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))

        val target = destPath()
        val result = downloader.download(
            url = server.url("/video.mp4").toString(),
            destPath = target,
        )

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue("异常应为 IOException", ex is IOException)
        assertEquals("下载失败：HTTP 403", ex?.message)
        // 失败时不应生成最终文件
        assertFalse(File(target).exists())
    }

    @Test
    fun `成功下载后临时part文件被清理`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("cleanup-test"),
        )

        val target = destPath()
        downloader.download(
            url = server.url("/video.mp4").toString(),
            destPath = target,
        )

        assertTrue(File(target).exists())
        // .part 临时文件应被重命名为目标文件
        assertFalse(File("$target.part").exists())
    }

    @Test
    fun `请求携带UserAgent请求头`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("ua"),
        )

        downloader.download(
            url = server.url("/video.mp4").toString(),
            destPath = destPath(),
        )

        val request = server.takeRequest()
        assertEquals(
            "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 VideoWorkshop/1.0",
            request.getHeader("User-Agent"),
        )
    }
}

package com.videoworkshop.feature.goods

import androidx.lifecycle.SavedStateHandle
import com.videoworkshop.domain.usecase.SearchGoodsUseCase
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * [GoodsViewModel] 的单元测试。
 *
 * 重点覆盖 [GoodsViewModel.parseManualLink]：从用户粘贴的淘宝/京东/拼多多商品链接中
 * 正则提取商品 ID。测试用例覆盖：
 *
 * 1. 淘宝合法链接：`?id=数字` 提取
 * 2. 京东合法链接（path 形式 `item.jd.com/数字.html`）提取
 * 3. 京东合法链接（query 形式 `goods_id=数字`）提取
 * 4. 拼多多合法链接：`goods_id=数字` 提取
 * 5. 非法链接：返回 null
 * 6. 空字符串：返回 null
 * 7. 空白字符串：返回 null
 *
 * 测试策略：
 * - 使用 [UnconfinedTestDispatcher] 替换 `Dispatchers.Main`，使 `viewModelScope`
 *   中的协程在虚拟时间内受控执行，避免 init 块的 debounce 搜索阻塞测试。
 * - [SearchGoodsUseCase] 用 MockK relaxed mock 注入，parseManualLink 测试不依赖其行为。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GoodsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var searchGoodsUseCase: SearchGoodsUseCase
    private lateinit var viewModel: GoodsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        searchGoodsUseCase = mockk(relaxed = true)
        val savedStateHandle = SavedStateHandle(mapOf("mode" to "video"))
        viewModel = GoodsViewModel(searchGoodsUseCase, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== 用例 1：淘宝合法链接 =====

    @Test
    fun parseManualLink_taobaoUrl_extractsGoodsId() {
        val link = "https://item.taobao.com/item.htm?id=123456789"
        val result = viewModel.parseManualLink(link)
        assertEquals("淘宝 ?id= 参数应被正确提取", "123456789", result)
    }

    // ===== 用例 2：京东合法链接（path 形式）=====
    // 注意：实现中 item.jd.com/(\d+)\.html 正则优先于 goods_id=(\d+) 正则，
    // 因此同时含 path ID 和 goods_id 的 URL 会优先返回 path ID。

    @Test
    fun parseManualLink_jdUrlWithPath_extractsPathId() {
        val link = "https://item.jd.com/100012345.html?goods_id=987654"
        val result = viewModel.parseManualLink(link)
        // 实现中 item.jd.com/数字.html 正则先于 goods_id 正则匹配，故返回 path 中的 ID
        assertEquals("京东 path 形式 ID 应被提取", "100012345", result)
    }

    // ===== 用例 3：京东合法链接（query 形式 goods_id）=====

    @Test
    fun parseManualLink_jdUrlWithGoodsId_extractsGoodsId() {
        val link = "https://m.jd.com/product?goods_id=987654"
        val result = viewModel.parseManualLink(link)
        assertEquals("京东 goods_id 参数应被正确提取", "987654", result)
    }

    // ===== 用例 4：拼多多合法链接 =====

    @Test
    fun parseManualLink_pddUrl_extractsGoodsId() {
        val link = "https://mobile.yangkeduo.com/goods.html?goods_id=111111"
        val result = viewModel.parseManualLink(link)
        assertEquals("拼多多 goods_id 参数应被正确提取", "111111", result)
    }

    // ===== 用例 5：非法链接 =====

    @Test
    fun parseManualLink_invalidUrl_returnsNull() {
        val link = "https://example.com/some-random-page"
        val result = viewModel.parseManualLink(link)
        assertNull("非法链接应返回 null", result)
    }

    // ===== 用例 6：空字符串 =====

    @Test
    fun parseManualLink_emptyString_returnsNull() {
        val result = viewModel.parseManualLink("")
        assertNull("空字符串应返回 null", result)
    }

    // ===== 用例 7：空白字符串 =====

    @Test
    fun parseManualLink_blankString_returnsNull() {
        val result = viewModel.parseManualLink("   ")
        assertNull("空白字符串应返回 null", result)
    }
}

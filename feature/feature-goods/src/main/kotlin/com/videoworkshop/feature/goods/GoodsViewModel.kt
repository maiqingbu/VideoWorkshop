package com.videoworkshop.feature.goods

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoworkshop.domain.model.AllianceProvider
import com.videoworkshop.domain.model.ContentType
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.usecase.SearchGoodsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 商品搜索页 ViewModel。
 *
 * 从导航参数 `mode`（video / image）读取创作模式，决定选中商品后的跳转去向。
 * 关键词与平台变化经 300ms 去抖后触发 [SearchGoodsUseCase]；
 * 关键词为空时返回对应平台的热门推荐列表。
 *
 * 当通过 `goods-library` 路由进入时（mode 参数为空），按 VIDEO 模式处理，
 * 并将 [isLibrary] 置为 true，UI 侧据此切换标题。
 */
@HiltViewModel
class GoodsViewModel @Inject constructor(
    private val searchGoodsUseCase: SearchGoodsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 创作模式，由导航参数决定。`goods-library` 入口无 mode 参数，默认按 VIDEO 处理。 */
    private val mode: ContentType = when (savedStateHandle.get<String>("mode")) {
        "image" -> ContentType.IMAGE
        else -> ContentType.VIDEO
    }

    /** 是否为「商品库」独立入口（goods-library 路由无 mode 参数）。 */
    private val isLibrary: Boolean = savedStateHandle.get<String>("mode") == null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * 当前选中的联盟平台。
     *
     * 默认值按 mode 差异化：VIDEO 默认淘宝、IMAGE 默认拼多多，命中各自主流带货场景。
     */
    private val _selectedProvider = MutableStateFlow(
        when (mode) {
            ContentType.VIDEO -> AllianceProvider.TAOBAO
            ContentType.IMAGE -> AllianceProvider.PDD
        }
    )
    val selectedProvider: StateFlow<AllianceProvider> = _selectedProvider.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Goods>>(emptyList())
    val searchResults: StateFlow<List<Goods>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedGoods = MutableStateFlow<Goods?>(null)
    val selectedGoods: StateFlow<Goods?> = _selectedGoods.asStateFlow()

    /** 一次性错误提示（如链接解析失败），UI 显示后调用 [consumeError] 清空。 */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 聚合后的 UI 状态，供 [GoodsScreen] 单一订阅。 */
    val uiState: StateFlow<GoodsUiState> =
        combine(
            _searchQuery,
            _selectedProvider,
            _searchResults,
            _isSearching,
            _selectedGoods
        ) { query, provider, results, searching, selected ->
            GoodsUiState(
                query = query,
                provider = provider,
                results = results,
                isSearching = searching,
                selectedGoods = selected,
                mode = mode,
                isLibrary = isLibrary
            )
        }.combine(_error) { state, error ->
            state.copy(error = error)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GoodsUiState(mode = mode, isLibrary = isLibrary)
        )

    init {
        // 关键词 / 平台变化时去抖搜索
        viewModelScope.launch {
            combine(_searchQuery, _selectedProvider) { query, provider -> query to provider }
                .debounce(300)
                .distinctUntilChanged()
                .collect { (query, provider) -> executeSearch(query, provider) }
        }
    }

    /** 更新搜索关键词，输入即触发去抖搜索。 */
    fun search(query: String) {
        _searchQuery.value = query
    }

    /** 切换联盟平台，自动重新搜索。 */
    fun selectProvider(provider: AllianceProvider) {
        _selectedProvider.value = provider
    }

    /** 选中商品，路由侧观察 [uiState] 中 selectedGoods 后触发跳转。 */
    fun selectGoods(goods: Goods) {
        _selectedGoods.value = goods
    }

    /** 路由侧完成导航后清空选中商品，避免重复跳转。 */
    fun consumeSelectedGoods() {
        _selectedGoods.value = null
    }

    /** UI 显示错误 Snackbar 后调用，清空错误态。 */
    fun consumeError() {
        _error.value = null
    }

    /** 链接解析失败时由路由侧调用，触发 Snackbar 提示。 */
    fun onManualLinkFailed() {
        _error.value = "链接格式无法识别"
    }

    /**
     * 从用户粘贴的商品链接中正则提取商品 ID。
     *
     * 支持：
     * - 淘宝：`item.taobao.com/item.htm?id=xxx` 或任意 `?id=xxx` / `&id=xxx`
     * - 京东：`item.jd.com/xxx.html` 或任意 `?goods_id=xxx` / `&goods_id=xxx`
     * - 拼多多：`mobile.yangkeduo.com/goods.html?goods_id=xxx`（与京东共用 goods_id 规则）
     *
     * @return 解析出的商品 ID；无法识别时返回 null。
     */
    fun parseManualLink(link: String): String? {
        if (link.isBlank()) return null
        // 淘宝：id=数字
        Regex("""[?&]id=(\d+)""").find(link)?.let { return it.groupValues[1] }
        // 京东：item.jd.com/数字.html
        Regex("""item\.jd\.com/(\d+)\.html""").find(link)?.let { return it.groupValues[1] }
        // 京东 / 拼多多：goods_id=数字
        Regex("""[?&]goods_id=(\d+)""").find(link)?.let { return it.groupValues[1] }
        return null
    }

    private fun executeSearch(query: String, provider: AllianceProvider) {
        _isSearching.value = true
        viewModelScope.launch {
            try {
                _searchResults.value = searchGoodsUseCase(query, provider)
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
}

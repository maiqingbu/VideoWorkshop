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
 */
@HiltViewModel
class GoodsViewModel @Inject constructor(
    private val searchGoodsUseCase: SearchGoodsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 创作模式，由导航参数决定。 */
    private val mode: ContentType = when (savedStateHandle.get<String>("mode")) {
        "image" -> ContentType.IMAGE
        else -> ContentType.VIDEO
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedProvider = MutableStateFlow(AllianceProvider.TAOBAO)
    val selectedProvider: StateFlow<AllianceProvider> = _selectedProvider.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Goods>>(emptyList())
    val searchResults: StateFlow<List<Goods>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedGoods = MutableStateFlow<Goods?>(null)
    val selectedGoods: StateFlow<Goods?> = _selectedGoods.asStateFlow()

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
                mode = mode
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GoodsUiState(mode = mode)
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

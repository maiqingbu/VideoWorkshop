package com.videoworkshop.feature.goods

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.videoworkshop.core.designsystem.theme.BrandRed
import com.videoworkshop.core.designsystem.theme.BrandRedDark
import com.videoworkshop.core.designsystem.theme.NeutralGray
import com.videoworkshop.core.designsystem.theme.NeutralGrayDark
import com.videoworkshop.core.designsystem.theme.NeutralGrayLight
import com.videoworkshop.core.ui.components.Badge
import com.videoworkshop.core.ui.components.BadgeType
import com.videoworkshop.core.ui.components.GradientButton
import com.videoworkshop.core.ui.components.VWTopBar
import com.videoworkshop.domain.model.AllianceProvider
import com.videoworkshop.domain.model.Goods

/**
 * 商品搜索页 —— 选择带货商品。
 *
 * 顶部 [VWTopBar] + 圆角搜索框 + 平台切换 Tab，下方为商品列表（搜索前展示热门推荐），
 * 底部为可折叠的「手动输入商品链接」区域。
 */
@Composable
fun GoodsScreen(
    uiState: GoodsUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onProviderSelected: (AllianceProvider) -> Unit,
    onGoodsSelected: (Goods) -> Unit,
    onManualLinkSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { VWTopBar(title = "选择带货商品", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 搜索框 + 平台 Tab（固定在顶部）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                SearchBox(query = uiState.query, onQueryChange = onQueryChange)
                Spacer(Modifier.height(12.dp))
                ProviderTabBar(
                    selected = uiState.provider,
                    onSelect = onProviderSelected
                )
            }

            // 结果列表（可滚动）
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                item {
                    SectionTitle(
                        if (uiState.query.isBlank()) "热门推荐" else "搜索结果"
                    )
                }

                when {
                    uiState.isSearching && uiState.results.isEmpty() -> {
                        item { LoadingRow() }
                    }
                    uiState.results.isEmpty() -> {
                        item { EmptyResults() }
                    }
                    else -> {
                        items(uiState.results, key = { it.id }) { goods ->
                            GoodsCard(goods = goods, onSelect = { onGoodsSelected(goods) })
                        }
                    }
                }

                item { ManualLinkSection(onSubmit = onManualLinkSubmit) }

                item {
                    Spacer(Modifier.height(20.dp))
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

/**
 * 圆角胶囊搜索框。
 */
@Composable
private fun SearchBox(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "输入关键词或粘贴商品链接",
                color = NeutralGray
            )
        },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = NeutralGray)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "清除")
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = BrandRed,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = BrandRed,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * 平台切换 Tab（淘宝 / 京东 / 拼多多），选中项带红色下划线。
 */
@Composable
private fun ProviderTabBar(
    selected: AllianceProvider,
    onSelect: (AllianceProvider) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        AllianceProvider.entries.forEach { provider ->
            ProviderTab(
                modifier = Modifier.weight(1f),
                label = providerName(provider),
                selected = provider == selected,
                onClick = { onSelect(provider) }
            )
        }
    }
}

@Composable
private fun ProviderTab(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (selected) BrandRed else NeutralGrayDark,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) BrandRed else Color.Transparent)
        )
    }
}

/**
 * 单个商品卡片。
 */
@Composable
private fun GoodsCard(goods: Goods, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 商品图片占位（彩色渐变 + 真实图加载失败回退到渐变）
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(providerGradient(goods.provider))
            ) {
                AsyncImage(
                    model = goods.imageUrl,
                    contentDescription = goods.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goods.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                PlatformChip(provider = goods.provider)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "¥${formatPrice(goods.price)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandRed
                    )
                    val originalPrice = goods.originalPrice
                    if (originalPrice != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "¥${formatPrice(originalPrice)}",
                            fontSize = 12.sp,
                            color = NeutralGray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Badge(
                    text = "佣金 ${(goods.commissionRate * 100).toInt()}%",
                    type = BadgeType.GREEN
                )
            }

            Spacer(Modifier.width(8.dp))

            SelectButton(onClick = onSelect)
        }
    }
}

/**
 * 平台标签（轻量彩色 Chip）。
 */
@Composable
private fun PlatformChip(provider: AllianceProvider) {
    val color = providerColor(provider)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = providerName(provider),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SelectButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = BrandRed,
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * 底部可折叠的「手动输入商品链接」区域。
 */
@Composable
private fun ManualLinkSection(onSubmit: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = BrandRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "手动输入商品链接",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = NeutralGray,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("粘贴淘宝 / 京东 / 拼多多商品链接", color = NeutralGray)
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = BrandRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            cursorColor = BrandRed
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    GradientButton(
                        text = "解析并选择",
                        onClick = { onSubmit(link) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = link.isNotBlank()
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = BrandRed, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
    }
}

@Composable
private fun EmptyResults() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = NeutralGrayLight,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(text = "没有找到相关商品", color = NeutralGray, fontSize = 14.sp)
        }
    }
}

// ===== 工具函数 =====

private fun providerName(provider: AllianceProvider): String = when (provider) {
    AllianceProvider.TAOBAO -> "淘宝"
    AllianceProvider.JD -> "京东"
    AllianceProvider.PDD -> "拼多多"
}

private fun providerColor(provider: AllianceProvider): Color = when (provider) {
    AllianceProvider.TAOBAO -> Color(0xFFFF7B00)
    AllianceProvider.JD -> BrandRed
    AllianceProvider.PDD -> Color(0xFFD63384)
}

private fun providerGradient(provider: AllianceProvider): Brush = when (provider) {
    AllianceProvider.TAOBAO -> Brush.linearGradient(listOf(Color(0xFFFF8C1A), Color(0xFFFF5500)))
    AllianceProvider.JD -> Brush.linearGradient(listOf(BrandRed, BrandRedDark))
    AllianceProvider.PDD -> Brush.linearGradient(listOf(Color(0xFFFF4D6D), Color(0xFFD63384)))
}

private fun formatPrice(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

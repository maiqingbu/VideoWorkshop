package com.videoworkshop.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoworkshop.core.designsystem.theme.CardShape

/**
 * 统一卡片样式：14dp 圆角 + 阴影，自动适配主题 surface 色。
 *
 * 传入 [onClick] 时表现为可点击卡片，否则为静态展示卡片。
 */
@Composable
fun VWCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val baseModifier = modifier.fillMaxWidth()
    if (onClick != null) {
        Surface(
            modifier = baseModifier,
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            tonalElevation = 0.dp,
            onClick = onClick
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Surface(
            modifier = baseModifier,
            shape = CardShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

package com.videoworkshop.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 素简工坊页面骨架。
 *
 * 提供统一的纸色背景（Light 态）与内容布局，禁止 Feature 页面直接声明背景色。
 *
 * @param contentPadding 内容内边距，默认 16dp。
 */
@Composable
fun VWPageScaffold(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = com.videoworkshop.core.designsystem.theme.VWSpacing.lg,
        vertical = com.videoworkshop.core.designsystem.theme.VWSpacing.lg
    ),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            content()
        }
    }
}
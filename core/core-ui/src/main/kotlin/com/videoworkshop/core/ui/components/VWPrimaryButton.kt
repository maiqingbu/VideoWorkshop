package com.videoworkshop.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.videoworkshop.core.designsystem.theme.PineOnPrimary
import com.videoworkshop.core.designsystem.theme.VWRadius
import com.videoworkshop.core.designsystem.theme.VWSpacing
import com.videoworkshop.core.designsystem.theme.VWTypeScale

/**
 * 素简工坊主操作按钮：纯色松墨绿实心圆角按钮，不使用渐变。
 *
 * 按钮文字使用明确动词：新建、导入、生成、保存、分享、重试。
 */
@Composable
fun VWPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val active = enabled && !loading
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(VWSpacing.touchTarget)
            .background(
                color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                shape = VWRadius.field
            )
            .clickable(enabled = active) { onClick() }
            .padding(horizontal = VWSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = PineOnPrimary
                )
            } else {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = PineOnPrimary
                    )
                    Spacer(Modifier.width(VWSpacing.sm))
                }
                Text(
                    text = text,
                    style = VWTypeScale.button,
                    color = PineOnPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
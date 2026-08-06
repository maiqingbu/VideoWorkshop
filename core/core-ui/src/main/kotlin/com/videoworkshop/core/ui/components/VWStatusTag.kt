package com.videoworkshop.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.videoworkshop.core.designsystem.theme.InkSecondary
import com.videoworkshop.core.designsystem.theme.PineContainer
import com.videoworkshop.core.designsystem.theme.PinePrimary
import com.videoworkshop.core.designsystem.theme.VWRadius

/**
 * 状态标签变体。
 *
 * 状态必须包含文字，不能仅用颜色区分。
 */
enum class VWStatusType(val containerColor: Color, val contentColor: Color) {
    /** 进行中 / 选中（松墨绿） */
    PRIMARY(containerColor = PineContainer, contentColor = PinePrimary),

    /** 中性 / 草稿 */
    NEUTRAL(containerColor = Color(0xFFEFEFEB), contentColor = InkSecondary),

    /** 成功 */
    SUCCESS(containerColor = Color(0xFFE2ECE6), contentColor = Color(0xFF43775A)),

    /** 警告 */
    WARNING(containerColor = Color(0xFFF3EADB), contentColor = Color(0xFF94692F)),

    /** 错误 / 删除 */
    ERROR(containerColor = Color(0xFFF5E3E1), contentColor = Color(0xFFB34B40))
}

/**
 * 素简工坊状态标签：浅色底色 + 文字，不使用纯色块。
 */
@Composable
fun VWStatusTag(
    text: String,
    modifier: Modifier = Modifier,
    type: VWStatusType = VWStatusType.NEUTRAL,
) {
    Text(
        text = text,
        modifier = modifier
            .background(color = type.containerColor, shape = VWRadius.badge)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = type.contentColor,
        style = com.videoworkshop.core.designsystem.theme.VWTypeScale.label
    )
}
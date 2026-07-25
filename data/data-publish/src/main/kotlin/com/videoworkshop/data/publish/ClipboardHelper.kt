package com.videoworkshop.data.publish

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 剪贴板工具。
 *
 * 封装 [ClipboardManager]，用于在发布前将商品推广链接复制到系统剪贴板，
 * 便于用户在目标平台中粘贴。
 *
 * @param context 应用上下文，通过 Hilt @ApplicationContext 注入
 */
class ClipboardHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 将文本复制到系统剪贴板。
     *
     * @param text 待复制文本（通常为商品推广链接）
     * @return `true` 表示复制成功
     */
    fun copyToClipboard(text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(CLIP_LABEL, text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            false
        }
    }

    private companion object {
        const val CLIP_LABEL = "video_workshop_goods_link"
    }
}

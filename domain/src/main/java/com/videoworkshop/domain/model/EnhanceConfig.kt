package com.videoworkshop.domain.model

/**
 * 视频增强配置。
 *
 * @param copy          叠加的文案，为空则不替换文案
 * @param voice         语音合成音色，为空则保留原声
 * @param subtitle      是否生成并叠加字幕
 * @param subtitleStyle 字幕样式描述，为空则使用默认样式
 * @param bgm           背景音乐资源标识，为空则不加 BGM
 * @param stickers      贴纸资源标识列表
 */
data class EnhanceConfig(
    val copy: String? = null,
    val voice: VoiceProfile? = null,
    val subtitle: Boolean = false,
    val subtitleStyle: String? = null,
    val bgm: String? = null,
    val stickers: List<String> = emptyList()
)

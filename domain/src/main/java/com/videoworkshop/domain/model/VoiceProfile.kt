package com.videoworkshop.domain.model

/**
 * 语音合成音色配置。
 *
 * @param displayName  展示名称
 * @param azureVoiceId 对应的 Azure TTS 音色 ID
 */
enum class VoiceProfile(
    val displayName: String,
    val azureVoiceId: String
) {
    EMOTIONAL_FEMALE(
        displayName = "情感女声",
        azureVoiceId = "zh-CN-XiaoyiNeural"
    ),
    ENERGETIC_MALE(
        displayName = "活力男声",
        azureVoiceId = "zh-CN-YunyangNeural"
    ),
    DIALECT(
        displayName = "方言播报",
        azureVoiceId = "zh-CN-liaoning-XiaobeiNeural"
    )
}

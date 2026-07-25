package com.videoworkshop.data.ai.tts

import com.videoworkshop.domain.model.VoiceProfile

/**
 * 音色配置 -> Azure TTS voiceId 映射。
 *
 * 统一在 TTS 层集中维护 Azure 音色 ID，与 domain 层 [VoiceProfile] 解耦，
 * 便于后续替换或扩展音色资源而不影响领域模型。
 */
object VoiceProfileMapper {

    fun toAzureVoiceId(voice: VoiceProfile): String = when (voice) {
        VoiceProfile.EMOTIONAL_FEMALE -> "zh-CN-XiaoxiaoNeural"
        VoiceProfile.ENERGETIC_MALE -> "zh-CN-YunyangNeural"
        VoiceProfile.DIALECT -> "zh-CN-henan"
    }
}

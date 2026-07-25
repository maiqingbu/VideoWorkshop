package com.videoworkshop.domain.usecase

import com.videoworkshop.domain.model.VoiceProfile
import com.videoworkshop.domain.repository.AiRepository
import javax.inject.Inject

/**
 * 语音合成，返回生成音频的本地路径。
 */
class SynthesizeVoiceUseCase @Inject constructor(
    private val aiRepo: AiRepository
) {
    suspend operator fun invoke(text: String, voice: VoiceProfile): String {
        return aiRepo.synthesizeVoice(text, voice)
    }
}

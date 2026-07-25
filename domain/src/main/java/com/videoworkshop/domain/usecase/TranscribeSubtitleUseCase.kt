package com.videoworkshop.domain.usecase

import com.videoworkshop.domain.repository.AiRepository
import javax.inject.Inject

/**
 * 音频字幕转写，返回转写文本。
 */
class TranscribeSubtitleUseCase @Inject constructor(
    private val aiRepo: AiRepository
) {
    suspend operator fun invoke(audioPath: String): String {
        return aiRepo.transcribeSubtitle(audioPath)
    }
}

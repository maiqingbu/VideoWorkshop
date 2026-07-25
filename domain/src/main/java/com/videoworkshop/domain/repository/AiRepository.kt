package com.videoworkshop.domain.repository

import com.videoworkshop.domain.model.CopyResult
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.ImageTemplate
import com.videoworkshop.domain.model.VoiceProfile

/**
 * AI 能力仓库，统一封装文案生成、语音合成、字幕转写等能力。
 */
interface AiRepository {

    /**
     * 生成视频带货文案，返回多个候选结果。
     */
    suspend fun generateVideoCopy(goodsName: String, price: Double, keywords: String?): List<CopyResult>

    /**
     * 基于商品与图文模板生成图文文案。
     */
    suspend fun generateImageCopy(goods: Goods, template: ImageTemplate): CopyResult

    /**
     * 根据正文内容生成标题与话题标签，返回 [Pair]（标题, 标签列表）。
     */
    suspend fun generateTitleAndTags(content: String, goodsName: String): Pair<String, List<String>>

    /**
     * 语音合成，返回生成音频的本地路径。
     */
    suspend fun synthesizeVoice(text: String, voice: VoiceProfile): String

    /**
     * 字幕转写，返回转写文本。
     */
    suspend fun transcribeSubtitle(audioPath: String): String
}

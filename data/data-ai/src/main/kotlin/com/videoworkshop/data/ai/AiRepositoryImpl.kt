package com.videoworkshop.data.ai

import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.core.datastore.PreferenceRepository
import com.videoworkshop.data.ai.asr.GroqWhisperClient
import com.videoworkshop.data.ai.asr.SrtBuilder
import com.videoworkshop.data.ai.llm.LlmProviderRouter
import com.videoworkshop.data.ai.llm.model.ChatMessage
import com.videoworkshop.data.ai.llm.prompt.PromptTemplates
import com.videoworkshop.data.ai.tts.AzureTtsClient
import com.videoworkshop.domain.model.CopyResult
import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.ImageTemplate
import com.videoworkshop.domain.model.VoiceProfile
import com.videoworkshop.domain.repository.AiRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

/**
 * [AiRepository] 实现：统一编排文案生成（LLM）、语音合成（Azure TTS）、字幕转写（Groq Whisper）。
 *
 * 所有 API Key 均从 [PreferenceRepository]（core-datastore）读取，不从 BuildConfig 注入。
 */
class AiRepositoryImpl @Inject constructor(
    private val router: LlmProviderRouter,
    private val azureTtsClient: AzureTtsClient,
    private val groqWhisperClient: GroqWhisperClient,
    private val preferenceRepository: PreferenceRepository,
    private val json: Json,
    private val dispatchers: DispatcherProvider
) : AiRepository {

    override suspend fun generateVideoCopy(
        goodsName: String,
        price: Double,
        keywords: String?
    ): List<CopyResult> = withContext(dispatchers.io) {
        val apiKey = preferenceRepository.deepseekKey.first()
        require(apiKey.isNotBlank()) { "未配置 DeepSeek API Key" }

        val prompt = PromptTemplates.videoCopyPrompt(goodsName, price, keywords)
        val raw = router.chat(chatMessages(prompt), apiKey)
        val copies = parseCopyList(raw)
        if (copies.isEmpty()) throw IOException("AI 未返回有效文案")
        copies
    }

    override suspend fun generateImageCopy(goods: Goods, template: ImageTemplate): CopyResult =
        withContext(dispatchers.io) {
            val apiKey = preferenceRepository.deepseekKey.first()
            require(apiKey.isNotBlank()) { "未配置 DeepSeek API Key" }

            val prompt = PromptTemplates.imageCopyPrompt(goods, template)
            val raw = router.chat(chatMessages(prompt), apiKey)
            parseSingleCopy(raw)
        }

    override suspend fun generateTitleAndTags(
        content: String,
        goodsName: String
    ): Pair<String, List<String>> = withContext(dispatchers.io) {
        val apiKey = preferenceRepository.deepseekKey.first()
        require(apiKey.isNotBlank()) { "未配置 DeepSeek API Key" }

        val prompt = PromptTemplates.titleTagPrompt(content, goodsName)
        val raw = router.chat(chatMessages(prompt), apiKey)
        parseTitleTag(raw)
    }

    override suspend fun synthesizeVoice(text: String, voice: VoiceProfile): String =
        withContext(dispatchers.io) {
            val apiKey = preferenceRepository.azureKey.first()
            require(apiKey.isNotBlank()) { "未配置 Azure TTS API Key" }

            azureTtsClient.synthesize(text, voice, apiKey, AzureTtsClient.DEFAULT_REGION)
                .getOrThrow()
        }

    override suspend fun transcribeSubtitle(audioPath: String): String =
        withContext(dispatchers.io) {
            val apiKey = preferenceRepository.groqKey.first()
            require(apiKey.isNotBlank()) { "未配置 Groq API Key" }

            val srt = groqWhisperClient.transcribe(audioPath, apiKey).getOrThrow()
            // 解析后重新格式化，保证 SRT 序号连续、时间戳格式统一
            val segments = SrtBuilder.parseSrt(srt)
            SrtBuilder.buildSrt(segments)
        }

    // ===== 内部工具 =====

    private fun chatMessages(prompt: String): List<ChatMessage> = listOf(
        ChatMessage(
            role = "system",
            content = "你是专业的短视频带货与图文种草文案创作助手。" +
                "必须严格按用户要求的 JSON 格式返回，禁止输出解释文字、Markdown 代码块标记或任何多余内容。"
        ),
        ChatMessage(role = "user", content = prompt)
    )

    /**
     * 从模型返回中提取纯 JSON 文本：剥离 ```json 代码块围栏，
     * 并截取首个 `{` 到末个 `}` 之间的内容，提升解析鲁棒性。
     */
    private fun extractJson(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```").trim()
            val fenceEnd = s.lastIndexOf("```")
            if (fenceEnd >= 0) s = s.substring(0, fenceEnd).trim()
        }
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start in 0 until end) {
            s = s.substring(start, end + 1)
        }
        return s
    }

    private fun parseCopyList(raw: String): List<CopyResult> = runCatching {
        json.decodeFromString<CopyListDto>(extractJson(raw)).copies.map { it.toDomain() }
    }.getOrElse { throw IOException("解析视频文案失败: ${it.message}", it) }

    private fun parseSingleCopy(raw: String): CopyResult = runCatching {
        json.decodeFromString<CopyDto>(extractJson(raw)).toDomain()
    }.getOrElse { throw IOException("解析图文文案失败: ${it.message}", it) }

    private fun parseTitleTag(raw: String): Pair<String, List<String>> = runCatching {
        val dto = json.decodeFromString<TitleTagDto>(extractJson(raw))
        dto.title to dto.tags
    }.getOrElse { throw IOException("解析标题标签失败: ${it.message}", it) }

    @Serializable
    private data class CopyListDto(
        val copies: List<CopyDto> = emptyList()
    )

    @Serializable
    private data class CopyDto(
        val title: String = "",
        val body: String = "",
        val sellingPoints: List<String> = emptyList(),
        val tags: List<String> = emptyList()
    ) {
        fun toDomain() = CopyResult(title, body, sellingPoints, tags)
    }

    @Serializable
    private data class TitleTagDto(
        val title: String = "",
        val tags: List<String> = emptyList()
    )
}

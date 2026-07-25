package com.videoworkshop.data.ai.tts

import android.content.Context
import com.videoworkshop.core.common.AppConstants
import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.domain.model.VoiceProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Azure Cognitive Services TTS 客户端（核心）。
 *
 * 走 REST 接口 `POST https://{region}.tts.cognitiveservices.azure.com/cognitiveservices/v1`，
 * 请求体为 SSML，响应为 mp3 二进制，落盘后返回本地文件路径。
 */
class AzureTtsClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val dispatchers: DispatcherProvider
) {

    /**
     * 合成语音。
     *
     * @param text    待合成文本
     * @param voice   音色配置
     * @param apiKey  Azure 密钥
     * @param region  Azure 区域，如 eastus
     * @return [Result] 成功携带生成的 mp3 文件绝对路径
     */
    suspend fun synthesize(
        text: String,
        voice: VoiceProfile,
        apiKey: String,
        region: String
    ): Result<String> = withContext(dispatchers.io) {
        runCatching {
            require(text.isNotBlank()) { "合成文本不能为空" }
            require(apiKey.isNotBlank()) { "Azure API Key 不能为空" }
            require(region.isNotBlank()) { "Azure Region 不能为空" }

            val voiceId = VoiceProfileMapper.toAzureVoiceId(voice)
            val ssml = buildSsml(voiceId, text)
            val body = ssml.toRequestBody(SSML_MEDIA_TYPE)

            val request = Request.Builder()
                .url("https://$region.tts.cognitiveservices.azure.com/cognitiveservices/v1")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/ssml+xml")
                .addHeader("X-Microsoft-OutputFormat", OUTPUT_FORMAT)
                .addHeader("User-Agent", "VideoWorkshop")
                .post(body)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string()?.take(500).orEmpty()
                    throw IOException("Azure TTS 合成失败: HTTP ${response.code} $err")
                }
                val bytes = response.body?.bytes()
                    ?: throw IOException("Azure TTS 返回空响应")

                val dir = File(
                    context.filesDir,
                    "${AppConstants.APP_DIR_NAME}/${AppConstants.SUBDIR_AUDIO}"
                ).apply { mkdirs() }
                val file = File(dir, "tts_${System.currentTimeMillis()}.mp3")
                file.writeBytes(bytes)
                file.absolutePath
            }
        }
    }

    /**
     * 构造 SSML，统一使用 cheerful 风格的 express-as 表达。
     *
     * 显式声明 mstts 命名空间，避免 `mstts:express-as` 解析失败。
     */
    private fun buildSsml(voiceId: String, text: String): String =
        """
        <speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" xmlns:mstts="https://www.w3.org/2001/mstts" xml:lang="zh-CN">
            <voice name="$voiceId">
                <mstts:express-as style="cheerful">
                    ${escapeXml(text)}
                </mstts:express-as>
            </voice>
        </speak>
        """.trimIndent()

    private fun escapeXml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    companion object {
        val SSML_MEDIA_TYPE = "application/ssml+xml".toMediaType()
        const val OUTPUT_FORMAT = "audio-16khz-128kbitrate-mono-mp3"

        /** 默认 Azure 区域；非密钥配置，不随 API Key 写入 DataStore。 */
        const val DEFAULT_REGION = "eastus"
    }
}

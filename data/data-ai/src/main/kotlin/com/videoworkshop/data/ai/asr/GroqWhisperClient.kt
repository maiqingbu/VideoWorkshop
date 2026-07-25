package com.videoworkshop.data.ai.asr

import com.videoworkshop.core.common.DispatcherProvider
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

/**
 * Groq Whisper 语音转写客户端（核心）。
 *
 * 走 OpenAI 兼容接口 `POST https://api.groq.com/openai/v1/audio/transcriptions`，
 * 以 multipart 上传音频，返回 SRT 格式字幕。受 Groq 单文件 25MB 限制。
 */
class GroqWhisperClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val dispatchers: DispatcherProvider
) {

    /**
     * 转写音频为 SRT 字幕。
     *
     * @param audioPath 音频文件本地路径
     * @param apiKey    Groq API Key
     * @return [Result] 成功携带 SRT 文本
     */
    suspend fun transcribe(audioPath: String, apiKey: String): Result<String> =
        withContext(dispatchers.io) {
            runCatching {
                require(apiKey.isNotBlank()) { "Groq API Key 不能为空" }

                val file = File(audioPath)
                if (!file.exists()) {
                    throw FileNotFoundException("音频文件不存在: $audioPath")
                }
                if (file.length() > MAX_SIZE_BYTES) {
                    throw IOException(
                        "音频文件超过 Groq 25MB 限制: ${file.length()} bytes（${file.length() / 1_048_576} MB）"
                    )
                }

                val mediaType = guessMediaType(file).toMediaType()
                val filePart = MultipartBody.Part.createFormData(
                    name = "file",
                    filename = file.name,
                    body = file.asRequestBody(mediaType)
                )
                val multipart = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(filePart)
                    .addFormDataPart("model", MODEL)
                    .addFormDataPart("response_format", "srt")
                    .build()

                val request = Request.Builder()
                    .url(ENDPOINT)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(multipart)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string()?.take(500).orEmpty()
                        throw IOException("Groq Whisper 转写失败: HTTP ${response.code} $err")
                    }
                    response.body?.string()
                        ?: throw IOException("Groq Whisper 返回空响应")
                }
            }
        }

    private fun guessMediaType(file: File): String = when (file.extension.lowercase()) {
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        "webm" -> "audio/webm"
        else -> "application/octet-stream"
    }

    companion object {
        const val ENDPOINT: String = "https://api.groq.com/openai/v1/audio/transcriptions"
        const val MODEL: String = "whisper-large-v3"

        /** Groq 单文件大小上限：25 MB。 */
        const val MAX_SIZE_BYTES: Long = 25L * 1024 * 1024
    }
}

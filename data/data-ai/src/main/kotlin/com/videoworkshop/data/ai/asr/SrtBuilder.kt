package com.videoworkshop.data.ai.asr

import kotlinx.serialization.Serializable

/**
 * 单段字幕。
 *
 * @param index     序号（从 1 开始）
 * @param startTime 起始时间戳，形如 `00:00:01,200`
 * @param endTime   结束时间戳，形如 `00:00:03,500`
 * @param text      字幕文本
 */
@Serializable
data class SubtitleSegment(
    val index: Int,
    val startTime: String,
    val endTime: String,
    val text: String
)

/**
 * SRT 字幕构建器。
 *
 * 将 Groq Whisper 返回的 SRT 文本解析为 [SubtitleSegment] 列表，
 * 再重新格式化为标准 SRT，保证序号连续、时间戳格式统一（逗号分隔毫秒）。
 */
object SrtBuilder {

    private val TIME_LINE_REGEX =
        Regex("""(\d{1,2}:\d{2}:\d{2}[.,]\d{1,3})\s*-->\s*(\d{1,2}:\d{2}:\d{2}[.,]\d{1,3})""")

    /**
     * 解析 SRT 文本为有序段落。无法识别的块会被跳过，序号重新编排。
     */
    fun parseSrt(srtText: String): List<SubtitleSegment> {
        if (srtText.isBlank()) return emptyList()

        val normalized = srtText.replace("\r\n", "\n").replace("\r", "\n").trim()
        val blocks = normalized.split(Regex("\n\\s*\n"))
        val segments = mutableListOf<SubtitleSegment>()
        var counter = 1

        for (block in blocks) {
            val lines = block.trim().split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue

            val timeLineIdx = lines.indexOfFirst { TIME_LINE_REGEX.containsMatchIn(it) }
            if (timeLineIdx < 0) continue

            val match = TIME_LINE_REGEX.find(lines[timeLineIdx]) ?: continue
            val start = normalizeTimestamp(match.groupValues[1])
            val end = normalizeTimestamp(match.groupValues[2])
            val text = lines.drop(timeLineIdx + 1).joinToString("\n").trim()
            if (text.isEmpty()) continue

            segments.add(SubtitleSegment(counter++, start, end, text))
        }
        return segments
    }

    /**
     * 将段落重新格式化为标准 SRT 文本。
     */
    fun buildSrt(segments: List<SubtitleSegment>): String {
        if (segments.isEmpty()) return ""
        val sb = StringBuilder()
        for (seg in segments) {
            sb.append(seg.index).append('\n')
            sb.append(seg.startTime).append(" --> ").append(seg.endTime).append('\n')
            sb.append(seg.text).append('\n')
            sb.append('\n')
        }
        return sb.toString().trimEnd()
    }

    /** 统一为 `HH:MM:SS,mmm` 格式（毫秒用逗号）。 */
    private fun normalizeTimestamp(raw: String): String {
        val comma = raw.replace('.', ',')
        val timePart = comma.substringBeforeLast(',')
        val msPart = comma.substringAfterLast(',')
        val paddedMs = msPart.padEnd(3, '0').take(3)
        return "$timePart,$paddedMs"
    }
}

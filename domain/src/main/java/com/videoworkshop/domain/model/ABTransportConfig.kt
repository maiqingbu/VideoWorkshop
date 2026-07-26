package com.videoworkshop.domain.model

/**
 * AB 搬运配置。
 *
 * A 视频作为音频源，B 视频作为画面源，按 [mode] 指定的方式合成输出。
 *
 * @param videoAPath       A 视频（音频源）路径
 * @param videoBPath       B 视频（画面源）路径
 * @param mode             合成模式
 * @param durationStrategy 时长对齐策略
 * @param volumeRatioA     A 音量比例（0.0 ~ 1.0），仅 [ABTransportMode.MIX] 时生效
 * @param volumeRatioB     B 原声音量比例（0.0 ~ 1.0），仅 [ABTransportMode.MIX] 时生效
 * @param segmentA         A 自定义起止点，null 表示使用全长
 * @param segmentB         B 自定义起止点，null 表示使用全长
 * @param outputPath       输出路径
 */
data class ABTransportConfig(
    val videoAPath: String,
    val videoBPath: String,
    val mode: ABTransportMode,
    val durationStrategy: DurationStrategy,
    val volumeRatioA: Float = 1.0f,
    val volumeRatioB: Float = 0.5f,
    val segmentA: TimelineSegment? = null,
    val segmentB: TimelineSegment? = null,
    val outputPath: String
)

/**
 * AB 搬运合成模式。
 */
enum class ABTransportMode {
    /** 纯音轨替换：B 画面 + A 音频 */
    PURE_REPLACE,

    /** 音轨混合：B 画面 + A 音频 + B 原声混音 */
    MIX
}

/**
 * AB 搬运时长对齐策略。
 */
enum class DurationStrategy {
    /** 截断到短者（min(A, B)） */
    TRUNCATE,

    /** 循环到长者（max(A, B)） */
    LOOP,

    /** 自定义起止点（依赖 segmentA/segmentB） */
    CUSTOM
}

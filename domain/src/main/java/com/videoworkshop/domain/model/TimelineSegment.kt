package com.videoworkshop.domain.model

/**
 * 时间轴片段，表示 [startMs, endMs) 区间。
 *
 * @param startMs 起始时间（毫秒，>= 0）
 * @param endMs   结束时间（毫秒，> startMs）
 */
data class TimelineSegment(
    val startMs: Long,
    val endMs: Long
) {
    /** 片段时长（毫秒） */
    val durationMs: Long
        get() = endMs - startMs

    init {
        require(startMs >= 0) { "startMs 不能为负数" }
        require(endMs > startMs) { "endMs 必须大于 startMs" }
    }
}

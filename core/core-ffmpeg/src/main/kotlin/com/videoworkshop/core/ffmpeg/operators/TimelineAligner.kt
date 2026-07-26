package com.videoworkshop.core.ffmpeg.operators

import com.videoworkshop.core.ffmpeg.pipeline.MultiInputCommandBuilder
import com.videoworkshop.domain.model.TimelineSegment

/**
 * 时间轴对齐器（无状态）。
 *
 * 针对 AB 搬运场景，根据 [com.videoworkshop.domain.model.DurationStrategy]
 * 将两个输入视频的时间轴对齐到统一长度。
 *
 * 三种策略与对应方法：
 * - [applyTruncate]：截断到 min(A, B)，仅追加 `-t` 输出时长限制
 * - [applyLoop]：循环到 max(A, B)，对短视频输入前置 `-stream_loop -1`
 * - [applyCustomSegment]：按自定义起止点裁剪，对 A/B 分别使用 `-ss -t -i` 形式
 *
 * 设计说明：
 * - 本类无状态，可单例使用，也可每次实例化。
 * - 不直接依赖 [com.videoworkshop.core.ffmpeg.FfprobeHelper]；时长由调用方通过
 *   领域模型（如 [com.videoworkshop.domain.model.VideoClip]）预先解析后传入，
 *   保持职责单一、便于单元测试。
 * - 输出秒数格式化策略见 [formatSeconds]：整数秒输出 `"30"`，非整数秒输出 `"15.5"`。
 */
class TimelineAligner {

    /**
     * 截断对齐：输出时长 = min(A, B)。
     *
     * 仅向 [builder] 追加 `-t` 输出时长限制（位于编码器之后、输出之前，由
     * [MultiInputCommandBuilder.addExtra] 添加），不修改输入。
     *
     * 调用方应在调用前通过 [MultiInputCommandBuilder.addInput] 添加 A/B 两个输入。
     *
     * 边界场景：当 A 时长 == B 时长时，仍追加 `-t <duration>`，保证输出严格不超过该时长，
     * 行为可预测。
     *
     * @param builder      命令构建器
     * @param durationAMs  A 视频时长（毫秒）
     * @param durationBMs  B 视频时长（毫秒）
     * @return 同一个 [builder]，支持链式调用
     */
    fun applyTruncate(
        builder: MultiInputCommandBuilder,
        durationAMs: Long,
        durationBMs: Long
    ): MultiInputCommandBuilder {
        val minMs = minOf(durationAMs, durationBMs)
        if (minMs <= 0L) return builder
        builder.addExtra("-t").addExtra(formatSeconds(minMs))
        return builder
    }

    /**
     * 循环对齐：输出时长 = max(A, B)。
     *
     * 对较短的视频输入前置 `-stream_loop -1`（无限循环），并通过 `-t` 限制输出总时长
     * 为 max(A, B)。当 A、B 时长相等时，不对任何输入加 `-stream_loop`。
     *
     * 注意：FFmpeg 要求 `-stream_loop` 必须位于对应 `-i` 之前，因此本方法会通过
     * [MultiInputCommandBuilder.addInputWithOptions] 直接添加 A/B 输入。
     * 调用方**不应**在调用本方法前自行 [MultiInputCommandBuilder.addInput]，
     * 否则会产生重复输入。
     *
     * `-stream_loop -1` 表示无限循环，配合输出端 `-t` 即可精确控制输出时长；
     * 无需预估循环次数，避免因时长不整除导致的循环不足或冗余。
     *
     * @param builder      命令构建器
     * @param pathA        A 视频路径
     * @param pathB        B 视频路径
     * @param durationAMs  A 视频时长（毫秒）
     * @param durationBMs  B 视频时长（毫秒）
     * @return 同一个 [builder]，支持链式调用
     */
    fun applyLoop(
        builder: MultiInputCommandBuilder,
        pathA: String,
        pathB: String,
        durationAMs: Long,
        durationBMs: Long
    ): MultiInputCommandBuilder {
        val maxMs = maxOf(durationAMs, durationBMs)
        // 较短的输入需要循环；相等时两者都不循环
        val optionsA = if (durationAMs < durationBMs) listOf("-stream_loop", "-1") else emptyList()
        val optionsB = if (durationBMs < durationAMs) listOf("-stream_loop", "-1") else emptyList()
        builder.addInputWithOptions(pathA, optionsA)
        builder.addInputWithOptions(pathB, optionsB)
        if (maxMs > 0L) {
            builder.addExtra("-t").addExtra(formatSeconds(maxMs))
        }
        return builder
    }

    /**
     * 自定义起止点裁剪。
     *
     * 对 A/B 分别使用 `-ss {start} -t {duration} -i {path}` 形式（input seeking，
     * 解码前 seek，比 output seeking 的 `-ss` 后置更快且更精确）。
     *
     * 调用方**不应**在调用本方法前自行 [MultiInputCommandBuilder.addInput]，
     * 否则会产生重复输入。
     *
     * 输出端不再追加 `-t`，因每个输入的 `-t` 已限定其各自的读取时长；
     * 最终输出时长为 min(segmentA.duration, segmentB.duration)（由 FFmpeg 在
     * 流结束/对齐时自然截断）。
     *
     * @param builder   命令构建器
     * @param pathA     A 视频路径
     * @param pathB     B 视频路径
     * @param segmentA  A 时间片段 [TimelineSegment]
     * @param segmentB  B 时间片段 [TimelineSegment]
     * @return 同一个 [builder]，支持链式调用
     */
    fun applyCustomSegment(
        builder: MultiInputCommandBuilder,
        pathA: String,
        pathB: String,
        segmentA: TimelineSegment,
        segmentB: TimelineSegment
    ): MultiInputCommandBuilder {
        builder.addInputWithOptions(pathA, buildInputSeekingOptions(segmentA))
        builder.addInputWithOptions(pathB, buildInputSeekingOptions(segmentB))
        return builder
    }

    /**
     * 构建 input seeking 选项列表：`-ss {startSec} -t {durationSec}`。
     *
     * @param segment 时间片段
     * @return 选项 token 列表，按 FFmpeg 命令顺序排列
     */
    private fun buildInputSeekingOptions(segment: TimelineSegment): List<String> {
        return listOf(
            "-ss", formatSeconds(segment.startMs),
            "-t", formatSeconds(segment.durationMs)
        )
    }

    /**
     * 将毫秒格式化为秒字符串。
     *
     * - 整数秒（如 `30000ms`）输出 `"30"`，与 FFmpeg 命令习惯一致
     * - 非整数秒（如 `15500ms`）输出 `"15.5"`（去掉尾部多余的 0 与小数点）
     *
     * 使用 3 位小数格式化（毫秒精度），再 `trimEnd` 去掉尾部 0 和孤立的小数点。
     *
     * @param ms 毫秒值
     * @return 秒字符串
     */
    private fun formatSeconds(ms: Long): String {
        return if (ms % 1000L == 0L) {
            (ms / 1000L).toString()
        } else {
            val seconds = ms / 1000.0
            String.format("%.3f", seconds).trimEnd('0').trimEnd('.')
        }
    }
}

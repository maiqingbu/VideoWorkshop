package com.videoworkshop.core.ffmpeg.pipeline

/**
 * 多输入命令构建器。
 *
 * 用于拼装需要多个输入文件（多个 `-i`）与多个流选择（多个 `-map`）的 FFmpeg 命令，
 * 典型场景为 AB 搬运（从输入 A 取视频流、从输入 B 取音频流合并输出）。
 *
 * 与 [DedupCommandBuilder] 的差异：
 * - [DedupCommandBuilder] 面向单一去重场景，参数通过 [DedupConfig] 一次性传入，
 *   `build()` 返回完整命令字符串（含 `ffmpeg` 前缀）。
 * - 本构建器面向多输入通用场景，采用链式 API 逐步装配参数，
 *   `build()` 返回参数列表（不含 `ffmpeg` 前缀），便于调用方按需拼接或转交执行。
 *
 * 命令结构（按 [build] 输出顺序）：
 * ```
 * [<inputOptions1>...] -i <input1> [<inputOptions2>...] -i <input2> ... \
 *   [-filter_complex "<filter>"] \
 *   -map <spec1> -map <spec2> ... \
 *   [-c:v <codec>] [-c:a <codec>] \
 *   [<extra>...] \
 *   <output>
 * ```
 *
 * 使用示例（AB 搬运）：
 * ```
 * val args = MultiInputCommandBuilder()
 *     .addInput("/path/A.mp4")
 *     .addInput("/path/B.mp4")
 *     .addMap("0:v:0")
 *     .addMap("1:a:0")
 *     .videoCodec("libx264")
 *     .audioCodec("aac")
 *     .output("/path/out.mp4")
 *     .build()
 * ```
 *
 * 使用示例（带输入选项，如 -stream_loop / -ss）：
 * ```
 * val args = MultiInputCommandBuilder()
 *     .addInputWithOptions("/path/A.mp4", listOf("-stream_loop", "-1"))
 *     .addInputWithOptions("/path/B.mp4", listOf("-ss", "5", "-t", "15"))
 *     .addMap("0:v:0")
 *     .output("/path/out.mp4")
 *     .build()
 * ```
 */
class MultiInputCommandBuilder {

    /**
     * 单个输入项：包含位于 `-i` 之前的输入选项（如 `-ss`/`-t`/`-stream_loop`）和输入路径。
     *
     * 当 [options] 为空列表时，等价于仅有 `-i <path>`，与 [addInput] 行为一致。
     */
    private data class InputEntry(val options: List<String>, val path: String)

    /** 输入项列表（按添加顺序保留，每个对应一组 `[options...] -i <path>`）。 */
    private val inputEntries = mutableListOf<InputEntry>()

    /** 流选择规格列表（按添加顺序保留，每个对应一个 `-map`，如 `"0:v:0"`）。 */
    private val maps = mutableListOf<String>()

    /** 额外参数列表（按添加顺序保留，位于编码器之后、输出之前）。 */
    private val extras = mutableListOf<String>()

    /** 输出文件路径，由 [output] 设置。 */
    private var outputPath: String? = null

    /** 视频编码器，由 [videoCodec] 设置，如 `"libx264"`、`"copy"`。 */
    private var videoCodecValue: String? = null

    /** 音频编码器，由 [audioCodec] 设置，如 `"aac"`、`"copy"`。 */
    private var audioCodecValue: String? = null

    /** filter_complex 滤镜图，由 [filterComplex] 设置。 */
    private var filterComplexValue: String? = null

    /**
     * 添加一个输入文件路径（对应一个 `-i` 参数），不带任何输入选项。
     *
     * 等价于 [addInputWithOptions] 传入空选项列表。
     *
     * @param path 输入文件路径。
     * @return 当前构建器，支持链式调用。
     */
    fun addInput(path: String): MultiInputCommandBuilder {
        inputEntries.add(InputEntry(emptyList(), path))
        return this
    }

    /**
     * 添加一个输入文件路径，并附带位于 `-i` 之前的输入选项。
     *
     * 用于表达 FFmpeg 的 input options，例如：
     * - `-stream_loop -1`（循环输入，必须位于 `-i` 之前）
     * - `-ss 5 -t 15`（input seeking，必须位于 `-i` 之前才更快）
     *
     * 调用方需自行保证选项与值的配对顺序，构建器原样输出。
     *
     * @param path    输入文件路径。
     * @param options 位于 `-i` 之前的输入选项列表（如 `listOf("-stream_loop", "-1")`），
     *                空列表等价于 [addInput]。
     * @return 当前构建器，支持链式调用。
     */
    fun addInputWithOptions(path: String, options: List<String>): MultiInputCommandBuilder {
        inputEntries.add(InputEntry(options, path))
        return this
    }

    /**
     * 添加一个流选择规格（对应一个 `-map` 参数）。
     *
     * @param streamSpec 流规格，如 `"0:v:0"`（第 0 个输入的第 0 路视频流）、`"1:a:0"`。
     * @return 当前构建器，支持链式调用。
     */
    fun addMap(streamSpec: String): MultiInputCommandBuilder {
        maps.add(streamSpec)
        return this
    }

    /**
     * 设置输出文件路径。位于命令末尾。
     *
     * @param path 输出文件路径。
     * @return 当前构建器，支持链式调用。
     */
    fun output(path: String): MultiInputCommandBuilder {
        outputPath = path
        return this
    }

    /**
     * 设置视频编码器（对应 `-c:v`）。
     *
     * @param codec 编码器名称，如 `"libx264"`、`"h264_mediacodec"`、`"copy"`。
     * @return 当前构建器，支持链式调用。
     */
    fun videoCodec(codec: String): MultiInputCommandBuilder {
        videoCodecValue = codec
        return this
    }

    /**
     * 设置音频编码器（对应 `-c:a`）。
     *
     * @param codec 编码器名称，如 `"aac"`、`"copy"`。
     * @return 当前构建器，支持链式调用。
     */
    fun audioCodec(codec: String): MultiInputCommandBuilder {
        audioCodecValue = codec
        return this
    }

    /**
     * 添加一个额外参数（原样追加到参数列表中，位于编码器之后、输出之前）。
     *
     * 适用于无法通过专用方法表达的参数，如 `"-y"`、`"-pix_fmt"`、`"yuv420p"`、`"-t"` 等。
     * 调用方需自行保证参数与值的配对顺序。
     *
     * @param arg 单个参数或值。
     * @return 当前构建器，支持链式调用。
     */
    fun addExtra(arg: String): MultiInputCommandBuilder {
        extras.add(arg)
        return this
    }

    /**
     * 设置 filter_complex 滤镜图（对应 `-filter_complex`）。
     *
     * 仅在调用后才会出现在最终命令中，且位于 `-i` 之后、`-map` 之前。
     *
     * @param filter 滤镜图字符串，如 `"[0:v][1:v]concat=n=2:v=1[outv]"`。
     * @return 当前构建器，支持链式调用。
     */
    fun filterComplex(filter: String): MultiInputCommandBuilder {
        filterComplexValue = filter
        return this
    }

    /**
     * 构建命令参数列表。
     *
     * 输出顺序固定为：
     * 1. 各输入项（按添加顺序），每项为 `[options...] -i <path>`
     * 2. `-filter_complex` 滤镜图（若已通过 [filterComplex] 设置）
     * 3. 各 `-map` 流选择（按 [addMap] 顺序）
     * 4. `-c:v` 视频编码器（若已通过 [videoCodec] 设置）
     * 5. `-c:a` 音频编码器（若已通过 [audioCodec] 设置）
     * 6. 额外参数（按 [addExtra] 顺序）
     * 7. 输出路径（若已通过 [output] 设置）
     *
     * @return 参数列表（不含 `ffmpeg` 前缀），每个元素为一个独立 token。
     */
    fun build(): List<String> {
        val args = mutableListOf<String>()

        // 1. 输入项（每项先输出 options，再输出 -i <path>）
        for (entry in inputEntries) {
            for (opt in entry.options) {
                args.add(opt)
            }
            args.add("-i")
            args.add(entry.path)
        }

        // 2. filter_complex（必须在 -map 之前）
        filterComplexValue?.let { filter ->
            args.add("-filter_complex")
            args.add(filter)
        }

        // 3. 流选择
        for (spec in maps) {
            args.add("-map")
            args.add(spec)
        }

        // 4. 视频编码器
        videoCodecValue?.let { codec ->
            args.add("-c:v")
            args.add(codec)
        }

        // 5. 音频编码器
        audioCodecValue?.let { codec ->
            args.add("-c:a")
            args.add(codec)
        }

        // 6. 额外参数
        args.addAll(extras)

        // 7. 输出路径
        outputPath?.let { args.add(it) }

        return args
    }
}

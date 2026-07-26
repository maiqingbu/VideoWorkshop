package com.videoworkshop.core.ffmpeg.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [MultiInputCommandBuilder] 的单元测试。
 *
 * 覆盖单输入/多输入、单/多 `-map`、`-filter_complex`、额外参数及链式调用顺序等场景。
 * 所有用例均校验 [MultiInputCommandBuilder.build] 返回的参数列表（不含 `ffmpeg` 前缀）。
 */
class MultiInputCommandBuilderTest {

    // ===== 用例 1：单输入单 map 基础场景 =====
    @Test
    fun build_singleInputSingleMap_producesExpectedArgs() {
        val args = MultiInputCommandBuilder()
            .addInput("/input.mp4")
            .addMap("0:v:0")
            .output("/output.mp4")
            .build()

        val expected = listOf(
            "-i", "/input.mp4",
            "-map", "0:v:0",
            "/output.mp4"
        )
        assertEquals("单输入单 map 应生成预期参数列表", expected, args)
    }

    // ===== 用例 2：双输入双 map（AB 搬运场景：-i A -i B -map 0:v:0 -map 1:a:0）=====
    @Test
    fun build_dualInputsDualMaps_abTransportScenario() {
        val args = MultiInputCommandBuilder()
            .addInput("/A.mp4")
            .addInput("/B.mp4")
            .addMap("0:v:0")
            .addMap("1:a:0")
            .videoCodec("libx264")
            .audioCodec("aac")
            .output("/out.mp4")
            .build()

        val expected = listOf(
            "-i", "/A.mp4",
            "-i", "/B.mp4",
            "-map", "0:v:0",
            "-map", "1:a:0",
            "-c:v", "libx264",
            "-c:a", "aac",
            "/out.mp4"
        )
        assertEquals("AB 搬运场景应生成预期参数列表", expected, args)
    }

    // ===== 用例 3：含 filter_complex 的场景（应位于 -i 之后、-map 之前）=====
    @Test
    fun build_withFilterComplex_placedBeforeMap() {
        val filter = "[0:v][1:v]concat=n=2:v=1[outv]"
        val args = MultiInputCommandBuilder()
            .addInput("/A.mp4")
            .addInput("/B.mp4")
            .filterComplex(filter)
            .addMap("[outv]")
            .addMap("0:a:0")
            .output("/out.mp4")
            .build()

        val expected = listOf(
            "-i", "/A.mp4",
            "-i", "/B.mp4",
            "-filter_complex", filter,
            "-map", "[outv]",
            "-map", "0:a:0",
            "/out.mp4"
        )
        assertEquals("含 filter_complex 应生成预期参数列表", expected, args)

        // 显式校验相对顺序：-filter_complex 位于 -map 之前
        val filterIdx = args.indexOf("-filter_complex")
        val firstMapIdx = args.indexOf("-map")
        assertTrue(
            "-filter_complex 应位于首个 -map 之前",
            filterIdx in 0 until firstMapIdx
        )
    }

    // ===== 用例 4：含额外参数的场景（应位于编码器之后、输出之前）=====
    @Test
    fun build_withExtraArgs_placedAfterCodecsAndBeforeOutput() {
        val args = MultiInputCommandBuilder()
            .addInput("/input.mp4")
            .addMap("0:v:0")
            .videoCodec("libx264")
            .audioCodec("aac")
            .addExtra("-pix_fmt")
            .addExtra("yuv420p")
            .addExtra("-y")
            .output("/output.mp4")
            .build()

        val expected = listOf(
            "-i", "/input.mp4",
            "-map", "0:v:0",
            "-c:v", "libx264",
            "-c:a", "aac",
            "-pix_fmt", "yuv420p", "-y",
            "/output.mp4"
        )
        assertEquals("含额外参数应生成预期参数列表", expected, args)

        // 显式校验相对顺序：额外参数位于 -c:a 之后、输出路径之前
        val audioCodecIdx = args.indexOf("-c:a")
        val outputIdx = args.indexOf("/output.mp4")
        val pixFmtIdx = args.indexOf("-pix_fmt")
        assertTrue(
            "额外参数应位于 -c:a 之后",
            pixFmtIdx > audioCodecIdx
        )
        assertTrue(
            "额外参数应位于输出路径之前",
            pixFmtIdx < outputIdx
        )
    }

    // ===== 用例 5：链式调用顺序保持（多输入与多 map 按添加顺序输出）=====
    @Test
    fun build_chainedCalls_preserveInsertionOrder() {
        // 三个输入依次添加，三个 map 依次添加，校验输出顺序与添加顺序一致
        val args = MultiInputCommandBuilder()
            .addInput("/first.mp4")
            .addInput("/second.mp4")
            .addInput("/third.mp4")
            .addMap("0:v:0")
            .addMap("1:v:0")
            .addMap("2:a:0")
            .output("/out.mp4")
            .build()

        // 输入顺序：first → second → third
        val firstInputIdx = args.indexOf("/first.mp4")
        val secondInputIdx = args.indexOf("/second.mp4")
        val thirdInputIdx = args.indexOf("/third.mp4")
        assertTrue("首个输入应在第二个输入之前", firstInputIdx < secondInputIdx)
        assertTrue("第二个输入应在第三个输入之前", secondInputIdx < thirdInputIdx)

        // map 顺序：0:v:0 → 1:v:0 → 2:a:0
        val mapSpec0Idx = args.indexOf("0:v:0")
        val mapSpec1Idx = args.indexOf("1:v:0")
        val mapSpec2Idx = args.indexOf("2:a:0")
        assertTrue("首个 map 应在第二个 map 之前", mapSpec0Idx < mapSpec1Idx)
        assertTrue("第二个 map 应在第三个 map 之前", mapSpec1Idx < mapSpec2Idx)

        // 全部 map 应位于全部 -i 之后
        assertTrue("所有 -map 应位于所有 -i 之后", thirdInputIdx < mapSpec0Idx)

        // 输出路径应位于末尾
        assertEquals("输出路径应位于参数列表末尾", "/out.mp4", args.last())
    }

    // ===== 用例 6：未设置可选项时不应出现对应参数 =====
    @Test
    fun build_withoutOptionalFields_omitsCorrespondingFlags() {
        // 仅设置输入与输出，不设置 map / 编码器 / filter_complex / 额外参数
        val args = MultiInputCommandBuilder()
            .addInput("/input.mp4")
            .output("/output.mp4")
            .build()

        val expected = listOf("-i", "/input.mp4", "/output.mp4")
        assertEquals("仅输入与输出时应生成最小参数列表", expected, args)

        assertFalse("未设置 map 时不应出现 -map", args.contains("-map"))
        assertFalse("未设置视频编码器时不应出现 -c:v", args.contains("-c:v"))
        assertFalse("未设置音频编码器时不应出现 -c:a", args.contains("-c:a"))
        assertFalse("未设置 filter_complex 时不应出现 -filter_complex", args.contains("-filter_complex"))
    }

    // ===== 用例 7：未调用 build 前链式方法均返回当前构建器（间接校验链式可用）=====
    @Test
    fun chainedMethods_allReturnSameBuilderInstance() {
        val builder = MultiInputCommandBuilder()
        // 每个链式方法应返回同一个构建器实例，保证链式调用可用
        assertTrue("addInput 应返回同一实例", builder.addInput("/a.mp4") === builder)
        assertTrue("addMap 应返回同一实例", builder.addMap("0:v:0") === builder)
        assertTrue("output 应返回同一实例", builder.output("/o.mp4") === builder)
        assertTrue("videoCodec 应返回同一实例", builder.videoCodec("libx264") === builder)
        assertTrue("audioCodec 应返回同一实例", builder.audioCodec("aac") === builder)
        assertTrue("addExtra 应返回同一实例", builder.addExtra("-y") === builder)
        assertTrue("filterComplex 应返回同一实例", builder.filterComplex("[0:v]null") === builder)
    }
}

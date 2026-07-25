package com.videoworkshop.core.ffmpeg.algorithms

import com.videoworkshop.core.ffmpeg.pipeline.DedupStrength
import org.junit.Assert.*
import org.junit.Test

/**
 * [AudioReshaper] 单元测试。
 *
 * 验证 filter_complex 兼容的音频滤镜图生成，覆盖：
 * - 有效时长下三个强度档位的完整输出。
 * - 零 / 负时长默认回退到 1.0 秒。
 * - 滤镜图三段结构：equalizer、anoisesrc、amix。
 *
 * 注意：
 * - eqGain 为 Float，经 StringBuilder.append(Float) 输出，整数值保留 ".0"（如 1f -> "1.0"）。
 * - noiseAmplitude 为 Float，经 formatNumber(Float) -> formatNumber(Double) 转为 Double 后输出，
 *   因此保留完整的双精度字面量（如 0.002f -> "0.0020000000949949026"）。
 */
class AudioReshaperTest {

    /** 测试用视频时长（秒），整数时长会被 formatNumber 格式化为 "10"。 */
    private val duration = 10.0

    @Test
    fun buildFilter_light_有效时长_完整滤镜图() {
        val filter = AudioReshaper.buildFilter(DedupStrength.LIGHT, duration)
        // eqGain=1.0；noiseAmplitude=0.002f -> toDouble -> "0.0020000000949949026"
        val expected = "[0:a]equalizer=f=1000.0:t=q:w=1.0:g=1.0[a0];" +
            "anoisesrc=d=10:c=pink:a=0.0020000000949949026[noise];" +
            "[a0][noise]amix=inputs=2:duration=first:dropout=0[aout]"
        assertEquals(expected, filter)
    }

    @Test
    fun buildFilter_standard_有效时长_完整滤镜图() {
        val filter = AudioReshaper.buildFilter(DedupStrength.STANDARD, duration)
        val expected = "[0:a]equalizer=f=1000.0:t=q:w=1.0:g=2.0[a0];" +
            "anoisesrc=d=10:c=pink:a=0.003000000026077032[noise];" +
            "[a0][noise]amix=inputs=2:duration=first:dropout=0[aout]"
        assertEquals(expected, filter)
    }

    @Test
    fun buildFilter_deep_有效时长_完整滤镜图() {
        val filter = AudioReshaper.buildFilter(DedupStrength.DEEP, duration)
        val expected = "[0:a]equalizer=f=1000.0:t=q:w=1.0:g=3.0[a0];" +
            "anoisesrc=d=10:c=pink:a=0.004999999888241291[noise];" +
            "[a0][noise]amix=inputs=2:duration=first:dropout=0[aout]"
        assertEquals(expected, filter)
    }

    @Test
    fun buildFilter_零时长_默认回退到1秒() {
        // durationSec = 0 不合法，应回退到 1.0，anoisesrc 的 d 参数为 "1"
        DedupStrength.values().forEach { strength ->
            val filter = AudioReshaper.buildFilter(strength, 0.0)
            assertTrue("应包含 anoisesrc=d=1: $filter", filter.contains("anoisesrc=d=1:c=pink"))
            // 不应出现 d=0
            assertFalse("不应出现 d=0: $filter", filter.contains("anoisesrc=d=0"))
        }
    }

    @Test
    fun buildFilter_负时长_默认回退到1秒() {
        // 负时长同样不合法，应回退到 1.0
        DedupStrength.values().forEach { strength ->
            val filter = AudioReshaper.buildFilter(strength, -5.0)
            assertTrue("应包含 anoisesrc=d=1: $filter", filter.contains("anoisesrc=d=1:c=pink"))
            assertFalse("不应出现负时长: $filter", filter.contains("anoisesrc=d=-5"))
        }
    }

    @Test
    fun buildFilter_零与负时长结果一致() {
        // 0.0 与 -5.0 都应回退到 1.0，故两者的 anoisesrc 段应相同
        val zero = AudioReshaper.buildFilter(DedupStrength.STANDARD, 0.0)
        val negative = AudioReshaper.buildFilter(DedupStrength.STANDARD, -5.0)
        assertEquals(zero, negative)
        assertTrue(zero.contains("anoisesrc=d=1:c=pink"))
    }

    @Test
    fun buildFilter_小数时长_保留小数原值() {
        // 12.5 不是整数，formatNumber 应保留 "12.5"
        val filter = AudioReshaper.buildFilter(DedupStrength.STANDARD, 12.5)
        assertTrue("应保留小数时长: $filter", filter.contains("anoisesrc=d=12.5:c=pink"))
    }

    @Test
    fun buildFilter_整数时长去除尾零() {
        // 30.0 为整数，formatNumber 应输出 "30"
        val filter = AudioReshaper.buildFilter(DedupStrength.STANDARD, 30.0)
        assertTrue(filter.contains("anoisesrc=d=30:c=pink"))
        assertFalse(filter.contains("anoisesrc=d=30.0"))
    }

    @Test
    fun buildFilter_包含equalizer段() {
        // 第一段：对输入音轨施加均衡器，输出标签 [a0]
        val filter = AudioReshaper.buildFilter(DedupStrength.STANDARD, duration)
        val segments = filter.split(";")
        assertEquals("滤镜图应由 3 段组成", 3, segments.size)
        val eq = segments[0]
        assertTrue("应以 [0:a] 输入标签开头: $eq", eq.startsWith("[0:a]equalizer="))
        assertTrue("应包含中心频率 f=1000.0: $eq", eq.contains("f=1000.0"))
        assertTrue("应包含滤波类型 t=q: $eq", eq.contains("t=q"))
        assertTrue("应包含带宽 w=1.0: $eq", eq.contains("w=1.0"))
        assertTrue("应包含增益 g=2.0: $eq", eq.contains("g=2.0"))
        assertTrue("应以 [a0] 输出标签结尾: $eq", eq.endsWith("[a0]"))
    }

    @Test
    fun buildFilter_包含anoisesrc段() {
        // 第二段：生成粉噪声，输出标签 [noise]
        val filter = AudioReshaper.buildFilter(DedupStrength.STANDARD, duration)
        val segments = filter.split(";")
        val noise = segments[1]
        assertTrue("应以 anoisesrc= 开头: $noise", noise.startsWith("anoisesrc="))
        assertTrue("应包含时长 d=10: $noise", noise.contains("d=10"))
        assertTrue("应包含颜色 c=pink: $noise", noise.contains("c=pink"))
        assertTrue("应包含幅度 a=0.003000000026077032: $noise", noise.contains("a=0.003000000026077032"))
        assertTrue("应以 [noise] 输出标签结尾: $noise", noise.endsWith("[noise]"))
    }

    @Test
    fun buildFilter_包含amix段() {
        // 第三段：混合 [a0] 与 [noise]，输出标签 [aout]
        val filter = AudioReshaper.buildFilter(DedupStrength.STANDARD, duration)
        val segments = filter.split(";")
        val mix = segments[2]
        assertEquals("[a0][noise]amix=inputs=2:duration=first:dropout=0[aout]", mix)
        assertTrue("应以两个输入标签开头: $mix", mix.startsWith("[a0][noise]amix="))
        assertTrue("应包含 inputs=2: $mix", mix.contains("inputs=2"))
        assertTrue("应包含 duration=first: $mix", mix.contains("duration=first"))
        assertTrue("应包含 dropout=0: $mix", mix.contains("dropout=0"))
        assertTrue("应以 [aout] 输出标签结尾: $mix", mix.endsWith("[aout]"))
    }

    @Test
    fun buildFilter_eqGain随强度档位变化() {
        // LIGHT=1.0, STANDARD=2.0, DEEP=3.0
        assertEquals(
            "1.0",
            AudioReshaper.buildFilter(DedupStrength.LIGHT, duration)
                .substringAfter(":g=").substringBefore("[a0]")
        )
        assertEquals(
            "2.0",
            AudioReshaper.buildFilter(DedupStrength.STANDARD, duration)
                .substringAfter(":g=").substringBefore("[a0]")
        )
        assertEquals(
            "3.0",
            AudioReshaper.buildFilter(DedupStrength.DEEP, duration)
                .substringAfter(":g=").substringBefore("[a0]")
        )
    }

    @Test
    fun buildFilter_noiseAmplitude随强度档位变化() {
        // 不同强度的噪声幅度应不同
        val light = AudioReshaper.buildFilter(DedupStrength.LIGHT, duration)
        val standard = AudioReshaper.buildFilter(DedupStrength.STANDARD, duration)
        val deep = AudioReshaper.buildFilter(DedupStrength.DEEP, duration)
        assertTrue(light.contains("a=0.0020000000949949026"))
        assertTrue(standard.contains("a=0.003000000026077032"))
        assertTrue(deep.contains("a=0.004999999888241291"))
    }

    @Test
    fun buildFilter_均衡器参数为固定常量() {
        // f=1000.0, t=q, w=1.0 不随强度变化
        DedupStrength.values().forEach { strength ->
            val filter = AudioReshaper.buildFilter(strength, duration)
            assertTrue(filter.contains("f=1000.0"))
            assertTrue(filter.contains("t=q"))
            assertTrue(filter.contains("w=1.0"))
        }
    }

    @Test
    fun buildFilter_输出标签为aout() {
        // 调用方应 -map "[aout]"，故输出标签必须为 [aout]
        DedupStrength.values().forEach { strength ->
            val filter = AudioReshaper.buildFilter(strength, duration)
            assertTrue("应以 [aout] 结尾: $filter", filter.endsWith("[aout]"))
        }
    }

    @Test
    fun buildFilter_滤镜图由三段以分号连接() {
        // 完整滤镜图结构：equalizer段;anoisesrc段;amix段
        val filter = AudioReshaper.buildFilter(DedupStrength.LIGHT, duration)
        val segments = filter.split(";")
        assertEquals(3, segments.size)
        assertTrue(segments[0].startsWith("[0:a]equalizer="))
        assertTrue(segments[1].startsWith("anoisesrc="))
        assertTrue(segments[2].startsWith("[a0][noise]amix="))
    }
}

package com.videoworkshop.core.ffmpeg.algorithms

import com.videoworkshop.core.ffmpeg.pipeline.DedupStrength
import org.junit.Assert.*
import org.junit.Test

/**
 * [BitrateModifier] 单元测试。
 *
 * 验证码率缩放计算、最低码率兜底（MIN_BITRATE = 200000）以及零码率等场景。
 *
 * 注意：[DedupStrength.bitrateFactor] 为 Float，乘法以 Float 精度运算后再 toLong() 截断，
 * 因此边界用例的期望值已按 Float 运算结果精确校准。
 */
class BitrateModifierTest {

    @Test
    fun buildParam_2mbps_各档位按系数缩放() {
        // 原始码率 2,000,000 bps（2 Mbps）
        val original = 2_000_000L
        // LIGHT: 2_000_000 * 1.1 = 2_200_000
        assertEquals("2200000", BitrateModifier.buildParam(original, DedupStrength.LIGHT))
        // STANDARD: 2_000_000 * 1.15 = 2_300_000
        assertEquals("2300000", BitrateModifier.buildParam(original, DedupStrength.STANDARD))
        // DEEP: 2_000_000 * 1.2 = 2_400_000
        assertEquals("2400000", BitrateModifier.buildParam(original, DedupStrength.DEEP))
    }

    @Test
    fun buildParam_1mbps_各档位按系数缩放() {
        // 原始码率 1,000,000 bps（1 Mbps）
        val original = 1_000_000L
        assertEquals("1100000", BitrateModifier.buildParam(original, DedupStrength.LIGHT))
        assertEquals("1150000", BitrateModifier.buildParam(original, DedupStrength.STANDARD))
        assertEquals("1200000", BitrateModifier.buildParam(original, DedupStrength.DEEP))
    }

    @Test
    fun buildParam_强度越高目标码率越高() {
        // 同一原始码率下，DEEP > STANDARD > LIGHT
        val original = 5_000_000L
        val light = BitrateModifier.buildParam(original, DedupStrength.LIGHT).toLong()
        val standard = BitrateModifier.buildParam(original, DedupStrength.STANDARD).toLong()
        val deep = BitrateModifier.buildParam(original, DedupStrength.DEEP).toLong()
        assertTrue("DEEP 应高于 STANDARD", deep > standard)
        assertTrue("STANDARD 应高于 LIGHT", standard > light)
    }

    @Test
    fun buildParam_低于最低码率_强制抬升至200000() {
        // 原始码率 100,000 bps，缩放后仍远低于 MIN_BITRATE，应被强制抬升至 200000
        val original = 100_000L
        assertEquals("200000", BitrateModifier.buildParam(original, DedupStrength.LIGHT))
        assertEquals("200000", BitrateModifier.buildParam(original, DedupStrength.STANDARD))
        assertEquals("200000", BitrateModifier.buildParam(original, DedupStrength.DEEP))
    }

    @Test
    fun buildParam_边界_目标码率刚好低于200000时抬升() {
        // DEEP 档位系数 1.2f（Float 运算）：166666 * 1.2f ≈ 199999.2 -> toLong() = 199999
        // 低于 MIN_BITRATE(200000)，应被 coerceAtLeast 抬升至 200000
        assertEquals("200000", BitrateModifier.buildParam(166_666L, DedupStrength.DEEP))
    }

    @Test
    fun buildParam_边界_目标码率刚好高于200000时保留() {
        // DEEP 档位系数 1.2f（Float 运算）：166668 * 1.2f ≈ 200001.6 -> toLong() = 200001
        // 高于 MIN_BITRATE(200000)，应原样保留
        assertEquals("200001", BitrateModifier.buildParam(166_668L, DedupStrength.DEEP))
    }

    @Test
    fun buildParam_零码率_强制抬升至200000() {
        // 原始码率为 0，缩放后仍为 0，应被强制抬升至 MIN_BITRATE
        assertEquals("200000", BitrateModifier.buildParam(0L, DedupStrength.LIGHT))
        assertEquals("200000", BitrateModifier.buildParam(0L, DedupStrength.STANDARD))
        assertEquals("200000", BitrateModifier.buildParam(0L, DedupStrength.DEEP))
    }

    @Test
    fun buildParam_极小正码率_强制抬升至200000() {
        // 原始码率为 1 bps，缩放后仍远低于 MIN_BITRATE
        assertEquals("200000", BitrateModifier.buildParam(1L, DedupStrength.DEEP))
    }

    @Test
    fun buildParam_结果始终为纯数字字符串() {
        // 校验所有档位、若干原始码率下，结果均为纯数字（无小数点、无符号）
        listOf(0L, 1L, 100_000L, 1_000_000L, 5_000_000L).forEach { original ->
            DedupStrength.values().forEach { strength ->
                val result = BitrateModifier.buildParam(original, strength)
                assertTrue("结果应为纯数字: $result", result.matches(Regex("\\d+")))
            }
        }
    }

    @Test
    fun buildParam_结果不低于200000() {
        // 不论输入如何，输出码率均不应低于 MIN_BITRATE
        listOf(0L, 1L, 50_000L, 100_000L, 166_666L).forEach { original ->
            DedupStrength.values().forEach { strength ->
                val result = BitrateModifier.buildParam(original, strength).toLong()
                assertTrue("码率不应低于 200000: $result", result >= 200_000L)
            }
        }
    }
}

package com.videoworkshop.core.ffmpeg.algorithms

import com.videoworkshop.core.ffmpeg.pipeline.DedupStrength
import org.junit.Assert.*
import org.junit.Test

/**
 * [ColorShifter] 单元测试。
 *
 * 验证 hue 滤镜字符串的生成。注意：Kotlin 中 Float.toString() 对整数值浮点
 * 会保留 ".0" 后缀（如 3f -> "3.0"），饱和度系数则保留其字面小数（如 1.03f -> "1.03"）。
 */
class ColorShifterTest {

    @Test
    fun buildFilter_light_色相3_0_饱和度1_03() {
        // LIGHT: hueShift = 3f -> "3.0"，saturationFactor = 1.03f -> "1.03"
        assertEquals("hue=h=3.0:s=1.03", ColorShifter.buildFilter(DedupStrength.LIGHT))
    }

    @Test
    fun buildFilter_standard_色相5_0_饱和度1_05() {
        // STANDARD: hueShift = 5f -> "5.0"，saturationFactor = 1.05f -> "1.05"
        assertEquals("hue=h=5.0:s=1.05", ColorShifter.buildFilter(DedupStrength.STANDARD))
    }

    @Test
    fun buildFilter_deep_色相8_0_饱和度1_08() {
        // DEEP: hueShift = 8f -> "8.0"，saturationFactor = 1.08f -> "1.08"
        assertEquals("hue=h=8.0:s=1.08", ColorShifter.buildFilter(DedupStrength.DEEP))
    }

    @Test
    fun buildFilter_色相值带小数尾零() {
        // Float.toString 对整数值浮点保留 ".0" 后缀
        DedupStrength.values().forEach { strength ->
            val filter = ColorShifter.buildFilter(strength)
            val hPart = filter.substringAfter("hue=h=").substringBefore(":")
            assertTrue("色相应以 .0 结尾: $hPart", hPart.endsWith(".0"))
        }
    }

    @Test
    fun buildFilter_强度越高色相偏移越大() {
        val light = ColorShifter.buildFilter(DedupStrength.LIGHT).substringAfter("h=").substringBefore(":").toFloat()
        val standard = ColorShifter.buildFilter(DedupStrength.STANDARD).substringAfter("h=").substringBefore(":").toFloat()
        val deep = ColorShifter.buildFilter(DedupStrength.DEEP).substringAfter("h=").substringBefore(":").toFloat()
        assertTrue("DEEP 色相应最大", deep > standard)
        assertTrue("STANDARD 色相应大于 LIGHT", standard > light)
    }

    @Test
    fun buildFilter_强度越高饱和度系数越大() {
        val light = ColorShifter.buildFilter(DedupStrength.LIGHT).substringAfter(":s=").toFloat()
        val standard = ColorShifter.buildFilter(DedupStrength.STANDARD).substringAfter(":s=").toFloat()
        val deep = ColorShifter.buildFilter(DedupStrength.DEEP).substringAfter(":s=").toFloat()
        assertTrue("DEEP 饱和度应最大", deep > standard)
        assertTrue("STANDARD 饱和度应大于 LIGHT", standard > light)
    }

    @Test
    fun buildFilter_格式符合hue滤镜规范() {
        // 输出格式：hue=h={hueShift}:s={saturationFactor}
        DedupStrength.values().forEach { strength ->
            val filter = ColorShifter.buildFilter(strength)
            assertTrue(filter.startsWith("hue=h="))
            assertTrue(filter.contains(":s="))
            assertEquals("hue=h=${strength.hueShift}:s=${strength.saturationFactor}", filter)
        }
    }
}

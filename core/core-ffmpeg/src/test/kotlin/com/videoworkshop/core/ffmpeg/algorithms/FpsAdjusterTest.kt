package com.videoworkshop.core.ffmpeg.algorithms

import com.videoworkshop.core.ffmpeg.pipeline.DedupStrength
import org.junit.Assert.*
import org.junit.Test

/**
 * [FpsAdjuster] 单元测试。
 *
 * 验证不同去重强度下 fps 滤镜字符串的生成，重点覆盖帧率的整数 / 小数格式化逻辑：
 * - 整数帧率（如 29.0、28.0）应去除小数尾零，输出 "29"、"28"。
 * - 小数帧率（如 29.97）应保留原值，输出 "29.97"。
 */
class FpsAdjusterTest {

    @Test
    fun buildFilter_light_保留小数帧率29_97() {
        // LIGHT 档位帧率为 29.97，属于小数帧率，应原样输出
        val filter = FpsAdjuster.buildFilter(DedupStrength.LIGHT)
        assertEquals("fps=29.97", filter)
    }

    @Test
    fun buildFilter_standard_整数帧率29_去除尾零() {
        // STANDARD 档位帧率为 29.0，应格式化为整数 "29"
        val filter = FpsAdjuster.buildFilter(DedupStrength.STANDARD)
        assertEquals("fps=29", filter)
    }

    @Test
    fun buildFilter_deep_整数帧率28_去除尾零() {
        // DEEP 档位帧率为 28.0，应格式化为整数 "28"
        val filter = FpsAdjuster.buildFilter(DedupStrength.DEEP)
        assertEquals("fps=28", filter)
    }

    @Test
    fun buildFilter_整数与小数格式化分支不同() {
        // 校验整数帧率分支不包含小数点，小数帧率分支包含小数点
        val light = FpsAdjuster.buildFilter(DedupStrength.LIGHT)
        val standard = FpsAdjuster.buildFilter(DedupStrength.STANDARD)
        val deep = FpsAdjuster.buildFilter(DedupStrength.DEEP)

        assertTrue("小数帧率应保留小数点: $light", light.contains("."))
        assertFalse("整数帧率不应包含小数点: $standard", standard.contains("."))
        assertFalse("整数帧率不应包含小数点: $deep", deep.contains("."))
    }

    @Test
    fun buildFilter_所有档位均以fps前缀开头() {
        // 统一校验所有档位输出均以 "fps=" 前缀开头
        DedupStrength.values().forEach { strength ->
            val filter = FpsAdjuster.buildFilter(strength)
            assertTrue("应以 fps= 开头: $filter", filter.startsWith("fps="))
        }
    }

    @Test
    fun buildFilter_整数帧率与toLong一致() {
        // STANDARD(29.0) 与 DEEP(28.0) 的输出应为 fps= 后直接跟整数
        assertEquals("fps=29", "fps=${DedupStrength.STANDARD.fps.toLong()}")
        assertEquals("fps=28", "fps=${DedupStrength.DEEP.fps.toLong()}")
    }
}

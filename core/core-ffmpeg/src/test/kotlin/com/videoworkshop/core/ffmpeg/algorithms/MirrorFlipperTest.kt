package com.videoworkshop.core.ffmpeg.algorithms

import org.junit.Assert.*
import org.junit.Test

/**
 * [MirrorFlipper] 单元测试。
 *
 * 镜像翻转不依赖强度档位，始终返回固定的 hflip 滤镜字符串，
 * 用于对视频进行水平镜像翻转以改变像素排列。
 */
class MirrorFlipperTest {

    @Test
    fun buildFilter_返回hflip() {
        assertEquals("hflip", MirrorFlipper.buildFilter())
    }

    @Test
    fun buildFilter_多次调用结果一致() {
        // 确保无状态、幂等
        val first = MirrorFlipper.buildFilter()
        val second = MirrorFlipper.buildFilter()
        assertEquals(first, second)
        assertEquals("hflip", first)
    }

    @Test
    fun buildFilter_结果非空且为纯字母() {
        val filter = MirrorFlipper.buildFilter()
        assertTrue("结果不应为空", filter.isNotEmpty())
        assertTrue("结果应为纯字母: $filter", filter.matches(Regex("[a-zA-Z]+")))
    }

    @Test
    fun buildFilter_结果为水平翻转而非垂直翻转() {
        // 应为 hflip（水平翻转），而非 vflip（垂直翻转）
        val filter = MirrorFlipper.buildFilter()
        assertEquals("hflip", filter)
        assertNotEquals("vflip", filter)
    }
}

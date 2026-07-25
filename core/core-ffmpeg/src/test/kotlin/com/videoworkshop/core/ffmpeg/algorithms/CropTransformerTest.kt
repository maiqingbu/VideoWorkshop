package com.videoworkshop.core.ffmpeg.algorithms

import com.videoworkshop.core.ffmpeg.pipeline.DedupStrength
import org.junit.Assert.*
import org.junit.Test

/**
 * [CropTransformer] 单元测试。
 *
 * 验证 crop 滤镜字符串的生成，覆盖三个去重强度档位。
 * 裁剪量始终为偶数（2 * cropPixels），以保证编码器要求的偶数宽高。
 */
class CropTransformerTest {

    @Test
    fun buildFilter_light_裁剪2像素_总裁剪4() {
        // LIGHT: cropPixels = 2，总裁剪 4，偏移 (2, 2)
        assertEquals("crop=iw-4:ih-4:2:2", CropTransformer.buildFilter(DedupStrength.LIGHT))
    }

    @Test
    fun buildFilter_standard_裁剪3像素_总裁剪6() {
        // STANDARD: cropPixels = 3，总裁剪 6，偏移 (3, 3)
        assertEquals("crop=iw-6:ih-6:3:3", CropTransformer.buildFilter(DedupStrength.STANDARD))
    }

    @Test
    fun buildFilter_deep_裁剪4像素_总裁剪8() {
        // DEEP: cropPixels = 4，总裁剪 8，偏移 (4, 4)
        assertEquals("crop=iw-8:ih-8:4:4", CropTransformer.buildFilter(DedupStrength.DEEP))
    }

    @Test
    fun buildFilter_强度越高裁剪量越大() {
        // DEEP > STANDARD > LIGHT
        val light = CropTransformer.buildFilter(DedupStrength.LIGHT)
        val standard = CropTransformer.buildFilter(DedupStrength.STANDARD)
        val deep = CropTransformer.buildFilter(DedupStrength.DEEP)
        val lightTotal = light.substringAfter("iw-").substringBefore(":").toInt()
        val standardTotal = standard.substringAfter("iw-").substringBefore(":").toInt()
        val deepTotal = deep.substringAfter("iw-").substringBefore(":").toInt()
        assertTrue("DEEP 裁剪量应最大", deepTotal > standardTotal)
        assertTrue("STANDARD 裁剪量应大于 LIGHT", standardTotal > lightTotal)
    }

    @Test
    fun buildFilter_格式符合crop滤镜规范() {
        // 校验输出始终符合 crop=iw-W:ih-H:X:Y 的格式，且 W = H = 2 * X = 2 * Y
        DedupStrength.values().forEach { strength ->
            val filter = CropTransformer.buildFilter(strength)
            val crop = strength.cropPixels
            val total = 2 * crop
            assertEquals("crop=iw-$total:ih-$total:$crop:$crop", filter)
            assertTrue(filter.startsWith("crop=iw-"))
            assertTrue(filter.contains(":ih-"))
        }
    }

    @Test
    fun buildFilter_总裁剪量始终为偶数() {
        // 编码器要求偶数宽高，2 * cropPixels 必为偶数
        DedupStrength.values().forEach { strength ->
            val filter = CropTransformer.buildFilter(strength)
            // 解析 "crop=iw-{total}:ih-..." 中的 total
            val total = filter.substringAfter("crop=iw-").substringBefore(":").toInt()
            assertEquals("总裁剪量应为偶数: $total", 0, total % 2)
        }
    }

    @Test
    fun buildFilter_宽高裁剪量与偏移量一致() {
        // 宽方向与高方向的裁剪量应相等，且 x/y 偏移等于单边裁剪像素数
        DedupStrength.values().forEach { strength ->
            val filter = CropTransformer.buildFilter(strength)
            val parts = filter.removePrefix("crop=").split(":")
            assertEquals(4, parts.size)
            val widthCrop = parts[0] // iw-{total}
            val heightCrop = parts[1] // ih-{total}
            val x = parts[2].toInt()
            val y = parts[3].toInt()
            assertTrue(widthCrop.startsWith("iw-"))
            assertTrue(heightCrop.startsWith("ih-"))
            assertEquals(x, y)
            assertEquals(2 * x, widthCrop.removePrefix("iw-").toInt())
            assertEquals(2 * y, heightCrop.removePrefix("ih-").toInt())
        }
    }
}

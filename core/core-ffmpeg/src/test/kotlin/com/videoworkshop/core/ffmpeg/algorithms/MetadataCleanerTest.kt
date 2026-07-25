package com.videoworkshop.core.ffmpeg.algorithms

import org.junit.Assert.*
import org.junit.Test

/**
 * [MetadataCleaner] 单元测试。
 *
 * 验证返回的 FFmpeg 参数能同时清除全局元数据（-map_metadata -1）
 * 与章节信息（-map_chapters -1），防止平台通过元数据比对判定为重复。
 */
class MetadataCleanerTest {

    @Test
    fun buildParam_返回固定参数字符串() {
        assertEquals("-map_metadata -1 -map_chapters -1", MetadataCleaner.buildParam())
    }

    @Test
    fun buildParam_包含清除全局元数据选项() {
        val param = MetadataCleaner.buildParam()
        assertTrue("应包含 -map_metadata -1: $param", param.contains("-map_metadata -1"))
    }

    @Test
    fun buildParam_包含清除章节信息选项() {
        val param = MetadataCleaner.buildParam()
        assertTrue("应包含 -map_chapters -1: $param", param.contains("-map_chapters -1"))
    }

    @Test
    fun buildParam_同时包含两个清除选项() {
        // 两个选项必须同时存在，缺一不可
        val param = MetadataCleaner.buildParam()
        assertTrue(param.contains("-map_metadata -1"))
        assertTrue(param.contains("-map_chapters -1"))
    }

    @Test
    fun buildParam_多次调用结果一致() {
        // 无状态、幂等
        val first = MetadataCleaner.buildParam()
        val second = MetadataCleaner.buildParam()
        assertEquals(first, second)
    }

    @Test
    fun buildParam_清除值为负一() {
        // -1 表示清除，两个选项的值均应为 -1
        val param = MetadataCleaner.buildParam()
        assertTrue(param.contains("metadata -1"))
        assertTrue(param.contains("chapters -1"))
    }
}

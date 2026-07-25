package com.videoworkshop.core.ffmpeg.algorithms

import org.junit.Assert.*
import org.junit.Test

/**
 * [Md5Modifier] 单元测试。
 *
 * 验证返回的 FFmpeg 参数包含 bitexact 标志，用于移除编码器写入的
 * 确定性元数据与填充字节，进一步保证输出文件唯一性（改变文件 MD5）。
 */
class Md5ModifierTest {

    @Test
    fun buildParam_返回bitexact参数() {
        assertEquals("-fflags +bitexact", Md5Modifier.buildParam())
    }

    @Test
    fun buildParam_包含fflags选项() {
        val param = Md5Modifier.buildParam()
        assertTrue("应包含 -fflags: $param", param.contains("-fflags"))
    }

    @Test
    fun buildParam_包含bitexact标志() {
        val param = Md5Modifier.buildParam()
        assertTrue("应包含 +bitexact: $param", param.contains("+bitexact"))
    }

    @Test
    fun buildParam_标志为加号前缀() {
        // +bitexact 表示启用，而非 -bitexact 禁用
        val param = Md5Modifier.buildParam()
        assertTrue(param.contains("+bitexact"))
        assertFalse(param.contains("-bitexact"))
    }

    @Test
    fun buildParam_多次调用结果一致() {
        // 无状态、幂等
        val first = Md5Modifier.buildParam()
        val second = Md5Modifier.buildParam()
        assertEquals(first, second)
    }
}

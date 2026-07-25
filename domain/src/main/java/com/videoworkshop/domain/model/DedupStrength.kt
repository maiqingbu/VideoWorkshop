package com.videoworkshop.domain.model

/**
 * 去重强度档位。每个档位对应一组去重参数映射：
 *
 * 强度越高，各项去重手段的偏移量越大，去重效果越明显。
 *
 * @param fps              目标帧率（支持小数，如 29.97）
 * @param bitrateFactor    码率系数（相对原始码率的乘数，>1 表示提高码率）
 * @param cropPixels       裁剪像素数（每边裁切量）
 * @param hueShift         色相偏移角度（度）
 * @param saturationFactor 饱和度系数（1.0 = 不变，>1 增强饱和度）
 * @param eqGain           音频 EQ 增益（dB）
 * @param noiseAmplitude   底噪幅度（0.0 ~ 1.0）
 */
enum class DedupStrength(
    val fps: Double,
    val bitrateFactor: Float,
    val cropPixels: Int,
    val hueShift: Float,
    val saturationFactor: Float,
    val eqGain: Float,
    val noiseAmplitude: Float
) {
    LIGHT(
        fps = 29.97,
        bitrateFactor = 1.1f,
        cropPixels = 2,
        hueShift = 3f,
        saturationFactor = 1.03f,
        eqGain = 1f,
        noiseAmplitude = 0.002f
    ),
    STANDARD(
        fps = 29.0,
        bitrateFactor = 1.15f,
        cropPixels = 3,
        hueShift = 5f,
        saturationFactor = 1.05f,
        eqGain = 2f,
        noiseAmplitude = 0.003f
    ),
    DEEP(
        fps = 28.0,
        bitrateFactor = 1.2f,
        cropPixels = 4,
        hueShift = 8f,
        saturationFactor = 1.08f,
        eqGain = 3f,
        noiseAmplitude = 0.005f
    )
}

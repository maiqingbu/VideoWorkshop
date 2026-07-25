package com.videoworkshop.domain.model

/**
 * 视频去重配置。各项开关控制是否启用对应的去重手段，
 * [strength] 决定各手段的作用强度。
 *
 * @param md5Modify       是否修改文件 MD5（重封装）
 * @param fpsAdjust       是否调整帧率
 * @param bitrateModify   是否修改码率
 * @param cropTransform   是否进行裁剪变换
 * @param mirrorFlip      是否镜像/翻转
 * @param colorShift      是否色相/饱和度偏移
 * @param audioReshape    是否重塑音轨
 * @param metadataClean   是否清理元数据
 * @param strength        去重强度
 */
data class DedupConfig(
    val md5Modify: Boolean = true,
    val fpsAdjust: Boolean = true,
    val bitrateModify: Boolean = true,
    val cropTransform: Boolean = true,
    val mirrorFlip: Boolean = true,
    val colorShift: Boolean = true,
    val audioReshape: Boolean = true,
    val metadataClean: Boolean = true,
    val strength: DedupStrength = DedupStrength.STANDARD
)

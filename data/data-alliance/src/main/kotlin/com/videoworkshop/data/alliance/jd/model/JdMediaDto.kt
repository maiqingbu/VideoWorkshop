package com.videoworkshop.data.alliance.jd.model

import kotlinx.serialization.Serializable

/**
 * 京东联盟 `jd.union.open.goods.media.query` 响应信封。
 *
 * @param code    业务码，0 表示成功
 * @param message 业务提示信息
 * @param data    媒体（视频）列表
 */
@Serializable
data class JdMediaResponse(
    val code: Int? = null,
    val message: String? = null,
    val data: List<JdMediaDto> = emptyList(),
)

/**
 * 京东联盟商品媒体（视频）节点。
 *
 * @param videoUrl   视频下载地址
 * @param duration   视频时长（毫秒）
 * @param format     视频格式，例如 "mp4"
 * @param videoImage 视频封面图地址
 */
@Serializable
data class JdMediaDto(
    val videoUrl: String? = null,
    val duration: Long? = null,
    val format: String? = null,
    val videoImage: String? = null,
)

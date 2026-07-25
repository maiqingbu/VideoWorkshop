package com.videoworkshop.data.publish

/**
 * 发布过程中抛出的异常。
 *
 * @param message 异常描述信息
 */
data class PublishException(
    override val message: String
) : Exception(message)

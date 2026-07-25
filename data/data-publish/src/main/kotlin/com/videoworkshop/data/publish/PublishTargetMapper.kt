package com.videoworkshop.data.publish

import com.videoworkshop.domain.model.PublishTarget

/**
 * 发布目标平台 -> 包名映射器。
 *
 * 将领域层的 [PublishTarget] 枚举映射为对应平台 App 的包名，
 * 用于构造定向分享 Intent（setPackage）。
 *
 * 映射关系：
 * - [PublishTarget.DOUYIN]  -> com.ss.android.ugc.aweme
 * - [PublishTarget.KUAISHOU] -> com.smile.gifmaker
 * - [PublishTarget.XHS]      -> com.xingin.xhs
 */
object PublishTargetMapper {

    /**
     * 将 [PublishTarget] 映射为目标平台包名。
     *
     * @param target 目标平台
     * @return 对应 App 的包名
     */
    fun map(target: PublishTarget): String = when (target) {
        PublishTarget.DOUYIN -> "com.ss.android.ugc.aweme"
        PublishTarget.KUAISHOU -> "com.smile.gifmaker"
        PublishTarget.XHS -> "com.xingin.xhs"
    }
}

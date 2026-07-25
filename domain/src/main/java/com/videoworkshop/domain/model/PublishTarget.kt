package com.videoworkshop.domain.model

/**
 * 内容发布目标平台。
 *
 * @param displayName 展示名称
 * @param packageName 目标 App 包名，用于拉起发布
 */
enum class PublishTarget(
    val displayName: String,
    val packageName: String
) {
    DOUYIN(
        displayName = "抖音",
        packageName = "com.ss.android.ugc.aweme"
    ),
    KUAISHOU(
        displayName = "快手",
        packageName = "com.smile.gifmaker"
    ),
    XHS(
        displayName = "小红书",
        packageName = "com.xingin.xhs"
    )
}

package com.videoworkshop.domain.model

/**
 * 创作项目类型。
 *
 * 指项目要产出的内容形态，决定后续脚本、素材、渲染与发布链路。
 */
enum class ProjectType {
    /** 视频带货 */
    VIDEO_COMMERCE,

    /** 图文带货 */
    IMAGE_COMMERCE,

    /** AB 搬运（音画重组） */
    AB_RECOMPOSE,

    /** 视频二创加工 */
    VIDEO_REWORK,

    /** 长视频切片 */
    LONG_VIDEO_CLIP
}

/**
 * 创作项目生命周期状态。
 */
enum class ProjectStatus {
    /** 草稿 */
    DRAFT,

    /** 素材准备中 */
    PREPARING,

    /** 处理中 */
    PROCESSING,

    /** 可发布 */
    READY_TO_PUBLISH,

    /** 已发布 */
    PUBLISHED,

    /** 失败 */
    FAILED,

    /** 已归档 */
    ARCHIVED
}

/**
 * 创作项目聚合根。
 *
 * 一个项目负责关联商品快照、素材、脚本、任务、成片与发布记录。
 * 所有业务 ID 统一使用 [String] UUID，避免数据库与领域层出现双 ID 体系。
 *
 * @param id              项目唯一 ID（UUID 字符串）
 * @param title           项目标题
 * @param type            项目类型
 * @param status          项目状态
 * @param goodsSnapshotId 关联的商品快照 ID（可空）
 * @param targetPlatforms 目标发布平台集合
 * @param coverAssetId    封面素材 ID（可空）
 * @param createdAt       创建时间戳（毫秒）
 * @param updatedAt       最近更新时间戳（毫秒）
 * @param lastOpenedAt    最近打开时间戳（毫秒）
 */
data class Project(
    val id: String,
    val title: String,
    val type: ProjectType,
    val status: ProjectStatus,
    val goodsSnapshotId: String? = null,
    val targetPlatforms: Set<PublishTarget> = emptySet(),
    val coverAssetId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis()
)
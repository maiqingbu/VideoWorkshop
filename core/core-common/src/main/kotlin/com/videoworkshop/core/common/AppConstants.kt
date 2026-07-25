package com.videoworkshop.core.common

/**
 * 应用全局常量。
 *
 * 集中管理视频处理相关的限制阈值、数据库与偏好名称、网络超时等常量，
 * 便于各模块统一引用。
 */
object AppConstants {

    // ===== 视频处理限制 =====
    /** 单条视频最大时长（秒） */
    const val MAX_VIDEO_DURATION_SEC = 180

    /** 单条视频最大体积（MB） */
    const val MAX_VIDEO_SIZE_MB = 200

    /** 视频最大输出分辨率（短边像素） */
    const val MAX_RESOLUTION = 1080

    // ===== 文件与存储 =====
    /** 应用工作根目录名 */
    const val APP_DIR_NAME = "VideoWorkshop"

    const val SUBDIR_VIDEOS = "videos"
    const val SUBDIR_IMAGES = "images"
    const val SUBDIR_AUDIO = "audio"
    const val SUBDIR_OUTPUT = "output"
    const val SUBDIR_THUMBNAILS = "thumbnails"

    /** 视频处理通知渠道 */
    const val CHANNEL_VIDEO_PROCESS = "video_process"

    // ===== 数据库 =====
    const val DATABASE_NAME = "videoworkshop.db"
    const val DATABASE_VERSION = 1

    // ===== DataStore =====
    const val PREFERENCES_NAME = "videoworkshop_preferences"

    // ===== 网络 =====
    /** 连接超时（秒） */
    const val NETWORK_CONNECT_TIMEOUT_SEC = 30L

    /** 读取超时（秒） */
    const val NETWORK_READ_TIMEOUT_SEC = 60L

    /** 写入超时（秒） */
    const val NETWORK_WRITE_TIMEOUT_SEC = 60L

    /** 请求重试次数 */
    const val NETWORK_RETRY_COUNT = 3

    /** 重试间隔基数（毫秒） */
    const val NETWORK_RETRY_DELAY_MS = 500L
}

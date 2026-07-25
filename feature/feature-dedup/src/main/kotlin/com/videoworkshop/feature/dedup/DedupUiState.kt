package com.videoworkshop.feature.dedup

/**
 * 去重模式。
 *
 * - [QUICK]   一键去重：8 项算法全自动启用，隐藏自定义列表。
 * - [CUSTOM]  自定义：展示去重项目开关列表，由用户自由组合。
 */
enum class DedupMode {
    QUICK,
    CUSTOM
}

/**
 * 去重页面整体 UI 状态，用于驱动页面分区渲染。
 *
 * - [Idle]        空闲：可编辑配置并开始去重。
 * - [Processing]  处理中：展示进度与当前步骤文案。
 * - [Completed]   完成：展示产物路径并提供下一步入口。
 * - [Error]       错误：展示错误信息并可重试。
 */
sealed class DedupUiState {

    /** 空闲态：用户可调整去重配置。 */
    data object Idle : DedupUiState()

    /**
     * 处理中态。
     *
     * @param progress    总体进度（0.0 ~ 1.0）
     * @param currentStep 当前步骤文案，例如 "正在执行帧率调整"
     */
    data class Processing(
        val progress: Float,
        val currentStep: String
    ) : DedupUiState()

    /**
     * 完成态。
     *
     * @param outputPath 去重产物文件路径
     */
    data class Completed(val outputPath: String) : DedupUiState()

    /**
     * 错误态。
     *
     * @param message 错误描述
     */
    data class Error(val message: String) : DedupUiState()
}

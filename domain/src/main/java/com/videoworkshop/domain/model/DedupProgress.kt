package com.videoworkshop.domain.model

/**
 * 去重进度。
 *
 * @param currentStep 当前步骤描述
 * @param stepIndex   当前步骤索引（从 0 开始）
 * @param totalSteps  总步骤数
 * @param progress    总体进度（0.0 ~ 1.0）
 */
data class DedupProgress(
    val currentStep: String,
    val stepIndex: Int,
    val totalSteps: Int,
    val progress: Float
)

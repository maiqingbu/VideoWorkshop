package com.videoworkshop.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 处理任务实体（去重/增强/生成/发布等异步任务）。
 *
 * @param id          任务唯一标识。
 * @param type        任务类型：dedup / enhance / generate / publish。
 * @param inputPath   输入文件路径。
 * @param outputPath  输出文件路径（任务完成前可空）。
 * @param status      状态：pending / running / success / failed / cancelled。
 * @param progress    进度 0 - 100。
 * @param config      任务配置（JSON 字符串）。
 * @param createdAt   创建时间戳。
 * @param updatedAt   最近更新时间戳。
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val inputPath: String,
    val outputPath: String? = null,
    val status: String,
    val progress: Int = 0,
    val config: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

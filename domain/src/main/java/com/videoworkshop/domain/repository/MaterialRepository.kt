package com.videoworkshop.domain.repository

import com.videoworkshop.domain.model.MaterialEntity

/**
 * 素材库仓库。
 */
interface MaterialRepository {

    /**
     * 获取全部素材。
     */
    suspend fun getMaterials(): List<MaterialEntity>

    /**
     * 保存一个素材并返回入库后的实体。
     *
     * @param path   本地文件路径
     * @param source 素材来源描述
     * @param type   素材类型，例如 "video" / "image" / "audio"
     */
    suspend fun saveMaterial(path: String, source: String, type: String): MaterialEntity

    /**
     * 删除指定 ID 的素材。
     */
    suspend fun deleteMaterial(id: Long)
}

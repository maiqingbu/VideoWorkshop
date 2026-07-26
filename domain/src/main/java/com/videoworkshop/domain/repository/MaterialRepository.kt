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
     * @param path      本地文件路径或内容 URI 字符串
     * @param source    素材来源描述
     * @param type      素材类型，例如 "video" / "image" / "audio"
     * @param thumbnail 缩略图本地路径（可选）
     */
    suspend fun saveMaterial(
        path: String,
        source: String,
        type: String,
        thumbnail: String? = null
    ): MaterialEntity

    /**
     * 更新素材的编辑字段（名称标签等元信息）。
     *
     * @param id    素材 ID
     * @param tags  新标签集合
     * @param note  新备注
     */
    suspend fun updateMaterial(id: Long, tags: List<String>, note: String)

    /**
     * 批量删除指定 ID 的素材。
     *
     * @param ids 待删除素材 ID 集合
     */
    suspend fun deleteMaterials(ids: Set<Long>)

    /**
     * 删除指定 ID 的素材。
     */
    suspend fun deleteMaterial(id: Long)
}

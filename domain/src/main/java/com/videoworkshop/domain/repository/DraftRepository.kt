package com.videoworkshop.domain.repository

import com.videoworkshop.domain.model.Draft

/**
 * 草稿仓库。
 */
interface DraftRepository {

    /**
     * 保存草稿，返回草稿 ID。
     */
    suspend fun saveDraft(draft: Draft): Long

    /**
     * 获取全部草稿。
     */
    suspend fun getDrafts(): List<Draft>

    /**
     * 删除指定草稿。
     */
    suspend fun deleteDraft(id: Long)
}

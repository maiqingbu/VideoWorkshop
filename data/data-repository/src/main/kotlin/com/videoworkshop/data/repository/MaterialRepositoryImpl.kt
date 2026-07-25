package com.videoworkshop.data.repository

import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.core.database.dao.MaterialDao
import com.videoworkshop.core.database.entity.MaterialEntity as DbMaterialEntity
import com.videoworkshop.domain.model.MaterialEntity
import com.videoworkshop.domain.repository.MaterialRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [MaterialRepository] 实现：基于 Room [MaterialDao] 的素材库读写。
 *
 * 领域层 [MaterialEntity]（id: Long）与数据库层
 * [DbMaterialEntity]（id: String）之间存在 ID 类型差异，
 * 本实现通过 toString / toLongOrNull 完成双向转换。
 *
 * @param materialDao Room 素材 DAO
 * @param dispatchers  协程调度器
 */
class MaterialRepositoryImpl @Inject constructor(
    private val materialDao: MaterialDao,
    private val dispatchers: DispatcherProvider
) : MaterialRepository {

    override suspend fun getMaterials(): List<MaterialEntity> =
        withContext(dispatchers.io) {
            // MaterialDao.observeAll 返回 Flow，通过 first() 获取当前快照
            materialDao.observeAll().first().map { it.toDomain() }
        }

    override suspend fun saveMaterial(
        path: String,
        source: String,
        type: String
    ): MaterialEntity = withContext(dispatchers.io) {
        val id = System.currentTimeMillis()
        val now = System.currentTimeMillis()

        val dbEntity = DbMaterialEntity(
            id = id.toString(),
            localPath = path,
            source = source,
            type = type,
            thumbnail = null,
            tags = emptyList(),
            createdAt = now
        )
        materialDao.upsert(dbEntity)

        MaterialEntity(
            id = id,
            path = path,
            source = source,
            type = type,
            createdAt = now
        )
    }

    override suspend fun deleteMaterial(id: Long) = withContext(dispatchers.io) {
        materialDao.deleteById(id.toString())
    }

    /**
     * 数据库实体 -> 领域实体
     */
    private fun DbMaterialEntity.toDomain(): MaterialEntity = MaterialEntity(
        id = id.toLongOrNull() ?: 0L,
        path = localPath,
        source = source,
        type = type,
        createdAt = createdAt
    )
}

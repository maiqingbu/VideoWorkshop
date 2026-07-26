package com.videoworkshop.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.core.database.dao.MaterialDao
import com.videoworkshop.core.database.entity.MaterialEntity as DbMaterialEntity
import com.videoworkshop.domain.model.MaterialEntity
import com.videoworkshop.domain.repository.MaterialRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * [MaterialRepository] 实现：基于 Room [MaterialDao] 的素材库读写。
 *
 * 领域层 [MaterialEntity]（id: Long）与数据库层
 * [DbMaterialEntity]（id: String）之间存在 ID 类型差异，
 * 本实现通过 toString / toLongOrNull 完成双向转换。
 *
 * 仓库层负责把 content:// URI 复制到 App 私有目录（[Context.filesDir]），
 * 持久化本地路径，避免 URI 权限丢失后无法访问。
 *
 * @param context     应用上下文（用于 ContentResolver / 文件复制）
 * @param materialDao Room 素材 DAO
 * @param dispatchers  协程调度器
 */
class MaterialRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val materialDao: MaterialDao,
    private val dispatchers: DispatcherProvider
) : MaterialRepository {

    /** 素材文件在 App 私有目录下的子目录名。 */
    private val materialDir: File by lazy {
        File(context.filesDir, "materials").apply { if (!exists()) mkdirs() }
    }

    override suspend fun getMaterials(): List<MaterialEntity> =
        withContext(dispatchers.io) {
            // MaterialDao.observeAll 返回 Flow，通过 first() 获取当前快照
            materialDao.observeAll().first().map { it.toDomain() }
        }

    override suspend fun saveMaterial(
        path: String,
        source: String,
        type: String,
        thumbnail: String?
    ): MaterialEntity = withContext(dispatchers.io) {
        val id = System.currentTimeMillis()
        val now = System.currentTimeMillis()

        // 处理 content:// URI：复制到 App 私有目录，得到稳定本地路径
        val localPath = resolveLocalPath(path, id)

        val dbEntity = DbMaterialEntity(
            id = id.toString(),
            localPath = localPath,
            source = source,
            type = type,
            thumbnail = thumbnail,
            tags = emptyList(),
            note = "",
            createdAt = now
        )
        materialDao.upsert(dbEntity)

        MaterialEntity(
            id = id,
            path = localPath,
            source = source,
            type = type,
            thumbnail = thumbnail,
            tags = emptyList(),
            note = "",
            createdAt = now
        )
    }

    override suspend fun updateMaterial(id: Long, tags: List<String>, note: String) =
        withContext(dispatchers.io) {
            materialDao.updateMeta(id = id.toString(), tags = tags, note = note)
        }

    override suspend fun deleteMaterials(ids: Set<Long>) = withContext(dispatchers.io) {
        if (ids.isEmpty()) return@withContext
        materialDao.deleteByIds(ids.map { it.toString() })
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
        thumbnail = thumbnail,
        tags = tags,
        note = note,
        createdAt = createdAt
    )

    /**
     * 将 [path] 解析为稳定的本地路径：
     * - 若是 content:// URI，复制到 [materialDir] 并返回绝对路径
     * - 若已是本地路径，原样返回
     */
    private fun resolveLocalPath(path: String, id: Long): String {
        val uri = runCatching { Uri.parse(path) }.getOrNull() ?: return path
        if (uri.scheme?.equals("content", ignoreCase = true) != true) {
            // 已是本地 file:// 或绝对路径
            return if (uri.scheme?.equals("file", ignoreCase = true) == true) {
                uri.path ?: path
            } else {
                path
            }
        }
        return runCatching { copyUriToInternal(uri, id) }.getOrElse { path }
    }

    /**
     * 通过 ContentResolver 复制 content:// URI 到内部目录，返回新文件绝对路径。
     */
    private fun copyUriToInternal(uri: Uri, id: Long): String {
        val displayName = queryDisplayName(uri)
        val ext = displayName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        val suffix = if (ext.isNotEmpty()) ".$ext" else ""
        val target = File(materialDir, "material_${id}${suffix}")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return uri.toString()
        return target.absolutePath
    }

    /** 查询 content:// URI 的 DISPLAY_NAME，用于推导扩展名。 */
    private fun queryDisplayName(uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(0) ?: ""
                    } else ""
                } ?: ""
        }.getOrDefault("")
    }
}

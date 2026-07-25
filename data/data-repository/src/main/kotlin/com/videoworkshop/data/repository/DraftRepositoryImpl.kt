package com.videoworkshop.data.repository

import com.videoworkshop.core.common.DispatcherProvider
import com.videoworkshop.core.database.dao.DraftDao
import com.videoworkshop.core.database.entity.DraftEntity as DbDraftEntity
import com.videoworkshop.domain.model.ContentType
import com.videoworkshop.domain.model.Draft
import com.videoworkshop.domain.repository.DraftRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [DraftRepository] 实现：基于 Room [DraftDao] 的草稿读写。
 *
 * 领域层 [Draft] 与数据库层 [DbDraftEntity] 之间存在字段差异：
 * - ID: Long <-> String（通过 toString / toLongOrNull 转换）
 * - type: [ContentType] <-> String（VIDEO -> "video", IMAGE -> "image"）
 * - goodsId: String <-> String?（非空保证）
 * - mediaPaths: List<String> <-> 编码至 content 字段（分隔符存储）
 *
 * @param draftDao   Room 草稿 DAO
 * @param dispatchers 协程调度器
 */
class DraftRepositoryImpl @Inject constructor(
    private val draftDao: DraftDao,
    private val dispatchers: DispatcherProvider
) : DraftRepository {

    override suspend fun saveDraft(draft: Draft): Long =
        withContext(dispatchers.io) {
            val id = if (draft.id > 0L) draft.id else System.currentTimeMillis()
            val now = System.currentTimeMillis()

            // 将 mediaPaths 编码到 content 中
            val encodedContent = encodeContent(draft.content, draft.mediaPaths)

            val dbEntity = DbDraftEntity(
                id = id.toString(),
                type = draft.type.toDbType(),
                goodsId = draft.goodsId,
                content = encodedContent,
                createdAt = draft.createdAt.takeIf { it > 0L } ?: now,
                updatedAt = now
            )
            draftDao.upsert(dbEntity)

            id
        }

    override suspend fun getDrafts(): List<Draft> =
        withContext(dispatchers.io) {
            draftDao.observeAll().first().map { it.toDomain() }
        }

    override suspend fun deleteDraft(id: Long) =
        withContext(dispatchers.io) {
            draftDao.deleteById(id.toString())
        }

    // ===== 类型转换 =====

    /**
     * [ContentType] -> 数据库存储字符串
     */
    private fun ContentType.toDbType(): String = when (this) {
        ContentType.VIDEO -> "video"
        ContentType.IMAGE -> "image"
    }

    /**
     * 数据库字符串 -> [ContentType]
     */
    private fun String.toContentType(): ContentType = when (this) {
        "video" -> ContentType.VIDEO
        "image" -> ContentType.IMAGE
        else -> ContentType.VIDEO
    }

    /**
     * 数据库实体 -> 领域实体
     */
    private fun DbDraftEntity.toDomain(): Draft {
        val (content, mediaPaths) = decodeContent(this.content)
        return Draft(
            id = id.toLongOrNull() ?: 0L,
            type = type.toContentType(),
            goodsId = goodsId ?: "",
            content = content,
            mediaPaths = mediaPaths,
            createdAt = createdAt
        )
    }

    // ===== mediaPaths 编码/解码 =====

    /**
     * 将 mediaPaths 编码到 content 字段中。
     *
     * 格式：`{原始文案}{MEDIA_SEPARATOR}{path1}{PATH_SEPARATOR}path2...`
     * 无 mediaPaths 时不追加分隔符。
     */
    private fun encodeContent(content: String, mediaPaths: List<String>): String {
        if (mediaPaths.isEmpty()) return content
        return content + MEDIA_SEPARATOR + mediaPaths.joinToString(PATH_SEPARATOR)
    }

    /**
     * 从 content 字段中解码原始文案与 mediaPaths。
     */
    private fun decodeContent(encoded: String): Pair<String, List<String>> {
        val separatorIndex = encoded.indexOf(MEDIA_SEPARATOR)
        if (separatorIndex < 0) {
            return encoded to emptyList()
        }
        val content = encoded.substring(0, separatorIndex)
        val pathsStr = encoded.substring(separatorIndex + MEDIA_SEPARATOR.length)
        val paths = pathsStr.split(PATH_SEPARATOR).filter { it.isNotBlank() }
        return content to paths
    }

    private companion object {
        const val MEDIA_SEPARATOR = "\n<<<MEDIA_PATHS>>>\n"
        const val PATH_SEPARATOR = "|"
    }
}

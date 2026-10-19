package com.zensa.app.data.repository

import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.dao.ModuleDao
import com.zensa.app.data.dao.NoteDao
import com.zensa.app.data.entity.NoteEntity
import com.zensa.app.data.entity.SyncQueueEntity
import com.zensa.app.data.mapper.NoteMapper
import com.zensa.app.data.remote.ZensaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * NoteRepository — offline-first access to notes.
 */
class NoteRepository(
    private val dao: NoteDao,
    private val moduleDao: ModuleDao,
    private val api: ZensaApiService
) {

    var syncRepository: SyncRepository? = null

    fun observeForUser(userId: String?): Flow<List<NoteEntity>> =
        dao.observeForUser(userId)

    fun observeForModule(moduleLocalId: String): Flow<List<NoteEntity>> =
        dao.observeForModule(moduleLocalId)

    suspend fun getByLocalId(localId: String): NoteEntity? =
        dao.getByLocalId(localId)

    suspend fun search(userId: String?, q: String): List<NoteEntity> =
        dao.search(userId, q)

    suspend fun refresh(): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getNotes()
            if (response.isSuccessful && response.body() != null) {
                val entities = response.body()!!.map { dto ->
                    val moduleLocalId = dto.moduleId?.let {
                        moduleDao.getByServerId(it)?.localId
                    }
                    NoteMapper.fromDto(dto, moduleLocalId = moduleLocalId)
                }
                dao.upsertAll(entities)
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("Failed to refresh notes (${response.code()})")
            }
        } catch (e: IOException) {
            ApiResult.Error("Offline — showing saved notes")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun create(
        userId: String?,
        moduleLocalId: String?,
        title: String,
        content: String
    ): ApiResult<NoteEntity> = withContext(Dispatchers.IO) {

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val localId = NoteMapper.newLocalId()

        val local = NoteEntity(
            localId = localId,
            serverId = null,
            userId = userId,
            moduleLocalId = moduleLocalId,
            title = title.trim(),
            content = content.trim(),
            createdAt = now,
            updatedAt = now,
            isSynced = false
        )
        dao.upsert(local)

        try {
            val moduleServerId = moduleLocalId?.let {
                moduleDao.getByLocalId(it)?.serverId
            }
            val response = api.createNote(NoteMapper.toRequestDto(local, moduleServerId))
            if (response.isSuccessful && response.body() != null) {
                val serverEntity = NoteMapper.fromDto(
                    dto = response.body()!!,
                    localId = localId,
                    moduleLocalId = moduleLocalId
                )
                dao.upsert(serverEntity)
                return@withContext ApiResult.Success(serverEntity)
            }
        } catch (_: Exception) { }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_NOTE,
            localId,
            SyncQueueEntity.OP_CREATE
        )
        ApiResult.Success(local)
    }

    suspend fun update(
        localId: String,
        moduleLocalId: String?,
        title: String,
        content: String
    ): ApiResult<NoteEntity> = withContext(Dispatchers.IO) {

        val existing = dao.getByLocalId(localId)
            ?: return@withContext ApiResult.Error("Note not found")

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val updated = existing.copy(
            moduleLocalId = moduleLocalId,
            title = title.trim(),
            content = content.trim(),
            updatedAt = now,
            isSynced = false,
            localUpdatedAt = System.currentTimeMillis()
        )
        dao.update(updated)

        val serverId = existing.serverId
        if (serverId != null) {
            try {
                val moduleServerId = moduleLocalId?.let {
                    moduleDao.getByLocalId(it)?.serverId
                }
                api.updateNote(serverId, NoteMapper.toRequestDto(updated, moduleServerId))
                dao.markSynced(localId)
                return@withContext ApiResult.Success(updated)
            } catch (_: Exception) { }
        }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_NOTE,
            localId,
            SyncQueueEntity.OP_UPDATE
        )
        ApiResult.Success(updated)
    }

    suspend fun delete(localId: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val existing = dao.getByLocalId(localId)
            ?: return@withContext ApiResult.Error("Note not found")

        dao.softDelete(localId)

        val serverId = existing.serverId
        if (serverId != null) {
            try {
                api.deleteNote(serverId)
                dao.hardDelete(localId)
                return@withContext ApiResult.Success(Unit)
            } catch (_: Exception) { }
        } else {
            dao.hardDelete(localId)
            return@withContext ApiResult.Success(Unit)
        }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_NOTE,
            localId,
            SyncQueueEntity.OP_DELETE
        )
        ApiResult.Success(Unit)
    }
}

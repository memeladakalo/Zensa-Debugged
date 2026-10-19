package com.zensa.app.data.repository

import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.dao.ModuleDao
import com.zensa.app.data.dao.StudySessionDao
import com.zensa.app.data.entity.StudySessionEntity
import com.zensa.app.data.entity.SyncQueueEntity
import com.zensa.app.data.mapper.StudySessionMapper
import com.zensa.app.data.remote.ZensaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StudySessionRepository(
    private val dao: StudySessionDao,
    private val moduleDao: ModuleDao,
    private val api: ZensaApiService
) {

    var syncRepository: SyncRepository? = null

    fun observeForUser(userId: String?): Flow<List<StudySessionEntity>> =
        dao.observeForUser(userId)

    suspend fun getForUser(userId: String?): List<StudySessionEntity> =
        dao.getForUser(userId)

    suspend fun totalCompletedSeconds(userId: String?): Int =
        dao.totalCompletedSeconds(userId)

    suspend fun completedCount(userId: String?): Int =
        dao.completedCount(userId)

    suspend fun completedSince(sinceIso: String, userId: String?): List<StudySessionEntity> =
        dao.completedSince(sinceIso, userId)

    suspend fun refresh(): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getStudySessions()
            if (response.isSuccessful && response.body() != null) {
                val entities = response.body()!!.map { dto ->
                    val moduleLocalId = dto.moduleId?.let {
                        moduleDao.getByServerId(it)?.localId
                    }
                    StudySessionMapper.fromDto(dto, moduleLocalId = moduleLocalId)
                }
                dao.upsertAll(entities)
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("Failed to refresh sessions (${response.code()})")
            }
        } catch (e: IOException) {
            ApiResult.Error("Offline — showing saved sessions")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun recordCompletedSession(
        userId: String?,
        moduleLocalId: String?,
        startedAt: String,
        endedAt: String,
        durationSeconds: Int
    ): ApiResult<StudySessionEntity> = withContext(Dispatchers.IO) {

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val localId = StudySessionMapper.newLocalId()

        val local = StudySessionEntity(
            localId = localId,
            serverId = null,
            userId = userId,
            moduleLocalId = moduleLocalId,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            completed = true,
            createdAt = now,
            updatedAt = now,
            isSynced = false
        )
        dao.upsert(local)

        try {
            val moduleServerId = moduleLocalId?.let {
                moduleDao.getByLocalId(it)?.serverId
            }
            val response = api.createStudySession(
                StudySessionMapper.toRequestDto(local, moduleServerId)
            )
            if (response.isSuccessful && response.body() != null) {
                val serverEntity = StudySessionMapper.fromDto(
                    dto = response.body()!!,
                    localId = localId,
                    moduleLocalId = moduleLocalId
                )
                dao.upsert(serverEntity)
                return@withContext ApiResult.Success(serverEntity)
            }
        } catch (_: Exception) { }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_STUDY_SESSION,
            localId,
            SyncQueueEntity.OP_CREATE
        )
        ApiResult.Success(local)
    }

    suspend fun delete(localId: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val existing = dao.getByLocalId(localId)
            ?: return@withContext ApiResult.Error("Session not found")

        dao.softDelete(localId)

        val serverId = existing.serverId
        if (serverId != null) {
            try {
                api.deleteStudySession(serverId)
                dao.hardDelete(localId)
                return@withContext ApiResult.Success(Unit)
            } catch (_: Exception) { }
        } else {
            dao.hardDelete(localId)
            return@withContext ApiResult.Success(Unit)
        }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_STUDY_SESSION,
            localId,
            SyncQueueEntity.OP_DELETE
        )
        ApiResult.Success(Unit)
    }
}

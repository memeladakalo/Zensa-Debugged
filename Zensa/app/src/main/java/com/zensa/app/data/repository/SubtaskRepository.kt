package com.zensa.app.data.repository

import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.dao.AssignmentDao
import com.zensa.app.data.dao.SubtaskDao
import com.zensa.app.data.entity.SubtaskEntity
import com.zensa.app.data.entity.SyncQueueEntity
import com.zensa.app.data.mapper.SubtaskMapper
import com.zensa.app.data.remote.ZensaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SubtaskRepository(
    private val dao: SubtaskDao,
    private val assignmentDao: AssignmentDao,
    private val api: ZensaApiService
) {

    var syncRepository: SyncRepository? = null

    fun observeSubtasks(assignmentLocalId: String): Flow<List<SubtaskEntity>> =
        dao.observeForAssignment(assignmentLocalId)

    suspend fun getForAssignment(assignmentLocalId: String): List<SubtaskEntity> =
        dao.getForAssignment(assignmentLocalId)

    suspend fun getByLocalId(localId: String): SubtaskEntity? =
        dao.getByLocalId(localId)

    suspend fun refreshForAssignment(assignmentLocalId: String): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val assignment = assignmentDao.getByLocalId(assignmentLocalId)
                    ?: return@withContext ApiResult.Error("Assignment not found")
                val serverAssignmentId = assignment.serverId
                    ?: return@withContext ApiResult.Success(Unit)

                val response = api.getSubtasks(serverAssignmentId)
                if (response.isSuccessful && response.body() != null) {
                    val entities = response.body()!!.map {
                        SubtaskMapper.fromDto(it, assignmentLocalId)
                    }
                    dao.upsertAll(entities)
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error("Failed to refresh subtasks (${response.code()})")
                }
            } catch (e: IOException) {
                ApiResult.Error("Offline — showing saved subtasks")
            } catch (e: Exception) {
                ApiResult.Error("Unexpected error: ${e.message}")
            }
        }

    suspend fun create(
        assignmentLocalId: String,
        title: String
    ): ApiResult<SubtaskEntity> = withContext(Dispatchers.IO) {

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val localId = SubtaskMapper.newLocalId()
        val position = dao.countForAssignment(assignmentLocalId)

        val local = SubtaskEntity(
            localId = localId,
            serverId = null,
            assignmentLocalId = assignmentLocalId,
            title = title.trim(),
            position = position,
            isCompleted = false,
            createdAt = now,
            updatedAt = now,
            isSynced = false
        )
        dao.upsert(local)

        try {
            val assignment = assignmentDao.getByLocalId(assignmentLocalId)
            val serverAssignmentId = assignment?.serverId
            if (serverAssignmentId != null) {
                val response = api.createSubtask(
                    serverAssignmentId,
                    SubtaskMapper.toRequestDto(local)
                )
                if (response.isSuccessful && response.body() != null) {
                    val serverEntity = SubtaskMapper.fromDto(
                        response.body()!!,
                        assignmentLocalId = assignmentLocalId,
                        localId = localId
                    )
                    dao.upsert(serverEntity)
                    return@withContext ApiResult.Success(serverEntity)
                }
            }
        } catch (_: Exception) { }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_SUBTASK,
            localId,
            SyncQueueEntity.OP_CREATE
        )
        ApiResult.Success(local)
    }

    suspend fun update(
        localId: String,
        title: String
    ): ApiResult<SubtaskEntity> = withContext(Dispatchers.IO) {

        val existing = dao.getByLocalId(localId)
            ?: return@withContext ApiResult.Error("Subtask not found")

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val updated = existing.copy(
            title = title.trim(),
            updatedAt = now,
            isSynced = false,
            localUpdatedAt = System.currentTimeMillis()
        )
        dao.update(updated)

        val serverId = existing.serverId
        if (serverId != null) {
            try {
                api.updateSubtask(serverId, SubtaskMapper.toRequestDto(updated))
                dao.markSynced(localId)
                return@withContext ApiResult.Success(updated)
            } catch (_: Exception) { }
        }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_SUBTASK,
            localId,
            SyncQueueEntity.OP_UPDATE
        )
        ApiResult.Success(updated)
    }

    suspend fun setCompleted(
        localId: String,
        completed: Boolean
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {

        val existing = dao.getByLocalId(localId)
            ?: return@withContext ApiResult.Error("Subtask not found")

        dao.setCompleted(localId, completed)

        val serverId = existing.serverId
        if (serverId != null) {
            try {
                val refreshed = dao.getByLocalId(localId) ?: existing
                api.updateSubtask(serverId, SubtaskMapper.toRequestDto(refreshed))
                dao.markSynced(localId)
                return@withContext ApiResult.Success(Unit)
            } catch (_: Exception) { }
        }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_SUBTASK,
            localId,
            SyncQueueEntity.OP_UPDATE
        )
        ApiResult.Success(Unit)
    }

    suspend fun delete(localId: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val existing = dao.getByLocalId(localId)
            ?: return@withContext ApiResult.Error("Subtask not found")

        dao.softDelete(localId)

        val serverId = existing.serverId
        if (serverId != null) {
            try {
                api.deleteSubtask(serverId)
                dao.hardDelete(localId)
                return@withContext ApiResult.Success(Unit)
            } catch (_: Exception) { }
        } else {
            dao.hardDelete(localId)
            return@withContext ApiResult.Success(Unit)
        }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_SUBTASK,
            localId,
            SyncQueueEntity.OP_DELETE
        )
        ApiResult.Success(Unit)
    }
}

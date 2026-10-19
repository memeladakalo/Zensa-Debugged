package com.zensa.app.data.repository

import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.dao.AssignmentDao
import com.zensa.app.data.dao.ModuleDao
import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.data.entity.SyncQueueEntity
import com.zensa.app.data.mapper.AssignmentMapper
import com.zensa.app.data.remote.ZensaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AssignmentRepository(
    private val dao: AssignmentDao,
    private val moduleDao: ModuleDao,
    private val api: ZensaApiService
) {

    var syncRepository: SyncRepository? = null

    fun observeAssignments(userId: String?): Flow<List<AssignmentEntity>> =
        dao.observeForUser(userId)

    fun observeAssignmentsForModule(moduleLocalId: String): Flow<List<AssignmentEntity>> =
        dao.observeForModule(moduleLocalId)

    fun observePending(): Flow<List<AssignmentEntity>> = dao.observePending()

    suspend fun getByLocalId(localId: String): AssignmentEntity? =
        dao.getByLocalId(localId)

    suspend fun refresh(): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAssignments()
            if (response.isSuccessful && response.body() != null) {
                val entities = response.body()!!.map { dto ->
                    val moduleLocalId = dto.moduleId?.let {
                        moduleDao.getByServerId(it)?.localId
                    }
                    AssignmentMapper.fromDto(dto, moduleLocalId = moduleLocalId)
                }
                dao.upsertAll(entities)
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("Failed to refresh assignments (${response.code()})")
            }
        } catch (e: IOException) {
            ApiResult.Error("Offline — showing saved assignments")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun create(
        userId: String?,
        moduleLocalId: String?,
        title: String,
        description: String,
        priority: String,
        status: String,
        dueDate: String
    ): ApiResult<AssignmentEntity> = withContext(Dispatchers.IO) {

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val localId = AssignmentMapper.newLocalId()

        val local = AssignmentEntity(
            localId = localId,
            serverId = null,
            userId = userId,
            moduleLocalId = moduleLocalId,
            title = title.trim(),
            description = description.trim(),
            priority = priority,
            status = status,
            dueDate = dueDate,
            isCompleted = false,
            createdAt = now,
            updatedAt = now,
            isSynced = false
        )
        dao.upsert(local)

        try {
            val moduleServerId = moduleLocalId?.let {
                moduleDao.getByLocalId(it)?.serverId
            }
            val response = api.createAssignment(
                AssignmentMapper.toRequestDto(local, moduleServerId)
            )
            if (response.isSuccessful && response.body() != null) {
                val serverEntity = AssignmentMapper.fromDto(
                    dto = response.body()!!,
                    localId = localId,
                    moduleLocalId = moduleLocalId
                )
                dao.upsert(serverEntity)
                return@withContext ApiResult.Success(serverEntity)
            }
        } catch (_: Exception) { }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_ASSIGNMENT,
            localId,
            SyncQueueEntity.OP_CREATE
        )
        ApiResult.Success(local)
    }

    suspend fun update(
        localId: String,
        moduleLocalId: String?,
        title: String,
        description: String,
        priority: String,
        status: String,
        dueDate: String,
        isCompleted: Boolean
    ): ApiResult<AssignmentEntity> = withContext(Dispatchers.IO) {

        val existing = dao.getByLocalId(localId)
            ?: return@withContext ApiResult.Error("Assignment not found")

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val updated = existing.copy(
            moduleLocalId = moduleLocalId,
            title = title.trim(),
            description = description.trim(),
            priority = priority,
            status = status,
            dueDate = dueDate,
            isCompleted = isCompleted,
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
                api.updateAssignment(
                    serverId,
                    AssignmentMapper.toRequestDto(updated, moduleServerId)
                )
                dao.markSynced(localId)
                return@withContext ApiResult.Success(updated)
            } catch (_: Exception) { }
        }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_ASSIGNMENT,
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
            ?: return@withContext ApiResult.Error("Assignment not found")

        dao.setCompleted(localId, completed)

        val serverId = existing.serverId
        if (serverId != null) {
            try {
                val refreshed = dao.getByLocalId(localId) ?: existing
                val moduleServerId = refreshed.moduleLocalId?.let {
                    moduleDao.getByLocalId(it)?.serverId
                }
                api.updateAssignment(
                    serverId,
                    AssignmentMapper.toRequestDto(refreshed, moduleServerId)
                )
                dao.markSynced(localId)
                return@withContext ApiResult.Success(Unit)
            } catch (_: Exception) { }
        }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_ASSIGNMENT,
            localId,
            SyncQueueEntity.OP_UPDATE
        )
        ApiResult.Success(Unit)
    }

    suspend fun delete(localId: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val existing = dao.getByLocalId(localId)
            ?: return@withContext ApiResult.Error("Assignment not found")

        dao.softDelete(localId)

        val serverId = existing.serverId
        if (serverId != null) {
            try {
                api.deleteAssignment(serverId)
                dao.hardDelete(localId)
                return@withContext ApiResult.Success(Unit)
            } catch (_: Exception) { }
        } else {
            dao.hardDelete(localId)
            return@withContext ApiResult.Success(Unit)
        }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_ASSIGNMENT,
            localId,
            SyncQueueEntity.OP_DELETE
        )
        ApiResult.Success(Unit)
    }
}

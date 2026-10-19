package com.zensa.app.data.repository

import com.zensa.app.core.network.ApiResult
import com.zensa.app.core.network.NetworkMonitor
import com.zensa.app.data.dao.AssignmentDao
import com.zensa.app.data.dao.ModuleDao
import com.zensa.app.data.dao.NoteDao
import com.zensa.app.data.dao.StudySessionDao
import com.zensa.app.data.dao.SubtaskDao
import com.zensa.app.data.dao.SyncQueueDao
import com.zensa.app.data.entity.SyncQueueEntity
import com.zensa.app.data.mapper.SyncMapper
import com.zensa.app.data.remote.ZensaApiService
import com.zensa.app.data.remote.dto.SyncRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * SyncRepository
 *
 * Two responsibilities:
 *   1. enqueue(...)  — called by other repositories after every
 *                      offline-capable write, to record the pending
 *                      operation in the sync queue.
 *   2. pushPending() — called by the SyncWorker when the device is
 *                      online, to send the queue to POST /api/sync
 *                      and process the response.
 */
class SyncRepository(
    private val appContext: android.content.Context,
    private val queueDao: SyncQueueDao,
    private val moduleDao: ModuleDao,
    private val assignmentDao: AssignmentDao,
    private val subtaskDao: SubtaskDao,
    private val studySessionDao: StudySessionDao,
    private val noteDao: NoteDao,
    private val api: ZensaApiService,
    private val networkMonitor: NetworkMonitor
) {

    private val json = Json { encodeDefaults = true }
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // ---------- Observable state ----------

    val pendingCount: Flow<Int> = queueDao.observePendingCount()

    fun isOnline(): Boolean = networkMonitor.isOnline()

    // ---------- Enqueue ----------

    suspend fun enqueue(
        entityType: String,
        entityLocalId: String,
        operation: String
    ) = withContext(Dispatchers.IO) {
        val item = SyncQueueEntity(
            queueId = UUID.randomUUID().toString(),
            entityType = entityType,
            entityLocalId = entityLocalId,
            operation = operation,
            createdAt = LocalDateTime.now().format(iso)
        )
        queueDao.enqueue(item)
    }

    /**
     * Enqueues the operation AND asks WorkManager to run a one-shot
     * sync as soon as the network is available.
     */
    suspend fun enqueueAndSchedule(
        entityType: String,
        entityLocalId: String,
        operation: String
    ) {
        enqueue(entityType, entityLocalId, operation)
        com.zensa.app.sync.SyncScheduler.requestImmediateSync(appContext)
    }

    // ---------- Push ----------

    suspend fun pushPending(): ApiResult<Int> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline()) {
            return@withContext ApiResult.Error("Offline — will retry later")
        }

        val pending = queueDao.nextBatch()
        if (pending.isEmpty()) return@withContext ApiResult.Success(0)

        val operations = pending.mapNotNull { item ->
            val payload = buildPayload(item) ?: return@mapNotNull null
            val serverId = lookupServerId(item)
            SyncMapper.toOperationDto(item, serverId, payload)
        }

        if (operations.isEmpty()) return@withContext ApiResult.Success(0)

        try {
            val response = api.sync(SyncRequestDto(operations))
            if (!response.isSuccessful || response.body() == null) {
                return@withContext ApiResult.Error("Sync failed (${response.code()})")
            }

            var applied = 0
            for (result in response.body()!!.results) {
                val item = queueDao.getById(result.queueId) ?: continue
                if (result.success) {
                    applyServerId(item, result.serverId)
                    queueDao.remove(item.queueId)
                    applied++
                } else {
                    handleFailure(item, result.error ?: "Unknown error")
                }
            }
            ApiResult.Success(applied)
        } catch (e: IOException) {
            ApiResult.Error("Offline — will retry later")
        } catch (e: Exception) {
            ApiResult.Error("Sync error: ${e.message}")
        }
    }

    // ---------- Internals ----------

    private suspend fun buildPayload(item: SyncQueueEntity): String? {
        if (item.operation == SyncQueueEntity.OP_DELETE) return null

        return when (item.entityType) {
            SyncQueueEntity.TYPE_MODULE -> {
                val m = moduleDao.getByLocalId(item.entityLocalId) ?: return null
                json.encodeToString(
                    ModulePayload.serializer(),
                    ModulePayload(
                        moduleCode = m.moduleCode,
                        name = m.name,
                        description = m.description,
                        colour = m.colour
                    )
                )
            }
            SyncQueueEntity.TYPE_ASSIGNMENT -> {
                val a = assignmentDao.getByLocalId(item.entityLocalId) ?: return null
                val moduleServerId = a.moduleLocalId?.let {
                    moduleDao.getByLocalId(it)?.serverId
                }
                json.encodeToString(
                    AssignmentPayload.serializer(),
                    AssignmentPayload(
                        moduleId = moduleServerId,
                        title = a.title,
                        description = a.description,
                        priority = a.priority,
                        status = a.status,
                        dueDate = a.dueDate,
                        isCompleted = a.isCompleted
                    )
                )
            }
            SyncQueueEntity.TYPE_SUBTASK -> {
                val s = subtaskDao.getByLocalId(item.entityLocalId) ?: return null
                val parentAssignmentServerId = assignmentDao
                    .getByLocalId(s.assignmentLocalId)?.serverId
                json.encodeToString(
                    SubtaskPayload.serializer(),
                    SubtaskPayload(
                        assignmentServerId = parentAssignmentServerId,
                        title = s.title,
                        position = s.position,
                        isCompleted = s.isCompleted
                    )
                )
            }
            SyncQueueEntity.TYPE_STUDY_SESSION -> {
                val s = studySessionDao.getByLocalId(item.entityLocalId) ?: return null
                val moduleServerId = s.moduleLocalId?.let {
                    moduleDao.getByLocalId(it)?.serverId
                }
                json.encodeToString(
                    StudySessionPayload.serializer(),
                    StudySessionPayload(
                        moduleId = moduleServerId,
                        startedAt = s.startedAt,
                        endedAt = s.endedAt,
                        durationSeconds = s.durationSeconds,
                        completed = s.completed
                    )
                )
            }
            SyncQueueEntity.TYPE_NOTE -> {
                val n = noteDao.getByLocalId(item.entityLocalId) ?: return null
                val moduleServerId = n.moduleLocalId?.let {
                    moduleDao.getByLocalId(it)?.serverId
                }
                json.encodeToString(
                    NotePayload.serializer(),
                    NotePayload(
                        moduleId = moduleServerId,
                        title = n.title,
                        content = n.content,
                        attachmentUrl = n.attachmentUrl
                    )
                )
            }
            else -> null
        }
    }

    private suspend fun lookupServerId(item: SyncQueueEntity): String? =
        when (item.entityType) {
            SyncQueueEntity.TYPE_MODULE ->
                moduleDao.getByLocalId(item.entityLocalId)?.serverId
            SyncQueueEntity.TYPE_ASSIGNMENT ->
                assignmentDao.getByLocalId(item.entityLocalId)?.serverId
            SyncQueueEntity.TYPE_SUBTASK ->
                subtaskDao.getByLocalId(item.entityLocalId)?.serverId
            SyncQueueEntity.TYPE_STUDY_SESSION ->
                studySessionDao.getByLocalId(item.entityLocalId)?.serverId
            SyncQueueEntity.TYPE_NOTE ->
                noteDao.getByLocalId(item.entityLocalId)?.serverId
            else -> null
        }

    private suspend fun applyServerId(item: SyncQueueEntity, serverId: String?) {
        if (serverId == null) return
        when (item.entityType) {
            SyncQueueEntity.TYPE_MODULE -> {
                val row = moduleDao.getByLocalId(item.entityLocalId) ?: return
                moduleDao.upsert(row.copy(serverId = serverId, isSynced = true))
            }
            SyncQueueEntity.TYPE_ASSIGNMENT -> {
                val row = assignmentDao.getByLocalId(item.entityLocalId) ?: return
                assignmentDao.upsert(row.copy(serverId = serverId, isSynced = true))
            }
            SyncQueueEntity.TYPE_SUBTASK -> {
                val row = subtaskDao.getByLocalId(item.entityLocalId) ?: return
                subtaskDao.upsert(row.copy(serverId = serverId, isSynced = true))
            }
            SyncQueueEntity.TYPE_STUDY_SESSION -> {
                val row = studySessionDao.getByLocalId(item.entityLocalId) ?: return
                studySessionDao.upsert(row.copy(serverId = serverId, isSynced = true))
            }
            SyncQueueEntity.TYPE_NOTE -> {
                val row = noteDao.getByLocalId(item.entityLocalId) ?: return
                noteDao.upsert(row.copy(serverId = serverId, isSynced = true))
            }
        }
    }

    private suspend fun handleFailure(item: SyncQueueEntity, error: String) {
        val nextRetry = item.retryCount + 1
        val failed = nextRetry >= SyncQueueEntity.MAX_RETRIES
        queueDao.update(
            item.copy(
                retryCount = nextRetry,
                lastError = error,
                failed = failed
            )
        )
    }
}

// ---------- Payloads sent to the server ----------

@kotlinx.serialization.Serializable
data class ModulePayload(
    val moduleCode: String,
    val name: String,
    val description: String,
    val colour: String
)

@kotlinx.serialization.Serializable
data class AssignmentPayload(
    val moduleId: String?,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val dueDate: String,
    val isCompleted: Boolean
)

@kotlinx.serialization.Serializable
data class SubtaskPayload(
    val assignmentServerId: String?,
    val title: String,
    val position: Int,
    val isCompleted: Boolean
)

@kotlinx.serialization.Serializable
data class StudySessionPayload(
    val moduleId: String?,
    val startedAt: String,
    val endedAt: String?,
    val durationSeconds: Int,
    val completed: Boolean
)

@kotlinx.serialization.Serializable
data class NotePayload(
    val moduleId: String?,
    val title: String,
    val content: String,
    val attachmentUrl: String?
)

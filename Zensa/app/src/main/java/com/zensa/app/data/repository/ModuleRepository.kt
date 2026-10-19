package com.zensa.app.data.repository

import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.dao.ModuleDao
import com.zensa.app.data.entity.ModuleEntity
import com.zensa.app.data.entity.SyncQueueEntity
import com.zensa.app.data.mapper.ModuleMapper
import com.zensa.app.data.remote.ZensaApiService
import com.zensa.app.data.remote.dto.ModuleRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ModuleRepository(
    private val dao: ModuleDao,
    private val api: ZensaApiService
) {

    /**
     * Set by ServiceLocator after both repositories exist.
     * Kept as a nullable field to avoid a circular constructor
     * dependency between Module and Sync repositories.
     */
    var syncRepository: SyncRepository? = null

    fun observeModules(userId: String?): Flow<List<ModuleEntity>> =
        dao.observeForUser(userId)

    suspend fun refresh(userId: String?): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getModules()
            if (response.isSuccessful && response.body() != null) {
                val entities = response.body()!!.map { ModuleMapper.fromDto(it) }
                dao.upsertAll(entities)
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("Failed to refresh modules (${response.code()})")
            }
        } catch (e: IOException) {
            ApiResult.Error("Offline — showing saved modules")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun create(
        userId: String?,
        moduleCode: String,
        name: String,
        description: String,
        colour: String
    ): ApiResult<ModuleEntity> = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val localId = ModuleMapper.newLocalId()

        val local = ModuleEntity(
            localId = localId,
            serverId = null,
            userId = userId,
            moduleCode = moduleCode.trim().uppercase(),
            name = name.trim(),
            description = description.trim(),
            colour = colour,
            createdAt = now,
            updatedAt = now,
            isSynced = false
        )
        dao.upsert(local)

        // Best-effort immediate push.
        try {
            val response = api.createModule(
                ModuleRequestDto(
                    moduleCode = local.moduleCode,
                    name = local.name,
                    description = local.description,
                    colour = local.colour
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val serverEntity = ModuleMapper.fromDto(response.body()!!, localId)
                dao.upsert(serverEntity)
                return@withContext ApiResult.Success(serverEntity)
            }
        } catch (_: Exception) { }

        // Failed — enqueue for the background sync worker.
        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_MODULE,
            localId,
            SyncQueueEntity.OP_CREATE
        )
        ApiResult.Success(local)
    }

    suspend fun update(
        localId: String,
        moduleCode: String,
        name: String,
        description: String,
        colour: String
    ): ApiResult<ModuleEntity> = withContext(Dispatchers.IO) {
        val existing = dao.getByLocalId(localId)
            ?: return@withContext ApiResult.Error("Module not found")

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val updated = existing.copy(
            moduleCode = moduleCode.trim().uppercase(),
            name = name.trim(),
            description = description.trim(),
            colour = colour,
            updatedAt = now,
            isSynced = false,
            localUpdatedAt = System.currentTimeMillis()
        )
        dao.update(updated)

        val serverId = existing.serverId
        if (serverId != null) {
            try {
                api.updateModule(
                    serverId,
                    ModuleRequestDto(
                        moduleCode = updated.moduleCode,
                        name = updated.name,
                        description = updated.description,
                        colour = updated.colour
                    )
                )
                dao.markSynced(localId)
                return@withContext ApiResult.Success(updated)
            } catch (_: Exception) { }
        }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_MODULE,
            localId,
            SyncQueueEntity.OP_UPDATE
        )
        ApiResult.Success(updated)
    }

    suspend fun delete(localId: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val existing = dao.getByLocalId(localId)
            ?: return@withContext ApiResult.Error("Module not found")

        dao.softDelete(localId)

        val serverId = existing.serverId
        if (serverId != null) {
            try {
                api.deleteModule(serverId)
                dao.hardDelete(localId)
                return@withContext ApiResult.Success(Unit)
            } catch (_: Exception) { }
        } else {
            dao.hardDelete(localId)
            return@withContext ApiResult.Success(Unit)
        }

        syncRepository?.enqueueAndSchedule(
            SyncQueueEntity.TYPE_MODULE,
            localId,
            SyncQueueEntity.OP_DELETE
        )
        ApiResult.Success(Unit)
    }
}

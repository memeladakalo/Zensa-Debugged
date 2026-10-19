package com.zensa.app.data.mapper

import com.zensa.app.data.entity.SyncQueueEntity
import com.zensa.app.data.remote.dto.SyncOperationDto
import kotlinx.serialization.json.Json

/**
 * SyncMapper — converts a SyncQueueEntity into a SyncOperationDto
 * for the server.
 *
 * The `payload` field is a JSON string produced by whichever
 * repository enqueued the operation (module, assignment, etc.).
 * The mapper is agnostic to that content.
 */
object SyncMapper {

    private val json = Json { encodeDefaults = true }

    fun toOperationDto(
        entity: SyncQueueEntity,
        serverId: String?,
        payloadJson: String?
    ): SyncOperationDto = SyncOperationDto(
        queueId = entity.queueId.toString(),
        entityType = entity.entityType,
        entityLocalId = entity.entityLocalId,
        serverId = serverId,
        operation = entity.operation,
        payload = payloadJson
    )
}

package com.zensa.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Data sent to the Zensa REST API when a local operation
 * needs to be synchronised with the server.
 */
@Serializable
data class SyncOperationDto(
    val queueId: String,
    val entityType: String,
    val entityLocalId: String,
    val serverId: String? = null,
    val operation: String,
    val payload: String? = null
)
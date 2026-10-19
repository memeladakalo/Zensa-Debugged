package com.zensa.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val queueId: String,
    val createdAt: Long,
    val failed: Boolean = false,


    val synced: Boolean = false,

    // Note: You likely have other fields here (like actionType, payload, etc.)
    // Keep them below this line. The `synced`, `failed`, and `createdAt`
    // columns are required by your SyncQueueDao queries.
)
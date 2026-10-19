package com.zensa.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room entity for a module.
 *
 * Additional fields beyond the server's:
 *   - isSynced      : has this row been pushed to the server?
 *   - isDeleted     : soft delete (needed for offline sync)
 *   - localUpdatedAt: last change time on this device
 *   - serverId      : the server's UUID (may differ from localId)
 *
 * localId is the primary key. It is generated on-device so that
 * offline-created rows have a stable identity until they are
 * synced, at which point serverId is filled in.
 */
@Entity(tableName = "modules")
data class ModuleEntity(
    @PrimaryKey val localId: String,
    val serverId: String? = null,
    val userId: String? = null,
    val moduleCode: String,
    val name: String,
    val description: String = "",
    val colour: String = "#6A4BAF",
    val createdAt: String,
    val updatedAt: String,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val localUpdatedAt: Long = System.currentTimeMillis()
)

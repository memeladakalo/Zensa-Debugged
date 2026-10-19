package com.zensa.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local Room entity for an academic assignment.
 *
 * Offline-first notes:
 *   - localId is generated on-device (never changes)
 *   - serverId is filled in after the first successful sync
 *   - isSynced = false means "still needs to be pushed to the server"
 *   - isDeleted = true is a soft delete (needed so the sync worker
 *     can tell the server to delete it later)
 *
 * moduleLocalId links to ModuleEntity.localId. If the module is
 * deleted, the assignment's module reference is set to NULL
 * (ON DELETE SET NULL) so the assignment is not lost.
 */
@Entity(
    tableName = "assignments",
    foreignKeys = [
        ForeignKey(
            entity = ModuleEntity::class,
            parentColumns = ["localId"],
            childColumns = ["moduleLocalId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("moduleLocalId"),
        Index("dueDate"),
        Index("priority")
    ]
)
data class AssignmentEntity(

    @PrimaryKey
    val localId: String,

    /** Server-issued UUID. Null until the first sync succeeds. */
    val serverId: String? = null,

    /** Owner. Null while offline and the user has not yet been resolved. */
    val userId: String? = null,

    /** Links to ModuleEntity.localId. Null if not tied to a module. */
    val moduleLocalId: String? = null,

    val title: String,
    val description: String = "",

    /**
     * One of: "LOW", "MEDIUM", "HIGH", "URGENT".
     * Stored as a string so Room doesn't need a type converter,
     * and so it matches the backend's JSON value directly.
     */
    val priority: String = "MEDIUM",

    /**
     * One of: "PENDING", "IN_PROGRESS", "COMPLETED".
     * Kept in sync with isCompleted below.
     */
    val status: String = "PENDING",

    /** ISO-8601 local date-time, e.g. "2026-03-15T14:30:00". */
    val dueDate: String,

    val isCompleted: Boolean = false,

    val createdAt: String,
    val updatedAt: String,

    // ---- Sync bookkeeping ----
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val localUpdatedAt: Long = System.currentTimeMillis()
)

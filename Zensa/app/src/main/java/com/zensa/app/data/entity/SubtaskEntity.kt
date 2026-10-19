package com.zensa.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local Room entity for a subtask.
 *
 * A subtask belongs to exactly one assignment. If the parent
 * assignment is permanently deleted, its subtasks are deleted too
 * (CASCADE) — otherwise the database would fill up with orphan
 * rows nobody can reach.
 *
 * Offline-first fields follow the same pattern as AssignmentEntity:
 *   localId, serverId, isSynced, isDeleted, localUpdatedAt.
 */
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = AssignmentEntity::class,
            parentColumns = ["localId"],
            childColumns = ["assignmentLocalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("assignmentLocalId")
    ]
)
data class SubtaskEntity(

    @PrimaryKey
    val localId: String,

    val serverId: String? = null,

    /**
     * Links to AssignmentEntity.localId.
     * Non-null: a subtask with no parent assignment is meaningless.
     */
    val assignmentLocalId: String,

    val title: String,

    /** Display order within the parent assignment. */
    val position: Int = 0,

    val isCompleted: Boolean = false,

    val createdAt: String,
    val updatedAt: String,

    // ---- Sync bookkeeping ----
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val localUpdatedAt: Long = System.currentTimeMillis()
)

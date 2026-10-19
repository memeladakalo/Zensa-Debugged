package com.zensa.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local Room entity for a note.
 *
 * A note can optionally be attached to a module. If the module is
 * deleted, the note survives with a null module reference
 * (ON DELETE SET NULL) — a student's writing should not be lost
 * because a module was removed.
 *
 * Offline-first fields follow the established pattern:
 *   localId, serverId, isSynced, isDeleted, localUpdatedAt.
 */
@Entity(
    tableName = "notes",
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
        Index("updatedAt")
    ]
)
data class NoteEntity(

    @PrimaryKey
    val localId: String,

    val serverId: String? = null,

    val userId: String? = null,

    /** Optional. Null means the note is not tied to a module. */
    val moduleLocalId: String? = null,

    val title: String,

    /** Free text. Room maps String to TEXT in SQLite. */
    val content: String = "",

    /** Reserved for POE — the planning PDF lists attachments as future. */
    val attachmentUrl: String? = null,

    val createdAt: String,
    val updatedAt: String,

    // ---- Sync bookkeeping ----
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val localUpdatedAt: Long = System.currentTimeMillis()
)

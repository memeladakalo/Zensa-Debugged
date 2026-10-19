package com.zensa.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local Room entity for a completed study session.
 *
 * A session records focused study time — either tied to a module
 * or to nothing in particular. It feeds:
 *   - Progress statistics
 *   - The study-streak calculation
 *   - Gamification points
 *
 * Offline-first: sessions are always written to Room first.
 * The repository pushes them to the server in the background.
 */
@Entity(
    tableName = "study_sessions",
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
        Index("startedAt")
    ]
)
data class StudySessionEntity(

    @PrimaryKey
    val localId: String,

    val serverId: String? = null,

    val userId: String? = null,

    /** Links to ModuleEntity.localId, or null for a module-less session. */
    val moduleLocalId: String? = null,

    /** ISO-8601 local date-time. */
    val startedAt: String,

    /** ISO-8601 local date-time. Null while a session is in progress. */
    val endedAt: String? = null,

    /** Total focused seconds. Zero while in progress. */
    val durationSeconds: Int = 0,

    /**
     * True only after the user explicitly completes the session.
     * Pausing, closing the app, or losing connectivity must NOT
     * mark a session as completed.
     */
    val completed: Boolean = false,

    val createdAt: String,
    val updatedAt: String,

    // ---- Sync bookkeeping ----
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val localUpdatedAt: Long = System.currentTimeMillis()
)

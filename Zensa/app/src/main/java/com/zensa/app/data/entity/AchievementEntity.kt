package com.zensa.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AchievementEntity
 *
 * We only store UNLOCKED achievements here. Locked badges are
 * computed live by AchievementsUseCase from the raw data
 * (assignments, subtasks, study sessions).
 *
 * This keeps the table tiny and means adding or tuning a badge
 * does not require a Room migration.
 *
 * `id` matches the stable id declared in the Achievement enum.
 */
@Entity(tableName = "achievements")
data class AchievementEntity(

    @PrimaryKey
    val achievementId: String,

    /** ISO date-time when this was unlocked. */
    val unlockedAt: String,

    /** True once the sync worker has pushed this row to the server. */
    val isSynced: Boolean = false,

    /** Soft delete flag, kept for parity with the other tables. */
    val isDeleted: Boolean = false,

    val localUpdatedAt: Long = System.currentTimeMillis()
)

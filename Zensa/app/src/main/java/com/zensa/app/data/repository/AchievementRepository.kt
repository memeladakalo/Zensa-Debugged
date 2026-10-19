package com.zensa.app.data.repository

import com.zensa.app.data.dao.AchievementDao
import com.zensa.app.data.entity.AchievementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * AchievementRepository
 *
 * Persistence for unlocked achievements. There is no server table
 * for these — they are derived state. This repository only stores
 * the first-unlock date so the UI can display "Unlocked on ...".
 *
 * Because nothing is pushed to the server, this repository does
 * not touch the sync queue. If a future POE requirement adds a
 * server-side achievement record, the shape is already here.
 */
class AchievementRepository(
    private val dao: AchievementDao
) {

    fun observeAll(): Flow<List<AchievementEntity>> = dao.observeAll()

    suspend fun getAll(): List<AchievementEntity> = dao.getAll()

    /**
     * Returns a map of achievementId -> ISO datetime of unlock.
     * Suitable for direct injection into AchievementsUseCase.
     */
    suspend fun unlockedAtMap(): Map<String, String> = withContext(Dispatchers.IO) {
        dao.getAll().associate { it.achievementId to it.unlockedAt }
    }

    /**
     * Records that a badge has just been unlocked. Idempotent —
     * OnConflictStrategy.IGNORE means a re-insert is a no-op.
     */
    suspend fun recordUnlocked(achievementId: String) = withContext(Dispatchers.IO) {
        val entry = AchievementEntity(
            achievementId = achievementId,
            unlockedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        dao.insert(entry)
    }

    /**
     * Convenience: records every badge in [ids] that is not yet
     * stored. Called by the AchievementsViewModel after computing
     * the current snapshot.
     */
    suspend fun recordAll(ids: Collection<String>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val existing = dao.unlockedIds().toHashSet()
        val now = LocalDateTime.now()
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val newEntries = ids
            .filter { it !in existing }
            .map { AchievementEntity(achievementId = it, unlockedAt = now) }
        if (newEntries.isNotEmpty()) {
            dao.insertAll(newEntries)
        }
    }
}

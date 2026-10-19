package com.zensa.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zensa.app.data.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * StudySessionDao — Room access object for study sessions.
 *
 * Sessions are always written locally first, then best-effort
 * pushed to the server. Soft-deleted rows are hidden from the UI
 * but visible to the sync worker.
 */
@Dao
interface StudySessionDao {

    // ---------- Reads ----------

    @Query(
        "SELECT * FROM study_sessions " +
        "WHERE isDeleted = 0 AND (userId IS NULL OR userId = :userId) " +
        "ORDER BY startedAt DESC"
    )
    fun observeForUser(userId: String?): Flow<List<StudySessionEntity>>

    @Query(
        "SELECT * FROM study_sessions " +
        "WHERE isDeleted = 0 AND (userId IS NULL OR userId = :userId) " +
        "ORDER BY startedAt DESC"
    )
    suspend fun getForUser(userId: String?): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): StudySessionEntity?

    @Query("SELECT * FROM study_sessions WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): StudySessionEntity?

    /** Sum of completed durations (in seconds) for a user. */
    @Query(
        "SELECT COALESCE(SUM(durationSeconds), 0) FROM study_sessions " +
        "WHERE isDeleted = 0 AND completed = 1 " +
        "AND (userId IS NULL OR userId = :userId)"
    )
    suspend fun totalCompletedSeconds(userId: String?): Int

    /** Number of completed sessions for a user. */
    @Query(
        "SELECT COUNT(*) FROM study_sessions " +
        "WHERE isDeleted = 0 AND completed = 1 " +
        "AND (userId IS NULL OR userId = :userId)"
    )
    suspend fun completedCount(userId: String?): Int

    /**
     * Completed sessions started on or after [sinceIso].
     * Used by the streak calculation.
     */
    @Query(
        "SELECT * FROM study_sessions " +
        "WHERE isDeleted = 0 AND completed = 1 " +
        "AND startedAt >= :sinceIso " +
        "AND (userId IS NULL OR userId = :userId) " +
        "ORDER BY startedAt DESC"
    )
    suspend fun completedSince(sinceIso: String, userId: String?): List<StudySessionEntity>

    // ---------- Writes ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: StudySessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<StudySessionEntity>)

    @Update
    suspend fun update(session: StudySessionEntity)

    @Query(
        "UPDATE study_sessions SET isDeleted = 1, isSynced = 0, " +
        "localUpdatedAt = :now WHERE localId = :localId"
    )
    suspend fun softDelete(localId: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM study_sessions WHERE localId = :localId")
    suspend fun hardDelete(localId: String)

    // ---------- Sync worker support ----------

    @Query("SELECT * FROM study_sessions WHERE isSynced = 0")
    suspend fun getUnsynced(): List<StudySessionEntity>

    @Query("UPDATE study_sessions SET isSynced = 1 WHERE localId = :localId")
    suspend fun markSynced(localId: String)
}

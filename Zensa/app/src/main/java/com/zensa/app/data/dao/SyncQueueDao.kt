package com.zensa.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zensa.app.data.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * SyncQueueDao — the persisted work queue for offline-first writes.
 */
@Dao
interface SyncQueueDao {

    // ---------- Enqueue ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueAll(items: List<SyncQueueEntity>)

    // ---------- Reads ----------

    /**
     * Oldest first. The worker processes rows in this order so
     * relationships stay consistent (e.g. a module is pushed
     * before assignments that reference it).
     */
    @Query(
        "SELECT * FROM sync_queue " +
        "WHERE synced = 0 AND failed = 0 " +
        "ORDER BY createdAt ASC"
    )
    suspend fun nextBatch(): List<SyncQueueEntity>

    /** Live count of pending items — used by the Settings screen. */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE synced = 0 AND failed = 0")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE synced = 0 AND failed = 0")
    suspend fun pendingCount(): Int

    @Query(
        "SELECT * FROM sync_queue " +
        "WHERE synced = 0 AND failed = 0 " +
        "ORDER BY createdAt ASC"
    )
    suspend fun allPending(): List<SyncQueueEntity>

    @Query(
        "SELECT * FROM sync_queue " +
        "WHERE failed = 1 " +
        "ORDER BY createdAt ASC"
    )
    suspend fun failedItems(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE queueId = :id LIMIT 1")
    suspend fun getById(id: String): SyncQueueEntity?

    // ---------- Lifecycle ----------

    @Update
    suspend fun update(item: SyncQueueEntity)

    /** Called after the server confirms the operation. */
    @Query("DELETE FROM sync_queue WHERE queueId = :id")
    suspend fun remove(id: String)

    @Query("DELETE FROM sync_queue WHERE synced = 1")
    suspend fun purgeCompleted()

    /** Used by "clear failed queue" in Settings (POE). */
    @Query("DELETE FROM sync_queue WHERE failed = 1")
    suspend fun clearFailed()
}

package com.zensa.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zensa.app.data.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * SubtaskDao — Room access object for subtasks.
 *
 * Every subtask belongs to exactly one assignment, so most queries
 * are scoped by assignmentLocalId. Soft-deleted rows are hidden
 * from the UI but readable by the sync worker.
 */
@Dao
interface SubtaskDao {

    // ---------- Reads used by the UI ----------

    /** Reactive list of live subtasks for one assignment, ordered. */
    @Query(
        "SELECT * FROM subtasks " +
        "WHERE isDeleted = 0 AND assignmentLocalId = :assignmentLocalId " +
        "ORDER BY position ASC, createdAt ASC"
    )
    fun observeForAssignment(assignmentLocalId: String): Flow<List<SubtaskEntity>>

    /** One-shot read (used by Smart Focus progress calculation). */
    @Query(
        "SELECT * FROM subtasks " +
        "WHERE isDeleted = 0 AND assignmentLocalId = :assignmentLocalId " +
        "ORDER BY position ASC, createdAt ASC"
    )
    suspend fun getForAssignment(assignmentLocalId: String): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): SubtaskEntity?

    @Query("SELECT * FROM subtasks WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): SubtaskEntity?

    // ---------- Writes ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subtask: SubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subtasks: List<SubtaskEntity>)

    @Update
    suspend fun update(subtask: SubtaskEntity)

    /** Flip a subtask's completed flag and mark it as needing a sync. */
    @Query(
        "UPDATE subtasks " +
        "SET isCompleted = :completed, isSynced = 0, localUpdatedAt = :now " +
        "WHERE localId = :localId"
    )
    suspend fun setCompleted(
        localId: String,
        completed: Boolean,
        now: Long = System.currentTimeMillis()
    )

    /** Soft delete — hidden from UI, flagged for sync. */
    @Query(
        "UPDATE subtasks SET isDeleted = 1, isSynced = 0, " +
        "localUpdatedAt = :now WHERE localId = :localId"
    )
    suspend fun softDelete(localId: String, now: Long = System.currentTimeMillis())

    /** Permanently remove a row after a confirmed server delete. */
    @Query("DELETE FROM subtasks WHERE localId = :localId")
    suspend fun hardDelete(localId: String)

    // ---------- Sync worker support ----------

    @Query("SELECT * FROM subtasks WHERE isSynced = 0")
    suspend fun getUnsynced(): List<SubtaskEntity>

    @Query("UPDATE subtasks SET isSynced = 1 WHERE localId = :localId")
    suspend fun markSynced(localId: String)

    // ---------- Aggregate helpers for progress ----------

    @Query(
        "SELECT COUNT(*) FROM subtasks " +
        "WHERE isDeleted = 0 AND assignmentLocalId = :assignmentLocalId"
    )
    suspend fun countForAssignment(assignmentLocalId: String): Int

    @Query(
        "SELECT COUNT(*) FROM subtasks " +
        "WHERE isDeleted = 0 AND assignmentLocalId = :assignmentLocalId " +
        "AND isCompleted = 1"
    )
    suspend fun countCompletedForAssignment(assignmentLocalId: String): Int
}

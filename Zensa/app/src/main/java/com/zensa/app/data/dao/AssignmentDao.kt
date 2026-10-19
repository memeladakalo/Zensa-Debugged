package com.zensa.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zensa.app.data.entity.AssignmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * AssignmentDao — Room access object for assignments.
 *
 * All queries exclude soft-deleted rows (isDeleted = 1) so the UI
 * never sees anything the user has removed. The sync worker reads
 * soft-deleted rows separately so it can push deletions.
 */
@Dao
interface AssignmentDao {

    // ---------- Reads used by the UI ----------

    /** Reactive list of all live assignments, newest first. */
    @Query(
        "SELECT * FROM assignments " +
        "WHERE isDeleted = 0 AND (userId IS NULL OR userId = :userId) " +
        "ORDER BY dueDate ASC"
    )
    fun observeForUser(userId: String?): Flow<List<AssignmentEntity>>

    /** Assignments belonging to one module. */
    @Query(
        "SELECT * FROM assignments " +
        "WHERE isDeleted = 0 AND moduleLocalId = :moduleLocalId " +
        "ORDER BY dueDate ASC"
    )
    fun observeForModule(moduleLocalId: String): Flow<List<AssignmentEntity>>

    /** Filter by priority. */
    @Query(
        "SELECT * FROM assignments " +
        "WHERE isDeleted = 0 AND priority = :priority " +
        "ORDER BY dueDate ASC"
    )
    fun observeByPriority(priority: String): Flow<List<AssignmentEntity>>

    /** Only assignments still pending, ordered by due date. */
    @Query(
        "SELECT * FROM assignments " +
        "WHERE isDeleted = 0 AND isCompleted = 0 " +
        "ORDER BY dueDate ASC"
    )
    fun observePending(): Flow<List<AssignmentEntity>>

    /** One-shot read for the ViewModel / Smart Focus calculation. */
    @Query(
        "SELECT * FROM assignments " +
        "WHERE isDeleted = 0 AND (userId IS NULL OR userId = :userId)"
    )
    suspend fun getForUser(userId: String?): List<AssignmentEntity>

    @Query("SELECT * FROM assignments WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): AssignmentEntity?

    @Query("SELECT * FROM assignments WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): AssignmentEntity?

    // ---------- Writes ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(assignment: AssignmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(assignments: List<AssignmentEntity>)

    @Update
    suspend fun update(assignment: AssignmentEntity)

    /** Convenience: mark an assignment complete / incomplete. */
    @Query(
        "UPDATE assignments " +
        "SET isCompleted = :completed, " +
        "    status = CASE WHEN :completed = 1 THEN 'COMPLETED' ELSE 'PENDING' END, " +
        "    isSynced = 0, " +
        "    localUpdatedAt = :now " +
        "WHERE localId = :localId"
    )
    suspend fun setCompleted(
        localId: String,
        completed: Boolean,
        now: Long = System.currentTimeMillis()
    )

    /** Soft delete — hides from UI, marks for sync. */
    @Query(
        "UPDATE assignments SET isDeleted = 1, isSynced = 0, " +
        "localUpdatedAt = :now WHERE localId = :localId"
    )
    suspend fun softDelete(localId: String, now: Long = System.currentTimeMillis())

    /** Permanently remove a row after a confirmed server delete. */
    @Query("DELETE FROM assignments WHERE localId = :localId")
    suspend fun hardDelete(localId: String)

    // ---------- Sync worker support ----------

    @Query("SELECT * FROM assignments WHERE isSynced = 0")
    suspend fun getUnsynced(): List<AssignmentEntity>

    @Query("UPDATE assignments SET isSynced = 1 WHERE localId = :localId")
    suspend fun markSynced(localId: String)

    /** Count of pending (not completed, not deleted) assignments. */
    @Query(
        "SELECT COUNT(*) FROM assignments " +
        "WHERE isDeleted = 0 AND isCompleted = 0"
    )
    suspend fun countPending(): Int
}

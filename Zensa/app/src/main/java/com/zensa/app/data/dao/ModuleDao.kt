package com.zensa.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zensa.app.data.entity.ModuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * ModuleDao — Room data access object for modules.
 *
 * All queries are suspend or return Flow so they never run on the
 * main thread. Room itself enforces this at compile time.
 */
@Dao
interface ModuleDao {

    /** Reactive list of non-deleted modules for a user. */
    @Query(
        "SELECT * FROM modules " +
        "WHERE isDeleted = 0 AND (userId IS NULL OR userId = :userId) " +
        "ORDER BY createdAt DESC"
    )
    fun observeForUser(userId: String?): Flow<List<ModuleEntity>>

    /** One-shot read — used by the sync worker. */
    @Query(
        "SELECT * FROM modules " +
        "WHERE isDeleted = 0 AND (userId IS NULL OR userId = :userId) " +
        "ORDER BY createdAt DESC"
    )
    suspend fun getForUser(userId: String?): List<ModuleEntity>

    @Query("SELECT * FROM modules WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): ModuleEntity?

    @Query("SELECT * FROM modules WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): ModuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(module: ModuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(modules: List<ModuleEntity>)

    @Update
    suspend fun update(module: ModuleEntity)

    /** Soft delete — sets isDeleted and marks the row unsynced. */
    @Query(
        "UPDATE modules SET isDeleted = 1, isSynced = 0, localUpdatedAt = :now " +
        "WHERE localId = :localId"
    )
    suspend fun softDelete(localId: String, now: Long = System.currentTimeMillis())

    /** All rows that still need to be pushed to the server. */
    @Query("SELECT * FROM modules WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ModuleEntity>

    /** Called after a successful sync. */
    @Query("UPDATE modules SET isSynced = 1 WHERE localId = :localId")
    suspend fun markSynced(localId: String)

    /** Fully remove a row after a confirmed server delete. */
    @Query("DELETE FROM modules WHERE localId = :localId")
    suspend fun hardDelete(localId: String)
}

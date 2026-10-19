package com.zensa.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zensa.app.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * NoteDao — Room access object for notes.
 */
@Dao
interface NoteDao {

    // ---------- Reads ----------

    @Query(
        "SELECT * FROM notes " +
        "WHERE isDeleted = 0 AND (userId IS NULL OR userId = :userId) " +
        "ORDER BY updatedAt DESC"
    )
    fun observeForUser(userId: String?): Flow<List<NoteEntity>>

    @Query(
        "SELECT * FROM notes " +
        "WHERE isDeleted = 0 AND moduleLocalId = :moduleLocalId " +
        "ORDER BY updatedAt DESC"
    )
    fun observeForModule(moduleLocalId: String): Flow<List<NoteEntity>>

    @Query(
        "SELECT * FROM notes " +
        "WHERE isDeleted = 0 AND (userId IS NULL OR userId = :userId) " +
        "ORDER BY updatedAt DESC"
    )
    suspend fun getForUser(userId: String?): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): NoteEntity?

    @Query(
        "SELECT * FROM notes " +
        "WHERE isDeleted = 0 AND (userId IS NULL OR userId = :userId) " +
        "AND (title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%') " +
        "ORDER BY updatedAt DESC"
    )
    suspend fun search(userId: String?, q: String): List<NoteEntity>

    @Query(
        "SELECT * FROM notes " +
        "WHERE isDeleted = 0 AND (userId IS NULL OR userId = :userId)"
    )
    suspend fun observeCountRaw(userId: String?): Flow<Int>

    // ---------- Writes ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Query(
        "UPDATE notes SET isDeleted = 1, isSynced = 0, " +
        "localUpdatedAt = :now WHERE localId = :localId"
    )
    suspend fun softDelete(localId: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE localId = :localId")
    suspend fun hardDelete(localId: String)

    // ---------- Sync worker support ----------

    @Query("SELECT * FROM notes WHERE isSynced = 0")
    suspend fun getUnsynced(): List<NoteEntity>

    @Query("UPDATE notes SET isSynced = 1 WHERE localId = :localId")
    suspend fun markSynced(localId: String)
}

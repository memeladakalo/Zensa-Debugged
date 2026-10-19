package com.zensa.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zensa.app.data.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

/**
 * AchievementDao — Room access object for unlocked achievements.
 */
@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements WHERE isDeleted = 0")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isDeleted = 0")
    suspend fun getAll(): List<AchievementEntity>

    @Query(
        "SELECT * FROM achievements " +
        "WHERE achievementId = :id AND isDeleted = 0 LIMIT 1"
    )
    suspend fun getById(id: String): AchievementEntity?

    @Query(
        "SELECT achievementId FROM achievements WHERE isDeleted = 0"
    )
    suspend fun unlockedIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<AchievementEntity>)

    @Query(
        "UPDATE achievements SET isSynced = 1 " +
        "WHERE achievementId = :id"
    )
    suspend fun markSynced(id: String)

    @Query(
        "UPDATE achievements SET isDeleted = 1, isSynced = 0, " +
        "localUpdatedAt = :now WHERE achievementId = :id"
    )
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM achievements WHERE isDeleted = 1")
    suspend fun purgeDeleted()
}

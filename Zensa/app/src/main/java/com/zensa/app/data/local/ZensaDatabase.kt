package com.zensa.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zensa.app.core.constants.AppConstants
import com.zensa.app.data.dao.AchievementDao
import com.zensa.app.data.dao.AssignmentDao
import com.zensa.app.data.dao.ModuleDao
import com.zensa.app.data.dao.NoteDao
import com.zensa.app.data.dao.StudySessionDao
import com.zensa.app.data.dao.SubtaskDao
import com.zensa.app.data.dao.SyncQueueDao
import com.zensa.app.data.entity.AchievementEntity
import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.data.entity.ModuleEntity
import com.zensa.app.data.entity.NoteEntity
import com.zensa.app.data.entity.StudySessionEntity
import com.zensa.app.data.entity.SubtaskEntity
import com.zensa.app.data.entity.SyncQueueEntity

@Database(
    entities = [
        ModuleEntity::class,
        AssignmentEntity::class,
        SubtaskEntity::class,
        StudySessionEntity::class,
        SyncQueueEntity::class,
        NoteEntity::class,
        AchievementEntity::class
    ],
    version = AppConstants.DATABASE_VERSION,
    exportSchema = false
)
abstract class ZensaDatabase : RoomDatabase() {

    abstract fun moduleDao(): ModuleDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun noteDao(): NoteDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile private var INSTANCE: ZensaDatabase? = null

        fun get(context: Context): ZensaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZensaDatabase::class.java,
                    AppConstants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

package com.voicetrail.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [JournalEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VoiceTrailDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        @Volatile
        private var instance: VoiceTrailDatabase? = null

        fun getInstance(context: Context): VoiceTrailDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VoiceTrailDatabase::class.java,
                    "voicetrail.db"
                ).build().also { instance = it }
            }
        }
    }
}


package com.lazyjournal.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [JournalEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LazyJournalDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        @Volatile
        private var instance: LazyJournalDatabase? = null

        fun getInstance(context: Context): LazyJournalDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LazyJournalDatabase::class.java,
                    "lazy_journal.db"
                ).build().also { instance = it }
            }
        }
    }
}

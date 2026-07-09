package com.lazyjournal.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [JournalEntryEntity::class],
    version = 2,
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
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE journal_entries
                    ADD COLUMN transcript_status TEXT NOT NULL DEFAULT 'pending'
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    ALTER TABLE journal_entries
                    ADD COLUMN transcript_error TEXT
                    """.trimIndent()
                )
            }
        }
    }
}

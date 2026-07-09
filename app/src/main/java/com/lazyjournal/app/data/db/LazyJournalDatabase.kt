package com.lazyjournal.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [JournalEntryEntity::class],
    version = 3,
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
                    .addMigrations(MIGRATION_2_3)
                    .addCallback(DatabaseCallback)
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.createJournalEntryFts()
            }
        }

        private val DatabaseCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.createJournalEntryFts()
            }
        }

        private fun SupportSQLiteDatabase.createJournalEntryFts() {
            execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS journal_entries_fts
                USING fts5(
                    transcript,
                    content='journal_entries',
                    content_rowid='id'
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS journal_entries_fts_ai
                AFTER INSERT ON journal_entries
                BEGIN
                    INSERT INTO journal_entries_fts(rowid, transcript)
                    VALUES (new.id, new.transcript);
                END
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS journal_entries_fts_ad
                AFTER DELETE ON journal_entries
                BEGIN
                    INSERT INTO journal_entries_fts(journal_entries_fts, rowid, transcript)
                    VALUES ('delete', old.id, old.transcript);
                END
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS journal_entries_fts_au
                AFTER UPDATE OF transcript ON journal_entries
                BEGIN
                    INSERT INTO journal_entries_fts(journal_entries_fts, rowid, transcript)
                    VALUES ('delete', old.id, old.transcript);
                    INSERT INTO journal_entries_fts(rowid, transcript)
                    VALUES (new.id, new.transcript);
                END
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO journal_entries_fts(journal_entries_fts)
                VALUES ('rebuild')
                """.trimIndent()
            )
        }
    }
}

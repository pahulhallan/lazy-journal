package com.lazyjournal.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    val transcript: String,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "location_label")
    val locationLabel: String?,
    @ColumnInfo(name = "audio_file_path")
    val audioFilePath: String,
    val tags: String
)

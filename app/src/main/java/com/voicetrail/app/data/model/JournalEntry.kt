package com.voicetrail.app.data.model

data class JournalEntry(
    val id: Long,
    val createdAt: Long,
    val transcript: String,
    val latitude: Double?,
    val longitude: Double?,
    val locationLabel: String?,
    val audioFilePath: String,
    val tags: List<String>
)


package com.lazyjournal.app.data.model

data class JournalEntry(
    val id: Long,
    val createdAt: Long,
    val transcript: String,
    val latitude: Double?,
    val longitude: Double?,
    val locationLabel: String?,
    val audioFilePath: String,
    val tags: List<String>,
    val transcriptStatus: TranscriptStatus,
    val transcriptError: String?
)

enum class TranscriptStatus {
    Pending,
    Running,
    Complete,
    Failed;

    val storageValue: String
        get() = name.lowercase()

    companion object {
        fun fromStorageValue(value: String): TranscriptStatus {
            return entries.firstOrNull { it.storageValue == value } ?: Pending
        }
    }
}

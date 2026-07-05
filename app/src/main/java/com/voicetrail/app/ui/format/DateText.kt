package com.voicetrail.app.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val entryDateFormatter = DateTimeFormatter
    .ofPattern("MMM d, yyyy h:mm a", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

fun formatEntryDate(epochMillis: Long): String {
    return entryDateFormatter.format(Instant.ofEpochMilli(epochMillis))
}

fun formatElapsed(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}


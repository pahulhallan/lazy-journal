package com.lazyjournal.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lazyjournal.app.data.model.TranscriptStatus
import com.lazyjournal.app.ui.LocalAppContainer
import com.lazyjournal.app.ui.format.formatEntryDate
import java.io.File

@Composable
fun EntryDetailScreen(
    innerPadding: PaddingValues,
    entryId: Long,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val factory = remember(entryId, container) {
        EntryDetailViewModelFactory(
            entryId = entryId,
            repository = container.repository,
            audioPlayer = container.audioPlayer
        )
    }
    val viewModel: EntryDetailViewModel = viewModel(
        key = "entry-detail-$entryId",
        factory = factory
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SnackbarHost(hostState = snackbarHostState)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Entry",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val entry = uiState.entry
        if (entry == null) {
            Text(
                text = "Entry not found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        Text(
            text = formatEntryDate(entry.createdAt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = viewModel::togglePlayback,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null
            )
            Text(
                text = if (uiState.isPlaying) "Stop playback" else "Play recording",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        DetailCard(title = "Transcript") {
            AssistChip(
                onClick = {},
                label = { Text(entry.transcriptStatus.label) }
            )
            Text(
                text = entry.transcript.ifBlank { "Transcript pending" },
                style = MaterialTheme.typography.bodyLarge,
                color = if (entry.transcript.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (entry.transcriptStatus == TranscriptStatus.Failed && !entry.transcriptError.isNullOrBlank()) {
                Text(
                    text = entry.transcriptError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        DetailCard(title = "Location") {
            Text(
                text = locationText(entry.latitude, entry.longitude, entry.locationLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        DetailCard(title = "Tags") {
            if (entry.tags.isEmpty()) {
                Text(
                    text = "No tags",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    entry.tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(tag) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        DetailCard(title = "Audio file") {
            Text(
                text = File(entry.audioFilePath).name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.audioFilePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val TranscriptStatus.label: String
    get() = when (this) {
        TranscriptStatus.Pending -> "Pending"
        TranscriptStatus.Running -> "Transcribing locally"
        TranscriptStatus.Complete -> "Complete"
        TranscriptStatus.Failed -> "Transcription unavailable"
    }

@Composable
private fun DetailCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

private fun locationText(
    latitude: Double?,
    longitude: Double?,
    label: String?
): String {
    if (latitude == null || longitude == null) {
        return "No location captured"
    }
    val coordinateText = "%.5f, %.5f".format(latitude, longitude)
    return if (label.isNullOrBlank()) {
        coordinateText
    } else {
        "$label - $coordinateText"
    }
}

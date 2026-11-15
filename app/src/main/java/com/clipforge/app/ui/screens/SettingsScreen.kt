package com.clipforge.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clipforge.app.data.preferences.QueueMode
import com.clipforge.app.data.preferences.VideoQuality
import com.clipforge.app.ui.ClipForgeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ClipForgeViewModel, onBack: () -> Unit) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val updating by viewModel.isUpdatingEngine.collectAsStateWithLifecycle()
    val updateMessage by viewModel.engineUpdateMessage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("VIDEO QUALITY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("Choose the maximum source resolution for new downloads.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoQuality.entries.forEach { quality ->
                        FilterChip(
                            selected = preferences.quality == quality,
                            onClick = { viewModel.setQuality(quality) },
                            label = { Text(quality.title) },
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("QUEUE BEHAVIOR", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("Parallel clips use up to two workers and more network and battery.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                QueueMode.entries.forEach { mode ->
                    Card(
                        onClick = { viewModel.setQueueMode(mode) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (preferences.queueMode == mode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(mode.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(if (mode == QueueMode.SEQUENTIAL) "Reliable single-file processing" else "Two segments can cut at the same time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (preferences.queueMode == mode) Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("YT-DLP ENGINE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("Refresh the stable extraction engine when a source site changes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = viewModel::updateEngine, enabled = !updating, modifier = Modifier.fillMaxWidth()) {
                    if (updating) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                    else Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text(if (updating) "  Updating engine…" else "  Update yt-dlp Engine")
                }
                updateMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (it.startsWith("Stable")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Exports are saved to Movies/ClipForge/<Project>.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

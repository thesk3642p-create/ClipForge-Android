package com.clipforge.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clipforge.app.data.local.ClipEntity
import com.clipforge.app.data.local.ClipStatus
import com.clipforge.app.ui.ClipForgeViewModel
import com.clipforge.app.ui.components.ClipCard
import com.clipforge.app.ui.components.storageLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    projectId: String,
    viewModel: ClipForgeViewModel,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onPreview: (String) -> Unit,
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val project = projects.firstOrNull { it.project.id == projectId }
    val clipsFlow = remember(projectId) { viewModel.clipsFor(projectId) }
    val clips by clipsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val queuedCount = clips.count { it.status == ClipStatus.QUEUED }
    val waitingCount = queuedCount + clips.count { it.status == ClipStatus.SCHEDULED }
    val activeCount = clips.count { it.status == ClipStatus.DOWNLOADING }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.project?.name ?: "Project", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onImport) { Icon(Icons.Rounded.Add, contentDescription = "Add clips") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                project?.let { summary ->
                    ProjectSummaryCard(
                        clipCount = summary.clipCount,
                        completedCount = summary.completedCount,
                        storage = storageLabel(summary.storageBytes),
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.downloadAll(projectId) { Toast.makeText(context, it, Toast.LENGTH_LONG).show() } },
                            enabled = waitingCount > 0 && activeCount == 0,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(if (activeCount > 0) "  Downloading $activeCount" else "  Download all ($waitingCount)")
                        }
                        OutlinedButton(onClick = onImport) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Add clips")
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text("CLIP QUEUE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
            if (clips.isEmpty()) {
                item { EmptyQueue(onImport) }
            } else {
                items(clips, key = ClipEntity::id) { clip ->
                    ClipCard(
                        clip = clip,
                        onPreview = { onPreview(clip.id) },
                        onRetry = { viewModel.retry(clip.id) { Toast.makeText(context, it, Toast.LENGTH_LONG).show() } },
                        onCancel = { viewModel.cancel(clip.id) },
                        onDelete = { viewModel.removeClip(clip.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectSummaryCard(clipCount: Int, completedCount: Int, storage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("$clipCount clips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("$completedCount ready  ·  $storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyQueue(onImport: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Your queue is clear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Paste a clip list to build your edit library.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onImport) { Text("Add your first clips") }
    }
}

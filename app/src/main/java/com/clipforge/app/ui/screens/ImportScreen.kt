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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clipforge.app.domain.model.ClipDraft
import com.clipforge.app.domain.parser.BulkClipParser
import com.clipforge.app.domain.parser.Timecode
import com.clipforge.app.ui.ClipForgeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    projectId: String,
    viewModel: ClipForgeViewModel,
    onBack: () -> Unit,
) {
    var bulkMode by rememberSaveable { mutableStateOf(true) }
    var bulkText by rememberSaveable { mutableStateOf("") }
    var singleLabel by rememberSaveable { mutableStateOf("") }
    var singleUrl by rememberSaveable { mutableStateOf("") }
    var singleStart by rememberSaveable { mutableStateOf("") }
    var singleEnd by rememberSaveable { mutableStateOf("") }
    var drafts by remember { mutableStateOf(emptyList<ClipDraft>()) }
    var errors by remember { mutableStateOf(emptyList<String>()) }
    val parser = remember { BulkClipParser() }
    val context = LocalContext.current
    val projectName = viewModel.projects.collectAsStateWithLifecycle().value.firstOrNull { it.project.id == projectId }?.project?.name

    fun parseInput() {
        val result = if (bulkMode) parser.parse(bulkText) else parser.parseSingle(singleLabel.ifBlank { "Clip" }, singleUrl, singleStart, singleEnd)
        drafts = result.clips
        errors = result.errors
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add clips") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
            )
        },
        bottomBar = {
            if (drafts.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Button(
                        onClick = {
                            viewModel.addClips(
                                projectId,
                                drafts,
                                onDone = { onBack() },
                                onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() },
                            )
                        },
                        enabled = drafts.all(ClipDraft::isValid),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("  Add ${drafts.size} clips to queue")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("${projectName ?: "Project"} workspace", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Paste a script or add a single segment.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = bulkMode, onClick = { bulkMode = true; drafts = emptyList(); errors = emptyList() }, label = { Text("Bulk paste") }, leadingIcon = if (bulkMode) ({ Icon(Icons.Rounded.ContentPaste, null, Modifier.size(16.dp)) }) else null)
                    FilterChip(selected = !bulkMode, onClick = { bulkMode = false; drafts = emptyList(); errors = emptyList() }, label = { Text("Single clip") })
                }
            }
            if (bulkMode) {
                item {
                    OutlinedTextField(
                        value = bulkText,
                        onValueChange = { bulkText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Clip list") },
                        placeholder = { Text("01_Alien_Attack | https://youtube.com/watch?v=… | 00:02:46 - 00:06:47\n\nOr paste a JSON array with label, url, start, end." ) },
                        minLines = 8,
                        maxLines = 12,
                    )
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = singleLabel, onValueChange = { singleLabel = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Filename") }, placeholder = { Text("Alien attack") }, singleLine = true)
                        OutlinedTextField(value = singleUrl, onValueChange = { singleUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("YouTube URL") }, placeholder = { Text("https://youtu.be/…") }, singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = singleStart, onValueChange = { singleStart = it }, modifier = Modifier.weight(1f), label = { Text("Start time") }, placeholder = { Text("MM:SS") }, singleLine = true)
                            OutlinedTextField(value = singleEnd, onValueChange = { singleEnd = it }, modifier = Modifier.weight(1f), label = { Text("End time") }, placeholder = { Text("HH:MM:SS") }, singleLine = true)
                        }
                    }
                }
            }
            item {
                Button(onClick = ::parseInput, enabled = if (bulkMode) bulkText.isNotBlank() else singleUrl.isNotBlank()) {
                    Text(if (drafts.isEmpty()) "Review clips" else "Parse again")
                }
            }
            if (errors.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            errors.take(5).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                            if (errors.size > 5) Text("+${errors.size - 5} more entries need attention", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (drafts.isNotEmpty()) {
                item {
                    Column {
                        Text("READY TO ADD  ·  ${drafts.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text("Check each cut before it enters the queue.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                itemsIndexed(drafts, key = { index, draft -> "$index-${draft.url}" }) { index, draft ->
                    DraftCard(
                        index = index,
                        draft = draft,
                        onLabel = { updated -> drafts = drafts.toMutableList().also { it[index] = updated } },
                        onStartDelta = { delta ->
                            val next = draft.startSeconds + delta
                            if (next >= 0 && next < draft.endSeconds) drafts = drafts.toMutableList().also { it[index] = draft.copy(startSeconds = next) }
                        },
                        onEndDelta = { delta ->
                            val next = draft.endSeconds + delta
                            if (next > draft.startSeconds) drafts = drafts.toMutableList().also { it[index] = draft.copy(endSeconds = next) }
                        },
                        onDelete = {
                            drafts = drafts.filterIndexed { current, _ -> current != index }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftCard(
    index: Int,
    draft: ClipDraft,
    onLabel: (ClipDraft) -> Unit,
    onStartDelta: (Long) -> Unit,
    onEndDelta: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(index.plus(1).toString().padStart(2, '0'), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.size(9.dp))
                Text("${draft.durationSeconds.coerceAtLeast(0)}s segment", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete clip", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            OutlinedTextField(
                value = draft.label,
                onValueChange = { onLabel(draft.copy(label = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Clip label") },
                singleLine = true,
            )
            Text(draft.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TimeAdjuster("IN", Timecode.format(draft.startSeconds), onMinus = { onStartDelta(-1) }, onPlus = { onStartDelta(1) }, canMinus = draft.startSeconds > 0, canPlus = draft.startSeconds + 1 < draft.endSeconds, modifier = Modifier.weight(1f))
                TimeAdjuster("OUT", Timecode.format(draft.endSeconds), onMinus = { onEndDelta(-1) }, onPlus = { onEndDelta(1) }, canMinus = draft.endSeconds - 1 > draft.startSeconds, canPlus = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TimeAdjuster(
    label: String,
    time: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    canMinus: Boolean,
    canPlus: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMinus, enabled = canMinus, modifier = Modifier.size(34.dp)) { Icon(Icons.Rounded.Remove, contentDescription = "Move $label back one second", modifier = Modifier.size(18.dp)) }
            Text(time, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onPlus, enabled = canPlus, modifier = Modifier.size(34.dp)) { Icon(Icons.Rounded.Add, contentDescription = "Move $label forward one second", modifier = Modifier.size(18.dp)) }
        }
    }
}

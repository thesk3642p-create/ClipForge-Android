package com.clipforge.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clipforge.app.data.local.ClipEntity
import com.clipforge.app.data.local.ClipStatus
import com.clipforge.app.domain.parser.Timecode

@Composable
fun ClipCard(
    clip: ClipEntity,
    onPreview: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val index = clip.orderIndex.toString().padStart(2, '0')
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (clip.status == ClipStatus.COMPLETED) Modifier.clickable(onClick = onPreview) else Modifier,
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${index}_${clip.label}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "${Timecode.format(clip.startSeconds)} → ${Timecode.format(clip.endSeconds)}  ·  ${clip.endSeconds - clip.startSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusLabel(clip.status)
            }

            if (clip.status == ClipStatus.DOWNLOADING) {
                Spacer(Modifier.height(13.dp))
                LinearProgressIndicator(
                    progress = { clip.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${clip.progress}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    clip.etaSeconds?.let {
                        Spacer(Modifier.width(8.dp))
                        Text("· about ${formatEta(it)} left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onCancel, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel download", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when (clip.status) {
                        ClipStatus.COMPLETED -> TextButton(onClick = onPreview) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Preview")
                        }
                        ClipStatus.FAILED, ClipStatus.CANCELED -> if (clip.mediaUri == null) TextButton(onClick = onRetry) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Retry")
                        } else TextButton(onClick = onPreview) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Preview previous")
                        }
                        ClipStatus.QUEUED, ClipStatus.SCHEDULED -> Text("Waiting in queue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> Unit
                    }
                    if (clip.status == ClipStatus.COMPLETED || (clip.status == ClipStatus.FAILED && clip.mediaUri != null)) {
                        TextButton(onClick = onRetry) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if (clip.status == ClipStatus.COMPLETED) "Re-download" else "Retry")
                        }
                    }
                    clip.errorMessage?.takeIf { clip.status == ClipStatus.FAILED }?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    }
                    Spacer(Modifier.weight(1f))
                    if (clip.status == ClipStatus.COMPLETED) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove clip", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (clip.status == ClipStatus.QUEUED || clip.status == ClipStatus.SCHEDULED) {
                        IconButton(onClick = onCancel, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cancel clip", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (clip.status == ClipStatus.FAILED || clip.status == ClipStatus.CANCELED) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove clip", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLabel(status: String) {
    val (text, color) = when (status) {
        ClipStatus.COMPLETED -> "READY" to MaterialTheme.colorScheme.primary
        ClipStatus.DOWNLOADING -> "CUTTING" to MaterialTheme.colorScheme.tertiary
        ClipStatus.FAILED -> "FAILED" to MaterialTheme.colorScheme.error
        ClipStatus.CANCELED -> "CANCELED" to MaterialTheme.colorScheme.onSurfaceVariant
        ClipStatus.SCHEDULED -> "QUEUED" to MaterialTheme.colorScheme.secondary
        else -> "QUEUED" to MaterialTheme.colorScheme.secondary
    }
    Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
}

private fun formatEta(seconds: Long): String = when {
    seconds < 60 -> "${seconds.coerceAtLeast(0)}s"
    seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
    else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
}

package com.clipforge.app.ui.screens

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.clipforge.app.data.local.ClipEntity
import com.clipforge.app.domain.parser.Timecode
import com.clipforge.app.ui.ClipForgeViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipPreviewScreen(clipId: String, viewModel: ClipForgeViewModel, onBack: () -> Unit) {
    val clipFlow = remember(clipId) { viewModel.observeClip(clipId) }
    val clip = clipFlow.collectAsStateWithLifecycle(initialValue = null).value
    val context = LocalContext.current
    val mediaUri = clip?.mediaUri?.let { Uri.parse(it) }
    val player = remember(mediaUri) {
        mediaUri?.let { uri ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = false
            }
        }
    }
    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clip preview") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        if (clip == null) {
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        } else {
            PreviewContent(clip, player, mediaUri, padding)
        }
    }
}

@Composable
private fun PreviewContent(clip: ClipEntity, player: ExoPlayer?, mediaUri: Uri?, padding: PaddingValues) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(2.dp))
        if (player != null) {
            AndroidView(
                factory = { viewContext -> PlayerView(viewContext).apply { this.player = player; useController = true } },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                update = { it.player = player },
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Preview isn't available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("${clip.orderIndex.toString().padStart(2, '0')}_${clip.label}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${Timecode.format(clip.startSeconds)} → ${Timecode.format(clip.endSeconds)}  ·  ${clip.endSeconds - clip.startSeconds}s", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${DecimalFormat("0.0").format(clip.sizeBytes / 1_000_000.0)} MB  ·  Movies/ClipForge", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    mediaUri?.let { uri ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = ClipData.newRawUri("ClipForge video", uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share clip"))
                    }
                },
                enabled = mediaUri != null,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Text("  Share")
            }
            OutlinedButton(
                onClick = {
                    mediaUri?.let { uri ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "video/mp4")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Open with"))
                    }
                },
                enabled = mediaUri != null,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                Text("  Open in editor")
            }
        }
    }
}

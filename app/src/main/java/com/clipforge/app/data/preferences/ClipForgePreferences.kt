package com.clipforge.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class VideoQuality(val title: String, val format: String) {
    P720("720p", "bestvideo[ext=mp4][height<=720]+bestaudio[ext=m4a]/best[ext=mp4][height<=720]/best"),
    P1080("1080p", "bestvideo[ext=mp4][height<=1080]+bestaudio[ext=m4a]/best[ext=mp4]/best"),
    BEST("Best / 4K", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"),
}

enum class QueueMode(val title: String) {
    SEQUENTIAL("One at a time"),
    TWO_AT_ONCE("Up to two at once"),
}

data class AppPreferences(
    val quality: VideoQuality = VideoQuality.P1080,
    val queueMode: QueueMode = QueueMode.SEQUENTIAL,
)

private val Context.clipForgeDataStore by preferencesDataStore(name = "clipforge_settings")

class ClipForgePreferences(private val context: Context) {
    private val qualityKey = stringPreferencesKey("video_quality")
    private val queueModeKey = stringPreferencesKey("queue_mode")

    val preferences: Flow<AppPreferences> = context.clipForgeDataStore.data.map { values ->
        AppPreferences(
            quality = values[qualityKey]?.let { raw -> VideoQuality.entries.firstOrNull { it.name == raw } } ?: VideoQuality.P1080,
            queueMode = values[queueModeKey]?.let { raw -> QueueMode.entries.firstOrNull { it.name == raw } } ?: QueueMode.SEQUENTIAL,
        )
    }

    suspend fun setQuality(value: VideoQuality) {
        context.clipForgeDataStore.edit { it[qualityKey] = value.name }
    }

    suspend fun setQueueMode(value: QueueMode) {
        context.clipForgeDataStore.edit { it[queueModeKey] = value.name }
    }
}

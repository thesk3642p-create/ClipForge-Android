package com.clipforge.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clipforge.app.ClipForgeApp
import com.clipforge.app.data.ClipQueue
import com.clipforge.app.data.local.ClipStatus
import com.clipforge.app.data.preferences.AppPreferences
import com.clipforge.app.data.preferences.QueueMode
import com.clipforge.app.data.preferences.VideoQuality
import com.clipforge.app.domain.model.ClipDraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClipForgeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ClipForgeApp
    private val repository = app.container.repository
    private val queue = ClipQueue(application)

    val projects = repository.observeProjects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val preferences: StateFlow<AppPreferences> = app.container.preferences.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())
    private val _engineUpdateMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val engineUpdateMessage: StateFlow<String?> = _engineUpdateMessage
    private val _isUpdatingEngine = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isUpdatingEngine: StateFlow<Boolean> = _isUpdatingEngine

    fun clipsFor(projectId: String) = repository.observeClips(projectId)
    fun observeClip(id: String) = repository.observeClip(id)

    fun createProject(name: String, onCreated: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.createProject(name) }
                .onSuccess(onCreated)
                .onFailure { onError(it.message ?: "Couldn't create the project.") }
        }
    }

    fun addClips(projectId: String, drafts: List<ClipDraft>, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.addClips(projectId, drafts) }
                .onSuccess { onDone() }
                .onFailure { onError(it.message ?: "Couldn't save these clips.") }
        }
    }

    fun downloadAll(projectId: String, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                val savedPreferences = app.container.preferences.preferences.first()
                queue.enqueueProject(projectId, savedPreferences.queueMode)
            }
                .onFailure { onError(it.message ?: "Couldn't start the download queue.") }
        }
    }

    fun retry(clipId: String, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { queue.enqueueRetry(clipId) }
                .onFailure { onError(it.message ?: "Couldn't retry this clip.") }
        }
    }

    fun cancel(clipId: String) {
        viewModelScope.launch { queue.cancel(clipId) }
    }

    fun removeClip(clipId: String) {
        viewModelScope.launch {
            val clip = repository.getClip(clipId) ?: return@launch
            if (clip.status in setOf(ClipStatus.COMPLETED, ClipStatus.FAILED, ClipStatus.CANCELED, ClipStatus.QUEUED)) repository.deleteClip(clipId)
        }
    }

    fun setQuality(value: VideoQuality) {
        viewModelScope.launch { app.container.preferences.setQuality(value) }
    }

    fun setQueueMode(value: QueueMode) {
        viewModelScope.launch { app.container.preferences.setQueueMode(value) }
    }

    fun updateEngine() {
        if (_isUpdatingEngine.value) return
        viewModelScope.launch {
            _isUpdatingEngine.value = true
            _engineUpdateMessage.value = null
            runCatching { app.container.engine.updateStableEngine() }
                .onSuccess { _engineUpdateMessage.value = "Stable yt-dlp engine updated." }
                .onFailure { _engineUpdateMessage.value = it.message ?: "Couldn't update yt-dlp. Check your connection." }
            _isUpdatingEngine.value = false
        }
    }

    fun clearEngineUpdateMessage() {
        _engineUpdateMessage.value = null
    }
}

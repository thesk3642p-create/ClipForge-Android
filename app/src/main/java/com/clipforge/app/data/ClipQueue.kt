package com.clipforge.app.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.clipforge.app.ClipForgeApp
import com.clipforge.app.data.local.ClipStatus
import com.clipforge.app.data.preferences.QueueMode
import com.clipforge.app.worker.ClipDownloadWorker
import java.util.concurrent.TimeUnit

class ClipQueue(context: Context) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val app = appContext as ClipForgeApp
    private val repository = app.container.repository

    suspend fun enqueueProject(projectId: String, mode: QueueMode) {
        val ids = repository.pendingIds(projectId)
        if (ids.isEmpty()) return
        val requests = ids.map(::buildRequest)
        val chainName = "project-$projectId"
        ids.zip(requests).forEach { (id, request) -> repository.setScheduled(id, request.id.toString()) }
        val groupSize = if (mode == QueueMode.TWO_AT_ONCE) 2 else 1
        val groups = requests.chunked(groupSize)
        val chain = groups.drop(1).fold(
            workManager.beginUniqueWork(chainName, ExistingWorkPolicy.APPEND_OR_REPLACE, groups.first()),
        ) { continuation, group -> continuation.then(group) }
        chain.enqueue()
    }

    suspend fun enqueueRetry(clipId: String) {
        val clip = repository.getClip(clipId) ?: return
        if (clip.status !in setOf(ClipStatus.FAILED, ClipStatus.CANCELED)) return
        val request = buildRequest(clipId)
        repository.setScheduled(clipId, request.id.toString(), null)
        workManager.enqueueUniqueWork("retry-$clipId", ExistingWorkPolicy.REPLACE, request)
    }

    suspend fun cancel(clipId: String) {
        val clip = repository.getClip(clipId) ?: return
        repository.cancel(clipId)
        if (clip.status == ClipStatus.DOWNLOADING) app.container.engine.cancel("clipforge-$clipId")
    }

    private fun buildRequest(clipId: String): OneTimeWorkRequest = OneTimeWorkRequestBuilder<ClipDownloadWorker>()
        .setInputData(workDataOf(ClipDownloadWorker.KEY_CLIP_ID to clipId))
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build(),
        )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
        .addTag("clip-download")
        .addTag("clip-$clipId")
        .build()
}

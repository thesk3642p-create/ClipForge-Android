package com.clipforge.app.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.clipforge.app.ClipForgeApp
import com.clipforge.app.R
import com.clipforge.app.data.local.ClipStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@Keep
class ClipDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val app = applicationContext as ClipForgeApp
    private val repository = app.container.repository
    private val engine = app.container.engine
    private val clipId = inputData.getString(KEY_CLIP_ID)
    private val processId = "clipforge-${clipId ?: id}"

    override suspend fun doWork(): Result {
        val id = clipId ?: return Result.failure()
        val cache = java.io.File(applicationContext.cacheDir, "clipforge/segments/$id")
        val clip = repository.getClip(id) ?: return Result.failure()
        if (clip.status == ClipStatus.CANCELED) {
            cache.deleteRecursively()
            return Result.success()
        }
        return try {
            setForeground(createForegroundInfo(clip.label, 0))
            val project = repository.getProject(clip.projectId)
                ?: throw IllegalStateException("The project no longer exists.")
            val preferences = app.container.preferences.preferences.first()
            val claimed = repository.markDownloading(clip.id, this.id.toString())
            if (claimed == 0) return Result.success()
            val segment = engine.downloadSegment(
                clip = clip,
                cacheDirectory = cache,
                quality = preferences.quality,
                processId = processId,
                shouldContinue = { repository.getClip(id)?.status != ClipStatus.CANCELED },
            ) { progress, eta ->
                repository.updateProgress(clip.id, progress, eta)
                setProgress(workDataOf("progress" to progress, "etaSeconds" to (eta ?: -1L)))
                setForeground(createForegroundInfo(clip.label, progress))
            }
            if (repository.getClip(id)?.status == ClipStatus.CANCELED) {
                cache.deleteRecursively()
                return Result.success()
            }
            val export = engine.exportToMovies(segment, project.name, clip.orderIndex, clip.label)
            val completed = repository.markCompleted(clip.id, export.sizeBytes, export.uri.toString())
            if (completed == 0) engine.deleteExport(export.uri)
            cache.deleteRecursively()
            Result.success()
        } catch (cancelled: CancellationException) {
            engine.cancel(processId)
            val current = repository.getClip(id)
            if (current?.status == ClipStatus.DOWNLOADING) {
                repository.setScheduled(id, this.id.toString(), "Paused by Android; it will resume when scheduled.")
            }
            cache.deleteRecursively()
            throw cancelled
        } catch (error: Throwable) {
            if (repository.getClip(id)?.status == ClipStatus.CANCELED) {
                cache.deleteRecursively()
                return Result.success()
            }
            cache.deleteRecursively()
            val detail = error.message?.take(240) ?: "Download failed. Check the source and retry."
            if (runAttemptCount < MAX_RETRIES) {
                repository.setScheduled(id, this.id.toString(), detail)
                Result.retry()
            } else {
                repository.setTerminal(id, ClipStatus.FAILED, detail)
                Result.success()
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val clip = clipId?.let { repository.getClip(it) }
        return createForegroundInfo(clip?.label ?: "Preparing clip", clip?.progress ?: 0)
    }

    private fun createForegroundInfo(label: String, progress: Int): ForegroundInfo {
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ClipForge is cutting a segment")
            .setContentText(label)
            .setProgress(100, progress.coerceIn(0, 100), progress <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        val notificationId = clipId?.hashCode()?.and(Int.MAX_VALUE)?.coerceAtLeast(1) ?: FALLBACK_NOTIFICATION_ID
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        return ForegroundInfo(notificationId, notification, serviceType)
    }

    companion object {
        const val KEY_CLIP_ID = "clip_id"
        const val CHANNEL_ID = "clip_downloads"
        private const val FALLBACK_NOTIFICATION_ID = 4820
        private const val MAX_RETRIES = 2
    }
}

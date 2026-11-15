package com.clipforge.app.data

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import com.clipforge.app.data.local.ClipEntity
import com.clipforge.app.data.preferences.VideoQuality
import com.clipforge.app.domain.parser.Timecode
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class ExportedClip(val uri: Uri, val sizeBytes: Long)

class DownloadEngine(private val appContext: Context) {
    private val initMutex = Mutex()
    private val updateMutex = Mutex()
    private val engineAccess = ReentrantReadWriteLock()
    private val enginePreferences: SharedPreferences = appContext.getSharedPreferences(ENGINE_PREFERENCES, Context.MODE_PRIVATE)
    @Volatile private var initialized = false
    @Volatile private var engineGeneration = 0L

    suspend fun initialize() = withContext(Dispatchers.IO) {
        initializeNativeLibraries()
        updateStableEngineIfDue()
    }

    suspend fun updateStableEngine(): String = withContext(Dispatchers.IO) {
        initializeNativeLibraries()
        updateMutex.withLock {
            updateStableEngineLocked()
            val now = System.currentTimeMillis()
            enginePreferences.edit()
                .putLong(KEY_LAST_UPDATE_ATTEMPT, now)
                .putLong(KEY_LAST_SUCCESSFUL_UPDATE, now)
                .apply()
            engineGeneration++
        }
        "yt-dlp stable engine updated"
    }

    private suspend fun initializeNativeLibraries() {
        if (initialized) return
        initMutex.withLock {
            if (initialized) return@withLock
            YoutubeDL.getInstance().init(appContext)
            FFmpeg.getInstance().init(appContext)
            initialized = true
        }
    }

    private suspend fun updateStableEngineIfDue() {
        updateMutex.withLock {
            val now = System.currentTimeMillis()
            val lastSuccess = enginePreferences.getLong(KEY_LAST_SUCCESSFUL_UPDATE, 0L)
            val lastAttempt = enginePreferences.getLong(KEY_LAST_UPDATE_ATTEMPT, 0L)
            if (now - lastSuccess < ENGINE_UPDATE_INTERVAL_MS || now - lastAttempt < UPDATE_RETRY_INTERVAL_MS) return@withLock

            enginePreferences.edit().putLong(KEY_LAST_UPDATE_ATTEMPT, now).apply()
            try {
                updateStableEngineLocked()
                enginePreferences.edit().putLong(KEY_LAST_SUCCESSFUL_UPDATE, now).apply()
                engineGeneration++
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "Automatic yt-dlp update failed; the bundled engine will still be used.", error)
            }
        }
    }

    private fun updateStableEngineLocked() {
        val lock = engineAccess.writeLock()
        lock.lock()
        try {
            YoutubeDL.getInstance().updateYoutubeDL(appContext, YoutubeDL.UpdateChannel.STABLE)
        } finally {
            lock.unlock()
        }
    }

    private suspend fun updateAfterStaleEngineFailure(generationAtStart: Long) {
        updateMutex.withLock {
            if (engineGeneration != generationAtStart) return@withLock
            val now = System.currentTimeMillis()
            enginePreferences.edit().putLong(KEY_LAST_UPDATE_ATTEMPT, now).apply()
            updateStableEngineLocked()
            enginePreferences.edit().putLong(KEY_LAST_SUCCESSFUL_UPDATE, now).apply()
            engineGeneration++
        }
    }

    suspend fun downloadSegment(
        clip: ClipEntity,
        cacheDirectory: File,
        quality: VideoQuality,
        processId: String,
        shouldContinue: suspend () -> Boolean,
        onProgress: suspend (Int, Long?) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        initialize()
        cacheDirectory.deleteRecursively()
        check(cacheDirectory.mkdirs() || cacheDirectory.isDirectory) { "Couldn't create the private clip cache." }

        val output = File(cacheDirectory, "segment.%(ext)s").absolutePath
        val start = Timecode.format(clip.startSeconds)
        val end = Timecode.format(clip.endSeconds)
        val request = YoutubeDLRequest(clip.sourceUrl).apply {
            addOption("--download-sections", "*$start-$end")
            addOption("--force-keyframes-at-cuts")
            addOption("-f", quality.format)
            addOption("--merge-output-format", "mp4")
            addOption("--remux-video", "mp4")
            addOption("--no-playlist")
            addOption("--no-mtime")
            addOption("--force-overwrites")
            addOption("-o", output)
        }

        val lastProgressAt = AtomicLong(0L)
        val progressCallback: (Float, Long, String) -> kotlin.Unit = { progress, eta, _ ->
            val now = SystemClock.elapsedRealtime()
            val previous = lastProgressAt.get()
            if (now - previous >= 800L && lastProgressAt.compareAndSet(previous, now)) {
                runBlocking { onProgress(progress.roundToInt().coerceIn(0, 99), eta.takeIf { it >= 0L }) }
            }
            kotlin.Unit
        }
        check(shouldContinue()) { "Clip was canceled." }
        val generationAtStart = engineGeneration
        try {
            execute(request, processId, progressCallback)
        } catch (error: YoutubeDLException) {
            if (!error.isStaleEngineWarning()) throw error
            updateAfterStaleEngineFailure(generationAtStart)
            check(shouldContinue()) { "Clip was canceled." }
            cacheDirectory.deleteRecursively()
            check(cacheDirectory.mkdirs() || cacheDirectory.isDirectory) { "Couldn't recreate the private clip cache." }
            execute(request, processId, progressCallback)
        }
        // yt-dlp can leave per-format files such as segment.f137.mp4 beside the
        // post-processed output. Those are video-only/audio-only intermediates;
        // never export whichever MP4 happens to appear first in listFiles().
        val segment = File(cacheDirectory, "segment.mp4")
        check(segment.isFile && segment.length() > 0L) {
            "yt-dlp finished without the merged segment.mp4. The source streams could not be combined."
        }
        validateVideoSegment(segment, clip)
        segment
    }

    private fun validateVideoSegment(file: File, clip: ClipEntity) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                ?.equals("yes", ignoreCase = true) == true
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            check(hasVideo && width > 0 && height > 0) {
                "The downloaded MP4 has no playable video stream. Retry the clip or choose a lower quality."
            }
            val expectedMs = (clip.endSeconds - clip.startSeconds) * 1_000L
            val toleranceMs = maxOf(2_000L, expectedMs / 10L)
            check(durationMs > 0L && kotlin.math.abs(durationMs - expectedMs) <= toleranceMs) {
                "The downloaded segment is incomplete (${durationMs / 1_000L}s; expected ${expectedMs / 1_000L}s). Retry the clip."
            }
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: Exception) {
            throw IllegalStateException("The downloaded MP4 couldn't be opened as a video. Retry the clip.", error)
        } finally {
            retriever.release()
        }
    }

    private fun execute(request: YoutubeDLRequest, processId: String, progressCallback: (Float, Long, String) -> kotlin.Unit) {
        val lock = engineAccess.readLock()
        lock.lock()
        try {
            YoutubeDL.getInstance().execute(request, processId, progressCallback)
        } finally {
            lock.unlock()
        }
    }

    private fun Throwable.isStaleEngineWarning(): Boolean = generateSequence(this) { it.cause }
        .mapNotNull { it.message }
        .any { it.contains("older than 90 days", ignoreCase = true) || it.contains("older than 90", ignoreCase = true) }

    fun cancel(processId: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
    }

    suspend fun exportToMovies(
        source: File,
        projectName: String,
        orderIndex: Int,
        label: String,
    ): ExportedClip = withContext(Dispatchers.IO) {
        check(source.isFile && source.length() > 0L) { "The downloaded segment is empty." }
        val relativePath = "${Environment.DIRECTORY_MOVIES}/ClipForge/${sanitizePath(projectName)}"
        val fileName = "%02d_%s.mp4".format(Locale.US, orderIndex, sanitizeFileName(label))
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = appContext.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Android couldn't create the Movies library entry.")
        try {
            resolver.openOutputStream(uri, "w")?.use { output -> source.inputStream().buffered().use { input -> input.copyTo(output) } }
                ?: throw IllegalStateException("Android couldn't open the exported MP4.")
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            check(resolver.update(uri, values, null, null) > 0) { "Android couldn't publish the exported MP4." }
            ExportedClip(uri, source.length())
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    suspend fun deleteExport(uri: Uri) = withContext(Dispatchers.IO) {
        appContext.contentResolver.delete(uri, null, null)
    }

    private fun sanitizePath(value: String): String = value
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
        .trim().trim('.').ifBlank { "Project" }.take(80)

    private fun sanitizeFileName(value: String): String = value
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
        .replace(Regex("\\s+"), "_")
        .trim('_', '.').ifBlank { "Clip" }.take(100)

    companion object {
        private const val TAG = "ClipForgeEngine"
        private const val ENGINE_PREFERENCES = "clipforge_engine"
        private const val KEY_LAST_UPDATE_ATTEMPT = "yt_dlp_last_update_attempt"
        private const val KEY_LAST_SUCCESSFUL_UPDATE = "yt_dlp_last_successful_update"
        private const val ENGINE_UPDATE_INTERVAL_MS = 30L * 24L * 60L * 60L * 1000L
        private const val UPDATE_RETRY_INTERVAL_MS = 12L * 60L * 60L * 1000L
    }
}

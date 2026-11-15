package com.clipforge.app.data

import android.content.ContentValues
import android.content.Context
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
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class ExportedClip(val uri: Uri, val sizeBytes: Long)

class DownloadEngine(private val appContext: Context) {
    private val initMutex = Mutex()
    private val engineAccess = ReentrantReadWriteLock()
    @Volatile private var initialized = false

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        initMutex.withLock {
            if (initialized) return@withLock
            YoutubeDL.getInstance().init(appContext)
            FFmpeg.getInstance().init(appContext)
            initialized = true
        }
    }

    suspend fun updateStableEngine(): String = withContext(Dispatchers.IO) {
        initialize()
        val lock = engineAccess.writeLock()
        lock.lock()
        try {
            YoutubeDL.getInstance().updateYoutubeDL(appContext, YoutubeDL.UpdateChannel.STABLE)
        } finally {
            lock.unlock()
        }
        "yt-dlp stable engine updated"
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
        val lock = engineAccess.readLock()
        lock.lock()
        try {
            YoutubeDL.getInstance().execute(request, processId, progressCallback)
            cacheDirectory.listFiles()?.firstOrNull { it.isFile && it.extension.equals("mp4", ignoreCase = true) }
                ?: throw IllegalStateException("yt-dlp completed without an MP4 segment. Try another quality or source.")
        } finally {
            lock.unlock()
        }
    }

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
}

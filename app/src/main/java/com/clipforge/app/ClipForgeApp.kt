package com.clipforge.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.room.Room
import com.clipforge.app.data.ClipForgeRepository
import com.clipforge.app.data.DownloadEngine
import com.clipforge.app.data.local.ClipForgeDatabase
import com.clipforge.app.data.preferences.ClipForgePreferences
import com.clipforge.app.worker.ClipDownloadWorker
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClipForgeApp : Application(), Configuration.Provider {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val container: AppContainer by lazy { AppContainer(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setExecutor(Executors.newFixedThreadPool(2))
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        appScope.launch {
            runCatching { container.repository.recoverInterruptedDownloads() }
            runCatching { container.engine.initialize() }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ClipDownloadWorker.CHANNEL_ID,
                "Clip downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Progress for ClipForge segment downloads" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

class AppContainer(context: Application) {
    val database: ClipForgeDatabase = Room.databaseBuilder(
        context,
        ClipForgeDatabase::class.java,
        "clipforge.db",
    ).build()
    val repository = ClipForgeRepository(database.projectDao(), database.clipDao())
    val preferences = ClipForgePreferences(context)
    val engine = DownloadEngine(context.applicationContext)
}

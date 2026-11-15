package com.clipforge.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY orderIndex")
    fun observeForProject(projectId: String): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ClipEntity?

    @Query("SELECT * FROM clips WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ClipEntity?>

    @Query("SELECT * FROM clips WHERE id IN (:ids) ORDER BY orderIndex")
    suspend fun getByIds(ids: List<String>): List<ClipEntity>

    @Query("SELECT COALESCE(MAX(orderIndex), 0) + 1 FROM clips WHERE projectId = :projectId")
    suspend fun nextOrderIndex(projectId: String): Int

    @Query("SELECT id FROM clips WHERE projectId = :projectId AND status IN ('QUEUED', 'SCHEDULED') ORDER BY orderIndex")
    suspend fun pendingIds(projectId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(clips: List<ClipEntity>)

    @Query("UPDATE clips SET status = 'DOWNLOADING', progress = 0, etaSeconds = NULL, errorMessage = NULL, workId = :workId, attemptCount = attemptCount + 1 WHERE id = :id AND (status IN ('QUEUED', 'SCHEDULED') OR (status = 'DOWNLOADING' AND workId = :workId))")
    suspend fun markDownloading(id: String, workId: String): Int

    @Query("UPDATE clips SET status = 'SCHEDULED', progress = 0, etaSeconds = NULL WHERE status = 'DOWNLOADING'")
    suspend fun recoverInterruptedDownloads()

    @Query("UPDATE clips SET progress = :progress, etaSeconds = :eta WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun updateProgress(id: String, progress: Int, eta: Long?)

    @Query("UPDATE clips SET status = 'COMPLETED', progress = 100, etaSeconds = NULL, sizeBytes = :size, mediaUri = :uri, workId = NULL, errorMessage = NULL WHERE id = :id AND status = 'DOWNLOADING'")
    suspend fun markCompleted(id: String, size: Long, uri: String): Int

    @Query("UPDATE clips SET status = :status, errorMessage = :message, etaSeconds = NULL, workId = NULL WHERE id = :id")
    suspend fun setTerminal(id: String, status: String, message: String?)

    @Query("UPDATE clips SET status = 'SCHEDULED', progress = 0, etaSeconds = NULL, errorMessage = :message, workId = :workId WHERE id = :id")
    suspend fun setScheduled(id: String, workId: String?, message: String?)

    @Query("UPDATE clips SET workId = :workId WHERE id = :id")
    suspend fun setWorkId(id: String, workId: String?)

    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun delete(id: String)
}

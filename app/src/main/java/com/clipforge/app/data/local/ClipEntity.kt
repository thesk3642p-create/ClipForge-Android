package com.clipforge.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object ClipStatus {
    const val QUEUED = "QUEUED"
    const val SCHEDULED = "SCHEDULED"
    const val DOWNLOADING = "DOWNLOADING"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
    const val CANCELED = "CANCELED"
}

@Entity(
    tableName = "clips",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["projectId"]), Index(value = ["projectId", "orderIndex"], unique = true)],
)
data class ClipEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val orderIndex: Int,
    val label: String,
    val sourceUrl: String,
    val startSeconds: Long,
    val endSeconds: Long,
    val status: String = ClipStatus.QUEUED,
    val progress: Int = 0,
    val etaSeconds: Long? = null,
    val sizeBytes: Long = 0,
    val mediaUri: String? = null,
    val workId: String? = null,
    val errorMessage: String? = null,
    val attemptCount: Int = 0,
    val createdAt: Long,
)

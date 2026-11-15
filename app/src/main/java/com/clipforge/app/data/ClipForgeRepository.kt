package com.clipforge.app.data

import com.clipforge.app.data.local.ClipDao
import com.clipforge.app.data.local.ClipEntity
import com.clipforge.app.data.local.ClipStatus
import com.clipforge.app.data.local.ProjectDao
import com.clipforge.app.data.local.ProjectEntity
import com.clipforge.app.data.local.ProjectSummary
import com.clipforge.app.domain.model.ClipDraft
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ClipForgeRepository(
    private val projects: ProjectDao,
    private val clips: ClipDao,
) {
    fun observeProjects(): Flow<List<ProjectSummary>> = projects.observeSummaries()
    fun observeClips(projectId: String): Flow<List<ClipEntity>> = clips.observeForProject(projectId)
    fun observeClip(id: String): Flow<ClipEntity?> = clips.observeById(id)
    suspend fun getProject(id: String): ProjectEntity? = projects.getById(id)
    suspend fun getClip(id: String): ClipEntity? = clips.getById(id)
    suspend fun getClips(ids: List<String>): List<ClipEntity> = clips.getByIds(ids)
    suspend fun pendingIds(projectId: String): List<String> = clips.pendingIds(projectId)
    suspend fun recoverInterruptedDownloads() = clips.recoverInterruptedDownloads()

    suspend fun createProject(name: String): String {
        val now = System.currentTimeMillis()
        val cleanName = name.trim().take(80)
        require(cleanName.isNotBlank()) { "Project name can't be empty." }
        val id = UUID.randomUUID().toString()
        projects.insert(ProjectEntity(id, cleanName, now, now))
        return id
    }

    suspend fun addClips(projectId: String, drafts: List<ClipDraft>): List<String> {
        require(projects.getById(projectId) != null) { "Project no longer exists." }
        require(drafts.isNotEmpty() && drafts.all(ClipDraft::isValid)) { "Only valid clips can be added." }
        val firstOrder = clips.nextOrderIndex(projectId)
        val now = System.currentTimeMillis()
        val entities = drafts.mapIndexed { index, draft ->
            ClipEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                orderIndex = firstOrder + index,
                label = draft.label.trim().take(120),
                sourceUrl = draft.url,
                startSeconds = draft.startSeconds,
                endSeconds = draft.endSeconds,
                createdAt = now + index,
            )
        }
        clips.insertAll(entities)
        projects.touch(projectId, now)
        return entities.map { it.id }
    }

    suspend fun setScheduled(id: String, workId: String? = null, message: String? = null) = clips.setScheduled(id, workId, message)
    suspend fun setWorkId(id: String, workId: String?) = clips.setWorkId(id, workId)
    suspend fun markDownloading(id: String, workId: String): Int = clips.markDownloading(id, workId)
    suspend fun updateProgress(id: String, progress: Int, etaSeconds: Long?) = clips.updateProgress(id, progress.coerceIn(0, 100), etaSeconds)
    suspend fun markCompleted(id: String, size: Long, uri: String): Int = clips.markCompleted(id, size, uri)
    suspend fun setTerminal(id: String, status: String, message: String?) = clips.setTerminal(id, status, message)
    suspend fun deleteClip(id: String) = clips.delete(id)

    suspend fun cancel(id: String) {
        val current = clips.getById(id) ?: return
        if (current.status == ClipStatus.COMPLETED) return
        clips.setTerminal(id, ClipStatus.CANCELED, null)
    }
}

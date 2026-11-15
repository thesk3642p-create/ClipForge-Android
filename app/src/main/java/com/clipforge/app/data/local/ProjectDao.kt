package com.clipforge.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT p.*, COUNT(c.id) AS clipCount, SUM(CASE WHEN c.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCount, COALESCE(SUM(CASE WHEN c.status = 'COMPLETED' THEN c.sizeBytes ELSE 0 END), 0) AS storageBytes FROM projects p LEFT JOIN clips c ON c.projectId = p.id GROUP BY p.id ORDER BY p.updatedAt DESC")
    fun observeSummaries(): Flow<List<ProjectSummary>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(project: ProjectEntity)

    @Query("UPDATE projects SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: String, updatedAt: Long)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: String)
}

package com.clipforge.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class ProjectSummary(
    @androidx.room.Embedded val project: ProjectEntity,
    val clipCount: Int,
    val completedCount: Int,
    val storageBytes: Long,
)

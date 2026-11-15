package com.clipforge.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProjectEntity::class, ClipEntity::class], version = 1, exportSchema = false)
abstract class ClipForgeDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun clipDao(): ClipDao
}

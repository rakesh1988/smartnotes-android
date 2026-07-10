package com.smartnotes.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class SmartNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}

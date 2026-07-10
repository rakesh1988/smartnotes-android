package com.smartnotes.core.database

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = Note::class)
@Entity(tableName = "notes_fts")
data class NoteFtsEntity(
    @PrimaryKey
    val rowid: Long = 0,
    val title: String,
    val body: String
)

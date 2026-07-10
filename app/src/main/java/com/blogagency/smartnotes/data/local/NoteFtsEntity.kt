package com.blogagency.smartnotes.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "notes_fts")
data class NoteFtsEntity(
    @PrimaryKey
    val rowid: Long = 0,
    val title: String,
    val content: String
)

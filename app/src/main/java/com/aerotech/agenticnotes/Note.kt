package com.aerotech.agenticnotes

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val title: String,
    val content: String
)

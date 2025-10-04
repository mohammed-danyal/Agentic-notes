package com.aerotech.agenticnotes

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface NoteDao {
    // Upsert = (Insert + Update). A convenient function to add or modify a note.
    @Upsert
    suspend fun upsertNote(note: Note)

    @Query("SELECT * FROM notes_table ORDER BY title ASC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE id = :id")
    suspend fun getNoteById(id: UUID): Note?
}

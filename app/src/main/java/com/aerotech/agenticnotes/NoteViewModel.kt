package com.aerotech.agenticnotes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    // Get a reference to the DAO from the database
    private val noteDao = AppDatabase.getDatabase(application).noteDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // This now directly comes from the database and is always up to date
    private val _allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    // The UI will collect this flow, which is now powered by the database and search query
    val notes: StateFlow<List<Note>> = _searchQuery
        .combine(_allNotes) { query, notes ->
            if (query.isBlank()) {
                notes
            } else {
                notes.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.content.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // These functions now run on a background thread and talk to the database
    suspend fun getNoteById(id: UUID): Note? {
        return noteDao.getNoteById(id)
    }

    fun upsertNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) { // Use Dispatchers.IO for database operations
            noteDao.upsertNote(note)
        }
    }
    // In class NoteViewModel(...)

    fun saveNoteOnExit(noteId: UUID?, title: String, content: String) {
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()

        // Only save if there's something to save
        if (trimmedTitle.isNotBlank() || trimmedContent.isNotBlank()) {
            val noteToSave = Note(
                id = noteId ?: UUID.randomUUID(), // Use existing ID or create a new one
                title = trimmedTitle,
                content = trimmedContent
            )
            // The existing upsertNote function is perfect for this
            upsertNote(noteToSave)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}

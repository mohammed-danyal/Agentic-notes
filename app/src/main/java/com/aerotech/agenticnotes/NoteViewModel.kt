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
    private val _selectedNoteIds = MutableStateFlow<Set<UUID>>(emptySet())
    val selectedNoteIds: StateFlow<Set<UUID>> = _selectedNoteIds.asStateFlow()
    val isMultiSelectActive: StateFlow<Boolean> = _selectedNoteIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // The UI will collect this flow, which is now powered by the database and search query
    val notes: StateFlow<List<Note>> = combine(
        _allNotes,
        searchQuery,
    ) { notes, query ->
        val activeNotes = notes.filter { !it.isBinned } // Only show notes not in the bin
        if (query.isBlank()) {
            activeNotes
        } else {
            activeNotes.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- NEW: Flow for notes in the bin ---
    val binnedNotes: StateFlow<List<Note>> = _allNotes
        .map { notes -> notes.filter { it.isBinned } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- NEW: Multi-select functions ---
    fun toggleNoteSelection(noteId: UUID) {
        val currentSelection = _selectedNoteIds.value.toMutableSet()
        if (noteId in currentSelection) {
            currentSelection.remove(noteId)
        } else {
            currentSelection.add(noteId)
        }
        _selectedNoteIds.value = currentSelection
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    // --- NEW: Functions to move notes to and from the bin ---
    fun moveSelectedNotesToBin() {
        viewModelScope.launch(Dispatchers.IO) {
            val notesToBin = _selectedNoteIds.value
            notesToBin.forEach { noteId ->
                val note = getNoteById(noteId)?.copy(
                    isBinned = true,
                    binnedTimestamp = System.currentTimeMillis()
                )
                if (note != null) {
                    noteDao.upsertNote(note)
                }
            }
            // Use post to safely update the UI state from a background thread
            launch(Dispatchers.Main) {
                clearSelection()
            }
        }
    }

    fun restoreNote(noteId: UUID) {
        viewModelScope.launch(Dispatchers.IO) {
            getNoteById(noteId)?.copy(isBinned = false, binnedTimestamp = null)?.let {
                noteDao.upsertNote(it)
            }
        }
    }

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

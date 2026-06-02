package com.example.remindme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(
    private val noteDao: NoteDao,
    private val notebookDao: NotebookDao
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    private val _selectedNotebookId = MutableStateFlow<Long?>(null)
    val selectedNotebookId = _selectedNotebookId.asStateFlow()

    val allTags = noteDao.getAllTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allNotebooks = notebookDao.getAllNotebooks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val notes = combine(_searchQuery, _selectedTag, _selectedNotebookId) { query, tag, notebookId ->
        Triple(query, tag, notebookId)
    }.flatMapLatest { (query, tag, notebookId) ->
        when {
            notebookId != null -> noteDao.getNotesByNotebook(notebookId)
            tag != null -> noteDao.getNotesByTag(tag)
            query.isNotEmpty() -> noteDao.searchNotes(query)
            else -> noteDao.getAllNotesWithTags()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedNotes = noteDao.getDeletedNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
        if (_selectedTag.value != null) _selectedNotebookId.value = null
    }

    fun setSelectedNotebook(notebookId: Long?) {
        _selectedNotebookId.value = if (_selectedNotebookId.value == notebookId) null else notebookId
        if (_selectedNotebookId.value != null) _selectedTag.value = null
    }

    fun saveNote(note: Note, tags: List<Tag>) {
        viewModelScope.launch {
            noteDao.updateNoteWithTags(note, tags)
        }
    }

    fun moveToTrash(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(isDeleted = true, deletedAt = System.currentTimeMillis(), isPinned = false))
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(isDeleted = false, deletedAt = null))
        }
    }

    fun permanentDeleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }

    // Notebook management
    fun createNotebook(name: String, color: Long) {
        viewModelScope.launch {
            notebookDao.insert(Notebook(name = name, color = color))
        }
    }

    fun deleteNotebook(notebook: Notebook) {
        viewModelScope.launch {
            notebookDao.delete(notebook)
        }
    }

    fun renameNotebook(notebook: Notebook, newName: String) {
        viewModelScope.launch {
            notebookDao.update(notebook.copy(name = newName))
        }
    }

    fun toggleNotebookPin(notebook: Notebook) {
        viewModelScope.launch {
            notebookDao.update(notebook.copy(isPinned = !notebook.isPinned))
        }
    }
}

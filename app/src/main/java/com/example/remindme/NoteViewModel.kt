package com.example.remindme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(private val noteDao: NoteDao) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    val allTags = noteDao.getAllTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes = combine(_searchQuery, _selectedTag) { query, tag ->
        query to tag
    }.flatMapLatest { (query, tag) ->
        if (tag != null) {
            noteDao.getNotesByTag(tag)
        } else if (query.isNotEmpty()) {
            noteDao.searchNotes(query)
        } else {
            noteDao.getAllNotesWithTags()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun saveNote(note: Note, tags: List<Tag>) {
        viewModelScope.launch {
            noteDao.updateNoteWithTags(note, tags)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }
}

package com.example.remindme

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY isPinned DESC, createdAt DESC")
    fun searchNotes(query: String): Flow<List<NoteWithTags>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag)

    @Query("SELECT * FROM tags")
    fun getAllTags(): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteNoteTags(noteId: Long)

    @Transaction
    suspend fun updateNoteWithTags(note: Note, tags: List<Tag>) {
        val noteId = insertNote(note)
        deleteNoteTags(noteId)
        tags.forEach { tag ->
            insertTag(tag)
            insertNoteTagCrossRef(NoteTagCrossRef(noteId, tag.name))
        }
    }
    
    @Transaction
    @Query("""
        SELECT notes.* FROM notes 
        JOIN note_tag_cross_ref ON notes.id = note_tag_cross_ref.noteId 
        WHERE note_tag_cross_ref.tagName = :tagName
        ORDER BY isPinned DESC, createdAt DESC
    """)
    fun getNotesByTag(tagName: String): Flow<List<NoteWithTags>>
}

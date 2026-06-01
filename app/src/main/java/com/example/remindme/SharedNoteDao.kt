package com.example.remindme

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedNoteDao {
    @Query("SELECT * FROM shared_notes WHERE groupId = :groupId ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByGroup(groupId: String): Flow<List<SharedNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<SharedNoteEntity>)

    @Query("DELETE FROM shared_notes WHERE noteId = :noteId")
    suspend fun deleteNote(noteId: String)

    @Query("DELETE FROM shared_notes WHERE groupId = :groupId")
    suspend fun deleteNotesByGroup(groupId: String)
}

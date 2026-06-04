package com.example.remindme

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedNotebookDao {
    @Query("SELECT * FROM shared_notebooks WHERE groupId = :groupId ORDER BY isPinned DESC, createdAt DESC")
    fun getNotebooksByGroup(groupId: String): Flow<List<SharedNotebookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notebooks: List<SharedNotebookEntity>)

    @Query("DELETE FROM shared_notebooks WHERE id = :id")
    suspend fun deleteNotebook(id: String)

    @Query("DELETE FROM shared_notebooks WHERE groupId = :groupId")
    suspend fun deleteNotebooksByGroup(groupId: String)
}

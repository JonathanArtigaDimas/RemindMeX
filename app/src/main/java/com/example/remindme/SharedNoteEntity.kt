package com.example.remindme

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shared_notes")
data class SharedNoteEntity(
    @PrimaryKey val noteId: String,
    val groupId: String,
    val title: String,
    val content: String,
    val authorId: String,
    val authorName: String,
    val updatedAt: Long,
    val createdAt: Long,
    val imagePath: String? = null,
    val audioPath: String? = null,
    val color: Long = 0xFF1E293B,
    val isPinned: Boolean = false
)

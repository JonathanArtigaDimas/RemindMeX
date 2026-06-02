package com.example.remindme

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class UserFirestore(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val groupIds: List<String> = emptyList()
)

data class GroupFirestore(
    val groupId: String = "",
    val name: String = "Equipo",
    val inviteCode: String = "",
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val memberRoles: Map<String, String> = emptyMap(),
    val announcement: String = ""
)

data class SharedNotebookFirestore(
    val id: String = "",
    val groupId: String = "",
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val color: Long = 0xFF3B82F6
)

fun SharedNotebookFirestore.toEntity() = SharedNotebookEntity(
    id = id,
    groupId = groupId,
    name = name,
    createdAt = createdAt,
    color = color
)

data class NoteComment(
    val commentId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class MessageFirestore(
    val messageId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class NoteFirestore(
    val noteId: String = "",
    val groupId: String = "",
    val title: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val audioPath: String? = null,
    val color: Long = 0xFF1E293B,
    val isPinned: Boolean = false,
    val comments: List<NoteComment> = emptyList(),
    val notebookId: String? = null
)

sealed class UiNoteItem {
    data class Local(val noteWithTags: NoteWithTags) : UiNoteItem()
    data class Shared(val noteEntity: SharedNoteEntity) : UiNoteItem()
    data class NotebookItem(val notebook: Notebook) : UiNoteItem()
    
    val timestamp: Long
        get() = when(this) {
            is Local -> noteWithTags.note.createdAt
            is Shared -> noteEntity.createdAt
            is NotebookItem -> notebook.createdAt
        }

    val isPinned: Boolean
        get() = when(this) {
            is Local -> noteWithTags.note.isPinned
            is Shared -> noteEntity.isPinned
            is NotebookItem -> notebook.isPinned
        }
}

// Extension functions for mapping between Firestore and Room
fun NoteFirestore.toEntity(): SharedNoteEntity {
    val gson = Gson()
    return SharedNoteEntity(
        noteId = noteId,
        groupId = groupId,
        title = title,
        content = content,
        authorId = authorId,
        authorName = authorName,
        updatedAt = updatedAt,
        createdAt = createdAt,
        imagePath = imagePath,
        audioPath = audioPath,
        color = color,
        isPinned = isPinned,
        commentsJson = gson.toJson(comments),
        notebookId = notebookId
    )
}

fun SharedNoteEntity.toFirestore(): NoteFirestore {
    val gson = Gson()
    val itemType = object : TypeToken<List<NoteComment>>() {}.type
    val commentsList: List<NoteComment> = gson.fromJson(commentsJson, itemType) ?: emptyList()
    return NoteFirestore(
        noteId = noteId,
        groupId = groupId,
        title = title,
        content = content,
        authorId = authorId,
        authorName = authorName,
        updatedAt = updatedAt,
        createdAt = createdAt,
        imagePath = imagePath,
        audioPath = audioPath,
        color = color,
        isPinned = isPinned,
        comments = commentsList,
        notebookId = notebookId
    )
}

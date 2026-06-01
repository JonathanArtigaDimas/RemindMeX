package com.example.remindme

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
    val members: List<String> = emptyList()
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
    val isPinned: Boolean = false
)

// Extension functions for mapping between Firestore and Room
fun NoteFirestore.toEntity() = SharedNoteEntity(
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
    isPinned = isPinned
)

fun SharedNoteEntity.toFirestore() = NoteFirestore(
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
    isPinned = isPinned
)

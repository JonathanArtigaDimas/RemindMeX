package com.example.remindme

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val audioPath: String? = null,
    val isQuickNote: Boolean = false,
    val isPinned: Boolean = false,
    val color: Long = 0xFF1E293B,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val notebookId: Long? = null,
    val needsSync: Boolean = false, // Marcador para subida offline
    val imagePathsJson: String = "[]" // Soporte para múltiples imágenes
)

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey @androidx.annotation.NonNull val name: String
)

@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagName"],
    indices = [Index("tagName")]
)
data class NoteTagCrossRef(
    val noteId: Long,
    val tagName: String
)

data class NoteWithTags(
    @Embedded val note: Note,
    @Relation(
        parentColumn = "id",
        entityColumn = "name",
        associateBy = Junction(
            NoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagName"
        )
    )
    val tags: List<Tag>
)

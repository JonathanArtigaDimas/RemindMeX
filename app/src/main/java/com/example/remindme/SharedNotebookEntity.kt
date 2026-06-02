package com.example.remindme

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shared_notebooks")
data class SharedNotebookEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val color: Long = 0xFF3B82F6
)

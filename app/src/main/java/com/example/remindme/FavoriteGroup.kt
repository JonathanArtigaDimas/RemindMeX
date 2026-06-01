package com.example.remindme

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_groups")
data class FavoriteGroup(
    @PrimaryKey val groupId: String,
    val name: String,
    val lastAccessed: Long = System.currentTimeMillis()
)

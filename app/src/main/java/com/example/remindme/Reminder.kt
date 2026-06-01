package com.example.remindme

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String?,
    val dateTime: String, 
    val category: String?,
    val color: Long?,
    val sound: String? = "Campana",
    val repetition: String? = "Sin repetición",
    val repeatDays: String? = null, // Store as "1,2,3" for Mon, Tue, Wed...
    val isCompleted: Boolean = false,
    val type: String = "Recordatorio" // "Recordatorio" o "Alarma"
)

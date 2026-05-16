package com.example.remindme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun NewReminderModal(onDismiss: () -> Unit, onSave: (Reminder) -> Unit) {
    Text("New Reminder Form")
}

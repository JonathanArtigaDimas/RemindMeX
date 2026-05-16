package com.example.remindme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun ReminderHeader(reminderCount: Int) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    
    val (greeting, emoji) = when (hour) {
        in 6..11 -> "Buenos días" to "🌅"
        in 12..19 -> "Buenas tardes" to "☀️"
        else -> "Buenas noches" to "🌙"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$emoji $greeting Amor ❤️", 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Mis Recordatorios", 
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Custom Larger Badge
        Surface(
            modifier = Modifier
                .size(42.dp) // Aumentado el tamaño
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = reminderCount.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium, // Fuente más grande
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

package com.example.remindme.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(selectedTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            "inicio" to ("Inicio" to Icons.Default.Home),
            "notas" to ("Notas" to Icons.AutoMirrored.Filled.Notes),
            "calendario" to ("Calendario" to Icons.Default.CalendarMonth),
            "ajustes" to ("Ajustes" to Icons.Default.Settings)
        )

        items.forEach { (route, content) ->
            val (label, icon) = content
            val isSelected = selectedTab == route
            
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(route) },
                icon = { 
                    Icon(
                        imageVector = icon, 
                        contentDescription = label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    ) 
                },
                label = { 
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    ) 
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent, // Removes the background pill
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            )
        }
    }
}

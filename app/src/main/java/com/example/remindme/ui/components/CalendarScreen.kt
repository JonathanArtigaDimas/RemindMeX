package com.example.remindme.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remindme.Reminder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    reminders: List<Reminder>,
    onDayClick: (String) -> Unit
) {
    val initialPage = 500
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 1000 })
    val scope = rememberCoroutineScope()
    
    val currentCalendar = remember(pagerState.currentPage) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, pagerState.currentPage - initialPage)
        }
    }

    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentCalendar.time)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Calendario",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Month Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.width(24.dp))

            Text(
                text = monthName,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(160.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.width(24.dp))

            IconButton(onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Week Days
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            beyondViewportPageCount = 1
        ) { page ->
            val monthCalendar = Calendar.getInstance().apply {
                add(Calendar.MONTH, page - initialPage)
            }
            
            MonthGrid(
                calendar = monthCalendar,
                reminders = reminders,
                onDayClick = onDayClick
            )
        }
    }
}

@Composable
fun MonthGrid(
    calendar: Calendar,
    reminders: List<Reminder>,
    onDayClick: (String) -> Unit
) {
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK) - 1
    
    val prevMonthCalendar = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
    val daysInPrevMonth = prevMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    val totalCells = 42 // 6 rows * 7 days
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        userScrollEnabled = false // El scroll lo maneja el Pager o el contenedor principal si fuera necesario
    ) {
        items(totalCells) { index ->
            val dayNumber: Int
            val isCurrentMonth: Boolean
            
            if (index < firstDayOfWeek) {
                dayNumber = daysInPrevMonth - (firstDayOfWeek - index - 1)
                isCurrentMonth = false
            } else if (index < firstDayOfWeek + daysInMonth) {
                dayNumber = index - firstDayOfWeek + 1
                isCurrentMonth = true
            } else {
                dayNumber = index - (firstDayOfWeek + daysInMonth) + 1
                isCurrentMonth = false
            }

            val dateKey = if (isCurrentMonth) {
                String.format(Locale.getDefault(), "%02d/%02d/%d", dayNumber, currentMonth + 1, currentYear)
            } else ""

            CalendarDayCell(
                day = dayNumber,
                isCurrentMonth = isCurrentMonth,
                isToday = isCurrentMonth && isToday(dayNumber, currentMonth, currentYear),
                reminders = if (isCurrentMonth) reminders.filter { it.dateTime.startsWith(String.format(Locale.getDefault(), "%02d/%02d/%d", dayNumber, currentMonth + 1, currentYear)) } else emptyList(),
                onClick = { if (isCurrentMonth) onDayClick(dateKey) }
            )
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    reminders: List<Reminder>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else if (isCurrentMonth) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                fontSize = 16.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            
            if (reminders.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    reminders.take(3).forEach { reminder ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .padding(horizontal = 0.5.dp)
                                .clip(CircleShape)
                                .background(if (isToday) MaterialTheme.colorScheme.onPrimary else Color(reminder.color ?: 0xFF3B82F6))
                        )
                    }
                }
            }
        }
    }
}

fun isToday(day: Int, month: Int, year: Int): Boolean {
    val today = Calendar.getInstance()
    return today.get(Calendar.DAY_OF_MONTH) == day &&
            today.get(Calendar.MONTH) == month &&
            today.get(Calendar.YEAR) == year
}

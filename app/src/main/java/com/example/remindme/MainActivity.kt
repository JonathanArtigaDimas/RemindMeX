package com.example.remindme

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.remindme.ui.components.*
import com.example.remindme.ui.theme.RemindMeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

private val reminderDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        NotificationHelper.createNotificationChannel(this)
        checkAndRequestOverlayPermission()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val database = ReminderDatabase.getDatabase(this)
        val reminderDao = database.reminderDao()
        val noteDao = database.noteDao()
        val settingsManager = SettingsManager.getInstance(this)

        setContent {
            val currentTheme by settingsManager.currentTheme.collectAsState()
            val currentFont by settingsManager.currentFont.collectAsState()
            val fontSizeMultiplier by settingsManager.fontSizeMultiplier.collectAsState()
            val isFontBold by settingsManager.isFontBold.collectAsState()
            val isFontItalic by settingsManager.isFontItalic.collectAsState()

            RemindMeTheme(
                themeName = currentTheme, 
                fontName = currentFont,
                fontSizeMultiplier = fontSizeMultiplier,
                isFontBold = isFontBold,
                isFontItalic = isFontItalic
            ) {
                val noteViewModel: NoteViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return NoteViewModel(noteDao) as T
                        }
                    }
                )

                val tabs = listOf("inicio", "notas", "calendario", "ajustes")
                val pagerState = rememberPagerState(pageCount = { tabs.size })
                val coroutineScope = rememberCoroutineScope()
                
                val currentTab = tabs[pagerState.currentPage]

                val showReminderModal = remember { mutableStateOf(false) }
                val editingReminder = remember { mutableStateOf<Reminder?>(null) }
                
                val showNoteEditor = remember { mutableStateOf(false) }
                val showQuickNoteSheet = remember { mutableStateOf(false) }
                val editingNote = remember { mutableStateOf<NoteWithTags?>(null) }
                val isQuickNote = remember { mutableStateOf(false) }

                val reminders by reminderDao.getAllReminders().collectAsState(initial = emptyList())
                
                var reminderToDelete by remember { mutableStateOf<Reminder?>(null) }
                var toastMessage by remember { mutableStateOf<String?>(null) }
                
                LaunchedEffect(toastMessage) {
                    if (toastMessage != null) {
                        delay(3000)
                        toastMessage = null
                    }
                }

                val categorizedReminders = remember(reminders) {
                    val now = Calendar.getInstance()
                    val todayStr = reminderDateFormat.format(now.time)
                    
                    val helperCal = Calendar.getInstance()
                    val todayMillis = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    val next7Limit = todayMillis + (7 * 24 * 60 * 60 * 1000L)
                    
                    val today = mutableListOf<Reminder>()
                    val next7Days = mutableListOf<Reminder>()
                    val future = mutableListOf<Reminder>()

                    reminders.forEach { r ->
                        try {
                            val rDateStr = r.dateTime.split(" ")[0]
                            val rDate = reminderDateFormat.parse(rDateStr) ?: return@forEach
                            
                            helperCal.time = rDate
                            helperCal.set(Calendar.HOUR_OF_DAY, 0)
                            helperCal.set(Calendar.MINUTE, 0)
                            helperCal.set(Calendar.SECOND, 0)
                            helperCal.set(Calendar.MILLISECOND, 0)
                            val rDateMillis = helperCal.timeInMillis
                            
                            if (rDateStr == todayStr || rDateMillis <= todayMillis) {
                                today.add(r)
                            } else if (rDateMillis <= next7Limit) {
                                next7Days.add(r)
                            } else {
                                future.add(r)
                            }
                        } catch (e: Exception) {
                            future.add(r)
                        }
                    }
                    listOf(
                        "📅 Hoy" to today,
                        "🗓️ Próximos 7 Días" to next7Days,
                        "📆 Próximos Meses" to future
                    )
                }
                
                val showCalendarReminderDialog = remember { mutableStateOf(false) }
                val selectedCalendarDate = remember { mutableStateOf("") }
                val selectedDateReminders = remember {
                    derivedStateOf {
                        reminders.filter { it.dateTime.startsWith(selectedCalendarDate.value) }
                    }
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = { 
                            BottomNavigationBar(
                                selectedTab = currentTab,
                                onTabSelected = { tab ->
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(tabs.indexOf(tab))
                                    }
                                }
                            ) 
                        },
                        floatingActionButton = {
                            when (currentTab) {
                                "inicio" -> {
                                    FloatingActionButton(
                                        onClick = { 
                                            editingReminder.value = null
                                            showReminderModal.value = true 
                                        },
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
                                }
                                "notas" -> {
                                    FloatingActionButton(
                                        onClick = { showQuickNoteSheet.value = true },
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Icon(Icons.Default.ElectricBolt, contentDescription = "Nota rápida")
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            userScrollEnabled = false // Desactivamos el swipe para máxima fluidez
                        ) { page ->
                            when (tabs[page]) {
                                "inicio" -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        ReminderHeader(reminders.size)
                                        
                                        if (reminders.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(bottom = 80.dp) // Offset for FAB
                                                ) {
                                                    Text(
                                                        text = "🎉",
                                                        style = MaterialTheme.typography.displayLarge
                                                    )
                                                    Spacer(modifier = Modifier.height(24.dp))
                                                    Text(
                                                        text = "Sin recordatorios pendientes",
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "Toca el botón + para crear tu primer recordatorio.",
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        modifier = Modifier.padding(horizontal = 32.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(bottom = 16.dp)
                                            ) {
                                                categorizedReminders.forEach { (sectionTitle, items) ->
                                                    if (items.isNotEmpty()) {
                                                        item {
                                                            Text(
                                                                text = sectionTitle.uppercase(),
                                                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                                                style = MaterialTheme.typography.labelLarge,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.secondary,
                                                                letterSpacing = 1.sp
                                                            )
                                                        }
                                                        
                                                        items(
                                                            count = items.size,
                                                            key = { index -> items[index].id },
                                                            contentType = { "reminder" }
                                                        ) { index ->
                                                            val r = items[index]
                                                            val parts = r.dateTime.split(" ")
                                                            val rTime = if (parts.size >= 2) parts[1] else ""

                                                            ReminderCard(
                                                                title = r.title, 
                                                                subtitle = r.description ?: "", 
                                                                tag = r.category ?: "General", 
                                                                time = rTime, 
                                                                color = r.color ?: 0xFF3B82F6, 
                                                                isCompleted = r.isCompleted,
                                                                onToggleComplete = {
                                                                    lifecycleScope.launch {
                                                                        reminderDao.update(r.copy(isCompleted = !r.isCompleted))
                                                                    }
                                                                },
                                                                onEdit = {
                                                                    editingReminder.value = r
                                                                    showReminderModal.value = true
                                                                },
                                                                onDelete = { 
                                                                    reminderToDelete = r
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                "notas" -> {
                                    NotesScreen(
                                        viewModel = noteViewModel,
                                        onAddNote = {
                                            editingNote.value = null
                                            isQuickNote.value = false
                                            showNoteEditor.value = true
                                        },
                                        onEditNote = {
                                            editingNote.value = it
                                            isQuickNote.value = false
                                            showNoteEditor.value = true
                                        }
                                    )
                                }
                                "calendario" -> {
                                    CalendarScreen(
                                        reminders = reminders,
                                        onDayClick = { date ->
                                            selectedCalendarDate.value = date
                                            showCalendarReminderDialog.value = true
                                        }
                                    )
                                }
                                "ajustes" -> {
                                    SettingsScreen(
                                        settingsManager = settingsManager,
                                        onClearCompleted = {
                                            lifecycleScope.launch {
                                                val completedReminders = reminders.filter { it.isCompleted }
                                                completedReminders.forEach { reminderDao.delete(it) }
                                            }
                                        },
                                        onResetAll = {
                                            settingsManager.resetToDefault()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (showReminderModal.value) {
                        ReminderEditorOverlay(
                            editingReminder = editingReminder.value,
                            settingsManager = settingsManager,
                            onDismiss = { showReminderModal.value = false },
                            onSave = { r ->
                                lifecycleScope.launch {
                                    val time = r.dateTime.split(" ")[1]
                                    val formattedTime = try {
                                        val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        val sdf12 = SimpleDateFormat("h:mm a", Locale.getDefault())
                                        sdf12.format(sdf24.parse(time)!!)
                                    } catch (e: Exception) { time }

                                    if (r.id != 0) {
                                        reminderDao.update(r)
                                        toastMessage = "Recordatorio actualizado para las $formattedTime"
                                    } else {
                                        val newId = reminderDao.insert(r).toInt()
                                        val date = r.dateTime.split(" ")[0]
                                        ReminderScheduler.scheduleReminder(this@MainActivity, newId, r.title, date, time, r.repetition ?: "Sin repetición", r.repeatDays)
                                        toastMessage = "Recordatorio creado para las $formattedTime"
                                    }
                                }
                                showReminderModal.value = false
                            }
                        )
                    }

                    if (reminderToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { reminderToDelete = null },
                            title = { Text("Eliminar recordatorio") },
                            text = { Text("¿Estás seguro de que deseas eliminar este recordatorio? No podrás recuperarlo.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        reminderToDelete?.let { r ->
                                            ReminderScheduler.cancelReminder(this@MainActivity, r.id)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                reminderDao.delete(r)
                                            }
                                        }
                                        reminderToDelete = null
                                    }
                                ) {
                                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { reminderToDelete = null }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                    if (showNoteEditor.value) {
                        val tags by noteViewModel.allTags.collectAsState()
                        NoteEditorScreen(
                            noteWithTags = editingNote.value,
                            isQuickNote = isQuickNote.value,
                            availableTags = tags,
                            onDismiss = { showNoteEditor.value = false },
                            onSave = { note, tags ->
                                noteViewModel.saveNote(note, tags)
                            },
                            onDelete = { note ->
                                noteViewModel.deleteNote(note)
                            }
                        )
                    }

                    if (showQuickNoteSheet.value) {
                        QuickNoteBottomSheet(
                            onDismiss = { showQuickNoteSheet.value = false },
                            onSave = { content, imagePath, audioPath, color ->
                                val finalTitle = if (content.isEmpty()) {
                                    if (imagePath != null) "Foto rapida"
                                    else if (audioPath != null) "Audio rapido"
                                    else "Nota rapida"
                                } else ""
                                
                                val finalContent = if (content.isEmpty()) "sin descripcion" else content

                                noteViewModel.saveNote(
                                    Note(
                                        title = finalTitle, 
                                        content = finalContent,
                                        isQuickNote = true,
                                        imagePath = imagePath,
                                        audioPath = audioPath,
                                        color = color
                                    ),
                                    emptyList()
                                )
                                showQuickNoteSheet.value = false
                            }
                        )
                    }

                    if (showCalendarReminderDialog.value) {
                        CalendarDayDetailSheet(
                            date = selectedCalendarDate.value,
                            reminders = selectedDateReminders.value,
                            onDismiss = { showCalendarReminderDialog.value = false },
                            onCreateReminder = {
                                editingReminder.value = Reminder(
                                    title = "",
                                    description = "",
                                    dateTime = "${selectedCalendarDate.value} 12:00",
                                    category = "Personal",
                                    color = 0xFF3B82F6
                                )
                                showReminderModal.value = true
                                showCalendarReminderDialog.value = false
                            },
                            onToggleComplete = { r ->
                                lifecycleScope.launch {
                                    reminderDao.update(r.copy(isCompleted = !r.isCompleted))
                                }
                            },
                            onEditReminder = { r ->
                                editingReminder.value = r
                                showReminderModal.value = true
                                showCalendarReminderDialog.value = false
                            },
                            onDeleteReminder = { r ->
                                reminderToDelete = r
                            }
                        )
                    }

                    // Custom Toast matching user image
                    AnimatedVisibility(
                        visible = toastMessage != null,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp)
                    ) {
                        toastMessage?.let { msg ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFF2C2C2E).copy(alpha = 0.95f),
                                tonalElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // App Icon Container
                                    Surface(
                                        modifier = Modifier.size(42.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(Modifier.width(16.dp))
                                    
                                    Column {
                                        val parts = msg.split(" para las ")
                                        Text(
                                            text = if (parts.size > 1) parts[0] + " para las" else msg,
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                        if (parts.size > 1) {
                                            Text(
                                                text = parts[1],
                                                color = Color.White,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }
}

@Composable
fun ReminderEditorOverlay(
    editingReminder: Reminder?,
    settingsManager: SettingsManager,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    val calendar = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MINUTE, 1) }
    val defaultDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(calendar.time)
    val defaultTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(calendar.time)

    val initialDate = editingReminder?.dateTime?.split(" ")?.getOrNull(0) ?: defaultDate
    val initialTime = editingReminder?.dateTime?.split(" ")?.getOrNull(1) ?: defaultTime
    
    val defaultSound by settingsManager.defaultNotificationSound.collectAsState()

    NewReminderModal(
        initialTitle = editingReminder?.title ?: "",
        initialDescription = editingReminder?.description ?: "",
        initialDate = initialDate,
        initialTime = initialTime,
        initialCategory = editingReminder?.category ?: "Personal",
        initialColor = editingReminder?.color ?: 0xFF3B82F6,
        initialSound = editingReminder?.sound ?: defaultSound,
        initialRepetition = editingReminder?.repetition ?: "Sin repetición",
        initialRepeatDays = editingReminder?.repeatDays,
        onDismiss = onDismiss,
        onSave = { title, desc, date, time, category, color, sound, repetition, repeatDays ->
            val reminder = (editingReminder ?: Reminder(
                title = title, 
                description = desc, 
                dateTime = "$date $time",
                category = category,
                color = color
            )).copy(
                title = title,
                description = desc,
                dateTime = "$date $time",
                category = category,
                color = color,
                sound = sound,
                repetition = repetition,
                repeatDays = repeatDays
            )
            onSave(reminder)
        }
    )
}

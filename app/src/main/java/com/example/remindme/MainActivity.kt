package com.example.remindme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.remindme.ui.components.*
import com.example.remindme.ui.theme.RemindMeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SyncWorker.schedule(this)
        checkAndRequestOverlayPermission()
        checkAndRequestBatteryOptimization()

        setContent {
            val settingsManager = remember { SettingsManager.getInstance(applicationContext) }
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
                val database = ReminderDatabase.getDatabase(applicationContext)
                val reminderDao = database.reminderDao()
                val noteDao = database.noteDao()

                val noteViewModel: NoteViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return NoteViewModel(noteDao, database.notebookDao()) as T
                        }
                    }
                )

                val sharedViewModel: SharedNotesViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            val repo = SharedNotesRepository(
                                com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                                com.google.firebase.auth.FirebaseAuth.getInstance(),
                                database.sharedNoteDao(),
                                database.favoriteGroupDao(),
                                database.sharedNotebookDao()
                            )
                            val storageRepo = SharedStorageRepository(
                                com.google.firebase.storage.FirebaseStorage.getInstance()
                            )
                            return SharedNotesViewModel(repo, storageRepo, applicationContext) as T
                        }
                    }
                )

                val reminders by reminderDao.getAllReminders().collectAsState(initial = emptyList())
                
                val showReminderEditor = remember { mutableStateOf(false) }
                val editingReminder = remember { mutableStateOf<Reminder?>(null) }
                val showNoteEditor = remember { mutableStateOf(false) }
                val showQuickNote = remember { mutableStateOf(false) }
                val editingNote = remember { mutableStateOf<NoteWithTags?>(null) }
                val isQuickNote = remember { mutableStateOf(false) }
                val noteEditorIsShared = remember { mutableStateOf(false) }
                val editingSharedNoteId = remember { mutableStateOf<String?>(null) }
                val editingNoteComments = remember { mutableStateOf("[]") }
                
                var showReminderTrash by remember { mutableStateOf(false) }
                var reminderToDelete by remember { mutableStateOf<Reminder?>(null) }
                var showClearCompletedDialog by remember { mutableStateOf(false) }

                val selectedTab = remember { mutableStateOf("inicio") }
                val selectedDate = remember { mutableStateOf("") }
                val showCalendarSheet = remember { mutableStateOf(false) }
                
                val sharedNotes by sharedViewModel.sharedNotesFromRoom.collectAsState()
                
                LaunchedEffect(sharedNotes) {
                    editingSharedNoteId.value?.let { currentId ->
                        sharedNotes.find { it.noteId == currentId }?.let { updatedNote ->
                            editingNoteComments.value = updatedNote.commentsJson
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        bottomBar = {
                            BottomNavigationBar(
                                selectedTab = selectedTab.value,
                                onTabSelected = { selectedTab.value = it }
                            )
                        },
                        floatingActionButton = {
                            if (selectedTab.value == "inicio" && !showReminderTrash) {
                                FloatingActionButton(
                                    onClick = { 
                                        editingReminder.value = null
                                        showReminderEditor.value = true 
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Nuevo Recordatorio")
                                }
                            } else if (selectedTab.value == "notas") {
                                val currentNotebookId by noteViewModel.selectedNotebookId.collectAsState()
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // New Standard Note FAB
                                    FloatingActionButton(
                                        onClick = { 
                                            editingNote.value = NoteWithTags(
                                                note = Note(
                                                    title = "",
                                                    content = "",
                                                    notebookId = currentNotebookId
                                                ),
                                                tags = emptyList()
                                            )
                                            isQuickNote.value = false
                                            noteEditorIsShared.value = false
                                            editingSharedNoteId.value = null
                                            editingNoteComments.value = "[]"
                                            showNoteEditor.value = true
                                        },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(Icons.Default.EditNote, contentDescription = "Nueva Nota")
                                    }

                                    // Quick Note FAB
                                    FloatingActionButton(
                                        onClick = { showQuickNote.value = true },
                                        containerColor = Color(0xFFF59E0B),
                                        contentColor = Color.White,
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(Icons.Default.ElectricBolt, contentDescription = "Nota Rápida")
                                    }
                                }
                            } else if (selectedTab.value == "compartidas") {
                                val currentGroupId by sharedViewModel.currentGroupId.collectAsState()
                                val selectedInternalTab by sharedViewModel.selectedInternalTab.collectAsState()
                                if (currentGroupId != null && selectedInternalTab == 0) {
                                    FloatingActionButton(
                                        onClick = { 
                                            val currentNotebookId = sharedViewModel.selectedNotebookId.value
                                            editingNote.value = NoteWithTags(
                                                note = Note(
                                                    title = "",
                                                    content = "",
                                                    notebookId = currentNotebookId?.toLongOrNull()
                                                ),
                                                tags = emptyList()
                                            )
                                            isQuickNote.value = false
                                            noteEditorIsShared.value = true
                                            editingSharedNoteId.value = null
                                            showNoteEditor.value = true
                                        },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = "Compartir Nota")
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            when (selectedTab.value) {
                                "inicio" -> {
                                    val homeBg by settingsManager.homeBg.collectAsState()
                                    val bgOpacity by settingsManager.bgOpacity.collectAsState()
                                    BackgroundWrapper(imageUri = homeBg, opacity = bgOpacity) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val isSpecialMode by settingsManager.isSpecialMode.collectAsState()
                                                ReminderHeader(
                                                    reminderCount = reminders.count { !it.isCompleted && !it.isDeleted },
                                                    isSpecialMode = isSpecialMode
                                                )
                                                IconButton(
                                                    onClick = { showReminderTrash = !showReminderTrash },
                                                    modifier = Modifier.padding(end = 16.dp)
                                                ) {
                                                    Icon(
                                                        if (showReminderTrash) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.DeleteOutline,
                                                        contentDescription = "Papelera",
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                            
                                            if (showReminderTrash) {
                                                val trashedReminders = reminders.filter { it.isDeleted }.sortedByDescending { it.deletedAt }
                                                if (trashedReminders.isEmpty()) {
                                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        Text("Papelera vacía", color = Color.Gray)
                                                    }
                                                } else {
                                                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                                                        items(
                                                            count = trashedReminders.size,
                                                            key = { index -> "trash_${trashedReminders[index].id}" }
                                                        ) { index ->
                                                            val r = trashedReminders[index]
                                                            ReminderCard(
                                                                title = r.title,
                                                                subtitle = "Eliminado (Se borrará en 30 días)",
                                                                tag = r.category ?: "General",
                                                                time = "",
                                                                color = r.color ?: 0xFF3B82F6,
                                                                isCompleted = false,
                                                                onToggleComplete = {
                                                                    lifecycleScope.launch {
                                                                        reminderDao.update(r.copy(isDeleted = false, deletedAt = null))
                                                                    }
                                                                },
                                                                onEdit = {},
                                                                onDelete = {
                                                                    lifecycleScope.launch {
                                                                        reminderDao.delete(r)
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                val now = Calendar.getInstance()
                                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                                
                                                val todayStr = sdf.format(now.time)
                                                val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                                                val tomorrowStr = sdf.format(tomorrowCal.time)
                                                
                                                val weekLaterCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
                                                val monthLaterCal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }

                                                val activeReminders = reminders.filter { !it.isDeleted }
                                                
                                                val todayReminders = activeReminders.filter { 
                                                    it.dateTime.split(" ").getOrNull(0) == todayStr 
                                                }
                                                val tomorrowReminders = activeReminders.filter { 
                                                    it.dateTime.split(" ").getOrNull(0) == tomorrowStr 
                                                }
                                                
                                                val next7DaysReminders = activeReminders.filter {
                                                    val dateStr = it.dateTime.split(" ").getOrNull(0) ?: ""
                                                    try {
                                                        val date = sdf.parse(dateStr)
                                                        date != null && date.after(tomorrowCal.time) && !date.after(weekLaterCal.time)
                                                    } catch (e: Exception) { false }
                                                }
                                                
                                                val nextMonthReminders = activeReminders.filter {
                                                    val dateStr = it.dateTime.split(" ").getOrNull(0) ?: ""
                                                    try {
                                                        val date = sdf.parse(dateStr)
                                                        date != null && date.after(weekLaterCal.time) && !date.after(monthLaterCal.time)
                                                    } catch (e: Exception) { false }
                                                }

                                                if (activeReminders.isEmpty()) {
                                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text("✨", fontSize = 64.sp)
                                                            Spacer(Modifier.height(16.dp))
                                                            Text(
                                                                "No tienes tareas pendientes",
                                                                style = MaterialTheme.typography.headlineSmall,
                                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                                            )
                                                            Text(
                                                                "¡Disfruta de tu día libre!",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    LazyColumn(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentPadding = PaddingValues(16.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        if (todayReminders.isNotEmpty()) {
                                                            item { SectionHeader("HOY", Icons.Default.CalendarToday) }
                                                            items(todayReminders.size) { index ->
                                                                val r = todayReminders[index]
                                                                ReminderCard(
                                                                    title = r.title,
                                                                    subtitle = if (r.isCompleted) "¡Completado!" else "Pendiente",
                                                                    tag = r.category ?: "General",
                                                                    time = r.dateTime.split(" ").getOrNull(1) ?: "",
                                                                    color = r.color ?: 0xFF3B82F6,
                                                                    isCompleted = r.isCompleted,
                                                                    onToggleComplete = {
                                                                        lifecycleScope.launch {
                                                                            reminderDao.update(r.copy(isCompleted = !r.isCompleted))
                                                                        }
                                                                    },
                                                                    onEdit = {
                                                                        editingReminder.value = r
                                                                        showReminderEditor.value = true
                                                                    },
                                                                    onDelete = { reminderToDelete = r }
                                                                )
                                                            }
                                                        }

                                                        if (tomorrowReminders.isNotEmpty()) {
                                                            item { SectionHeader("MAÑANA", Icons.Default.CalendarMonth) }
                                                            items(tomorrowReminders.size) { index ->
                                                                val r = tomorrowReminders[index]
                                                                ReminderCard(
                                                                    title = r.title,
                                                                    subtitle = "Mañana",
                                                                    tag = r.category ?: "General",
                                                                    time = r.dateTime.split(" ").getOrNull(1) ?: "",
                                                                    color = r.color ?: 0xFF3B82F6,
                                                                    isCompleted = r.isCompleted,
                                                                    onToggleComplete = {
                                                                        lifecycleScope.launch {
                                                                            reminderDao.update(r.copy(isCompleted = !r.isCompleted))
                                                                        }
                                                                    },
                                                                    onEdit = {
                                                                        editingReminder.value = r
                                                                        showReminderEditor.value = true
                                                                    },
                                                                    onDelete = { reminderToDelete = r }
                                                                )
                                                            }
                                                        }

                                                        if (next7DaysReminders.isNotEmpty()) {
                                                            item { SectionHeader("PRÓXIMOS 7 DÍAS", Icons.Default.DateRange) }
                                                            items(next7DaysReminders.size) { index ->
                                                                val r = next7DaysReminders[index]
                                                                ReminderCard(
                                                                    title = r.title,
                                                                    subtitle = r.dateTime.split(" ").getOrNull(0) ?: "",
                                                                    tag = r.category ?: "General",
                                                                    time = r.dateTime.split(" ").getOrNull(1) ?: "",
                                                                    color = r.color ?: 0xFF3B82F6,
                                                                    isCompleted = r.isCompleted,
                                                                    onToggleComplete = {
                                                                        lifecycleScope.launch {
                                                                            reminderDao.update(r.copy(isCompleted = !r.isCompleted))
                                                                        }
                                                                    },
                                                                    onEdit = {
                                                                        editingReminder.value = r
                                                                        showReminderEditor.value = true
                                                                    },
                                                                    onDelete = { reminderToDelete = r }
                                                                )
                                                            }
                                                        }

                                                        if (nextMonthReminders.isNotEmpty()) {
                                                            item { SectionHeader("ESTE MES", Icons.Default.Event) }
                                                            items(nextMonthReminders.size) { index ->
                                                                val r = nextMonthReminders[index]
                                                                ReminderCard(
                                                                    title = r.title,
                                                                    subtitle = r.dateTime.split(" ").getOrNull(0) ?: "",
                                                                    tag = r.category ?: "General",
                                                                    time = r.dateTime.split(" ").getOrNull(1) ?: "",
                                                                    color = r.color ?: 0xFF3B82F6,
                                                                    isCompleted = r.isCompleted,
                                                                    onToggleComplete = {
                                                                        lifecycleScope.launch {
                                                                            reminderDao.update(r.copy(isCompleted = !r.isCompleted))
                                                                        }
                                                                    },
                                                                    onEdit = {
                                                                        editingReminder.value = r
                                                                        showReminderEditor.value = true
                                                                    },
                                                                    onDelete = { reminderToDelete = r }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                "notas" -> {
                                    val currentNotebookId by noteViewModel.selectedNotebookId.collectAsState()
                                    NotesScreen(
                                        viewModel = noteViewModel,
                                        sharedViewModel = sharedViewModel,
                                        settingsManager = settingsManager,
                                        onEditNote = { noteWithTags ->
                                            editingNote.value = noteWithTags
                                            isQuickNote.value = false
                                            noteEditorIsShared.value = false
                                            editingSharedNoteId.value = null
                                            editingNoteComments.value = "[]"
                                            showNoteEditor.value = true
                                        },
                                        onEditSharedNote = { sharedNote ->
                                            editingNote.value = NoteWithTags(
                                                note = Note(
                                                    title = sharedNote.title,
                                                    content = sharedNote.content,
                                                    createdAt = sharedNote.createdAt,
                                                    color = 0xFF1E293B
                                                ),
                                                tags = emptyList()
                                            )
                                            isQuickNote.value = false
                                            noteEditorIsShared.value = true
                                            editingSharedNoteId.value = sharedNote.noteId
                                            editingNoteComments.value = sharedNote.commentsJson
                                            showNoteEditor.value = true
                                        }
                                    )
                                }
                                "compartidas" -> {
                                    SharedNotesScreen(
                                        viewModel = sharedViewModel,
                                        settingsManager = settingsManager,
                                        onAddSharedNote = {
                                            val currentNotebookId = sharedViewModel.selectedNotebookId.value
                                            editingNote.value = NoteWithTags(
                                                note = Note(
                                                    title = "",
                                                    content = "",
                                                    notebookId = currentNotebookId?.toLongOrNull()
                                                ),
                                                tags = emptyList()
                                            )
                                            isQuickNote.value = false
                                            noteEditorIsShared.value = true
                                            editingSharedNoteId.value = null
                                            editingNoteComments.value = "[]"
                                            showNoteEditor.value = true
                                        },
                                        onEditSharedNote = { sharedNote ->
                                            editingNote.value = NoteWithTags(
                                                note = Note(
                                                    title = sharedNote.title,
                                                    content = sharedNote.content,
                                                    createdAt = sharedNote.createdAt,
                                                    color = sharedNote.color,
                                                    imagePath = sharedNote.imagePath,
                                                    audioPath = sharedNote.audioPath
                                                ),
                                                tags = emptyList()
                                            )
                                            isQuickNote.value = false
                                            noteEditorIsShared.value = true
                                            editingSharedNoteId.value = sharedNote.noteId
                                            editingNoteComments.value = sharedNote.commentsJson
                                            showNoteEditor.value = true
                                        }
                                    )
                                }
                                "calendario" -> {
                                    CalendarScreen(
                                        reminders = reminders,
                                        onDayClick = { date ->
                                            selectedDate.value = date
                                            showCalendarSheet.value = true
                                        }
                                    )
                                }
                                "ajustes" -> {
                                    SettingsScreen(
                                        settingsManager = settingsManager,
                                        onClearCompleted = { showClearCompletedDialog = true },
                                        onResetAll = { settingsManager.resetToDefault() }
                                    )
                                }
                            }
                        }
                    }

                    if (showReminderEditor.value) {
                        val r = editingReminder.value
                        val currentSelectedDate = selectedDate.value // Capture from calendar selection
                        
                        NewReminderModal(
                            initialTitle = r?.title ?: "",
                            initialDescription = r?.description ?: "",
                            initialDate = if (r != null) {
                                r.dateTime.split(" ").getOrNull(0) ?: ""
                            } else if (currentSelectedDate.isNotEmpty()) {
                                currentSelectedDate
                            } else "",
                            initialTime = r?.dateTime?.split(" ")?.getOrNull(1) ?: "",
                            initialCategory = r?.category ?: "Personal",
                            initialColor = r?.color ?: 0xFF3B82F6,
                            initialSound = r?.sound ?: "Campana",
                            initialRepetition = r?.repetition ?: "Sin repetición",
                            initialRepeatDays = r?.repeatDays,
                            initialType = r?.type ?: "Recordatorio",
                            onDismiss = { 
                                showReminderEditor.value = false
                                editingReminder.value = null
                            },
                            onSave = { title, desc, date, time, cat, color, sound, rep, days, type ->
                                val newR = r?.copy(
                                    title = title, description = desc, dateTime = "$date $time",
                                    category = cat, color = color, sound = sound, repetition = rep,
                                    repeatDays = days, type = type
                                ) ?: Reminder(
                                    id = 0, title = title, description = desc, dateTime = "$date $time",
                                    category = cat, color = color, sound = sound, repetition = rep,
                                    repeatDays = days, isCompleted = false, type = type
                                )
                                lifecycleScope.launch {
                                    if (newR.id == 0) {
                                        val id = reminderDao.insert(newR).toInt()
                                        val scheduledR = newR.copy(id = id)
                                        ReminderScheduler.scheduleReminder(
                                            context = this@MainActivity,
                                            id = id,
                                            title = scheduledR.title,
                                            date = scheduledR.dateTime.split(" ").getOrNull(0) ?: "",
                                            time = scheduledR.dateTime.split(" ").getOrNull(1) ?: "",
                                            repetition = scheduledR.repetition ?: "Sin repetición",
                                            repeatDays = scheduledR.repeatDays
                                        )
                                    } else {
                                        reminderDao.update(newR)
                                        ReminderScheduler.scheduleReminder(
                                            context = this@MainActivity,
                                            id = newR.id,
                                            title = newR.title,
                                            date = newR.dateTime.split(" ").getOrNull(0) ?: "",
                                            time = newR.dateTime.split(" ").getOrNull(1) ?: "",
                                            repetition = newR.repetition ?: "Sin repetición",
                                            repeatDays = newR.repeatDays
                                        )
                                    }
                                }
                                showReminderEditor.value = false
                                editingReminder.value = null
                            }
                        )
                    }

                    if (showNoteEditor.value) {
                        val tags by noteViewModel.allTags.collectAsState()
                        val notebooks by noteViewModel.allNotebooks.collectAsState()
                        val sharedNotebooks by sharedViewModel.sharedNotebooksFromRoom.collectAsState()
                        val currentGroupId by sharedViewModel.currentGroupId.collectAsState()
                        
                        NoteEditorScreen(
                            noteWithTags = editingNote.value,
                            isQuickNote = isQuickNote.value,
                            initialIsShared = noteEditorIsShared.value,
                            availableTags = tags,
                            availableNotebooks = notebooks,
                            sharedNotebooks = sharedNotebooks,
                            currentGroupId = currentGroupId,
                            commentsJson = editingNoteComments.value,
                            onDismiss = { 
                                showNoteEditor.value = false 
                                editingNote.value = null
                                noteEditorIsShared.value = false
                                editingSharedNoteId.value = null
                                editingNoteComments.value = "[]"
                            },
                            onSave = { note, tags, isShared ->
                                lifecycleScope.launch {
                                    if (isShared && currentGroupId != null) {
                                        sharedViewModel.shareExistingNote(
                                            groupId = currentGroupId!!, 
                                            title = note.title, 
                                            content = note.content,
                                            imagePath = note.imagePath,
                                            audioPath = note.audioPath,
                                            color = note.color,
                                            isPinned = note.isPinned,
                                            noteId = editingSharedNoteId.value,
                                            notebookId = note.notebookId?.toString()
                                        )
                                    } else {
                                        noteViewModel.saveNote(note, tags)
                                    }
                                }
                                showNoteEditor.value = false
                                editingNote.value = null
                                noteEditorIsShared.value = false
                                editingSharedNoteId.value = null
                                editingNoteComments.value = "[]"
                            },
                            onDelete = { note ->
                                noteViewModel.moveToTrash(note)
                                showNoteEditor.value = false
                                editingNote.value = null
                                noteEditorIsShared.value = false
                                editingSharedNoteId.value = null
                                editingNoteComments.value = "[]"
                            },
                            onAddComment = { _, text ->
                                editingSharedNoteId.value?.let { noteId ->
                                    sharedViewModel.addComment(noteId, text)
                                }
                            },
                            onEditComment = { _, commentId, text ->
                                editingSharedNoteId.value?.let { noteId ->
                                    sharedViewModel.editComment(noteId, commentId, text)
                                }
                            },
                            onDeleteComment = { _, commentId ->
                                editingSharedNoteId.value?.let { noteId ->
                                    sharedViewModel.deleteComment(noteId, commentId)
                                }
                            }
                        )
                    }

                    if (showQuickNote.value) {
                        val currentNotebookId by noteViewModel.selectedNotebookId.collectAsState()
                        QuickNoteBottomSheet(
                            onDismiss = { showQuickNote.value = false },
                            onSave = { content, imagePath, audioPath, color ->
                                val note = Note(
                                    title = "",
                                    content = content,
                                    imagePath = imagePath,
                                    audioPath = audioPath,
                                    color = color,
                                    notebookId = currentNotebookId
                                )
                                lifecycleScope.launch {
                                    noteViewModel.saveNote(note, emptyList())
                                }
                                showQuickNote.value = false
                            }
                        )
                    }

                    if (showClearCompletedDialog) {
                        AlertDialog(
                            onDismissRequest = { showClearCompletedDialog = false },
                            title = { Text("Limpiar completados") },
                            text = { Text("¿Deseas eliminar permanentemente todos los recordatorios completados?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        lifecycleScope.launch {
                                            val completed = reminders.filter { it.isCompleted }
                                            completed.forEach { reminderDao.delete(it) }
                                        }
                                        showClearCompletedDialog = false
                                    }
                                ) {
                                    Text("Limpiar Todo", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearCompletedDialog = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                    if (showCalendarSheet.value) {
                        CalendarDayDetailSheet(
                            date = selectedDate.value,
                            reminders = reminders.filter { it.dateTime.startsWith(selectedDate.value) && !it.isDeleted },
                            onDismiss = { showCalendarSheet.value = false },
                            onCreateReminder = {
                                editingReminder.value = null
                                showCalendarSheet.value = false
                                showReminderEditor.value = true
                            },
                            onToggleComplete = { r ->
                                lifecycleScope.launch {
                                    reminderDao.update(r.copy(isCompleted = !r.isCompleted))
                                }
                            },
                            onEditReminder = { r ->
                                editingReminder.value = r
                                showCalendarSheet.value = false
                                showReminderEditor.value = true
                            },
                            onDeleteReminder = { r ->
                                reminderToDelete = r
                            }
                        )
                    }

                    if (reminderToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { reminderToDelete = null },
                            title = { Text("Mover a la papelera") },
                            text = { Text("El recordatorio se moverá a la papelera y se eliminará permanentemente después de 30 días.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        reminderToDelete?.let { r ->
                                            ReminderScheduler.cancelReminder(this@MainActivity, r.id)
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                reminderDao.update(r.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
                                            }
                                        }
                                        reminderToDelete = null
                                    }
                                ) {
                                    Text("Mover a papelera", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { reminderToDelete = null }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun checkAndRequestBatteryOptimization() {
        val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
}

package com.example.remindme.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.remindme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NoteViewModel,
    sharedViewModel: SharedNotesViewModel,
    settingsManager: SettingsManager,
    onEditNote: (NoteWithTags) -> Unit,
    onEditSharedNote: (SharedNoteEntity) -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val deletedNotes by viewModel.deletedNotes.collectAsState()
    val notebooks by viewModel.allNotebooks.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val selectedNotebookId by viewModel.selectedNotebookId.collectAsState()
    
    val currentGroupId by sharedViewModel.currentGroupId.collectAsState()
    val notesBg by settingsManager.notesBg.collectAsState()
    val bgOpacity by settingsManager.bgOpacity.collectAsState()

    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var notebookToDelete by remember { mutableStateOf<Notebook?>(null) }
    var showTrash by remember { mutableStateOf(false) }
    var showAddNotebookDialog by remember { mutableStateOf(false) }
    var notebookToRename by remember { mutableStateOf<Notebook?>(null) }
    var newNotebookName by remember { mutableStateOf("") }

    val mergedItems = remember(notes, notebooks, searchQuery, selectedTag, selectedNotebookId, showTrash) {
        if (showTrash) {
            deletedNotes.map { UiNoteItem.Local(it) }
        } else if (selectedNotebookId != null) {
            notes.filter { it.note.notebookId == selectedNotebookId }
                .map { UiNoteItem.Local(it) }
        } else {
            val notebookItems = notebooks.map { UiNoteItem.NotebookItem(it) }
            val standaloneNotes = notes.filter { it.note.notebookId == null }
                .filter { 
                    (it.note.title.contains(searchQuery, ignoreCase = true) || 
                    it.note.content.contains(searchQuery, ignoreCase = true)) &&
                    (selectedTag == null || it.tags.any { tag -> tag.name == selectedTag })
                }
                .map { UiNoteItem.Local(it) }
            
            (notebookItems + standaloneNotes)
                .sortedWith(compareByDescending<UiNoteItem> { it.isPinned }.thenByDescending { it.timestamp })
        }
    }

    BackgroundWrapper(imageUri = notesBg, opacity = bgOpacity) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedNotebookId != null && !showTrash) {
                        IconButton(onClick = { viewModel.setSelectedNotebook(null) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        text = when {
                            showTrash -> "Papelera"
                            selectedNotebookId != null -> notebooks.find { it.id == selectedNotebookId }?.name ?: "Cuaderno"
                            else -> "Mis Notas"
                        },
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!showTrash) {
                        if (selectedNotebookId == null) {
                            IconButton(onClick = { showAddNotebookDialog = true }) {
                                Icon(Icons.Default.CreateNewFolder, "Nuevo cuaderno", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(onClick = { showTrash = true }) {
                            Icon(Icons.Default.DeleteOutline, "Ver papelera", tint = MaterialTheme.colorScheme.secondary)
                        }
                    } else {
                        IconButton(onClick = { showTrash = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver a notas", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            if (!showTrash && selectedNotebookId == null) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Buscar notas...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag.name,
                            onClick = { viewModel.setSelectedTag(tag.name) },
                            label = { Text(tag.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (mergedItems.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (showTrash) "🗑️" else "🎉", style = MaterialTheme.typography.displayLarge)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = if (showTrash) "Papelera vacía" else "Sin contenido", style = MaterialTheme.typography.titleLarge)
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(
                        items = mergedItems,
                        key = { item ->
                            when(item) {
                                is UiNoteItem.Local -> "local_${item.noteWithTags.note.id}"
                                is UiNoteItem.Shared -> "shared_${item.noteEntity.noteId}"
                                is UiNoteItem.NotebookItem -> "notebook_${item.notebook.id}"
                            }
                        }
                    ) { item ->
                        when(item) {
                            is UiNoteItem.Local -> {
                                NoteCard(
                                    item.noteWithTags, 
                                    isTrashMode = showTrash,
                                    onClick = { if (!showTrash) onEditNote(item.noteWithTags) },
                                    onTogglePin = { viewModel.togglePin(item.noteWithTags.note) },
                                    onDelete = { noteToDelete = item.noteWithTags.note },
                                    onRestore = { viewModel.restoreNote(item.noteWithTags.note) }
                                )
                            }
                            is UiNoteItem.NotebookItem -> {
                                NotebookCard(
                                    notebook = item.notebook,
                                    onClick = { viewModel.setSelectedNotebook(item.notebook.id) },
                                    onRename = { notebookToRename = item.notebook; newNotebookName = item.notebook.name },
                                    onTogglePin = { viewModel.toggleNotebookPin(item.notebook) },
                                    onDelete = { notebookToDelete = item.notebook }
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    if (showAddNotebookDialog) {
        AlertDialog(
            onDismissRequest = { showAddNotebookDialog = false },
            title = { Text("Nuevo Cuaderno") },
            text = {
                OutlinedTextField(
                    value = newNotebookName,
                    onValueChange = { newNotebookName = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newNotebookName.isNotBlank()) {
                        viewModel.createNotebook(newNotebookName, 0xFF3B82F6)
                        newNotebookName = ""
                        showAddNotebookDialog = false
                    }
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showAddNotebookDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (notebookToRename != null) {
        AlertDialog(
            onDismissRequest = { notebookToRename = null },
            title = { Text("Renombrar Cuaderno") },
            text = {
                OutlinedTextField(
                    value = newNotebookName,
                    onValueChange = { newNotebookName = it },
                    label = { Text("Nuevo nombre") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newNotebookName.isNotBlank() && notebookToRename != null) {
                        viewModel.renameNotebook(notebookToRename!!, newNotebookName)
                        newNotebookName = ""
                        notebookToRename = null
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { notebookToRename = null }) { Text("Cancelar") }
            }
        )
    }

    if (notebookToDelete != null) {
        AlertDialog(
            onDismissRequest = { notebookToDelete = null },
            title = { Text("Eliminar cuaderno") },
            text = { Text("¿Deseas eliminar el cuaderno \"${notebookToDelete?.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    notebookToDelete?.let { viewModel.deleteNotebook(it) }
                    notebookToDelete = null
                }) { Text("Eliminar", color = Color.Red) }
            }
        )
    }

    if (noteToDelete != null) {
        val isTrashItem = noteToDelete?.isDeleted == true
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(if (isTrashItem) "Eliminar permanentemente" else "Mover a la papelera") },
            text = { Text(if (isTrashItem) "¿Deseas eliminar esta nota para siempre?" else "La nota se moverá a la papelera.") },
            confirmButton = {
                TextButton(onClick = {
                    noteToDelete?.let { 
                        if (isTrashItem) viewModel.permanentDeleteNote(it) else viewModel.moveToTrash(it) 
                    }
                    noteToDelete = null
                }) { Text("Confirmar", color = Color.Red) }
            }
        )
    }
}

@Composable
fun NotebookCard(notebook: Notebook, onClick: () -> Unit, onRename: () -> Unit, onTogglePin: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (notebook.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (notebook.isPinned) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Surface(Modifier.size(64.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(text = notebook.name, fontWeight = FontWeight.ExtraBold, maxLines = 1, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    noteWithTags: NoteWithTags, 
    isTrashMode: Boolean = false,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit = {}
) {
    val note = noteWithTags.note
    val dateFormatter = remember { SimpleDateFormat("d/M/yyyy", Locale.getDefault()) }
    val dateString = dateFormatter.format(Date(note.createdAt))
    val context = androidx.compose.ui.platform.LocalContext.current
    val playingSoundPath by SoundManager.currentSoundPath.collectAsState()
    val isPlaying = playingSoundPath == note.audioPath && note.audioPath != null

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column {
            if (note.imagePath != null) {
                AsyncImage(
                    model = note.imagePath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(text = if (note.title.isNotEmpty()) note.title else note.content, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (!isTrashMode) {
                        IconButton(onClick = onTogglePin, modifier = Modifier.size(24.dp)) {
                            Icon(if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin, null, tint = if (note.isPinned) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (note.title.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = note.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                if (note.audioPath != null && !isTrashMode) {
                    Spacer(Modifier.height(12.dp))
                    Surface(Modifier.fillMaxWidth().height(44.dp).clickable { SoundManager.playSound(context, note.audioPath!!) }, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Nota de voz", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(text = dateString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    if (!isTrashMode) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = onRestore, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }
    }
}

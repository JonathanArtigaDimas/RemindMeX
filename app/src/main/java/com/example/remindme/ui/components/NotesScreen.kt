package com.example.remindme.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.remindme.Note
import com.example.remindme.NoteViewModel
import com.example.remindme.NoteWithTags
import com.example.remindme.SoundManager
import com.example.remindme.Tag

import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

sealed class UiNoteItem {
    data class Local(val noteWithTags: com.example.remindme.NoteWithTags) : UiNoteItem()
    data class Shared(val noteEntity: com.example.remindme.SharedNoteEntity) : UiNoteItem()
    
    val timestamp: Long get() = when(this) {
        is Local -> noteWithTags.note.createdAt
        is Shared -> noteEntity.updatedAt
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NoteViewModel,
    sharedViewModel: com.example.remindme.SharedNotesViewModel,
    onAddNote: (Boolean) -> Unit,
    onEditNote: (NoteWithTags) -> Unit,
    onEditSharedNote: (com.example.remindme.SharedNoteEntity) -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val sharedNotes by sharedViewModel.sharedNotesFromRoom.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    
    val currentGroupId by sharedViewModel.currentGroupId.collectAsState()

    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var noteToShare by remember { mutableStateOf<Note?>(null) }
    var showAddSharedNoteDialog by remember { mutableStateOf(false) }

    val mergedNotes = remember(notes, searchQuery, selectedTag) {
        notes.filter { 
            (it.note.title.contains(searchQuery, ignoreCase = true) || 
            it.note.content.contains(searchQuery, ignoreCase = true)) &&
            (selectedTag == null || it.tags.any { tag -> tag.name == selectedTag })
        }
        .map { UiNoteItem.Local(it) }
        .sortedByDescending { it.timestamp }
    }

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
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mis Notas",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Botón Nota Local
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAddNote(false) },
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("Nueva", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Botón Nota Compartida (Eliminado de aquí por petición del usuario)
            }
        }

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Buscar notas...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tags Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                FilterChip(
                    selected = selectedTag == tag.name,
                    onClick = { viewModel.setSelectedTag(tag.name) },
                    label = { Text(tag.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notes Grid
        if (mergedNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                        text = "Sin notas aún",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Empieza a crear notas locales o compartidas.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
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
                    items = mergedNotes,
                    key = { "local_${it.noteWithTags.note.id}" }
                ) { item ->
                    NoteCard(
                        item.noteWithTags, 
                        canShare = currentGroupId != null,
                        isSharedToCloud = false, // Ahora son independientes
                        onClick = { onEditNote(item.noteWithTags) },
                        onTogglePin = { viewModel.togglePin(item.noteWithTags.note) },
                        onDelete = { noteToDelete = item.noteWithTags.note },
                        onShare = { noteToShare = item.noteWithTags.note }
                    )
                }
            }
        }
    }

    if (showAddSharedNoteDialog) {
        // Diálogo informativo si no hay grupo (ya no llamamos al diálogo de creación directo aquí)
        AlertDialog(
            onDismissRequest = { showAddSharedNoteDialog = false },
            title = { Text("Grupo no seleccionado") },
            text = { Text("Para crear una nota compartida, primero debes unirte o crear un grupo en la sección de 'Notas Compartidas'.") },
            confirmButton = {
                Button(onClick = { showAddSharedNoteDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (noteToShare != null && currentGroupId != null) {
        AlertDialog(
            onDismissRequest = { noteToShare = null },
            title = { Text("Compartir nota") },
            text = { Text("¿Deseas subir esta nota al grupo de trabajo actual con todo su contenido (imágenes, audio y color)?") },
            confirmButton = {
                Button(onClick = {
                    noteToShare?.let { note ->
                        sharedViewModel.shareExistingNote(
                            groupId = currentGroupId!!, 
                            title = note.title, 
                            content = note.content,
                            imagePath = note.imagePath,
                            audioPath = note.audioPath,
                            color = note.color
                        )
                    }
                    noteToShare = null
                }) {
                    Text("Compartir")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToShare = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Eliminar nota", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("¿Estás seguro de que deseas eliminar esta nota? Esta acción no se puede deshacer.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    noteToDelete?.let { viewModel.deleteNote(it) }
                    noteToDelete = null
                }) {
                    Text("Eliminar", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun NoteCard(
    noteWithTags: NoteWithTags, 
    canShare: Boolean = false,
    isSharedToCloud: Boolean = false,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit = {}
) {
    val note = noteWithTags.note
    val dateFormatter = remember { SimpleDateFormat("d/M/yyyy", Locale.getDefault()) }
    val dateString = dateFormatter.format(Date(note.createdAt))
    val context = androidx.compose.ui.platform.LocalContext.current
    val playingSoundPath by SoundManager.currentSoundPath.collectAsState()
    val isPlaying = playingSoundPath == note.audioPath && note.audioPath != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            // Image at the top (full width, no padding)
            if (note.imagePath != null) {
                AsyncImage(
                    model = note.imagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp) // Reducido un poco el alto máximo
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(12.dp)) { // Padding reducido de 16 a 12
                // Title and Pin Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (note.title.isNotEmpty()) note.title else note.content,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = { onTogglePin() },
                        modifier = Modifier.size(24.dp) // Reducido de 28 a 24
                    ) {
                        Icon(
                            if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin note",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp) // Reducido de 20 a 18
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp)) // Reducido de 8 a 4

                // Content (Italic)
                if (note.title.isNotEmpty()) {
                    Text(
                        text = note.content,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (note.audioPath != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { SoundManager.playSound(context, note.audioPath!!) },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Grabación de voz",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer (Date and Trash)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateString,
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    IconButton(
                        onClick = { onDelete() },
                        modifier = Modifier.size(28.dp) // Reducido de 32 a 28
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    if (canShare) {
                        IconButton(
                            onClick = { if (!isSharedToCloud) onShare() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (isSharedToCloud) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                contentDescription = if (isSharedToCloud) "Ya compartida" else "Compartir nota",
                                tint = if (isSharedToCloud) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

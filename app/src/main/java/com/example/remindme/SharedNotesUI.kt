package com.example.remindme

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SharedNotesScreen(
    viewModel: SharedNotesViewModel,
    onAddSharedNote: () -> Unit,
    onEditSharedNote: (SharedNoteEntity) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val notes by viewModel.sharedNotesFromRoom.collectAsState()
    val currentGroupId by viewModel.currentGroupId.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    var generatedInviteCode by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (currentUser == null) {
            com.example.remindme.ui.components.AuthScreen(viewModel)
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (currentGroupId == null) {
                    GroupManagementScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        currentUser = currentUser!!,
                        onCodeGenerated = { generatedInviteCode = it }
                    )
                } else {
                    SharedNotesTeamsList(
                        notes = notes,
                        groupId = currentGroupId!!,
                        currentUser = currentUser!!,
                        viewModel = viewModel,
                        onExitGroup = { viewModel.selectGroup("") },
                        onAddNote = onAddSharedNote,
                        onEditNote = onEditSharedNote
                    )
                }
            }
        }
    }
    
    // Diálogo para mostrar el código de invitación generado
    if (generatedInviteCode != null) {
        AlertDialog(
            onDismissRequest = { generatedInviteCode = null; viewModel.resetState() },
            title = { Text("Grupo Creado") },
            text = { 
                Column {
                    Text("Comparte este código con tu equipo para que puedan unirse:")
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = generatedInviteCode!!,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { generatedInviteCode = null; viewModel.resetState() }) {
                    Text("Entendido")
                }
            }
        )
    }

    // Overlay de carga para subida de fotos/notas
    if (uiState is SharedNotesUiState.Loading) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Subiendo a la nube...")
                }
            },
            text = { Text("Estamos guardando tu imagen y nota en el servidor del equipo. Por favor, no cierres la app.") }
        )
    }
}

@Composable
fun GroupManagementScreen(
    viewModel: SharedNotesViewModel,
    uiState: SharedNotesUiState,
    currentUser: com.google.firebase.auth.FirebaseUser,
    onCodeGenerated: (String) -> Unit
) {
    var inviteCode by remember { mutableStateOf("") }
    var newGroupName by remember { mutableStateOf("") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    val favoriteGroups by viewModel.favoriteGroups.collectAsState()

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hola, ${currentUser.displayName ?: "Usuario"}", 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Colabora con tu equipo", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = { viewModel.logout() }) {
                Icon(Icons.Default.Logout, "Cerrar sesión", tint = MaterialTheme.colorScheme.error)
            }
        }
        
        if (favoriteGroups.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Tus Grupos Favoritos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            favoriteGroups.forEach { group ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { viewModel.selectGroup(group.groupId) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = group.name, fontWeight = FontWeight.Bold)
                            Text(text = "ID: ${group.groupId}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        IconButton(onClick = { viewModel.removeFavorite(group.groupId) }) {
                            Icon(Icons.Default.Star, "Quitar de favoritos", tint = Color(0xFFFFB300))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Text(
                    text = "O UNIRSE A OTRO", 
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GroupAdd, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Unirse a un grupo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { if (it.length <= 6) inviteCode = it.uppercase() },
                    label = { Text("Código de 6 dígitos") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.joinGroup(inviteCode) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = inviteCode.length == 6 && uiState !is SharedNotesUiState.Loading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState is SharedNotesUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Unirse al Grupo")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Text(
                text = "O CREA UNO NUEVO", 
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedButton(
            onClick = { showCreateGroupDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = uiState !is SharedNotesUiState.Loading,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Crear Nuevo Grupo de Trabajo")
        }

        if (uiState is SharedNotesUiState.Error) {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Nombre del Equipo") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Ej: Proyecto Final, Familia...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        if (newGroupName.isNotBlank()) {
                            viewModel.createNewGroup(newGroupName, onCodeGenerated)
                            showCreateGroupDialog = false
                            newGroupName = ""
                        }
                    },
                    enabled = newGroupName.isNotBlank()
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun SharedNotesTeamsList(
    notes: List<SharedNoteEntity>,
    groupId: String,
    currentUser: com.google.firebase.auth.FirebaseUser,
    viewModel: SharedNotesViewModel,
    onExitGroup: () -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (SharedNoteEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var noteToDelete by remember { mutableStateOf<SharedNoteEntity?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    
    val favoriteGroups by viewModel.favoriteGroups.collectAsState()
    val currentGroupName = remember(favoriteGroups, groupId) {
        favoriteGroups.find { it.groupId == groupId }?.name ?: "Notas del Equipo"
    }

    val filteredNotes = remember(notes, searchQuery) {
        notes.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentGroupName, 
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(onClick = { 
                        newGroupName = currentGroupName
                        showRenameDialog = true 
                    }) {
                        Icon(Icons.Default.Edit, "Cambiar nombre", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text("ID: ${groupId}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onAddNote,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir", style = MaterialTheme.typography.labelLarge)
                }
                
                Spacer(Modifier.width(8.dp))
                
                IconButton(onClick = onExitGroup) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Salir del grupo", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar en el equipo...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
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
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredNotes.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌥️", style = androidx.compose.ui.text.TextStyle(fontSize = 48.sp))
                    Spacer(Modifier.height(16.dp))
                    Text("No hay notas compartidas aún", color = Color.Gray)
                    TextButton(onClick = onAddNote) {
                        Text("Crear la primera nota del equipo")
                    }
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
                items(filteredNotes) { note ->
                    SharedNoteCard(
                        note = note,
                        onClick = { onEditNote(note) },
                        onTogglePin = { viewModel.togglePin(note) },
                        onDelete = { noteToDelete = note }
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renombrar Equipo") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Nuevo nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        if (newGroupName.isNotBlank()) {
                            viewModel.renameCurrentGroup(newGroupName)
                            showRenameDialog = false
                        }
                    },
                    enabled = newGroupName.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Eliminar nota del equipo") },
            text = { Text("¿Estás seguro de que deseas eliminar esta nota para todo el equipo? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    noteToDelete?.let { viewModel.deleteNote(it.noteId) }
                    noteToDelete = null
                }) {
                    Text("Eliminar", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun SharedNoteCard(
    note: SharedNoteEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
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
            // Image at the top
            if (note.imagePath != null) {
                AsyncImage(
                    model = note.imagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
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
                        onClick = onTogglePin,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin note",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Content
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
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Audio del equipo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Author Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person, 
                                contentDescription = null, 
                                modifier = Modifier.size(12.dp), 
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = note.authorName, 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 60.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date
                Text(
                    text = dateString,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

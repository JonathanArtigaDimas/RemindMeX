package com.example.remindme

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SharedNotesScreen(
    viewModel: SharedNotesViewModel,
    settingsManager: SettingsManager,
    onAddSharedNote: () -> Unit,
    onEditSharedNote: (SharedNoteEntity) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val notes by viewModel.sharedNotesFromRoom.collectAsState()
    val currentGroupId by viewModel.currentGroupId.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    val teamsBg by settingsManager.teamsBg.collectAsState()
    val bgOpacity by settingsManager.bgOpacity.collectAsState()
    
    var generatedInviteCode by remember { mutableStateOf<String?>(null) }

    com.example.remindme.ui.components.BackgroundWrapper(imageUri = teamsBg, opacity = bgOpacity) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
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
                            settingsManager = settingsManager,
                            onExitGroup = { viewModel.selectGroup("") },
                            onAddNote = onAddSharedNote,
                            onEditNote = onEditSharedNote
                        )
                    }
                }
            }
        }
    }
    
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

    if (uiState is SharedNotesUiState.Loading) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Procesando...")
                }
            },
            text = { Text("Un momento, por favor...") }
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
            .padding(28.dp) // More generous padding
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hola, ${currentUser.displayName ?: "Usuario"}", 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Colabora con tu equipo", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) // Softer secondary text
                )
            }
            IconButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
            ) {
                Icon(Icons.Default.Logout, "Cerrar sesión", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
        
        if (favoriteGroups.isNotEmpty()) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "Tus Grupos Favoritos",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            favoriteGroups.forEach { group ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp) // Increased negative space
                        .clickable { viewModel.selectGroup(group.groupId) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) // More subtle border
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp), // Increased internal padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = group.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "ID: ${group.groupId}", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) // More discrete ID
                            )
                        }
                        IconButton(onClick = { viewModel.removeFavorite(group.groupId) }) {
                            Icon(Icons.Default.Star, "Quitar de favoritos", tint = Color(0xFFFFB300).copy(alpha = 0.8f))
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
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Minimalist look
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) { // More padding
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GroupAdd, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Unirse a un grupo", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                
                Spacer(Modifier.height(20.dp))
                
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { if (it.length <= 6) inviteCode = it.uppercase() },
                    placeholder = { Text("Código de 6 dígitos", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                Spacer(Modifier.height(20.dp))
                
                Button(
                    onClick = { viewModel.joinGroup(inviteCode) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = inviteCode.length == 6 && uiState !is SharedNotesUiState.Loading,
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                ) {
                    if (uiState is SharedNotesUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Unirse al Grupo", fontWeight = FontWeight.Bold)
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
    settingsManager: SettingsManager,
    onExitGroup: () -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (SharedNoteEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedInternalTab by viewModel.selectedInternalTab.collectAsState()
    
    var noteToDelete by remember { mutableStateOf<SharedNoteEntity?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var showAddNotebookDialog by remember { mutableStateOf(false) }
    var notebookToRename by remember { mutableStateOf<SharedNotebookEntity?>(null) }
    var newGroupName by remember { mutableStateOf("") }
    var newNotebookName by remember { mutableStateOf("") }
    var announcementText by remember { mutableStateOf("") }
    var showSettingsMenu by remember { mutableStateOf(false) }
    
    val favoriteGroups by viewModel.favoriteGroups.collectAsState()
    val groupDetails by viewModel.currentGroupDetails.collectAsState()
    val members by viewModel.currentGroupMembers.collectAsState()
    val sharedNotebooks by viewModel.sharedNotebooksFromRoom.collectAsState()
    val selectedNotebookId by viewModel.selectedNotebookId.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val currentGroupName = remember(favoriteGroups, groupId) {
        favoriteGroups.find { it.groupId == groupId }?.name ?: "Notas del Equipo"
    }

    val filteredItems = remember(notes, sharedNotebooks, searchQuery, selectedNotebookId) {
        if (selectedNotebookId != null) {
            notes.filter { it.notebookId == selectedNotebookId }
                .map { UiNoteItem.Shared(it) }
        } else {
            val notebookItems = sharedNotebooks.map { 
                UiNoteItem.NotebookItem(Notebook(
                    id = it.id.hashCode().toLong(), 
                    name = it.name, 
                    color = it.color, 
                    createdAt = it.createdAt,
                    isPinned = it.isPinned
                ))
            }
            val standaloneNotes = notes.filter { it.notebookId == null }
                .filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    it.content.contains(searchQuery, ignoreCase = true)
                }
                .map { UiNoteItem.Shared(it) }
            
            (notebookItems + standaloneNotes)
                .sortedWith(compareByDescending<UiNoteItem> { it.isPinned }.thenByDescending { it.timestamp })
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (selectedNotebookId != null && selectedInternalTab == 0) {
                        IconButton(onClick = { viewModel.setSelectedNotebook(null) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        text = if (selectedNotebookId != null && selectedInternalTab == 0) 
                            sharedNotebooks.find { it.id == selectedNotebookId }?.name ?: "Cuaderno"
                        else currentGroupName, 
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedInternalTab == 0 && selectedNotebookId == null) {
                        IconButton(onClick = { showAddNotebookDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, "Nuevo cuaderno", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        }
                    }
                    
                    Box {
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Ajustes de grupo", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        
                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sincronizar") },
                                leadingIcon = { Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp)) },
                                onClick = { 
                                    SyncWorker.schedule(context)
                                    showSettingsMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Renombrar Equipo") },
                                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) },
                                onClick = { 
                                    newGroupName = currentGroupName
                                    showRenameDialog = true
                                    showSettingsMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Información del Equipo") },
                                leadingIcon = { Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp)) },
                                onClick = { 
                                    viewModel.fetchCurrentGroupDetails()
                                    showInfoDialog = true
                                    showSettingsMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Miembros del Equipo") },
                                leadingIcon = { Icon(Icons.Default.Groups, null, modifier = Modifier.size(18.dp)) },
                                onClick = { 
                                    showMembersDialog = true
                                    showSettingsMenu = false 
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text("Salir del Grupo", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
                                onClick = { 
                                    onExitGroup()
                                    showSettingsMenu = false 
                                }
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: $groupId", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedInternalTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedInternalTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {}
        ) {
            Tab(
                selected = selectedInternalTab == 0,
                onClick = { viewModel.setSelectedInternalTab(0) },
                text = { Text("Notas", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Note, null) }
            )
            Tab(
                selected = selectedInternalTab == 1,
                onClick = { viewModel.setSelectedInternalTab(1) },
                text = { Text("Chat Grupal", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Chat, null) }
            )
        }

        Spacer(Modifier.height(16.dp))

        if (selectedInternalTab == 0) {
            Column(modifier = Modifier.weight(1f)) {
                if (groupDetails != null && selectedNotebookId == null) {
                    val announcement = groupDetails?.announcement ?: ""
                    val isOwner = groupDetails?.ownerId == currentUser.uid
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clickable { if (isOwner) { announcementText = announcement; showAnnouncementDialog = true } },
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Campaign, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TABLÓN DEL EQUIPO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = if (announcement.isEmpty()) "Sin anuncios importantes aún." else announcement,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isOwner) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (selectedNotebookId == null) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar en el equipo...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (filteredItems.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌥️", style = androidx.compose.ui.text.TextStyle(fontSize = 48.sp))
                            Spacer(Modifier.height(16.dp))
                            Text("No hay contenido compartido aún", color = Color.Gray)
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
                            items = filteredItems,
                            key = { item ->
                                when(item) {
                                    is UiNoteItem.Shared -> "shared_${item.noteEntity.noteId}"
                                    is UiNoteItem.NotebookItem -> "shared_notebook_${item.notebook.name}_${item.notebook.createdAt}"
                                    else -> "unknown_${item.hashCode()}"
                                }
                            }
                        ) { item ->
                            when(item) {
                                is UiNoteItem.Shared -> {
                                    SharedNoteCard(
                                        note = item.noteEntity,
                                        viewModel = viewModel,
                                        onClick = { onEditNote(item.noteEntity) },
                                        onTogglePin = { viewModel.togglePin(item.noteEntity) },
                                        onDelete = { noteToDelete = item.noteEntity }
                                    )
                                }
                                is UiNoteItem.NotebookItem -> {
                                    val sharedNotebook = sharedNotebooks.find { it.id.hashCode().toLong() == item.notebook.id }
                                    sharedNotebook?.let { sn ->
                                        SharedNotebookCard(
                                            notebook = sn,
                                            onClick = { viewModel.setSelectedNotebook(sn.id) },
                                            onRename = { notebookToRename = sn; newNotebookName = sn.name },
                                            onTogglePin = { viewModel.toggleSharedNotebookPin(sn) },
                                            onDelete = { viewModel.deleteSharedNotebook(sn.id) }
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        } else {
            GroupChatView(viewModel, currentUser)
        }
    }

    if (showAddNotebookDialog) {
        AlertDialog(
            onDismissRequest = { showAddNotebookDialog = false },
            title = { Text("Nuevo Cuaderno Compartido") },
            text = {
                OutlinedTextField(
                    value = newNotebookName,
                    onValueChange = { newNotebookName = it },
                    label = { Text("Nombre del cuaderno") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNotebookName.isNotBlank()) {
                            viewModel.createSharedNotebook(newNotebookName, 0xFF3B82F6)
                            newNotebookName = ""
                            showAddNotebookDialog = false
                        }
                    },
                    enabled = newNotebookName.isNotBlank()
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNotebookDialog = false }) {
                    Text("Cancelar")
                }
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNotebookName.isNotBlank() && notebookToRename != null) {
                            viewModel.renameSharedNotebook(notebookToRename!!.id, newNotebookName)
                            newNotebookName = ""
                            notebookToRename = null
                        }
                    },
                    enabled = newNotebookName.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { notebookToRename = null }) {
                    Text("Cancelar")
                }
            }
        )
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

    if (showAnnouncementDialog) {
        AlertDialog(
            onDismissRequest = { showAnnouncementDialog = false },
            title = { Text("Actualizar Tablón") },
            text = {
                OutlinedTextField(
                    value = announcementText,
                    onValueChange = { announcementText = it },
                    label = { Text("Escribe la misión o reglas del equipo...") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.updateAnnouncement(announcementText)
                        showAnnouncementDialog = false
                    }
                ) {
                    Text("Publicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAnnouncementDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showInfoDialog && groupDetails != null) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Información del Equipo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Nombre: ${groupDetails!!.name}", fontWeight = FontWeight.Bold)
                    Text(text = "ID del Grupo: ${groupDetails!!.groupId}")
                    Text(text = "Código de Invitación: ${groupDetails!!.inviteCode}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    Text(text = "Miembros: ${groupDetails!!.members.size}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { showInfoDialog = false }) { Text("Entendido") }
            }
        )
    }

    if (showMembersDialog) {
        AlertDialog(
            onDismissRequest = { showMembersDialog = false },
            title = { Text("Miembros del Equipo") },
            text = {
                Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    members.forEach { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = member.name, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = member.email, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            if (groupDetails?.ownerId == currentUser.uid && member.uid != currentUser.uid) {
                                IconButton(onClick = { viewModel.removeMember(member.uid) }) {
                                    Icon(Icons.Default.PersonRemove, "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMembersDialog = false }) { Text("Cerrar") }
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
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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
fun SharedNotebookCard(notebook: SharedNotebookEntity, onClick: () -> Unit, onRename: () -> Unit, onTogglePin: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (notebook.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (notebook.isPinned) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Surface(Modifier.size(56.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f), shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(text = notebook.name, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun GroupChatView(viewModel: SharedNotesViewModel, currentUser: com.google.firebase.auth.FirebaseUser) {
    val messages by viewModel.currentGroupMessages.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    var editingMessageId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.authorId == currentUser.uid
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                
                var showMessageMenu by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    if (!isMe) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    msg.authorName.take(1), 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    
                    Box {
                        Surface(
                            color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 0.dp,
                                bottomEnd = if (isMe) 0.dp else 16.dp
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = { if (isMe) showMessageMenu = true }
                                ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isMe) "Tú (${msg.authorName})" else msg.authorName, 
                                fontWeight = FontWeight.ExtraBold, 
                                style = MaterialTheme.typography.labelSmall, 
                                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            
                            Text(
                                text = msg.text, 
                                style = MaterialTheme.typography.bodyMedium, 
                                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                                Row(
                                    modifier = Modifier.align(Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = time, 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = (if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.6f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showMessageMenu,
                            onDismissRequest = { showMessageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    editingMessageId = msg.messageId
                                    messageText = msg.text
                                    showMessageMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Borrar", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                                onClick = {
                                    viewModel.deleteGroupMessage(msg.messageId)
                                    showMessageMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column {
                if (editingMessageId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Editando mensaje...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { editingMessageId = null; messageText = "" }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Escribe un mensaje...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                if (editingMessageId != null) {
                                    viewModel.editGroupMessage(editingMessageId!!, messageText)
                                    editingMessageId = null
                                } else {
                                    viewModel.sendGroupMessage(messageText)
                                }
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            if (editingMessageId != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send, 
                            null, 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SharedNoteCard(
    note: SharedNoteEntity,
    viewModel: SharedNotesViewModel,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("d/M/yyyy", Locale.getDefault()) }
    val dateString = dateFormatter.format(Date(note.createdAt))
    val context = androidx.compose.ui.platform.LocalContext.current
    val playingSoundPath by SoundManager.currentSoundPath.collectAsState()
    val isPlaying = playingSoundPath == note.audioPath && note.audioPath != null
    
    val comments = remember(note.commentsJson) {
        val type = object : TypeToken<List<NoteComment>>() {}.type
        Gson().fromJson<List<NoteComment>>(note.commentsJson, type) ?: emptyList()
    }
    
    val currentUser by viewModel.currentUser.collectAsState()
    
    var showChat by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var editingCommentId by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp), // More rounded
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column {
            if (note.imagePath != null) {
                AsyncImage(
                    model = note.imagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(16.dp)) { // More padding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (note.title.isNotEmpty()) note.title else note.content,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
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
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (note.title.isNotEmpty()) {
                    Text(
                        text = note.content,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (note.audioPath != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clickable { SoundManager.playSound(context, note.audioPath!!) },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Nota de voz grupal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person, 
                                contentDescription = null, 
                                modifier = Modifier.size(12.dp), 
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = note.authorName, 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 70.dp)
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
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = dateString,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                TextButton(
                    onClick = { showChat = !showChat },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (comments.isEmpty()) "Comentar" else "${comments.size} comentarios", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                if (showChat) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        if (comments.isEmpty()) {
                            Text(
                                text = "Sin comentarios aún",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            comments.sortedBy { it.createdAt }.forEach { comment ->
                                val commentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(comment.createdAt))
                                val isMyComment = comment.authorId == currentUser?.uid
                                var showCommentOptions by remember { mutableStateOf(false) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(
                                            if (isMyComment) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .combinedClickable(
                                            onClick = { },
                                            onLongClick = { if (isMyComment) showCommentOptions = true }
                                        )
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isMyComment) "Tú" else comment.authorName, 
                                            fontWeight = FontWeight.ExtraBold, 
                                            style = MaterialTheme.typography.labelSmall, 
                                            color = if (isMyComment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = commentTime, 
                                                style = MaterialTheme.typography.labelSmall, 
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                fontSize = 9.sp
                                            )
                                            if (isMyComment) {
                                                Box {
                                                    DropdownMenu(
                                                        expanded = showCommentOptions,
                                                        onDismissRequest = { showCommentOptions = false }
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("Editar") },
                                                            leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) },
                                                            onClick = {
                                                                editingCommentId = comment.commentId
                                                                commentText = comment.text
                                                                showCommentOptions = false
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Borrar", color = MaterialTheme.colorScheme.error) },
                                                            leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
                                                            onClick = {
                                                                viewModel.deleteComment(note.noteId, comment.commentId)
                                                                showCommentOptions = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = comment.text, 
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isMyComment) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { 
                                    Text(
                                        if (editingCommentId != null) "Editando comentario..." else "Escribir comentario...", 
                                        fontSize = 12.sp, 
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    ) 
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                maxLines = 3,
                                trailingIcon = {
                                    if (editingCommentId != null) {
                                        IconButton(onClick = { editingCommentId = null; commentText = "" }) {
                                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            )
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        if (editingCommentId != null) {
                                            viewModel.editComment(note.noteId, editingCommentId!!, commentText)
                                            editingCommentId = null
                                        } else {
                                            viewModel.addComment(note.noteId, commentText)
                                        }
                                        commentText = ""
                                    }
                                },
                                enabled = commentText.isNotBlank(),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(
                                    if (editingCommentId != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                                    null, 
                                    tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

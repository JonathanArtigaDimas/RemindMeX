package com.example.remindme.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.remindme.*
import androidx.compose.material.icons.automirrored.filled.Send
import java.io.File
import java.io.FileOutputStream
import java.util.Objects

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteWithTags: NoteWithTags? = null,
    isQuickNote: Boolean = false,
    initialIsShared: Boolean = false,
    availableNotebooks: List<Notebook> = emptyList(),
    sharedNotebooks: List<SharedNotebookEntity> = emptyList(),
    currentGroupId: String? = null,
    commentsJson: String = "[]",
    onDismiss: () -> Unit,
    onSave: (Note, Boolean) -> Unit,
    onDelete: (Note) -> Unit,
    onAddComment: (String, String) -> Unit = { _, _ -> },
    onEditComment: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteComment: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val gson = remember { com.google.gson.Gson() }
    val commentType = remember { object : com.google.gson.reflect.TypeToken<List<NoteComment>>() {}.type }
    val pathsType = remember { object : com.google.gson.reflect.TypeToken<List<String>>() {}.type }

    val comments = remember(commentsJson) { 
        gson.fromJson<List<NoteComment>>(commentsJson, commentType) ?: emptyList()
    }
    var commentText by remember { mutableStateOf("") }
    var editingCommentId by remember { mutableStateOf<String?>(null) }

    var title by remember { 
        mutableStateOf(
            if (noteWithTags?.note?.isQuickNote == true && noteWithTags.note.title.isEmpty()) {
                noteWithTags.note.content
            } else {
                noteWithTags?.note?.title ?: ""
            }
        ) 
    }
    var content by remember { 
        mutableStateOf(
            if (noteWithTags?.note?.isQuickNote == true && noteWithTags.note.title.isEmpty()) {
                ""
            } else {
                noteWithTags?.note?.content ?: ""
            }
        ) 
    }
    
    val initialPaths: List<String> = remember(noteWithTags) {
        val json = noteWithTags?.note?.imagePathsJson ?: "[]"
        val list = gson.fromJson<List<String>>(json, pathsType) ?: emptyList()
        // Backward compatibility: add single imagePath if list is empty
        if (list.isEmpty() && noteWithTags?.note?.imagePath != null) {
            listOf(noteWithTags.note.imagePath)
        } else list
    }
    
    var imagePaths by remember { mutableStateOf(initialPaths) }
    var audioPath by remember { mutableStateOf(noteWithTags?.note?.audioPath) }
    val defaultNoteColor = 0xFF1E293B
    var selectedColor by remember { mutableStateOf(noteWithTags?.note?.color ?: defaultNoteColor) }
    
    var isShared by remember { mutableStateOf(initialIsShared) }
    var selectedNotebookId by remember { mutableStateOf(noteWithTags?.note?.notebookId) }
    var showFullscreenImage by remember { mutableStateOf<String?>(null) }

    val isRecording by SoundManager.isRecording.collectAsState()
    val playingSoundPath by SoundManager.currentSoundPath.collectAsState()
    val isPlaying = playingSoundPath == audioPath && audioPath != null

    // State for high-quality camera URI
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = File(context.filesDir, "note_image_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(it)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            imagePaths = imagePaths + file.absolutePath
        }
    }

    // High Quality Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraFile?.absolutePath?.let { path ->
                imagePaths = imagePaths + path
            }
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            SoundManager.startRecording(context)
        }
    }

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f),
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            text = if (noteWithTags == null) "Nueva Nota" else "Editar Nota",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = {
                            if (content.isNotEmpty() || title.isNotEmpty()) {
                                val note = (noteWithTags?.note ?: Note(title = title, content = content)).copy(
                                    title = title,
                                    content = content,
                                    imagePath = imagePaths.firstOrNull(),
                                    imagePathsJson = gson.toJson(imagePaths),
                                    audioPath = audioPath,
                                    color = selectedColor,
                                    isQuickNote = isQuickNote,
                                    notebookId = selectedNotebookId
                                )
                                onSave(note, isShared)
                                onDismiss()
                            }
                        }) {
                            Text("Guardar", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Media Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MediaButton(
                            text = "Cámara",
                            icon = Icons.Outlined.CameraAlt,
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                val file = File(context.filesDir, "note_cam_${System.currentTimeMillis()}.jpg")
                                tempCameraFile = file
                                val uri = FileProvider.getUriForFile(
                                    Objects.requireNonNull(context),
                                    context.packageName + ".fileprovider",
                                    file
                                )
                                tempCameraUri = uri
                                cameraLauncher.launch(uri)
                            }
                        )
                        MediaButton(
                            text = "Galería",
                            icon = Icons.Outlined.Image,
                            modifier = Modifier.weight(1f),
                            onClick = { imageLauncher.launch("image/*") }
                        )
                        MediaButton(
                            text = if (isRecording) "Detener" else "Grabar",
                            icon = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                if (isRecording) {
                                    audioPath = SoundManager.stopRecording(context)
                                } else {
                                    recordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Title
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Título", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                            cursorColor = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    // Shared Note Toggle
                    if (currentGroupId != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Compartir con el grupo",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Esta nota será visible para todo tu equipo",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Switch(
                                    checked = isShared,
                                    onCheckedChange = { isShared = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    // Notebooks Section
                    Text(
                        text = "Cuaderno:",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isShared) {
                            sharedNotebooks.forEach { notebook ->
                                FilterChip(
                                    selected = selectedNotebookId?.toString() == notebook.id,
                                    onClick = { selectedNotebookId = if (selectedNotebookId?.toString() == notebook.id) null else notebook.id.toLongOrNull() ?: notebook.id.hashCode().toLong() },
                                    label = { Text(notebook.name) },
                                    leadingIcon = { Icon(Icons.Default.Book, null, Modifier.size(16.dp)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        selectedContainerColor = Color(notebook.color),
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    )
                                )
                            }
                        } else {
                            availableNotebooks.forEach { notebook ->
                                FilterChip(
                                    selected = selectedNotebookId == notebook.id,
                                    onClick = { selectedNotebookId = if (selectedNotebookId == notebook.id) null else notebook.id },
                                    label = { Text(notebook.name) },
                                    leadingIcon = { Icon(Icons.Default.Book, null, Modifier.size(16.dp)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        selectedContainerColor = Color(notebook.color),
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Image Gallery
                    if (imagePaths.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            imagePaths.forEachIndexed { index, path ->
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    AsyncImage(
                                        model = path,
                                        contentDescription = "Imagen de la nota",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { showFullscreenImage = path },
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { imagePaths = imagePaths.filterIndexed { i, _ -> i != index } },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Audio Preview
                    audioPath?.let { path ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .height(56.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { SoundManager.playSound(context, path) }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir audio",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    "Nota de voz grabada",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { audioPath = null }) {
                                    Icon(Icons.Default.Delete, "Eliminar audio", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }

                    // Content
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Empieza a escribir...", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )

                    if (isShared && noteWithTags != null) {
                        Spacer(modifier = Modifier.height(48.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Conversación del Equipo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (comments.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    "No hay comentarios aún. ¡Sé el primero en participar!",
                                    modifier = Modifier.padding(24.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            comments.sortedBy { it.createdAt }.forEach { comment ->
                                val commentTime = java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(comment.createdAt))
                                val isMyComment = comment.authorId == currentUser?.uid
                                var showCommentOptions by remember { mutableStateOf(false) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .background(
                                            if (isMyComment) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), 
                                            RoundedCornerShape(16.dp)
                                        )
                                        .combinedClickable(
                                            onClick = { },
                                            onLongClick = { if (isMyComment) showCommentOptions = true }
                                        )
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isMyComment) "Tú" else comment.authorName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMyComment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = commentTime,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
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
                                                                onDeleteComment("", comment.commentId) // noteId será manejado en MainActivity
                                                                showCommentOptions = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = comment.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isMyComment) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    placeholder = { 
                                        Text(
                                            if (editingCommentId != null) "Editando comentario..." else "Escribir a tu equipo...", 
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        ) 
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    maxLines = 5,
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
                                                onEditComment("", editingCommentId!!, commentText)
                                                editingCommentId = null
                                            } else {
                                                onAddComment("", commentText)
                                            }
                                            commentText = ""
                                        }
                                    },
                                    enabled = commentText.isNotBlank()
                                ) {
                                    Icon(
                                        if (editingCommentId != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send, 
                                        null, 
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom
                }
            }
        }
    }

    if (showFullscreenImage != null) {
        FullscreenImageViewer(
            imagePath = showFullscreenImage!!,
            onDismiss = { showFullscreenImage = null }
        )
    }
}

@Composable
fun MediaButton(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun TagChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
    ) {
        Text(
            text = name,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

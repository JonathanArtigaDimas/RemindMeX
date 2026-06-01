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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.remindme.*
import java.io.File
import java.io.FileOutputStream
import java.util.Objects

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteWithTags: NoteWithTags? = null,
    isQuickNote: Boolean = false,
    initialIsShared: Boolean = false,
    availableTags: List<Tag> = emptyList(),
    currentGroupId: String? = null,
    onDismiss: () -> Unit,
    onSave: (Note, List<Tag>, Boolean) -> Unit,
    onDelete: (Note) -> Unit
) {
    val context = LocalContext.current
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
    var imagePath by remember { mutableStateOf(noteWithTags?.note?.imagePath) }
    var audioPath by remember { mutableStateOf(noteWithTags?.note?.audioPath) }
    val defaultNoteColor = 0xFF1E293B
    var selectedColor by remember { mutableStateOf(noteWithTags?.note?.color ?: defaultNoteColor) }
    
    var isShared by remember { mutableStateOf(initialIsShared) }

    val initialTags = noteWithTags?.tags?.map { it.name }?.toSet() ?: emptySet()
    var selectedTags by remember { mutableStateOf(initialTags) }
    
    val isRecording by SoundManager.isRecording.collectAsState()
    val playingSoundPath by SoundManager.currentSoundPath.collectAsState()
    val isPlaying = playingSoundPath == audioPath && audioPath != null

    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    
    // Combine preset tags with available tags from DB
    val allTagsList = remember(availableTags) {
        val baseTags = listOf("Trabajo", "Personal", "Ideas", "Importante")
        (baseTags + availableTags.map { it.name }).distinct()
    }
    val displayTags = remember { mutableStateListOf<String>().apply { addAll(allTagsList) } }

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
            imagePath = file.absolutePath
        }
    }

    // High Quality Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imagePath = tempCameraFile?.absolutePath
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
                                    imagePath = imagePath,
                                    audioPath = audioPath,
                                    color = selectedColor,
                                    isQuickNote = isQuickNote
                                )
                                onSave(note, selectedTags.map { Tag(it) }, isShared)
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tags Section
                    Text(
                        text = "Etiquetas (Opcional):",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Add Custom Tag Button
                        Surface(
                            modifier = Modifier.size(40.dp).clickable { showAddTagDialog = true },
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }

                        displayTags.forEach { tag ->
                            TagChip(
                                name = tag,
                                isSelected = selectedTags.contains(tag),
                                onClick = {
                                    selectedTags = if (selectedTags.contains(tag)) {
                                        selectedTags - tag
                                    } else {
                                        selectedTags + tag
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Image Preview
                    imagePath?.let {
                        Box(modifier = Modifier.padding(vertical = 16.dp)) {
                            AsyncImage(
                                model = it,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.FillWidth
                            )
                            IconButton(
                                onClick = { imagePath = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
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
                    
                    Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom
                }
            }
        }
    }

    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddTagDialog = false
                newTagName = ""
            },
            title = { Text("Nueva Etiqueta", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                TextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder = { Text("Nombre de la etiqueta") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTagName.isNotBlank()) {
                        val trimmedTag = newTagName.trim()
                        if (!displayTags.contains(trimmedTag)) {
                            displayTags.add(0, trimmedTag)
                        }
                        selectedTags = selectedTags + trimmedTag
                        showAddTagDialog = false
                        newTagName = ""
                    }
                }) {
                    Text("Agregar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddTagDialog = false
                    newTagName = ""
                }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
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

package com.example.remindme.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.remindme.SoundManager

@Composable
fun SoundLibraryContent(
    onSoundSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val isRecording by SoundManager.isRecording.collectAsState()
    val playingSoundPath by SoundManager.currentSoundPath.collectAsState()
    
    var showNameDialog by remember { mutableStateOf<String?>(null) }
    var tempName by remember { mutableStateOf("") }
    
    var customSounds by remember { mutableStateOf(SoundManager.getCustomSounds(context)) }

    val mp3Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val path = SoundManager.saveMp3ToInternalStorage(context, it)
            if (path != null) {
                customSounds = SoundManager.getCustomSounds(context)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            SoundManager.startRecording(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        // Recording Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            if (isRecording) {
                                val path = SoundManager.stopRecording(context)
                                if (path != null) {
                                    showNameDialog = path
                                    tempName = "Mi grabación ${System.currentTimeMillis() % 10000}"
                                }
                                customSounds = SoundManager.getCustomSounds(context)
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    shape = CircleShape,
                    color = if (isRecording) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else Color(0xFFD32F2F)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Record",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isRecording) "Grabando..." else "Grabar nuevo sonido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Graba tu propia voz para tus alarmas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        SoundSectionHeader("TUS GRABACIONES")

        val recordings = customSounds.filter { it.substringAfterLast("/").startsWith("recording_") }
        val importedSounds = customSounds.filter { !it.substringAfterLast("/").startsWith("recording_") }

        if (recordings.isEmpty()) {
            Text(
                "No tienes grabaciones aún",
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            recordings.forEach { path ->
                val displayTitle = SoundManager.getDisplayName(context, path)
                
                SoundItemCard(
                    title = displayTitle,
                    subtitle = "Grabación de voz",
                    icon = Icons.Default.Mic,
                    isPlaying = playingSoundPath == path,
                    onPlay = { SoundManager.playSound(context, path) },
                    onDelete = {
                        SoundManager.deleteSound(context, path)
                        customSounds = SoundManager.getCustomSounds(context)
                    },
                    onClick = { onSoundSelected(path) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SoundSectionHeader("SONIDOS IMPORTADOS")

        SoundItemCard(
            title = "Importar MP3",
            subtitle = "Selecciona un archivo del dispositivo",
            icon = Icons.Default.FileUpload,
            isPlaying = false,
            onPlay = { mp3Launcher.launch("audio/*") },
            onDelete = null,
            onClick = { mp3Launcher.launch("audio/*") }
        )

        if (importedSounds.isNotEmpty()) {
            importedSounds.forEach { path ->
                val displayTitle = SoundManager.getDisplayName(context, path)
                
                SoundItemCard(
                    title = displayTitle,
                    subtitle = "Audio importado",
                    icon = Icons.Default.MusicNote,
                    isPlaying = playingSoundPath == path,
                    onPlay = { SoundManager.playSound(context, path) },
                    onDelete = {
                        SoundManager.deleteSound(context, path)
                        customSounds = SoundManager.getCustomSounds(context)
                    },
                    onClick = { onSoundSelected(path) }
                )
            }
        }

        SoundSectionHeader("SONIDOS DEL SISTEMA")

        val systemSounds = listOf(
            Triple("Campana", "MP3", Icons.Default.Notifications),
            Triple("Cristal", "MP3", Icons.Outlined.Diamond),
            Triple("Clásico", "MP3", Icons.Default.Smartphone),
            Triple("Aviso", "MP3", Icons.Default.MusicNote)
        )

        systemSounds.forEach { (name, type, icon) ->
            SoundItemCard(
                title = name,
                subtitle = type,
                icon = icon,
                isPlaying = playingSoundPath == name,
                onPlay = { SoundManager.playSound(context, name) },
                onDelete = null,
                onClick = { onSoundSelected(name) }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showNameDialog != null) {
        AlertDialog(
            onDismissRequest = { showNameDialog = null },
            title = { Text("Nombre del audio") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    showNameDialog?.let { path ->
                        SoundManager.setDisplayName(context, path, tempName)
                        customSounds = SoundManager.getCustomSounds(context)
                    }
                    showNameDialog = null
                }) {
                    Text("Guardar")
                }
            }
        )
    }
}

@Composable
fun SoundSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, top = 16.dp)
    )
}

@Composable
fun SoundItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Play Button
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

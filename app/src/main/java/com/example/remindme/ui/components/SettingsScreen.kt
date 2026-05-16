package com.example.remindme.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remindme.SettingsManager
import com.example.remindme.SoundManager
import com.example.remindme.ui.theme.WineColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onClearCompleted: () -> Unit,
    onResetAll: () -> Unit
) {
    val context = LocalContext.current
    var activeSection by remember { mutableStateOf<String?>(null) }
    
    val currentTheme by settingsManager.currentTheme.collectAsState()
    val currentFont by settingsManager.currentFont.collectAsState()
    val hapticEnabled by settingsManager.hapticFeedback.collectAsState()
    val defaultSound by settingsManager.defaultNotificationSound.collectAsState()
    val playingSoundPath by SoundManager.currentSoundPath.collectAsState()

    // Intercept back button to return to settings menu
    BackHandler(enabled = activeSection != null) {
        activeSection = null
    }

    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(backgroundColor)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeSection != null) {
                        IconButton(onClick = { 
                            activeSection = if (activeSection == "SoundLibrary") "Sonidos" else null 
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    Text(
                        text = when (activeSection) {
                            "Temas" -> "🎨 Temas"
                            "Tipografías" -> "Tipografías"
                            "Sonidos" -> "🔊 Sonidos"
                            "Sistema" -> "⚙️ Sistema"
                            "SoundLibrary" -> "Biblioteca de Sonidos"
                            else -> "🎨 Personalización"
                        },
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(start = if (activeSection == null) 0.dp else 8.dp)
                    )
                }
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(8.dp))

                AnimatedContent(
                    targetState = activeSection,
                    transitionSpec = {
                        if (targetState != null) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    label = "SettingsNav"
                ) { section ->
                    Column {
                        if (section == null) {
                            // MAIN MENU
                            SettingsMenuCard(
                                title = "Temas",
                                subtitle = "Personaliza los colores",
                                icon = "🎨",
                                onClick = { activeSection = "Temas" }
                            )
                            SettingsMenuCard(
                                title = "Tipografías",
                                subtitle = "Selecciona fuentes",
                                icon = "A",
                                onClick = { activeSection = "Tipografías" }
                            )
                            SettingsMenuCard(
                                title = "Sonidos",
                                subtitle = "Efectos de audio",
                                icon = "🔊",
                                onClick = { activeSection = "Sonidos" }
                            )
                            SettingsMenuCard(
                                title = "Sistema",
                                subtitle = "Configuración general",
                                icon = "⚙️",
                                onClick = { activeSection = "Sistema" }
                            )
                        } else {
                            // SUB-SECTIONS
                            when (section) {
                                "Temas" -> {
                                    SettingsSectionHeader("TEMAS PREMIUM")
                                    val themes = listOf(
                                        Triple("Default", "Azul oscuro", Icons.Outlined.DarkMode),
                                        Triple("Coffee Milk", "Beige estético", Icons.Outlined.Coffee),
                                        Triple("Noir Mono", "Blanco y Negro", Icons.Outlined.Contrast),
                                        Triple("Cyber Neon", "Estilo neón", Icons.Outlined.ElectricBolt),
                                        Triple("Aesthetic", "Cálido y minimalista", Icons.Outlined.Palette),
                                        Triple("Sakura Dream", "Rosa primaveral", Icons.Outlined.Spa)
                                    )
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        themes.chunked(2).forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                rowItems.forEach { (name, desc, icon) ->
                                                    ThemeCard(
                                                        name = name,
                                                        desc = desc,
                                                        icon = icon,
                                                        isSelected = currentTheme == name,
                                                        onClick = { settingsManager.setTheme(name) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                if (rowItems.size < 2) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                                "Tipografías" -> {
                                    Column(modifier = Modifier.padding(bottom = 20.dp, top = 8.dp)) {
                                        Text(
                                            "Elige la tipografía perfecta para tu estilo",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }

                                    val fontOptions = listOf(
                                        "Sistema" to "Fuente predeterminada",
                                        "Playfair" to "Elegante y refinado",
                                        "Space Mono" to "Técnico y moderno"
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        fontOptions.chunked(2).forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                rowItems.forEach { (name, desc) ->
                                                    FontCard(
                                                        name = name,
                                                        desc = desc,
                                                        isSelected = currentFont == name,
                                                        onClick = { settingsManager.setFont(name) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                if (rowItems.size < 2) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(24.dp))
                                    SettingsSectionHeader("ESTILO Y TAMAÑO DE TEXTO")
                                    val fontSizeMultiplier by settingsManager.fontSizeMultiplier.collectAsState()
                                    val isFontBold by settingsManager.isFontBold.collectAsState()
                                    val isFontItalic by settingsManager.isFontItalic.collectAsState()

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Text(
                                                "Tamaño de fuente", 
                                                color = MaterialTheme.colorScheme.onSurface, 
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(Modifier.height(16.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val sizes = listOf(
                                                    "Pequeño" to 0.85f,
                                                    "Mediano" to 1.0f,
                                                    "Grande" to 1.25f
                                                )
                                                
                                                sizes.forEach { (label, multiplier) ->
                                                    val isSelected = fontSizeMultiplier == multiplier
                                                    Surface(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(40.dp)
                                                            .clickable { settingsManager.setFontSizeMultiplier(multiplier) },
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(12.dp),
                                                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = label,
                                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                style = MaterialTheme.typography.labelLarge,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    "Vista previa del texto", 
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center
                                                )
                                            }

                                            Spacer(Modifier.height(16.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                            Spacer(Modifier.height(16.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                StyleToggleCard(
                                                    text = "Negrita",
                                                    icon = Icons.Default.FormatBold,
                                                    isSelected = isFontBold,
                                                    onClick = { settingsManager.setFontBold(!isFontBold) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                StyleToggleCard(
                                                    text = "Cursiva",
                                                    icon = Icons.Default.FormatItalic,
                                                    isSelected = isFontItalic,
                                                    onClick = { settingsManager.setFontItalic(!isFontItalic) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                                "Sonidos" -> {
                                    SettingsSectionHeader("SONIDOS DE NOTIFICACIÓN")
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Column {
                                            val sounds = listOf(
                                                Triple("Clásico", "🔔", "Tono de campana tradicional"),
                                                Triple("Digitalic", "👾", "Sonido digital moderno"),
                                                Triple("Cristales", "💎", "Brillo de cristales"),
                                                Triple("Univerfield", "🌌", "Ambiente espacial"),
                                                Triple("Melodic", "🎵", "Melodía suave"),
                                                Triple("Aviso", "✨", "Notificación corta y brillante"),
                                                Triple("Campana", "🛎️", "Sonido de campana clásica"),
                                                Triple("Cristal", "💎", "Toque de cristal fino")
                                            )
                                            
                                            sounds.forEachIndexed { index, (name, emoji, desc) ->
                                                NotificationSoundRow(
                                                    name = name,
                                                    emoji = emoji,
                                                    description = desc,
                                                    isSelected = defaultSound == name,
                                                    isPlaying = playingSoundPath == name,
                                                    onPlay = { 
                                                        SoundManager.playSound(context, name) 
                                                    },
                                                    onClick = { 
                                                        settingsManager.setDefaultNotificationSound(name)
                                                    }
                                                )
                                                if (index < sounds.size - 1) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        color = Color.Black.copy(alpha = 0.05f)
                                                    )
                                                }
                                            }
                                            
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = Color.Black.copy(alpha = 0.05f)
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { activeSection = "SoundLibrary" }
                                                    .padding(20.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.AccountCircle, 
                                                    null, 
                                                    tint = MaterialTheme.colorScheme.onSurface, 
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(Modifier.width(16.dp))
                                                Text(
                                                    "Biblioteca y Grabadora de Voz", 
                                                    color = MaterialTheme.colorScheme.onSurface, 
                                                    style = MaterialTheme.typography.titleMedium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                                                    null, 
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                "SoundLibrary" -> {
                                    SoundLibraryContent(
                                        onSoundSelected = { path ->
                                            settingsManager.setDefaultNotificationSound(path)
                                            SoundManager.playSound(context, path)
                                        }
                                    )
                                }
                                "Sistema" -> {
                                    SettingsSectionHeader("AJUSTES DE SISTEMA")
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Waves, 
                                                        null, 
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(Modifier.width(16.dp))
                                                    Text(
                                                        "Vibración táctil", 
                                                        color = MaterialTheme.colorScheme.onSurface, 
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                }
                                                Switch(
                                                    checked = hapticEnabled,
                                                    onCheckedChange = { settingsManager.setHapticFeedback(it) },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                                                        checkedTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                                        uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(24.dp))

                                    SettingsSectionHeader("MANTENIMIENTO")
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            OutlinedButton(
                                                onClick = onClearCompleted,
                                                modifier = Modifier.fillMaxWidth(0.8f).height(48.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Limpiar completados")
                                            }
                                            
                                            OutlinedButton(
                                                onClick = onResetAll,
                                                modifier = Modifier.fillMaxWidth(0.8f).height(48.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(Icons.Default.Refresh, null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Restablecer todo")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(48.dp))
                
                // Version Footer pushed to bottom
                Text(
                    text = "RemindMe v1.0.4",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(20.dp))

                Text(
                    text = buildAnnotatedString {
                        append("⚖️ Licda. Mylene Dánae Castro de Artiga ")
                        withStyle(style = SpanStyle(color = WineColor)) {
                            append("♥️")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                
                Spacer(Modifier.height(80.dp)) // Padding for bottom nav
            }
        }
    }
}

@Composable
fun SettingsMenuCard(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.titleSmall,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 12.dp, top = 24.dp, start = 16.dp)
    )
}

@Composable
fun ThemeCard(name: String, desc: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val bgColor = when (name) {
        "Coffee Milk" -> Color(0xFFE6D7C3)
        "Noir Mono" -> Color.Black
        "Cyber Neon" -> Color(0xFF0D0221)
        "Aesthetic" -> Color(0xFFFEF9F3)
        "Sakura Dream" -> Color(0xFFFFF0F3)
        else -> Color(0xFF1E293B)
    }
    val contentColor = when (name) {
        "Coffee Milk" -> Color(0xFF2D241E)
        "Cyber Neon" -> Color(0xFFFE007F)
        "Aesthetic" -> Color(0xFFE68A56)
        "Sakura Dream" -> Color(0xFFF2A6B3)
        else -> Color.White
    }

    Card(
        modifier = modifier
            .height(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = name,
                color = contentColor,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = desc,
                color = contentColor.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FontCard(name: String, desc: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val accentColor = MaterialTheme.colorScheme.primary
    val neutralColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, accentColor) else BorderStroke(1.dp, neutralColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(56.dp), 
                shape = CircleShape, 
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Aa", 
                        color = Color.White, 
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = name, 
                color = MaterialTheme.colorScheme.onSurface, 
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = desc, 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), 
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StyleToggleCard(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val accentColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        color = if (isSelected) accentColor.copy(alpha = 0.1f) else surfaceColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else onSurface.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                color = if (isSelected) accentColor else onSurface.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun NotificationSoundRow(
    name: String, 
    emoji: String,
    description: String,
    isSelected: Boolean, 
    isPlaying: Boolean, 
    onPlay: () -> Unit, 
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.MusicNote, 
            null, 
            tint = MaterialTheme.colorScheme.primary, 
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "$emoji $name", 
                color = MaterialTheme.colorScheme.onSurface, 
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        IconButton(onClick = onPlay) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Detener" else "Reproducir",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        if (isSelected) {
            Icon(
                Icons.Default.Check, 
                null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MaintenanceRow(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 18.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

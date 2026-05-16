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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remindme.SoundManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewReminderModal(
    initialTitle: String = "",
    initialDescription: String = "",
    initialDate: String = "",
    initialTime: String = "",
    initialCategory: String = "Personal",
    initialColor: Long = 0xFF3B82F6,
    initialSound: String = "Campana",
    initialRepetition: String = "Sin repetición",
    initialRepeatDays: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Long, String, String, String?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedSound by remember { mutableStateOf(initialSound) }
    var selectedRepetition by remember { mutableStateOf(initialRepetition) }
    
    val initialDaysList = initialRepeatDays?.split(",")?.filter { it.isNotEmpty() }?.map { it.toInt() } ?: emptyList()
    var selectedDays by remember { mutableStateOf(initialDaysList.toSet()) }

    // State to refresh UI when sound starts/stops
    val playingSoundPath by SoundManager.currentSoundPath.collectAsState()
    
    var customSounds by remember { mutableStateOf(SoundManager.getCustomSounds(context)) }

    // Stop sound when modal is closed
    DisposableEffect(Unit) {
        onDispose {
            SoundManager.stopSound()
        }
    }

    // File Picker for MP3
    val mp3Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = SoundManager.saveMp3ToInternalStorage(context, it)
            if (path != null) {
                selectedSound = path
                customSounds = SoundManager.getCustomSounds(context)
                SoundManager.playSound(context, path)
            }
        }
    }

    val isTitleValid = title.trim().isNotEmpty()
    val buttonColor = if (isTitleValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    val calendar = Calendar.getInstance()
    val todayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
    calendar.add(Calendar.MINUTE, 1)
    val oneMinuteAheadTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)

    val finalInitialDate = if (initialDate.isEmpty()) todayDate else initialDate
    val finalInitialTime = if (initialTime.isEmpty()) oneMinuteAheadTime else initialTime

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() }
            sdf.parse(finalInitialDate)?.time
        } catch (e: Exception) { System.currentTimeMillis() }
    )
    val formattedDate = datePickerState.selectedDateMillis?.let { millis ->
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(utcCalendar.time)
    } ?: finalInitialDate

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = try { finalInitialTime.split(":")[0].toInt() } catch (e: Exception) { calendar.get(Calendar.HOUR_OF_DAY) },
        initialMinute = try { finalInitialTime.split(":")[1].toInt() } catch (e: Exception) { calendar.get(Calendar.MINUTE) },
        is24Hour = true
    )
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)

    val categories = listOf("Personal", "Trabajo", "Salud", "Finanzas", "Familia", "Otro")
    val colors = listOf(0xFF3B82F6, 0xFFEF4444, 0xFF10B981, 0xFFF59E0B, 0xFF8B5CF6, 0xFFEC4899, 0xFF06B6D4, 0xFF6366F1, 0xFF2DD4BF, 0xFFF97316, 0xFF84CC16, 0xFF64748B)
    val presetSounds = listOf("Campana", "Cristal", "Clásico", "Aviso")
    val repetitions = listOf("Sin repetición", "Diario", "Semanal", "Mensual")

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar", tint = MaterialTheme.colorScheme.secondary) }
                Text(if (initialTitle.isEmpty()) "Nuevo Recordatorio" else "Editar Recordatorio", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(48.dp))
            }

            SectionHeader("TÍTULO")
            CustomTextField(
                value = title, 
                onValueChange = { title = it }, 
                placeholder = "¿Qué quieres recordar?", 
                bgColor = MaterialTheme.colorScheme.surface,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            SectionHeader("DESCRIPCIÓN (OPCIONAL)")
            CustomTextField(
                value = description, 
                onValueChange = { description = it }, 
                placeholder = "Detalles adicionales...", 
                bgColor = MaterialTheme.colorScheme.surface, 
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            SectionHeader("FECHA Y HORA")
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                PickerField(formattedDate, Icons.Default.CalendarMonth, Modifier.weight(1f)) { showDatePicker = true }
                PickerField(formattedTime, Icons.Default.AccessTime, Modifier.weight(1f)) { showTimePicker = true }
            }

            SectionHeader("CATEGORÍA")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    CategoryChip(cat, selectedCategory == cat) { selectedCategory = it }
                }
            }

            SectionHeader("COLOR")
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                colors.forEach { col ->
                    ColorCircle(col, selectedColor == col) { selectedColor = it }
                }
            }

            SectionHeader("SONIDO")
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), Arrangement.spacedBy(12.dp)) {
                SoundActionCard("Importar MP3", Icons.Default.Folder, Modifier.weight(1f)) {
                    mp3Launcher.launch("audio/*")
                }
                SoundActionCard("Grabar", Icons.Default.Mic, Modifier.weight(1f)) {}
            }
            
            // Show custom imported sounds
            customSounds.forEach { path ->
                val fileName = path.substringAfterLast("/")
                SoundOptionRow(
                    name = "Personalizado: $fileName", 
                    isSelected = selectedSound == path,
                    isPlaying = playingSoundPath == path,
                    onPlay = { SoundManager.playSound(context, path) },
                    onClick = { selectedSound = path }
                )
            }

            if (customSounds.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))
            }

            presetSounds.forEach { s ->
                SoundOptionRow(
                    name = s, 
                    isSelected = selectedSound == s,
                    isPlaying = playingSoundPath == s,
                    onPlay = { SoundManager.playSound(context, s) },
                    onClick = { selectedSound = s }
                )
            }

            SectionHeader("REPETICIÓN")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp)) {
                repetitions.forEach { r ->
                    RepetitionChip(r, selectedRepetition == r) { selectedRepetition = it }
                }
            }

            if (selectedRepetition == "Semanal") {
                DaySelector(selectedDays) { selectedDays = it }
            }

            Button(
                onClick = { 
                    if (isTitleValid) {
                        val repeatDaysString = if (selectedRepetition == "Semanal") selectedDays.joinToString(",") else null
                        onSave(title, description, formattedDate, formattedTime, selectedCategory, selectedColor, selectedSound, selectedRepetition, repeatDaysString) 
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp).height(56.dp),
                enabled = isTitleValid,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor, disabledContainerColor = buttonColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (initialTitle.isEmpty()) "Crear Recordatorio" else "Guardar Cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isTitleValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    // Picker Dialogs (Date/Time)
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = { TextButton(onClick = { showTimePicker = false }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

// Sub-composables to clean up the code
@Composable
fun CategoryChip(name: String, isSelected: Boolean, onClick: (String) -> Unit) {
    val categoryIcons = mapOf("Personal" to Icons.Default.Person, "Trabajo" to Icons.Default.Work, "Salud" to Icons.Default.Favorite, "Finanzas" to Icons.Default.AttachMoney, "Familia" to Icons.Default.Group, "Otro" to Icons.Default.PushPin)
    val categoryColors = mapOf("Personal" to Color(0xFF8B5CF6), "Trabajo" to Color(0xFF10B981), "Salud" to Color(0xFFEF4444), "Finanzas" to Color(0xFFF59E0B), "Familia" to Color(0xFF3B82F6), "Otro" to Color(0xFF6B7280))
    val catColor = categoryColors[name] ?: Color.Gray
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) catColor else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable { onClick(name) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(categoryIcons[name] ?: Icons.Default.Person, null, tint = if (isSelected) Color.White else catColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ColorCircle(colorValue: Long, isSelected: Boolean, onClick: (Long) -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(colorValue))
            .clickable { onClick(colorValue) },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun RepetitionChip(name: String, isSelected: Boolean, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .clickable { onClick(name) }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(name, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
    }
}

@Composable
fun DaySelector(selectedDays: Set<Int>, onToggle: (Set<Int>) -> Unit) {
    val dayNames = listOf("L", "M", "M", "J", "V", "S", "D")
    val dayValues = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
    
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        dayNames.forEachIndexed { index, name ->
            val dayVal = dayValues[index]
            val isDaySelected = selectedDays.contains(dayVal)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (isDaySelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape)
                    .clickable {
                        onToggle(if (isDaySelected) selectedDays - dayVal else selectedDays + dayVal)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(name, color = if (isDaySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(text, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
}

@Composable
fun CustomTextField(
    value: String, 
    onValueChange: (String) -> Unit, 
    placeholder: String, 
    bgColor: Color, 
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = bgColor,
            unfocusedContainerColor = bgColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.onSurface,
            focusedTextColor = MaterialTheme.colorScheme.primary,
            unfocusedTextColor = MaterialTheme.colorScheme.primary
        ),
        minLines = minLines,
        keyboardOptions = keyboardOptions
    )
}

@Composable
fun PickerField(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
    }
}

@Composable
fun SoundActionCard(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(16.dp),
        Arrangement.Center,
        Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    }
}

@Composable
fun SoundOptionRow(name: String, isSelected: Boolean, isPlaying: Boolean, onPlay: () -> Unit, onClick: () -> Unit) {
    val soundIcon = when {
        name.contains("Personalizado") -> Icons.Default.MusicNote
        name == "Campana" -> Icons.Default.Notifications
        name == "Cristal" -> Icons.Default.Diamond
        name == "Clásico" -> Icons.Default.Smartphone
        else -> Icons.Default.MusicNote
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(soundIcon, null, tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Text(name, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        
        IconButton(onClick = onPlay, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onSurface, 
                modifier = Modifier.size(24.dp)
            )
        }
        
        if (isSelected) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        }
    }
}

package com.example.remindme

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remindme.ui.theme.RemindMeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReminderAlertActivity : ComponentActivity() {
    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ReminderActionReceiver.ACTION_DISMISS_ALARM) {
                VibrationManager.stopVibration(this@ReminderAlertActivity)
                SoundManager.stopSound()
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        val filter = android.content.IntentFilter(ReminderActionReceiver.ACTION_DISMISS_ALARM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dismissReceiver, filter)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        val title = intent.getStringExtra("title") ?: "Recordatorio"
        val description = intent.getStringExtra("description") ?: ""
        val colorLong = intent.getLongExtra("color", 0xFF3B82F6)
        val sound = intent.getStringExtra("sound") ?: "Campana"
        val type = intent.getStringExtra("type") ?: "Recordatorio"

        val isAlarm = type == "Alarma"

        // Reiniciar sonido y vibración
        SoundManager.stopSound()
        // Si es alarma, se pone en bucle (loop = true)
        SoundManager.playSound(this, sound, loop = isAlarm) 
        
        VibrationManager.stopVibration(this)
        // Vibración infinita si es alarma
        VibrationManager.startVibration(this, isAlarm = isAlarm)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        setContent {
            RemindMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0B1120).copy(alpha = 0.95f)
                ) {
                    ReminderAlertContent(
                        title = title,
                        description = description,
                        themeColor = Color(colorLong),
                        onComplete = {
                            VibrationManager.stopVibration(this)
                            SoundManager.stopSound()
                            completeReminder(id)
                            val mainIntent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(mainIntent)
                            finish()
                        },
                        onSnooze = {
                            VibrationManager.stopVibration(this)
                            SoundManager.stopSound()
                            snoozeReminder(id, title)
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(dismissReceiver)
        } catch (_: Exception) {
            // Ignorar si no estaba registrado
        }
        VibrationManager.stopVibration(this)
        SoundManager.stopSound()
    }

    private fun completeReminder(id: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
        
        CoroutineScope(Dispatchers.IO).launch {
            val dao = ReminderDatabase.getDatabase(this@ReminderAlertActivity).reminderDao()
            val existing = dao.getReminderById(id)
            if (existing != null) {
                val repetition = existing.repetition ?: "Sin repetición"
                if (repetition != "Sin repetición") {
                    // Reprogramar para la siguiente ocurrencia
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val currentDate = try {
                        sdf.parse(existing.dateTime)?.time ?: System.currentTimeMillis()
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }
                    val nextTime = ReminderScheduler.getNextOccurrence(currentDate, repetition, existing.repeatDays)
                    val nextDateTime = sdf.format(Date(nextTime))
                    
                    val updated = existing.copy(
                        dateTime = nextDateTime,
                        isCompleted = false // Se mantiene ACTIVO para mañana
                    )
                    dao.update(updated)
                    ReminderScheduler.scheduleReminder(
                        this@ReminderAlertActivity, id, existing.title, 
                        nextDateTime.split(" ")[0], nextDateTime.split(" ")[1], 
                        repetition, existing.repeatDays
                    )
                } else {
                    // Si no tiene repetición, se marca como completado normal
                    dao.update(existing.copy(isCompleted = true))
                }
            }
        }
    }

    private fun snoozeReminder(id: Int, title: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)

        val calendar = Calendar.getInstance().apply { add(Calendar.MINUTE, 10) }
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val newDate = sdfDate.format(calendar.time)
        val newTime = sdfTime.format(calendar.time)

        CoroutineScope(Dispatchers.IO).launch {
            val dao = ReminderDatabase.getDatabase(this@ReminderAlertActivity).reminderDao()
            val existing = dao.getReminderById(id)
            if (existing != null) {
                dao.update(existing.copy(
                    dateTime = "$newDate $newTime", 
                    isCompleted = false
                ))
                ReminderScheduler.scheduleReminder(this@ReminderAlertActivity, id, title, newDate, newTime, existing.repetition ?: "Sin repetición", existing.repeatDays)
            }
        }
    }
}

@Composable
fun ReminderAlertContent(
    title: String,
    description: String,
    themeColor: Color,
    onComplete: () -> Unit,
    onSnooze: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(themeColor.copy(alpha = 0.2f), CircleShape)
                        .border(2.dp, themeColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(themeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(35.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "RECORDATORIO",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                if (description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = description,
                        color = Color.LightGray,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Hecho", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.3f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Posponer 10 min", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

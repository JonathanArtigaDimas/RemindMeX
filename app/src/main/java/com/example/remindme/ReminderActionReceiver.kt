package com.example.remindme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReminderActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SNOOZE = "SNOOZE"
        const val ACTION_COMPLETE = "COMPLETE"
        const val ACTION_DEACTIVATE_FOR_TODAY = "DEACTIVATE_FOR_TODAY"
        const val ACTION_DISMISS_ALARM = "com.example.remindme.DISMISS_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val id = intent.getIntExtra("id", -1)
        val title = intent.getStringExtra("title") ?: "Recordatorio"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)

        // Detener sonido y vibración globalmente
        SoundManager.stopSound()
        VibrationManager.stopVibration(context)
        
        // Avisar a la actividad de alerta que se cierre si está abierta
        val dismissIntent = Intent(ACTION_DISMISS_ALARM)
        context.sendBroadcast(dismissIntent)

        if (action == ACTION_COMPLETE && id != -1) {
            markAsCompleted(context, id)
            // Abrir la app en la pantalla principal (igual que el botón "Hecho" de la pantalla)
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(mainIntent)
        } else if (action == ACTION_SNOOZE && id != -1) {
            snoozeReminder(context, id, title)
        } else if (action == ACTION_DEACTIVATE_FOR_TODAY && id != -1) {
            deactivateForToday(context, id)
        }
    }

    private fun deactivateForToday(context: Context, id: Int) {
        val database = ReminderDatabase.getDatabase(context)
        val dao = database.reminderDao()
        CoroutineScope(Dispatchers.IO).launch {
            val existing = dao.getReminderById(id)
            if (existing != null) {
                val repetition = existing.repetition ?: "Sin repetición"
                // Cancel current alarms
                ReminderScheduler.cancelReminder(context, id)

                // Skip all pre-notifications for today as well
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(id + 1000000) // 1h
                notificationManager.cancel(id + 2000000) // 10m
                notificationManager.cancel(id + 3000000) // 30m

                // Calculate next occurrence starting from tomorrow
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val currentDateTime = sdf.parse(existing.dateTime)
                
                val tomorrowCalendar = Calendar.getInstance().apply {
                    time = currentDateTime ?: Date()
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }

                val nextTime = ReminderScheduler.getNextOccurrence(tomorrowCalendar.timeInMillis, repetition, existing.repeatDays)
                val nextDateTime = sdf.format(Date(nextTime))
                
                val updated = existing.copy(
                    dateTime = nextDateTime,
                    isCompleted = false
                )
                dao.update(updated)
                ReminderScheduler.scheduleReminder(
                    context, id, existing.title, 
                    nextDateTime.split(" ")[0], nextDateTime.split(" ")[1], 
                    repetition, existing.repeatDays
                )
            }
        }
    }

    private fun markAsCompleted(context: Context, id: Int) {
        val database = ReminderDatabase.getDatabase(context)
        val dao = database.reminderDao()
        CoroutineScope(Dispatchers.IO).launch {
            val existing = dao.getReminderById(id)
            if (existing != null) {
                val repetition = existing.repetition ?: "Sin repetición"
                if (repetition != "Sin repetición") {
                    // Reprogramar para la siguiente ocurrencia
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val currentDate = sdf.parse(existing.dateTime)?.time ?: System.currentTimeMillis()
                    val nextTime = ReminderScheduler.getNextOccurrence(currentDate, repetition, existing.repeatDays)
                    val nextDateTime = sdf.format(Date(nextTime))
                    
                    val updated = existing.copy(
                        dateTime = nextDateTime,
                        isCompleted = false
                    )
                    dao.update(updated)
                    ReminderScheduler.scheduleReminder(
                        context, id, existing.title, 
                        nextDateTime.split(" ")[0], nextDateTime.split(" ")[1], 
                        repetition, existing.repeatDays
                    )
                } else {
                    dao.update(existing.copy(isCompleted = true))
                }
            }
        }
    }

    private fun snoozeReminder(context: Context, id: Int, title: String) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 10)
        
        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val newDate = sdfDate.format(calendar.time)
        val newTime = sdfTime.format(calendar.time)
        
        val database = ReminderDatabase.getDatabase(context)
        val dao = database.reminderDao()

        CoroutineScope(Dispatchers.IO).launch {
            val existing = dao.getReminderById(id)
            if (existing != null) {
                val updated = existing.copy(
                    dateTime = "$newDate $newTime",
                    isCompleted = false 
                )
                dao.update(updated)
                ReminderScheduler.scheduleReminder(context, id, title, newDate, newTime, existing.repetition ?: "Sin repetición", existing.repeatDays)
            }
        }
    }
}

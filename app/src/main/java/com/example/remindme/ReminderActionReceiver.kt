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
        }
    }

    private fun markAsCompleted(context: Context, id: Int) {
        val database = ReminderDatabase.getDatabase(context)
        val dao = database.reminderDao()
        CoroutineScope(Dispatchers.IO).launch {
            val existing = dao.getReminderById(id)
            if (existing != null) {
                dao.update(existing.copy(isCompleted = true))
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

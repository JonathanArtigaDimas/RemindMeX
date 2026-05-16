package com.example.remindme

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Recordatorio"
        val id = intent.getIntExtra("id", 0)
        val type = intent.getIntExtra("notification_type", 0) // 0: principal, 1: 1 hora, 2: 10 min, 3: 30 min

        if (type == 0) {
            handleMainReminder(context, id, title)
        } else {
            showPreNotification(context, id, title, type)
        }
    }

    private fun handleMainReminder(context: Context, id: Int, title: String) {
        val database = ReminderDatabase.getDatabase(context)
        val dao = database.reminderDao()
        
        CoroutineScope(Dispatchers.IO).launch {
            val existing = dao.getReminderById(id)
            if (existing == null) return@launch
            
            val description = existing.description ?: ""
            val color = existing.color ?: 0xFF3B82F6
            val repetition = existing.repetition ?: "Sin repetición"
            val repeatDays = existing.repeatDays
            val sound = existing.sound ?: "Campana"

            dao.update(existing.copy(isCompleted = true))

            if (repetition != "Sin repetición") {
                val parts = existing.dateTime.split(" ")
                if (parts.size == 2) {
                    ReminderScheduler.scheduleReminder(context, id, title, parts[0], parts[1], repetition, repeatDays)
                }
            }

            val alertIntent = Intent(context, ReminderAlertActivity::class.java).apply {
                putExtra("id", id)
                putExtra("title", title)
                putExtra("description", description)
                putExtra("color", color)
                putExtra("sound", sound)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(alertIntent)

            showNotification(context, id, title, description, sound, color)
        }
    }

    private fun showPreNotification(context: Context, id: Int, title: String, type: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val contentTitle = when (type) {
            1 -> "Falta 1 hora"
            3 -> "Faltan 30 minutos"
            else -> "Faltan 10 minutos"
        }
        val contentText = when (type) {
            1 -> "Falta 1 hora para tu recordatorio: $title"
            3 -> "Faltan 30 minutos para tu recordatorio: $title"
            else -> "Faltan 10 minutos para tu recordatorio: $title"
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(context, id + (type * 1000000), contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val settingsManager = SettingsManager.getInstance(context)
        val defaultSound = settingsManager.defaultNotificationSound.value
        // Usar canal SIN vibración para las pre-notificaciones
        val channelId = NotificationHelper.getChannelIdForSound(context, defaultSound, enableVibration = false)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(null) // Asegurar que no vibre en la notificación individual
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        notificationManager.notify(id + (type * 1000000), notification)
    }

    private fun showNotification(context: Context, id: Int, title: String, description: String, soundPath: String, color: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val contentIntent = Intent(context, ReminderAlertActivity::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("description", description)
            putExtra("color", color)
            putExtra("sound", soundPath)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(context, id, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE
            putExtra("id", id)
            putExtra("title", title)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(context, id + 1000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val doneIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_COMPLETE
            putExtra("id", id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(context, id + 2000, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800, 400, 800, 400, 800)
        
        val soundUri = SoundManager.getNotificationSoundUri(context, soundPath)
        val channelId = NotificationHelper.getChannelIdForSound(context, soundPath)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("¡Ya es hora!")
            .setContentText("Ya es hora de: $title")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri) 
            .setVibrate(vibrationPattern)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_edit, "Posponer 10 min", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Hecho", donePendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}

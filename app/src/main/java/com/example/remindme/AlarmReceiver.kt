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
        val repetition = intent.getStringExtra("repetition") ?: "Sin repetición"

        // Adquirir un WakeLock momentáneo para asegurar que el procesamiento termine
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "RemindMe:AlarmReceiver")
        wakeLock.acquire(5000) // 5 segundos son suficientes

        if (type == 0) {
            handleMainReminder(context, id, title)
        } else {
            showPreNotification(context, id, title, type, repetition)
        }
    }

    private fun handleMainReminder(context: Context, id: Int, title: String) {
        val database = ReminderDatabase.getDatabase(context)
        val dao = database.reminderDao()
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existing = dao.getReminderById(id)
                if (existing == null) return@launch
                
                val description = existing.description ?: ""
                val color = existing.color ?: 0xFF3B82F6
                val sound = existing.sound ?: "Campana"
                val reminderType = existing.type

                ReminderScheduler.completeReminderTask(context, existing, dao)

                val alertIntent = Intent(context, ReminderAlertActivity::class.java).apply {
                    putExtra("id", id)
                    putExtra("title", title)
                    putExtra("description", description)
                    putExtra("color", color)
                    putExtra("sound", sound)
                    putExtra("type", reminderType)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(alertIntent)

                showNotification(context, id, title, description, sound, color)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showPreNotification(context: Context, id: Int, title: String, type: Int, repetition: String) {
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

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(null) // Asegurar que no vibre en la notificación individual
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        // Botón "Desactivar por hoy" para Diario y Semanal
        if (repetition == "Diario" || repetition == "Semanal") {
            val deactivateIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_DEACTIVATE_FOR_TODAY
                putExtra("id", id)
            }
            val deactivatePendingIntent = PendingIntent.getBroadcast(
                context, 
                id + (type * 1000000) + 500, // Unique request code
                deactivateIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Desactivar por hoy", deactivatePendingIntent)
        }

        notificationManager.notify(id + (type * 1000000), builder.build())
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

package com.example.remindme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val BASE_CHANNEL_ID = "reminder_channel_"

    fun getChannelIdForSound(context: Context, soundPath: String, enableVibration: Boolean = true): String {
        // Clean sound name for ID (no special characters, just alphanumeric and underscores)
        val cleanName = soundPath.replace(Regex("[^A-Za-z0-9_]"), "_")
        val vibSuffix = if (enableVibration) "" else "_novib"
        val channelId = BASE_CHANNEL_ID + cleanName + vibSuffix
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Check if channel already exists
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val soundUri = SoundManager.getNotificationSoundUri(context, soundPath)
                val vibText = if (enableVibration) "" else " (Sin vibración)"
                val name = "Recordatorios$vibText ($soundPath)"
                val importance = NotificationManager.IMPORTANCE_HIGH
                
                val alarmAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                
                val channel = NotificationChannel(channelId, name, importance).apply {
                    description = "Notificaciones con sonido $soundPath"
                    this.enableVibration(enableVibration)
                    if (enableVibration) {
                        vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800, 400, 800, 400, 800)
                    } else {
                        vibrationPattern = null
                    }
                    setSound(soundUri, alarmAttributes)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
        return channelId
    }

    fun createNotificationChannel(context: Context) {
        // This is still useful for a default channel
        getChannelIdForSound(context, "Campana")
    }

    fun showTestNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = getChannelIdForSound(context, "Campana")
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Prueba de Notificación")
            .setContentText("Si ves esto, las notificaciones están funcionando.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .build()
        notificationManager.notify(999, notification)
    }
}

package com.example.remindme

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    fun scheduleReminder(context: Context, id: Int, title: String, date: String, time: String, repetition: String = "Sin repetición", repeatDays: String? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateObj = sdf.parse("$date $time")
            
            if (dateObj != null) {
                var triggerTime = dateObj.time
                
                // Si la fecha ya pasó o tiene repetición, calculamos la próxima ocurrencia
                if (triggerTime <= System.currentTimeMillis() || (repetition != "Sin repetición")) {
                    triggerTime = getNextOccurrence(triggerTime, repetition, repeatDays)
                }

                if (triggerTime > System.currentTimeMillis()) {
                    // 1. Programar alarma principal
                    scheduleSingleAlarm(context, id, title, triggerTime, 0)

                    // 2. Lógica de notificaciones previas escalonadas
                    val now = System.currentTimeMillis()
                    val oneHourMillis = 60 * 60 * 1000L
                    val thirtyMinutesMillis = 30 * 60 * 1000L
                    val tenMinutesMillis = 10 * 60 * 1000L

                    // Falta más de 1 hora
                    if (triggerTime - now > oneHourMillis) {
                        scheduleSingleAlarm(context, id, title, triggerTime - oneHourMillis, 1) // 1 hora antes
                    }

                    // Falta más de 30 minutos
                    if (triggerTime - now > thirtyMinutesMillis) {
                        scheduleSingleAlarm(context, id, title, triggerTime - thirtyMinutesMillis, 3) // 30 min antes
                    }

                    // Falta más de 10 minutos
                    if (triggerTime - now > tenMinutesMillis) {
                        scheduleSingleAlarm(context, id, title, triggerTime - tenMinutesMillis, 2) // 10 min antes
                    }
                    
                    Log.d(TAG, "Alarms scheduled for $id. Main at $triggerTime. Repetition: $repetition")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder: ${e.message}")
        }
    }

    private fun scheduleSingleAlarm(context: Context, id: Int, title: String, triggerTime: Long, type: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("id", id)
            putExtra("notification_type", type) // 0: principal, 1: 1 hora, 2: 10 min
            `package` = context.packageName
        }

        // Usar un requestCode único para cada tipo basado en el ID original
        val requestCode = when (type) {
            1 -> id + 1000000 // Offset para 1 hora
            2 -> id + 2000000 // Offset para 10 min
            3 -> id + 3000000 // Offset para 30 min
            else -> id
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val info = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
        alarmManager.setAlarmClock(info, pendingIntent)
    }

    private fun getNextOccurrence(startTime: Long, repetition: String, repeatDays: String?): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = startTime }
        val now = Calendar.getInstance()
        
        // Ensure we are at least at the current time or after
        if (calendar.before(now)) {
            when (repetition) {
                "Diario" -> {
                    while (calendar.before(now)) calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                "Semanal" -> {
                    val allowedDays = repeatDays?.split(",")?.map { it.toInt() }?.toSet() ?: emptySet()
                    
                    // Si no hay días seleccionados, tratamos como semanal normal (cada 7 días)
                    if (allowedDays.isEmpty()) {
                        while (calendar.before(now)) calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    } else {
                        // Buscar el próximo día permitido
                        calendar.add(Calendar.MINUTE, 1) // Avanzar un poco para no repetir el mismo instante
                        while (calendar.before(now) || !allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                }
                "Mensual" -> {
                    while (calendar.before(now)) calendar.add(Calendar.MONTH, 1)
                }
                else -> return startTime
            }
        } else if (repetition == "Semanal" && repeatDays != null) {
            // Caso donde la fecha inicial es futura, pero debemos validar si el día de la semana es permitido
            val allowedDays = repeatDays.split(",").map { it.toInt() }.toSet()
            if (allowedDays.isNotEmpty() && !allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                while (!allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        return calendar.timeInMillis
    }

    fun cancelReminder(context: Context, id: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Cancelar tanto la principal como las pre-notificaciones
        listOf(id, id + 1000000, id + 2000000, id + 3000000).forEach { requestCode ->
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                `package` = context.packageName
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}

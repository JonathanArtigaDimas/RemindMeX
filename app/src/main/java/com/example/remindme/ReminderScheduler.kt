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

    fun scheduleReminder(context: Context, id: Int, title: String, date: String, time: String, repetition: String = "Sin repetición", repeatDays: String? = null, customInterval: Int? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Primero, cancelar cualquier alarma previa para este ID
        cancelReminder(context, id)
        
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
                
                // Si la fecha ya pasó o tiene repetición, calculamos la PRÓXIMA ocurrencia real.
                if (triggerTime <= System.currentTimeMillis() || (repetition != "Sin repetición")) {
                    triggerTime = getNextOccurrence(triggerTime, repetition, repeatDays, customInterval)
                }

                if (triggerTime > System.currentTimeMillis()) {
                    // 1. Programar alarma principal
                    scheduleSingleAlarm(context, id, title, triggerTime, 0, repetition)

                    // 2. Lógica de notificaciones previas escalonadas
                    val now = System.currentTimeMillis()
                    val oneHourMillis = 60 * 60 * 1000L
                    val thirtyMinutesMillis = 30 * 60 * 1000L
                    val tenMinutesMillis = 10 * 60 * 1000L

                    // Falta más de 1 hora
                    if (triggerTime - now > oneHourMillis) {
                        scheduleSingleAlarm(context, id, title, triggerTime - oneHourMillis, 1, repetition) // 1 hora antes
                    }

                    // Falta más de 30 minutos
                    if (triggerTime - now > thirtyMinutesMillis) {
                        scheduleSingleAlarm(context, id, title, triggerTime - thirtyMinutesMillis, 3, repetition) // 30 min antes
                    }

                    // Falta más de 10 minutos
                    if (triggerTime - now > tenMinutesMillis) {
                        scheduleSingleAlarm(context, id, title, triggerTime - tenMinutesMillis, 2, repetition) // 10 min antes
                    }
                    
                    Log.d(TAG, "Alarms scheduled for $id. Main at $triggerTime. Repetition: $repetition")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder: ${e.message}")
        }
    }

    private fun scheduleSingleAlarm(context: Context, id: Int, title: String, triggerTime: Long, type: Int, repetition: String = "Sin repetición") {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("id", id)
            putExtra("notification_type", type) // 0: principal, 1: 1 hora, 2: 10 min
            putExtra("repetition", repetition)
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

    fun getNextOccurrence(startTime: Long, repetition: String, repeatDays: String?, customInterval: Int? = null): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = startTime }
        val now = Calendar.getInstance()
        
        // Buscamos la siguiente fecha válida que esté estrictamente en el futuro
        var safetyCount = 0
        do {
            when (repetition) {
                "Diario" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "Semanal" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "Mensual" -> calendar.add(Calendar.MONTH, 1)
                "Personalizado" -> {
                    val interval = customInterval ?: 1 // Mínimo 1 minuto
                    calendar.add(Calendar.MINUTE, interval)
                }
                else -> break
            }
            safetyCount++
            if (safetyCount > 366 * 2) break // Seguridad para evitar bucles infinitos
        } while (calendar.timeInMillis <= now.timeInMillis || (repetition == "Semanal" && !isValidDay(calendar, repeatDays)))

        return calendar.timeInMillis
    }

    suspend fun completeReminderTask(context: Context, reminder: Reminder, dao: ReminderDao) {
        val repetition = reminder.repetition ?: "Sin repetición"
        
        if (repetition == "Sin repetición") {
            // Caso normal: Solo marcar como completado
            dao.update(reminder.copy(isCompleted = true))
            cancelReminder(context, reminder.id)
        } else {
            // Caso recurrente: Calcular próxima ocurrencia y reprogramar
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val currentBaseTime = try {
                sdf.parse(reminder.dateTime)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            val nextOccurrence = getNextOccurrence(currentBaseTime, repetition, reminder.repeatDays, reminder.customInterval)
            val nextDateTimeStr = sdf.format(Date(nextOccurrence))
            val dateParts = nextDateTimeStr.split(" ")

            val updatedReminder = reminder.copy(
                dateTime = nextDateTimeStr,
                isCompleted = false // Se mantiene pendiente para la próxima vez
            )
            
            dao.update(updatedReminder)
            
            scheduleReminder(
                context = context,
                id = updatedReminder.id,
                title = updatedReminder.title,
                date = dateParts[0],
                time = dateParts[1],
                repetition = repetition,
                repeatDays = updatedReminder.repeatDays,
                customInterval = updatedReminder.customInterval
            )
        }
    }

    private fun isValidDay(calendar: Calendar, repeatDays: String?): Boolean {
        val allowedDays = repeatDays?.split(",")?.filter { it.isNotEmpty() }?.map { it.toInt() }?.toSet() ?: emptySet()
        if (allowedDays.isEmpty()) return true // Si no hay días marcados, cualquier día es válido (se comporta como semanal normal)
        return allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK))
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

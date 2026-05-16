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
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val id = intent.getIntExtra("id", -1)
        val title = intent.getStringExtra("title") ?: "Recordatorio"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)

        if (action == "SNOOZE" && id != -1) {
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
                    // Importante: Al posponer no queremos que la repetición original se pierda
                    ReminderScheduler.scheduleReminder(context, id, title, newDate, newTime, existing.repetition ?: "Sin repetición")
                }
            }
        }
    }
}

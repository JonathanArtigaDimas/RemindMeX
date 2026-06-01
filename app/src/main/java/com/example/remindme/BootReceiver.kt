package com.example.remindme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val pendingResult = goAsync()
            val database = ReminderDatabase.getDatabase(context)
            val dao = database.reminderDao()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reminders = dao.getAllRemindersList()
                    reminders.filter { !it.isCompleted }.forEach { reminder ->
                        val parts = reminder.dateTime.split(" ")
                        if (parts.size == 2) {
                            ReminderScheduler.scheduleReminder(
                                context, 
                                reminder.id, 
                                reminder.title, 
                                parts[0], 
                                parts[1], 
                                reminder.repetition ?: "Sin repetición",
                                reminder.repeatDays
                            )
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

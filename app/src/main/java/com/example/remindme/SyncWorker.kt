package com.example.remindme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = ReminderDatabase.getDatabase(applicationContext)
        val favoriteGroupDao = database.favoriteGroupDao()
        val firestore = FirebaseFirestore.getInstance()

        val favorites = favoriteGroupDao.getAllFavorites().first()
        
        for (group in favorites) {
            // 1. Buscar mensajes nuevos en el chat
            val chatSnapshot = firestore.collection("groups").document(group.groupId)
                .collection("chat")
                .whereGreaterThan("timestamp", group.lastCheckedTimestamp)
                .get()
                .await()

            if (!chatSnapshot.isEmpty) {
                showNotification(
                    "Novedades en ${group.name}",
                    "Hay ${chatSnapshot.size()} mensajes nuevos en el chat."
                )
            }

            // 2. Buscar notas nuevas
            val notesSnapshot = firestore.collection("shared_notes")
                .whereEqualTo("groupId", group.groupId)
                .whereGreaterThan("createdAt", group.lastCheckedTimestamp)
                .get()
                .await()

            if (!notesSnapshot.isEmpty) {
                showNotification(
                    "Novedades en ${group.name}",
                    "Se han añadido ${notesSnapshot.size()} notas nuevas al equipo."
                )
            }

            // Actualizar el timestamp local para no repetir notificaciones
            favoriteGroupDao.insertFavorite(group.copy(lastCheckedTimestamp = System.currentTimeMillis()))
        }

        return Result.success()
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "team_updates_bg"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alertas de Equipo", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        fun schedule(context: Context) {
            // El mínimo permitido por Android para trabajos periódicos es 15 minutos
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("team_sync_worker")
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "team_sync_worker",
                ExistingPeriodicWorkPolicy.UPDATE, // Actualiza la configuración si ya existe
                request
            )

            // TRUCO PRO: Lanzamos uno inmediato (OneTime) para que la primera revisión sea YA
            val immediateRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueue(immediateRequest)
        }
    }
}

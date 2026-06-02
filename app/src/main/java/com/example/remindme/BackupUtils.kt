package com.example.remindme

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.OutputStream

object BackupUtils {

    fun exportNotesToJson(context: Context, uri: Uri) {
        val database = ReminderDatabase.getDatabase(context)
        val noteDao = database.noteDao()
        val reminderDao = database.reminderDao()
        val favoriteGroupDao = database.favoriteGroupDao()
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Obtener datos locales
                val notes = noteDao.getAllNotesWithTags().first()
                val reminders = reminderDao.getAllReminders().first()
                val favoriteGroups = favoriteGroupDao.getAllFavorites().first()

                // 2. Obtener chats de Firestore para cada grupo favorito
                val groupChats = mutableMapOf<String, List<MessageFirestore>>()
                for (group in favoriteGroups) {
                    try {
                        val chatSnapshot = firestore.collection("groups").document(group.groupId)
                            .collection("chat")
                            .orderBy("timestamp")
                            .get()
                            .await()
                        val messages = chatSnapshot.toObjects(MessageFirestore::class.java)
                        groupChats[group.groupId] = messages
                    } catch (e: Exception) {
                        android.util.Log.e("BackupUtils", "Error al obtener chat del grupo ${group.groupId}: ${e.message}")
                    }
                }

                val backupData = mapOf(
                    "notes" to notes,
                    "reminders" to reminders,
                    "favoriteGroups" to favoriteGroups,
                    "groupChats" to groupChats,
                    "exportDate" to System.currentTimeMillis(),
                    "app" to "RemindMe"
                )

                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonString = gson.toJson(backupData)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Copia de seguridad (con chats) guardada con éxito", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun importNotesFromJson(context: Context, uri: Uri) {
        val database = ReminderDatabase.getDatabase(context)
        val noteDao = database.noteDao()
        val reminderDao = database.reminderDao()
        val favoriteGroupDao = database.favoriteGroupDao()
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw Exception("No se pudo leer el archivo")

                val gson = com.google.gson.Gson()
                val backupData: Map<String, Any> = gson.fromJson(jsonString, object : TypeToken<Map<String, Any>>() {}.type)

                // Restaurar Notas
                val notesJson = gson.toJson(backupData["notes"])
                val notesList: List<NoteWithTags> = gson.fromJson(notesJson, object : TypeToken<List<NoteWithTags>>() {}.type)
                notesList.forEach { noteWithTags ->
                    noteDao.updateNoteWithTags(noteWithTags.note.copy(id = 0), noteWithTags.tags)
                }

                // Restaurar Recordatorios
                val remindersJson = gson.toJson(backupData["reminders"])
                val remindersList: List<Reminder> = gson.fromJson(remindersJson, object : TypeToken<List<Reminder>>() {}.type)
                remindersList.forEach { reminder ->
                    reminderDao.insert(reminder.copy(id = 0))
                }

                // Restaurar Grupos Favoritos
                if (backupData.containsKey("favoriteGroups")) {
                    val groupsJson = gson.toJson(backupData["favoriteGroups"])
                    val groupsList: List<FavoriteGroup> = gson.fromJson(groupsJson, object : TypeToken<List<FavoriteGroup>>() {}.type)
                    groupsList.forEach { group ->
                        favoriteGroupDao.insertFavorite(group)
                    }
                }

                // Restaurar Chats (Subir a Firestore si no existen)
                if (backupData.containsKey("groupChats")) {
                    val chatsJson = gson.toJson(backupData["groupChats"])
                    val chatsMap: Map<String, List<MessageFirestore>> = gson.fromJson(chatsJson, object : TypeToken<Map<String, List<MessageFirestore>>>() {}.type)
                    
                    chatsMap.forEach { (groupId, messages) ->
                        val chatRef = firestore.collection("groups").document(groupId).collection("chat")
                        messages.forEach { msg ->
                            chatRef.document(msg.messageId).set(msg, com.google.firebase.firestore.SetOptions.merge())
                        }
                    }
                }

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Datos y Chats restaurados con éxito", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Error al importar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

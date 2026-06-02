package com.example.remindme

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SharedNotesRepository(
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val sharedNoteDao: SharedNoteDao,
    private val favoriteGroupDao: FavoriteGroupDao,
    private val sharedNotebookDao: SharedNotebookDao
) {

    // Favoritos
    fun getFavoriteGroups() = favoriteGroupDao.getAllFavorites()

    suspend fun addFavoriteGroup(groupId: String) {
        val groupName = "Equipo ${groupId.take(6)}" 
        favoriteGroupDao.insertFavorite(FavoriteGroup(groupId, groupName, System.currentTimeMillis()))
    }

    suspend fun removeFavoriteGroup(groupId: String) {
        favoriteGroupDao.deleteByGroupId(groupId)
    }

    suspend fun renameGroup(groupId: String, newName: String): Result<Unit> = try {
        firestore.collection("groups").document(groupId)
            .update("name", newName)
            .await()
        favoriteGroupDao.insertFavorite(FavoriteGroup(groupId, newName, System.currentTimeMillis()))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 1. Obtener detalles del grupo (para el modal de info)
    suspend fun getGroupDetails(groupId: String): Result<GroupFirestore> = try {
        val doc = firestore.collection("groups").document(groupId).get().await()
        val group = doc.toObject(GroupFirestore::class.java)
        if (group != null) Result.success(group) else Result.failure(Exception("Grupo no encontrado"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 2. Sincronización en tiempo real: Firestore -> Room
    @OptIn(ExperimentalCoroutinesApi::class)
    fun syncSharedNotesWithRoom(groupId: String): Flow<Unit> = callbackFlow {
        // Al empezar la sincronización, aprovechamos para refrescar el nombre local del grupo
        val groupRef = firestore.collection("groups").document(groupId)
        groupRef.get().addOnSuccessListener { snapshot ->
            snapshot.toObject(GroupFirestore::class.java)?.let { group ->
                kotlinx.coroutines.GlobalScope.launch {
                    favoriteGroupDao.insertFavorite(FavoriteGroup(groupId, group.name, System.currentTimeMillis()))
                }
            }
        }

        val listenerRegistration = firestore.collection("shared_notes")
            .whereEqualTo("groupId", groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    cancel("Error en Firestore", error)
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val notes = querySnapshot.toObjects(NoteFirestore::class.java)
                    
                    // Ejecutamos en un Scope de IO para no bloquear el listener
                    CoroutineScope(Dispatchers.IO).launch {
                        val entities = notes.map { it.toEntity() }
                        sharedNoteDao.upsertAll(entities) // Upsert evita duplicados
                    }
                }
            }
        
        // Sincronización de Cuadernos Compartidos
        val notebookListener = firestore.collection("groups").document(groupId)
            .collection("notebooks")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let { querySnapshot ->
                    val notebooks = querySnapshot.toObjects(SharedNotebookFirestore::class.java)
                    CoroutineScope(Dispatchers.IO).launch {
                        val entities = notebooks.map { it.toEntity() }
                        sharedNotebookDao.upsertAll(entities)
                    }
                }
            }
        
        awaitClose { 
            listenerRegistration.remove() 
            notebookListener.remove()
        }
    }

    // 2. Crear Grupo con Código Único
    suspend fun createGroup(name: String): Result<String> = try {
        val currentUser = auth.currentUser ?: throw Exception("No autenticado")
        val groupId = firestore.collection("groups").document().id
        val inviteCode = (1..6).map { (('A'..'Z') + ('0'..'9')).random() }.joinToString("")
        
        val newGroup = GroupFirestore(
            groupId = groupId,
            name = name,
            inviteCode = inviteCode,
            ownerId = currentUser.uid,
            members = listOf(currentUser.uid),
            memberRoles = mapOf(currentUser.uid to "owner")
        )
        
        firestore.runBatch { batch ->
            // Crear el grupo
            batch.set(firestore.collection("groups").document(groupId), newGroup)
            // Asegurar que el documento del usuario existe y añadir el grupo (set con merge evita el error NOT_FOUND)
            val userRef = firestore.collection("users").document(currentUser.uid)
            batch.set(userRef, mapOf("groupIds" to FieldValue.arrayUnion(groupId)), com.google.firebase.firestore.SetOptions.merge())
        }.await()
        
        // Guardamos en favoritos localmente con el nombre real
        favoriteGroupDao.insertFavorite(FavoriteGroup(groupId, name, System.currentTimeMillis()))
        
        Result.success(inviteCode)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 3. Unirse a un grupo con código
    suspend fun joinGroupWithCode(inviteCode: String): Result<Pair<String, String>> = try {
        val currentUser = auth.currentUser ?: throw Exception("No autenticado")
        val query = firestore.collection("groups")
            .whereEqualTo("inviteCode", inviteCode)
            .limit(1)
            .get()
            .await()

        if (query.isEmpty) throw Exception("Código de invitación no válido")
        
        val groupDoc = query.documents[0]
        val groupData = groupDoc.toObject(GroupFirestore::class.java)!!
        val groupId = groupData.groupId
        val groupName = groupData.name

        firestore.runBatch { batch ->
            batch.update(firestore.collection("groups").document(groupId), 
                "members", FieldValue.arrayUnion(currentUser.uid))
            batch.update(firestore.collection("groups").document(groupId),
                "memberRoles.${currentUser.uid}", "editor") // Rol por defecto: editor
            
            // Asegurar que el documento del usuario existe y añadir el grupo (set con merge evita el error NOT_FOUND)
            val userRef = firestore.collection("users").document(currentUser.uid)
            batch.set(userRef, mapOf("groupIds" to FieldValue.arrayUnion(groupId)), com.google.firebase.firestore.SetOptions.merge())
        }.await()
        
        // Guardamos en favoritos localmente con el nombre recuperado de la nube
        favoriteGroupDao.insertFavorite(FavoriteGroup(groupId, groupName, System.currentTimeMillis()))
        
        Result.success(groupId to groupName)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 4. Subir/Editar Nota
    suspend fun saveSharedNote(note: NoteFirestore) {
        firestore.collection("shared_notes").document(note.noteId).set(note).await()
    }

    // 4.1 Actualizar perfil de usuario para asegurar que aparezca en la lista
    suspend fun syncUserProfile() {
        val user = auth.currentUser ?: return
        val profile = mapOf(
            "uid" to user.uid,
            "name" to (user.displayName ?: "Usuario"),
            "email" to (user.email ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: "")
        )
        firestore.collection("users").document(user.uid)
            .set(profile, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    // 5. Eliminar Nota
    suspend fun deleteSharedNote(noteId: String): Result<Unit> = try {
        firestore.collection("shared_notes").document(noteId).delete().await()
        sharedNoteDao.deleteNote(noteId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 6. Alternar Anclado
    suspend fun togglePinSharedNote(note: SharedNoteEntity): Result<Unit> = try {
        val updatedNote = note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis())
        saveSharedNote(updatedNote.toFirestore())
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 7. Compartir una nota local existente a un grupo
    suspend fun shareLocalNoteToGroup(groupId: String, title: String, content: String): Result<Unit> = try {
        val user = auth.currentUser ?: throw Exception("No autenticado")
        val noteId = firestore.collection("shared_notes").document().id
        val note = NoteFirestore(
            noteId = noteId,
            groupId = groupId,
            title = title,
            content = content,
            authorId = user.uid,
            authorName = user.displayName ?: "Anon"
        )
        saveSharedNote(note)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 6. Obtener notas de Room como Flow (UI observa esto)
    fun getSharedNotesFromRoom(groupId: String): Flow<List<SharedNoteEntity>> {
        return sharedNoteDao.getNotesByGroup(groupId)
    }

    fun getSharedNotebooksFromRoom(groupId: String): Flow<List<SharedNotebookEntity>> {
        return sharedNotebookDao.getNotebooksByGroup(groupId)
    }

    suspend fun login(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, password).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun register(email: String, password: String, name: String): Result<Unit> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
            displayName = name
        }
        result.user?.updateProfile(profileUpdates)?.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> = try {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun logout() {
        auth.signOut()
    }

    fun getCurrentUser() = auth.currentUser

    // --- NUEVAS FUNCIONES PRO ---

    // 8. Crear Cuaderno Compartido
    suspend fun createSharedNotebook(groupId: String, name: String, color: Long): Result<Unit> = try {
        val id = firestore.collection("groups").document(groupId).collection("notebooks").document().id
        val notebook = SharedNotebookFirestore(id, groupId, name, System.currentTimeMillis(), color)
        firestore.collection("groups").document(groupId).collection("notebooks").document(id)
            .set(notebook)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 9. Eliminar Cuaderno Compartido
    suspend fun deleteSharedNotebook(groupId: String, notebookId: String): Result<Unit> = try {
        firestore.collection("groups").document(groupId).collection("notebooks").document(notebookId)
            .delete()
            .await()
        sharedNotebookDao.deleteNotebook(notebookId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 10. Obtener lista de miembros con sus perfiles
    fun getGroupMembers(groupId: String): Flow<List<UserFirestore>> = callbackFlow {
        val listener = firestore.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, _ ->
                val group = snapshot?.toObject(GroupFirestore::class.java) ?: return@addSnapshotListener
                val uids = group.members
                
                if (uids.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                firestore.collection("users").whereIn("uid", uids).get()
                    .addOnSuccessListener { usersSnapshot ->
                        val users = usersSnapshot.toObjects(UserFirestore::class.java)
                        // Inyectamos el rol desde el grupo al objeto User para el UI
                        val usersWithRoles = users.map { user ->
                            val role = group.memberRoles[user.uid] ?: "editor"
                            user.copy(name = "${user.name} ($role)") 
                        }
                        trySend(usersWithRoles)
                    }
            }
        awaitClose { listener.remove() }
    }

    // 9. Cambiar rol de un miembro
    suspend fun updateMemberRole(groupId: String, targetUid: String, newRole: String): Result<Unit> = try {
        firestore.collection("groups").document(groupId)
            .update("memberRoles.$targetUid", newRole)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 10. Eliminar miembro del grupo
    suspend fun removeMember(groupId: String, targetUid: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            val groupRef = firestore.collection("groups").document(groupId)
            batch.update(groupRef, "members", FieldValue.arrayRemove(targetUid))
            batch.update(groupRef, "memberRoles.$targetUid", FieldValue.delete())
            
            val userRef = firestore.collection("users").document(targetUid)
            batch.update(userRef, "groupIds", FieldValue.arrayRemove(groupId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 11. Actualizar Anuncio del Grupo
    suspend fun updateGroupAnnouncement(groupId: String, announcement: String): Result<Unit> = try {
        firestore.collection("groups").document(groupId)
            .update("announcement", announcement)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 12. Añadir comentario a una nota
    // 12. Añadir comentario a una nota
    suspend fun addComment(noteId: String, text: String): Result<Unit> = try {
        val user = auth.currentUser ?: throw Exception("No autenticado")
        
        val commentMap = mapOf(
            "commentId" to java.util.UUID.randomUUID().toString(),
            "authorId" to user.uid,
            "authorName" to (user.displayName ?: "Usuario"),
            "text" to text,
            "createdAt" to System.currentTimeMillis()
        )
        
        firestore.collection("shared_notes").document(noteId)
            .update(
                "comments", FieldValue.arrayUnion(commentMap),
                "updatedAt", System.currentTimeMillis()
            )
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        android.util.Log.e("SharedNotesRepo", "Error adding comment: ${e.message}", e)
        Result.failure(e)
    }

    // 12.1 Editar comentario
    suspend fun editComment(noteId: String, commentId: String, newText: String): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            val noteRef = firestore.collection("shared_notes").document(noteId)
            val snapshot = transaction.get(noteRef)
            val comments = snapshot.get("comments") as? List<Map<String, Any>> ?: emptyList()
            
            val updatedComments = comments.map { 
                if (it["commentId"] == commentId) {
                    it.toMutableMap().apply { put("text", newText) }
                } else it
            }
            
            transaction.update(noteRef, "comments", updatedComments)
            transaction.update(noteRef, "updatedAt", System.currentTimeMillis())
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 12.2 Borrar comentario
    suspend fun deleteComment(noteId: String, commentId: String): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            val noteRef = firestore.collection("shared_notes").document(noteId)
            val snapshot = transaction.get(noteRef)
            val comments = snapshot.get("comments") as? List<Map<String, Any>> ?: emptyList()
            
            val updatedComments = comments.filter { it["commentId"] != commentId }
            
            transaction.update(noteRef, "comments", updatedComments)
            transaction.update(noteRef, "updatedAt", System.currentTimeMillis())
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 13. CHAT COMPLETO: Enviar mensaje al grupo
    suspend fun sendGroupMessage(groupId: String, text: String): Result<Unit> = try {
        val user = auth.currentUser ?: throw Exception("No autenticado")
        val message = MessageFirestore(
            messageId = java.util.UUID.randomUUID().toString(),
            authorId = user.uid,
            authorName = user.displayName ?: "Usuario",
            text = text,
            timestamp = System.currentTimeMillis()
        )
        firestore.collection("groups").document(groupId)
            .collection("chat").document(message.messageId)
            .set(message)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 14. CHAT COMPLETO: Editar mensaje
    suspend fun editGroupMessage(groupId: String, messageId: String, newText: String): Result<Unit> = try {
        firestore.collection("groups").document(groupId)
            .collection("chat").document(messageId)
            .update("text", newText)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 15. CHAT COMPLETO: Borrar mensaje
    suspend fun deleteGroupMessage(groupId: String, messageId: String): Result<Unit> = try {
        firestore.collection("groups").document(groupId)
            .collection("chat").document(messageId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 16. CHAT COMPLETO: Escuchar mensajes en tiempo real
    fun getGroupMessages(groupId: String): Flow<List<MessageFirestore>> = callbackFlow {
        val listener = firestore.collection("groups").document(groupId)
            .collection("chat")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    cancel("Error en Chat", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(MessageFirestore::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }
}

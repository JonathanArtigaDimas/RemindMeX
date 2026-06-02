package com.example.remindme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SharedNotesViewModel(
    private val repository: SharedNotesRepository,
    private val storageRepository: SharedStorageRepository,
    private val context: android.content.Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("shared_notes_prefs", android.content.Context.MODE_PRIVATE)
    private val KEY_GROUP_ID = "last_group_id"

    private val _uiState = MutableStateFlow<SharedNotesUiState>(SharedNotesUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _currentGroupId = MutableStateFlow<String?>(prefs.getString(KEY_GROUP_ID, null))
    val currentGroupId = _currentGroupId.asStateFlow()

    private val _currentUser = MutableStateFlow(repository.getCurrentUser())
    val currentUser = _currentUser.asStateFlow()

    val favoriteGroups = repository.getFavoriteGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        _currentGroupId.value?.let { startSync(it) }
        viewModelScope.launch {
            repository.syncUserProfile()
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = SharedNotesUiState.Loading
            val result = repository.login(email, password)
            if (result.isSuccess) {
                _currentUser.value = repository.getCurrentUser()
                _uiState.value = SharedNotesUiState.Success
            } else {
                _uiState.value = SharedNotesUiState.Error(result.exceptionOrNull()?.message ?: "Error al iniciar sesión")
            }
        }
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _uiState.value = SharedNotesUiState.Loading
            val result = repository.register(email, password, name)
            if (result.isSuccess) {
                _currentUser.value = repository.getCurrentUser()
                _uiState.value = SharedNotesUiState.Success
            } else {
                val exception = result.exceptionOrNull()
                val message = if (exception is com.google.firebase.auth.FirebaseAuthException && 
                    exception.errorCode == "ERROR_OPERATION_NOT_ALLOWED") {
                    "Configuración requerida: El registro por Email está desactivado en tu consola de Firebase. Por favor, actívalo en la sección Authentication > Sign-in method."
                } else {
                    exception?.message ?: "Error al registrarse"
                }
                _uiState.value = SharedNotesUiState.Error(message)
            }
        }
    }

    fun onGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            android.util.Log.d("SharedNotesVM", "Starting Google Sign In with token: ${idToken.take(10)}...")
            _uiState.value = SharedNotesUiState.Loading
            val result = repository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                val user = repository.getCurrentUser()
                android.util.Log.d("SharedNotesVM", "Firebase Sign In Success: ${user?.email}")
                _currentUser.value = user
                _uiState.value = SharedNotesUiState.Success
            } else {
                val error = result.exceptionOrNull()
                android.util.Log.e("SharedNotesVM", "Firebase Sign In Failed: ${error?.message}", error)
                _uiState.value = SharedNotesUiState.Error(error?.message ?: "Error al iniciar sesión con Google")
            }
        }
    }

    fun logout() {
        _currentGroupId.value?.let { 
            com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("group_$it")
        }
        repository.logout()
        _currentUser.value = null
        _currentGroupId.value = null
    }

    // Observamos Room directamente (Single Source of Truth)
    @OptIn(ExperimentalCoroutinesApi::class)
    val sharedNotesFromRoom = _currentGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.getSharedNotesFromRoom(groupId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sharedNotebooksFromRoom = _currentGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.getSharedNotebooksFromRoom(groupId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedNotebookId = MutableStateFlow<String?>(null)
    val selectedNotebookId = _selectedNotebookId.asStateFlow()

    private val _selectedInternalTab = MutableStateFlow(0) // 0: Notas, 1: Chat
    val selectedInternalTab = _selectedInternalTab.asStateFlow()

    fun setSelectedInternalTab(index: Int) {
        _selectedInternalTab.value = index
    }

    fun setSelectedNotebook(notebookId: String?) {
        _selectedNotebookId.value = if (_selectedNotebookId.value == notebookId) null else notebookId
    }

    fun createSharedNotebook(name: String, color: Long) {
        val groupId = _currentGroupId.value ?: return
        viewModelScope.launch {
            repository.createSharedNotebook(groupId, name, color)
        }
    }

    fun deleteSharedNotebook(notebookId: String) {
        val groupId = _currentGroupId.value ?: return
        viewModelScope.launch {
            repository.deleteSharedNotebook(groupId, notebookId)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentGroupMembers = _currentGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.getGroupMembers(groupId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateMemberRole(targetUid: String, newRole: String) {
        val groupId = _currentGroupId.value ?: return
        viewModelScope.launch {
            repository.updateMemberRole(groupId, targetUid, newRole)
        }
    }

    fun removeMember(targetUid: String) {
        val groupId = _currentGroupId.value ?: return
        viewModelScope.launch {
            repository.removeMember(groupId, targetUid)
        }
    }

    fun addComment(noteId: String, text: String) {
        viewModelScope.launch {
            repository.addComment(noteId, text)
        }
    }

    fun editComment(noteId: String, commentId: String, newText: String) {
        viewModelScope.launch {
            repository.editComment(noteId, commentId, newText)
        }
    }

    fun deleteComment(noteId: String, commentId: String) {
        viewModelScope.launch {
            repository.deleteComment(noteId, commentId)
        }
    }

    val currentGroupMessages = _currentGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.getGroupMessages(groupId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendGroupMessage(text: String) {
        val groupId = _currentGroupId.value ?: return
        viewModelScope.launch {
            repository.sendGroupMessage(groupId, text)
        }
    }

    fun editGroupMessage(messageId: String, newText: String) {
        val groupId = _currentGroupId.value ?: return
        viewModelScope.launch {
            repository.editGroupMessage(groupId, messageId, newText)
        }
    }

    fun deleteGroupMessage(messageId: String) {
        val groupId = _currentGroupId.value ?: return
        viewModelScope.launch {
            repository.deleteGroupMessage(groupId, messageId)
        }
    }

    fun updateAnnouncement(newAnnouncement: String) {
        val groupId = _currentGroupId.value ?: return
        viewModelScope.launch {
            _uiState.value = SharedNotesUiState.Loading
            val result = repository.updateGroupAnnouncement(groupId, newAnnouncement)
            if (result.isSuccess) {
                _uiState.value = SharedNotesUiState.Success
            } else {
                _uiState.value = SharedNotesUiState.Error(result.exceptionOrNull()?.message ?: "Error al actualizar anuncio")
            }
        }
    }

    fun selectGroup(groupId: String) {
        _currentGroupId.value = groupId.ifEmpty { null }
        if (groupId.isNotEmpty()) {
            prefs.edit().putString(KEY_GROUP_ID, groupId).apply()
            viewModelScope.launch { 
                repository.addFavoriteGroup(groupId)
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("group_$groupId")
            }
            startSync(groupId)
        } else {
            prefs.edit().remove(KEY_GROUP_ID).apply()
        }
    }

    fun removeFavorite(groupId: String) {
        viewModelScope.launch {
            repository.removeFavoriteGroup(groupId)
            com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("group_$groupId")
        }
    }

    fun renameCurrentGroup(newName: String) {
        val groupId = _currentGroupId.value ?: return
        viewModelScope.launch {
            _uiState.value = SharedNotesUiState.Loading
            val result = repository.renameGroup(groupId, newName)
            if (result.isSuccess) {
                _uiState.value = SharedNotesUiState.Success
            } else {
                _uiState.value = SharedNotesUiState.Error(result.exceptionOrNull()?.message ?: "Error al renombrar")
            }
        }
    }

    private val _currentGroupDetails = MutableStateFlow<GroupFirestore?>(null)
    val currentGroupDetails = _currentGroupDetails.asStateFlow()

    fun fetchCurrentGroupDetails() {
        val groupId = _currentGroupId.value ?: return
        viewModelScope.launch {
            val result = repository.getGroupDetails(groupId)
            if (result.isSuccess) {
                _currentGroupDetails.value = result.getOrNull()
            }
        }
    }

    private fun startSync(groupId: String) {
        viewModelScope.launch {
            repository.syncSharedNotesWithRoom(groupId).collect()
        }
        // Suscribirse al tema del grupo para notificaciones
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("group_$groupId")
    }

    fun createNewGroup(name: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = SharedNotesUiState.Loading
            val result = repository.createGroup(name)
            if (result.isSuccess) {
                val inviteCode = result.getOrNull() ?: ""
                _uiState.value = SharedNotesUiState.Success
                onSuccess(inviteCode)
            } else {
                _uiState.value = SharedNotesUiState.Error(result.exceptionOrNull()?.message ?: "Error al crear grupo")
            }
        }
    }

    fun joinGroup(inviteCode: String) {
        viewModelScope.launch {
            _uiState.value = SharedNotesUiState.Loading
            val result = repository.joinGroupWithCode(inviteCode)
            if (result.isSuccess) {
                val (groupId, _) = result.getOrThrow()
                _uiState.value = SharedNotesUiState.Success
                selectGroup(groupId) // Cambiamos a la pantalla de notas inmediatamente
            } else {
                _uiState.value = SharedNotesUiState.Error(result.exceptionOrNull()?.message ?: "Código inválido")
            }
        }
    }

    fun addNote(title: String, content: String, imagePath: String? = null, audioPath: String? = null, color: Long = 0xFF1E293B, notebookId: String? = null) {
        val groupId = _currentGroupId.value ?: return
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _uiState.value = SharedNotesUiState.Loading
            
            // Verificamos conexión antes de subir
            val isOnline = isNetworkAvailable(context)

            val finalImagePath = if (imagePath?.startsWith("/") == true) {
                if (isOnline) {
                    val uploadedUrl = storageRepository.uploadFile(imagePath, "shared_images/${System.currentTimeMillis()}.jpg")
                    if (uploadedUrl == null) {
                        _uiState.value = SharedNotesUiState.Error("Fallo al subir la imagen. Verifica tu conexión.")
                        return@launch
                    }
                    uploadedUrl
                } else {
                    imagePath // Mantenemos ruta local si estamos offline
                }
            } else imagePath

            val finalAudioPath = if (audioPath?.startsWith("/") == true) {
                if (isOnline) {
                    val uploadedUrl = storageRepository.uploadFile(audioPath, "shared_audio/${System.currentTimeMillis()}.mp3")
                    if (uploadedUrl == null) {
                        _uiState.value = SharedNotesUiState.Error("Fallo al subir el audio.")
                        return@launch
                    }
                    uploadedUrl
                } else {
                    audioPath
                }
            } else audioPath

            val noteId = java.util.UUID.randomUUID().toString()
            val note = NoteFirestore(
                noteId = noteId,
                groupId = groupId,
                title = title,
                content = content,
                authorId = user.uid,
                authorName = user.displayName ?: "Anon",
                imagePath = finalImagePath,
                audioPath = finalAudioPath,
                color = color,
                notebookId = notebookId ?: _selectedNotebookId.value // Si no se pasa, usamos el actual de la UI
            )
            
            if (isOnline) {
                repository.saveSharedNote(note)
            } else {
                repository.saveSharedNote(note)
            }
            
            _uiState.value = SharedNotesUiState.Success
        }
    }

    private fun isNetworkAvailable(context: android.content.Context): Boolean {
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    fun shareExistingNote(groupId: String, title: String, content: String, imagePath: String? = null, audioPath: String? = null, color: Long = 0xFF1E293B, isPinned: Boolean = false, noteId: String? = null, notebookId: String? = null) {
        viewModelScope.launch {
            _uiState.value = SharedNotesUiState.Loading
            
            // Verificamos conexión antes de subir
            val isOnline = isNetworkAvailable(context)

            val finalImagePath = if (imagePath?.startsWith("/") == true) {
                if (isOnline) {
                    val uploadedUrl = storageRepository.uploadFile(imagePath, "shared_images/${System.currentTimeMillis()}.jpg")
                    if (uploadedUrl == null) {
                        _uiState.value = SharedNotesUiState.Error("Fallo al subir la imagen. Verifica tu conexión.")
                        return@launch
                    }
                    uploadedUrl
                } else {
                    imagePath // Mantenemos ruta local si estamos offline
                }
            } else imagePath

            val finalAudioPath = if (audioPath?.startsWith("/") == true) {
                if (isOnline) {
                    val uploadedUrl = storageRepository.uploadFile(audioPath, "shared_audio/${System.currentTimeMillis()}.mp3")
                    if (uploadedUrl == null) {
                        _uiState.value = SharedNotesUiState.Error("Fallo al subir el audio.")
                        return@launch
                    }
                    uploadedUrl
                } else {
                    audioPath
                }
            } else audioPath

            val finalNoteId = noteId ?: java.util.UUID.randomUUID().toString()
            val user = repository.getCurrentUser() 
            
            // CRÍTICO: Si notebookId es nulo, intentamos obtener el de la UI si el usuario está dentro de una carpeta
            val finalNotebookId = notebookId ?: _selectedNotebookId.value

            val note = NoteFirestore(
                noteId = finalNoteId,
                groupId = groupId,
                title = title,
                content = content,
                authorId = user?.uid ?: "anon",
                authorName = user?.displayName ?: "Anon",
                imagePath = finalImagePath,
                audioPath = finalAudioPath,
                color = color,
                isPinned = isPinned,
                notebookId = finalNotebookId
            )
            
            repository.saveSharedNote(note)
            _uiState.value = SharedNotesUiState.Success
        }
    }

    fun togglePin(note: SharedNoteEntity) {
        viewModelScope.launch {
            repository.togglePinSharedNote(note)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            _uiState.value = SharedNotesUiState.Loading
            val result = repository.deleteSharedNote(noteId)
            if (result.isSuccess) {
                _uiState.value = SharedNotesUiState.Success
            } else {
                _uiState.value = SharedNotesUiState.Error(result.exceptionOrNull()?.message ?: "Error al eliminar")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = SharedNotesUiState.Idle
    }
}

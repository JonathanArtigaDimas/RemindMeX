package com.example.remindme

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object SoundManager {
    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    
    private val _currentSoundPath = MutableStateFlow<String?>(null)
    val currentSoundPath: StateFlow<String?> = _currentSoundPath.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun playSound(context: Context, soundPath: String, loop: Boolean = false) {
        if (_currentSoundPath.value == soundPath && mediaPlayer?.isPlaying == true) {
            stopSound()
            return
        }

        stopSound()
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            mediaPlayer = if (soundPath.startsWith("/")) {
                MediaPlayer().apply {
                    setDataSource(soundPath)
                    setAudioAttributes(audioAttributes)
                    isLooping = loop
                    prepare()
                    start()
                }
            } else {
                val resId = getSoundResourceId(context, soundPath)
                if (resId != 0) {
                    MediaPlayer.create(context, resId).apply { 
                        setAudioAttributes(audioAttributes)
                        isLooping = loop
                        start() 
                    }
                } else null
            }
            _currentSoundPath.value = soundPath
            mediaPlayer?.setOnCompletionListener {
                if (!loop) {
                    _currentSoundPath.value = null
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error al reproducir sonido: ${e.message}")
        }
    }

    fun stopSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _currentSoundPath.value = null
    }

    fun startRecording(context: Context) {
        try {
            val fileName = "recording_${System.currentTimeMillis()}.mp3"
            recordingFile = File(context.filesDir, fileName)
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile?.absolutePath)
                prepare()
                start()
            }
            _isRecording.value = true
        } catch (e: Exception) {
            Log.e("SoundManager", "Error al iniciar grabación: ${e.message}")
        }
    }

    fun stopRecording(context: Context): String? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false
            val path = recordingFile?.absolutePath
            if (path != null) {
                saveCustomSoundPath(context, path)
            }
            path
        } catch (e: Exception) {
            Log.e("SoundManager", "Error al detener grabación: ${e.message}")
            _isRecording.value = false
            null
        }
    }

    fun deleteSound(context: Context, soundPath: String) {
        try {
            if (soundPath.startsWith("/")) {
                val file = File(soundPath)
                if (file.exists()) {
                    file.delete()
                }
                val prefs = context.getSharedPreferences("sounds", Context.MODE_PRIVATE)
                val sounds = prefs.getStringSet("custom_sounds", emptySet())?.toMutableSet() ?: mutableSetOf()
                sounds.remove(soundPath)
                prefs.edit().putStringSet("custom_sounds", sounds).apply()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error al eliminar sonido: ${e.message}")
        }
    }

    fun isPlaying(soundPath: String): Boolean {
        return _currentSoundPath.value == soundPath && mediaPlayer?.isPlaying == true
    }

    fun getSoundResourceId(context: Context, soundName: String): Int {
        // --- ACA DEBES AGREGAR LOS SONIDOS ---
        // Debes copiar tus archivos MP3 o WAV en la carpeta:
        // app/src/main/res/raw/
        // Y nombrarlos exactamente como se indica abajo (en minúsculas):
        
        val resName = when (soundName) {
            "Clásico" -> "clasico"    // Requiere clasico.mp3 o clasico.wav
            "Melodía" -> "melodia"    // Requiere melodia.mp3 o melodia.wav
            "Aviso"   -> "aviso"      // Requiere aviso.mp3 o aviso.wav
            "Trompeta" -> "trompeta"  // Requiere trompeta.mp3 o trompeta.wav
            "Campana" -> "campana"    // Requiere campana.mp3 o campana.wav
            "Cristal" -> "cristal"    // Requiere cristal.mp3 o cristal.wav
            else -> return 0
        }
        return context.resources.getIdentifier(resName, "raw", context.packageName)
    }

    fun getNotificationSoundUri(context: Context, soundPath: String): Uri {
        return if (soundPath.startsWith("/")) {
            Uri.fromFile(File(soundPath))
        } else {
            val resId = getSoundResourceId(context, soundPath)
            if (resId != 0) {
                Uri.parse("android.resource://" + context.packageName + "/" + resId)
            } else {
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
            }
        }
    }

    fun saveMp3ToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val fileName = "custom_sound_${System.currentTimeMillis()}.mp3"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            val path = file.absolutePath
            saveCustomSoundPath(context, path)
            path
        } catch (e: Exception) {
            Log.e("SoundManager", "Error al guardar MP3: ${e.message}")
            null
        }
    }

    private fun saveCustomSoundPath(context: Context, path: String) {
        val prefs = context.getSharedPreferences("sounds", Context.MODE_PRIVATE)
        val sounds = prefs.getStringSet("custom_sounds", emptySet())?.toMutableSet() ?: mutableSetOf()
        sounds.add(path)
        prefs.edit().putStringSet("custom_sounds", sounds).apply()
    }

    fun getCustomSounds(context: Context): List<String> {
        val prefs = context.getSharedPreferences("sounds", Context.MODE_PRIVATE)
        return prefs.getStringSet("custom_sounds", emptySet())?.toList() ?: emptyList()
    }
}

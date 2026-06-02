package com.example.remindme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("remindme_settings", Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(prefs.getString("theme", "Default") ?: "Default")
    val currentTheme: StateFlow<String> = _currentTheme

    private val _currentFont = MutableStateFlow(prefs.getString("font", "System") ?: "System")
    val currentFont: StateFlow<String> = _currentFont

    private val _hapticFeedback = MutableStateFlow(prefs.getBoolean("haptic_feedback", true))
    val hapticFeedback: StateFlow<Boolean> = _hapticFeedback

    private val _vibrationIntensity = MutableStateFlow(prefs.getFloat("vibration_intensity", 0.5f))
    val vibrationIntensity: StateFlow<Float> = _vibrationIntensity

    private val _defaultNotificationSound = MutableStateFlow(prefs.getString("default_sound", "Campana") ?: "Campana")
    val defaultNotificationSound: StateFlow<String> = _defaultNotificationSound

    private val _fontSizeMultiplier = MutableStateFlow(prefs.getFloat("font_size_multiplier", 1.0f))
    val fontSizeMultiplier: StateFlow<Float> = _fontSizeMultiplier

    private val _isFontBold = MutableStateFlow(prefs.getBoolean("is_font_bold", false))
    val isFontBold: StateFlow<Boolean> = _isFontBold

    private val _isFontItalic = MutableStateFlow(prefs.getBoolean("is_font_italic", false))
    val isFontItalic: StateFlow<Boolean> = _isFontItalic

    private val _isSpecialMode = MutableStateFlow(prefs.getBoolean("is_special_mode", false))
    val isSpecialMode: StateFlow<Boolean> = _isSpecialMode

    // Fondos de pantalla personalizados
    private val _homeBg = MutableStateFlow(prefs.getString("home_bg", null))
    val homeBg: StateFlow<String?> = _homeBg

    private val _notesBg = MutableStateFlow(prefs.getString("notes_bg", null))
    val notesBg: StateFlow<String?> = _notesBg

    private val _teamsBg = MutableStateFlow(prefs.getString("teams_bg", null))
    val teamsBg: StateFlow<String?> = _teamsBg

    private val _settingsBg = MutableStateFlow(prefs.getString("settings_bg", null))
    val settingsBg: StateFlow<String?> = _settingsBg

    private val _bgOpacity = MutableStateFlow(prefs.getFloat("bg_opacity", 0.5f))
    val bgOpacity: StateFlow<Float> = _bgOpacity

    fun setTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
        _currentTheme.value = theme
    }

    fun setFont(font: String) {
        prefs.edit().putString("font", font).apply()
        _currentFont.value = font
    }

    fun setHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback", enabled).apply()
        _hapticFeedback.value = enabled
    }

    fun setVibrationIntensity(intensity: Float) {
        prefs.edit().putFloat("vibration_intensity", intensity).apply()
        _vibrationIntensity.value = intensity
    }

    fun setDefaultNotificationSound(sound: String) {
        prefs.edit().putString("default_sound", sound).apply()
        _defaultNotificationSound.value = sound
    }

    fun setFontSizeMultiplier(multiplier: Float) {
        prefs.edit().putFloat("font_size_multiplier", multiplier).apply()
        _fontSizeMultiplier.value = multiplier
    }

    fun setFontBold(bold: Boolean) {
        prefs.edit().putBoolean("is_font_bold", bold).apply()
        _isFontBold.value = bold
    }

    fun setFontItalic(italic: Boolean) {
        prefs.edit().putBoolean("is_font_italic", italic).apply()
        _isFontItalic.value = italic
    }

    fun setSpecialMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_special_mode", enabled).apply()
        _isSpecialMode.value = enabled
    }

    fun setHomeBg(uri: String?) {
        prefs.edit().putString("home_bg", uri).apply()
        _homeBg.value = uri
    }

    fun setNotesBg(uri: String?) {
        prefs.edit().putString("notes_bg", uri).apply()
        _notesBg.value = uri
    }

    fun setTeamsBg(uri: String?) {
        prefs.edit().putString("teams_bg", uri).apply()
        _teamsBg.value = uri
    }

    fun setSettingsBg(uri: String?) {
        prefs.edit().putString("settings_bg", uri).apply()
        _settingsBg.value = uri
    }

    fun setBgOpacity(opacity: Float) {
        prefs.edit().putFloat("bg_opacity", opacity).apply()
        _bgOpacity.value = opacity
    }

    fun resetToDefault() {
        prefs.edit().clear().apply()
        _currentTheme.value = "Default"
        _currentFont.value = "System"
        _hapticFeedback.value = true
        _vibrationIntensity.value = 0.5f
        _defaultNotificationSound.value = "Campana"
        _fontSizeMultiplier.value = 1.0f
        _isFontBold.value = false
        _isFontItalic.value = false
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

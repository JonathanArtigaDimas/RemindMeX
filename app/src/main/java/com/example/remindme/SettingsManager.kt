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

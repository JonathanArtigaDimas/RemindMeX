package com.example.remindme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationManager {
    private var vibrator: Vibrator? = null

    fun startVibration(context: Context, isAlarm: Boolean = false) {
        if (vibrator != null) return
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Patrón: Vibrar 1s, Esperar 0.5s. 
        // Si es alarma, se repite infinitamente (índice 0). Si es recordatorio, solo 5 veces (-1).
        val pattern = if (isAlarm) {
            longArrayOf(0, 1200, 400) // Un poco más intensa para alarmas
        } else {
            longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000)
        }
        
        val repeatIndex = if (isAlarm) 0 else -1
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, repeatIndex))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, repeatIndex)
        }
    }

    fun stopVibration(context: Context) {
        if (vibrator == null) {
            // Re-obtener vibrador solo para cancelar si perdimos la instancia
            val tempVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            tempVibrator.cancel()
        } else {
            vibrator?.cancel()
            vibrator = null
        }
    }
}

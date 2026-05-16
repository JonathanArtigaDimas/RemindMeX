package com.example.remindme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationManager {
    private var vibrator: Vibrator? = null

    fun startVibration(context: Context) {
        if (vibrator != null) return
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Patrón: Vibrar 1s, Esperar 0.5s. Repetir 5 veces.
        val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // -1 en el argumento repeat indica que no debe entrar en bucle infinito
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
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

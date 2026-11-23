package com.arthur.arcade.util

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

fun startVibrating(context: Context) {
	val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
	val vibrator = vibratorManager.defaultVibrator

	if (vibrator.hasVibrator()) {
		val timings = longArrayOf(0, 100, 50)
		val amplitudes = intArrayOf(0, 255, 0)
		vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
	}
}

fun stopVibrating(context: Context) {
	val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
	val vibrator = vibratorManager.defaultVibrator
	vibrator.cancel()
}

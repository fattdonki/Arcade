package com.arthur.arcade.firewall

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.VpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

enum class ShizukuState {
	NotRunning,
	NeedsPermission,
	Ready
}

object PermissionManager {
	private val _notificationsEnabled = MutableStateFlow(false)
	val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

	private val _dndEnabled = MutableStateFlow(false)
	val dndEnabled: StateFlow<Boolean> = _dndEnabled.asStateFlow()

	private val _vpnGranted = MutableStateFlow(false)
	val vpnGranted: StateFlow<Boolean> = _vpnGranted.asStateFlow()

	private val _shizukuState = MutableStateFlow(ShizukuState.NotRunning)
	val shizukuState: StateFlow<ShizukuState> = _shizukuState.asStateFlow()

	fun refresh(context: Context) {
		val appContext = context.applicationContext
		val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		_notificationsEnabled.value = notificationManager.areNotificationsEnabled()
		_dndEnabled.value = notificationManager.isNotificationPolicyAccessGranted
		_vpnGranted.value = VpnService.prepare(appContext) == null
		_shizukuState.value = when {
			!Shizuku.pingBinder() -> ShizukuState.NotRunning
			Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> ShizukuState.NeedsPermission
			else -> ShizukuState.Ready
		}
	}

	fun onShizukuPermissionResult(granted: Boolean) {
		_shizukuState.value = if (granted) ShizukuState.Ready else ShizukuState.NeedsPermission
	}

	fun onNotificationResult(granted: Boolean) {
		_notificationsEnabled.value = granted
	}
}
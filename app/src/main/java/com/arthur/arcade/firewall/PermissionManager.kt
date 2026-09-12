package com.arthur.arcade.firewall

import android.app.NotificationManager
import android.content.Context
import android.net.VpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PermissionManager {
	private val _notificationsEnabled = MutableStateFlow(false)
	val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

	private val _dndEnabled = MutableStateFlow(false)
	val dndEnabled: StateFlow<Boolean> = _dndEnabled.asStateFlow()

	private val _vpnGranted = MutableStateFlow(false)
	val vpnGranted: StateFlow<Boolean> = _vpnGranted.asStateFlow()

	fun refresh(context: Context) {
		val appContext = context.applicationContext // Use ApplicationContext to prevent memory leaks
		val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		_notificationsEnabled.value = notificationManager.areNotificationsEnabled()
		_dndEnabled.value = notificationManager.isNotificationPolicyAccessGranted
		_vpnGranted.value = VpnService.prepare(appContext) == null
	}

	fun onNotificationResult(granted: Boolean) {
		_notificationsEnabled.value = granted
	}
}
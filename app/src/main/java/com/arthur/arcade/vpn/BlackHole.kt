package com.arthur.arcade.vpn

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import androidx.core.app.NotificationCompat
import com.arthur.arcade.R
import java.io.FileInputStream

@SuppressLint("VpnServicePolicy")
class BlackHoleVpn : VpnService() {

	companion object {
		const val TARGET_PACKAGE = "TARGET_PACKAGE"
		const val CHANNEL_ID = "arcade_vpn_channel"
		const val ACTION_STOP_VPN = "com.arthur.arcade.vpn.ACTION_STOP_VPN"
	}

	private var vpnThread: Thread? = null
	private var tunnelInterface: android.os.ParcelFileDescriptor? = null

	private var targetPackage: String? = null

	fun vpn(packageName: String) {
		tunnelInterface = Builder()
			.addAddress("10.0.0.1", 32)
			.addRoute("0.0.0.0", 0)
			.addRoute("::", 0)
			.addAllowedApplication(packageName)
			.establish() ?: run { stopSelf(); return }

		vpnThread = Thread {
			val buffer = ByteArray(32767)
			val input = FileInputStream(tunnelInterface?.fileDescriptor)
			try {
				while (!Thread.currentThread().isInterrupted) {
					input.read(buffer)
				}
			} catch (_: Exception) {}
		}.apply{ start() }
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent?.action == ACTION_STOP_VPN) {
			tunnelInterface?.close()
			tunnelInterface = null
			vpnThread?.interrupt()
			stopForeground(STOP_FOREGROUND_REMOVE)
			stopSelf()
			return START_NOT_STICKY
		}

		targetPackage = intent?.getStringExtra(TARGET_PACKAGE)
		targetPackage?.let { vpn(it) }

		createNotificationChannel()
		showNotification()

		return START_NOT_STICKY
	}

	override fun onDestroy() {
		tunnelInterface?.close()
		tunnelInterface = null

		vpnThread?.interrupt()
		vpnThread = null

		stopForeground(STOP_FOREGROUND_REMOVE)

		super.onDestroy()
	}

	private fun createNotificationChannel() {
		val name = "Arcade"
		val importance = NotificationManager.IMPORTANCE_LOW

		val channel = NotificationChannel(CHANNEL_ID, name, importance)

		val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.createNotificationChannel(channel)
	}

	private fun showNotification() {
		val stopIntent = Intent(this, BlackHoleVpn::class.java).apply {
			action = ACTION_STOP_VPN
		}

		val stopPendingIntent = PendingIntent.getService(
			this,
			0,
			stopIntent,
			PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
		)

		val iconRes = R.drawable.icon

		val notification = NotificationCompat.Builder(this, CHANNEL_ID)
			.setSmallIcon(iconRes)
			.setContentTitle("Arcade VPN is running")
			.setContentText("Internet blocking is active for your game.")
			.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
			.setOngoing(true)
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.build()

		startForeground(1, notification)
	}
}
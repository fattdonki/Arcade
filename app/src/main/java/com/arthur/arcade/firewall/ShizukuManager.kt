package com.arthur.arcade.firewall

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.arthur.arcade.IFirewallService
import rikka.shizuku.Shizuku
import com.arthur.arcade.BuildConfig

object ShizukuManager {
	private var firewallService: IFirewallService? = null

	private val serviceArgs = Shizuku.UserServiceArgs(
		ComponentName(BuildConfig.APPLICATION_ID, FirewallUserService::class.java.name)
	)
		.processNameSuffix("firewall_service")
		.debuggable(BuildConfig.DEBUG)
		.version(1)

	private val connection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName, binder: IBinder) {
			firewallService = IFirewallService.Stub.asInterface(binder)
			Log.d("ShizukuManager", "user service connected")
			enableFirewall()
		}
		override fun onServiceDisconnected(name: ComponentName) {
			firewallService = null
			Log.d("ShizukuManager", "user service disconnected")
		}
	}

	val isShizukuRunning: Boolean
		get() = firewallService != null

	fun bind() {
		if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
			Shizuku.bindUserService(serviceArgs, connection)
			Log.d("ShizukuManager", "Bind called")
		}
	}

	fun unbind() {
		Shizuku.unbindUserService(serviceArgs, connection, true)
	}

	fun enableFirewall() {
		firewallService?.setChain3Enabled(true)
	}

	fun disableFirewall() {
		firewallService?.setChain3Enabled(false)
	}

	fun block(packageName: String) {
		firewallService?.setPackageNetworkingEnabled(false, packageName)
	}

	fun unblock(packageName: String) {
		firewallService?.setPackageNetworkingEnabled(true, packageName)
	}
}
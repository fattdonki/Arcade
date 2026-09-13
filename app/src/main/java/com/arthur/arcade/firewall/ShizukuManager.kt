package com.arthur.arcade.firewall

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.arthur.arcade.BuildConfig
import com.arthur.arcade.IFirewallService
import com.arthur.arcade.SettingsRepository
import rikka.shizuku.Shizuku

object ShizukuManager {
	private var firewallService: IFirewallService? = null
	private var appContext: Context? = null
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
			appContext?.let { ctx ->
				SettingsRepository.loadAllBlockedPackages(ctx).forEach { block(it) }
			}
		}
		override fun onServiceDisconnected(name: ComponentName) {
			firewallService = null
			Log.d("ShizukuManager", "user service disconnected")
		}
	}

	val isShizukuRunning: Boolean
		get() = firewallService != null

	fun bind(context: Context) {
		appContext = context.applicationContext
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
package com.arthur.arcade.firewall

import android.util.Log
import com.arthur.arcade.IFirewallService

class FirewallUserService : IFirewallService.Stub() {
	override fun setChain3Enabled(enabled: Boolean) {
		val command = arrayOf("cmd", "connectivity", "set-chain3-enabled", enabled.toString())
		val process = Runtime.getRuntime().exec(command)
		val exitCode = process.waitFor()
		Log.d("FirewallUserService", "set-chain3-enabled $enabled -> exit code $exitCode")
	}

	override fun setPackageNetworkingEnabled(enabled: Boolean, packageName: String) {
		val command = arrayOf(
			"cmd",
			"connectivity",
			"set-package-networking-enabled",
			enabled.toString(),
			packageName
		)
		val process = Runtime.getRuntime().exec(command)
		val exitCode = process.waitFor()
		Log.d(
			"FirewallUserService",
			"set-package-networking-enabled $enabled $packageName -> exit code $exitCode"
		)
	}
}
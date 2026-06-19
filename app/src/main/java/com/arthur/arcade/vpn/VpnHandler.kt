package com.arthur.arcade.vpn

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.VpnService
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

interface VpnHandler {
	fun initVpnHandler(activity: ComponentActivity)
	fun checkAndStartVpn(packageName: String)
}

class VpnHandlerImpl: VpnHandler {
	private lateinit var activity: ComponentActivity
	private lateinit var vpnLauncher: ActivityResultLauncher<Intent>
	private var vpnTargetPackage: String? = null

	override fun initVpnHandler(activity: ComponentActivity) {
		this.activity = activity
		this.vpnLauncher = activity.registerForActivityResult(
			ActivityResultContracts.StartActivityForResult()
		){ result ->
			if (result.resultCode == RESULT_OK) {
				startVPN()
			}
		}
	}

	override fun checkAndStartVpn(packageName: String) {
		val intent = VpnService.prepare(activity)
		vpnTargetPackage = packageName

		if (intent != null) {
			vpnLauncher.launch(intent)
		} else {
			startVPN()
		}
	}

	private fun startVPN() {
		val intent = Intent(activity, BlackHoleVpn::class.java)

		intent.putExtra(BlackHoleVpn.TARGET_PACKAGE, vpnTargetPackage)

		activity.startService(intent)
	}
}
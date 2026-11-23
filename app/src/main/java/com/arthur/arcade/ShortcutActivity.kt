package com.arthur.arcade

import android.app.Activity
import android.os.Bundle
import com.arthur.arcade.data.SettingsRepository
import com.arthur.arcade.util.applyProfile


class ShortcutActivity : Activity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val packageName = intent.getStringExtra(EXTRA_PACKAGE)

		if (packageName != null) {
			val profile = SettingsRepository.loadProfile(this, packageName)

			applyProfile(this, packageName, profile)
			packageManager.getLaunchIntentForPackage(packageName)
				?.let { startActivity(it) }
		}
		finish()
	}

	companion object {
		const val EXTRA_PACKAGE = "package_name"
		const val ACTION_LAUNCH_GAME = "com.arthur.arcade.LAUNCH_GAME"
	}
}
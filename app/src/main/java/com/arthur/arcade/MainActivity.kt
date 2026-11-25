package com.arthur.arcade

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arthur.arcade.data.SettingsRepository
import com.arthur.arcade.firewall.PermissionManager
import com.arthur.arcade.firewall.ShizukuManager
import com.arthur.arcade.firewall.VpnHandler
import com.arthur.arcade.firewall.VpnHandlerImpl
import com.arthur.arcade.ui.screens.Home
import com.arthur.arcade.ui.screens.Onboarding
import com.arthur.arcade.ui.theme.ArcadeTheme
import com.arthur.arcade.ui.theme.Theme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity(), VpnHandler by VpnHandlerImpl() {
	val vpnPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) {
		PermissionManager.refresh(this)
	}

	val notificationPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted ->
		PermissionManager.onNotificationResult(isGranted)
	}

	val dndPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) {
		PermissionManager.refresh(this)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		installSplashScreen()

		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		initVpnHandler(this)
		ShizukuManager.bind(this)

		Shizuku.addRequestPermissionResultListener { _, grantResult ->
			PermissionManager.onShizukuPermissionResult(grantResult == PackageManager.PERMISSION_GRANTED)
		}

		setContent {
			val context = LocalContext.current

			val system = if (isSystemInDarkTheme()) Theme.Dark else Theme.Light

			var theme by rememberSaveable {
				mutableStateOf(SettingsRepository.loadTheme(context, system))
			}

			ArcadeTheme(theme) {
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

					var showOnboarding by remember {
						mutableStateOf(
							!SettingsRepository.hasSeenOnboarding(context)
						)
					}

					if (showOnboarding) {
						Onboarding(
							this,
							Modifier.padding(innerPadding),
							onContinue = {
								showOnboarding = false
								SettingsRepository.setHasSeenOnboarding(context)
							}
						)
					} else {
						Home(
							Modifier.padding(innerPadding),
							theme,
							onThemeSelected = { newTheme ->
								theme = newTheme
								SettingsRepository.saveTheme(context, newTheme)
							}
						)
					}
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()
		PermissionManager.refresh(this)
	}

	override fun onDestroy() {
		super.onDestroy()
		if (isFinishing) {
			ShizukuManager.disableFirewall()
			ShizukuManager.unbind()
		}
	}
}
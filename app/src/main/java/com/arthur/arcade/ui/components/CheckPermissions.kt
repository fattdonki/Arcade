package com.arthur.arcade.ui.components

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arthur.arcade.MainActivity
import com.arthur.arcade.firewall.PermissionManager
import com.arthur.arcade.firewall.ShizukuState
import rikka.shizuku.Shizuku


enum class Permissions {
	DND,
	VPN,
	Notifications,
	Shizuku,
	UpgradeToShizuku
}

const val REQUEST_CODE: Int = 8080


@Composable
fun CheckPermissions(
	context: Context,
	permissions: List<Permissions> = listOf(
		Permissions.DND, Permissions.VPN, Permissions.Notifications, Permissions.Shizuku
	),
	onDone: () -> Unit,
) {
	val notificationManager = remember(context) {
		context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	}

	var showDndDialog by remember {
		mutableStateOf(
			!notificationManager.isNotificationPolicyAccessGranted
				&& permissions.contains(Permissions.DND)
		)
	}
	var showVpnDialog by remember {
		mutableStateOf(
			VpnService.prepare(context) != null
					&& permissions.contains(Permissions.VPN)
					&& PermissionManager.shizukuState.value == ShizukuState.NotRunning
		)
	}
	var showNotificationDialog by remember {
		mutableStateOf(
			!notificationManager.areNotificationsEnabled()
					&& permissions.contains(Permissions.Notifications)
					&& PermissionManager.shizukuState.value == ShizukuState.NotRunning
		)
	}

	var showShizuku by remember {
		mutableStateOf(
			Shizuku.pingBinder()
					&& Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED
					&& permissions.contains(Permissions.Shizuku)
		)
	}

	var showUpgradeToShizuku by remember {
		mutableStateOf(permissions.contains(Permissions.UpgradeToShizuku))
	}

	if (showDndDialog) {
		AlertDialog(
			onDismissRequest = { showDndDialog = false },
			icon = {
				Icon(
					imageVector = Icons.Default.DoNotDisturbOn,
					contentDescription = "Do Not Disturb"
				)
			},
			title = {
				Text("DND Permission", style = MaterialTheme.typography.titleMedium)
			},
			text = {
				Text(
					"For Arcade to automatically enable Do Not Disturb, you need to grant it permission.",
					style = MaterialTheme.typography.bodyMedium
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
						if (context is MainActivity) {
							context.dndPermissionLauncher.launch(intent)
						}
						showDndDialog = false
					}
				) {
					Text("Grant Permission")
				}
			},
			dismissButton = {
				TextButton(onClick = { showDndDialog = false }) {
					Text("No thanks")
				}
			}
		)
	} else if (showVpnDialog) {
		AlertDialog(
			onDismissRequest = { showVpnDialog = false },
			icon = {
				Icon(
					imageVector = Icons.Default.VpnKey,
					contentDescription = "VPN"
				)
			},
			title = {
				Text("VPN Permission", style = MaterialTheme.typography.titleMedium)
			},
			text = {
				Text(
					"For Arcade to block internet access per game, you need to grant it VPN permission.",
					style = MaterialTheme.typography.bodyMedium
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						if (context is MainActivity) {
							val intent = VpnService.prepare(context)
							if (intent != null) {
								context.vpnPermissionLauncher.launch(intent)
							}
						}
						showVpnDialog = false
					}
				) {
					Text("Grant Permission")
				}
			},
			dismissButton = {
				TextButton(onClick = { showVpnDialog = false }) {
					Text("No thanks")
				}
			}
		)
	} else if (showNotificationDialog) {
		AlertDialog(
			onDismissRequest = { showNotificationDialog = false },
			icon = {
				Icon(
					imageVector = Icons.Default.Notifications,
					contentDescription = "Notifications"
				)
			},
			title = {
				Text("Notification Permission", style = MaterialTheme.typography.titleMedium)
			},
			text = {
				Text(
					"Arcade uses notifications to allow you to manually turn off the Arcade VPN.",
					style = MaterialTheme.typography.bodyMedium
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						if (context is MainActivity) {
							context.notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
						}
						showNotificationDialog = false
					}
				) {
					Text("Grant Permission")
				}
			},
			dismissButton = {
				TextButton(onClick = { showNotificationDialog = false }) {
					Text("No thanks")
				}
			}
		)
	} else if (showShizuku) {
		AlertDialog(
			onDismissRequest = { showShizuku = false },
			icon = {
				Icon(
					imageVector = Icons.Default.Adb,
					contentDescription = "Adb"
				)
			},
			title = {
				Text("Shizuku Permission", style = MaterialTheme.typography.titleMedium)
			},
			text = {
				Text(
					"Arcade supports a Shizuku powered firewall for the best performance.",
					style = MaterialTheme.typography.bodyMedium
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						Shizuku.requestPermission(REQUEST_CODE)
						showShizuku = false
					}
				) {
					Text("Grant Permission")
				}
			},
			dismissButton = {
				TextButton(
					onClick = {
						showNotificationDialog = true
						showVpnDialog = true
						showShizuku = false
					}
				) {
					Text("No thanks")
				}
			}
		)
	} else if (showUpgradeToShizuku) {
		AlertDialog(
			onDismissRequest = { showUpgradeToShizuku = false },
			icon = {
				Icon(
					imageVector = Icons.Default.Adb,
					contentDescription = "Adb"
				)
			},
			title = {
				Text("Upgrade to Shizuku", style = MaterialTheme.typography.titleMedium)
			},
			text = {
				Text(
					"Arcade supports a Shizuku powered firewall for the best performance. It will " +
							"significantly reduce background power draw compared to using a VPN.",
					style = MaterialTheme.typography.bodyMedium
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						Shizuku.requestPermission(REQUEST_CODE)
						showUpgradeToShizuku = false
					}
				) {
					Text("Grant Permission")
				}
			},
			dismissButton = {
				TextButton(
					onClick = { showUpgradeToShizuku = false }
				) {
					Text("No thanks")
				}
			}
		)
	}

	if (!showDndDialog && !showVpnDialog && !showNotificationDialog && !showShizuku && !showUpgradeToShizuku) {
		onDone()
	}
}
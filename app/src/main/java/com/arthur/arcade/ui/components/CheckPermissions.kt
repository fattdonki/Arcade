package com.arthur.arcade.ui.components

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import androidx.compose.material.icons.Icons
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

@Composable
@Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
fun CheckPermissions(context: Context) {
	val notificationManager = remember(context) {
		context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
	}

	var showDndDialog by remember { mutableStateOf(!notificationManager.isNotificationPolicyAccessGranted) }
	var showVpnDialog by remember { mutableStateOf(VpnService.prepare(context) != null) }
	var showNotificationDialog by remember { mutableStateOf(!notificationManager.areNotificationsEnabled())}

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
					Text("Cancel")
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
					Text("Cancel")
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
					"Arcade uses notifications to allow you to manually turn of the Arcade VPN.",
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
					Text("Cancel")
				}
			}
		)
	}
}
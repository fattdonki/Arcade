package com.arthur.arcade.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlin.collections.forEach

data class FabMenuItem(
	val label: String,
	val icon: ImageVector,
	val onClick: () -> Unit
)

@Composable
fun BoxScope.FabMenu(items: List<FabMenuItem>) {
	var expanded by remember { mutableStateOf(false) }

	BackHandler(expanded) { expanded = false }

	FloatingActionButtonMenu(
		modifier = Modifier
			.align(Alignment.BottomEnd)
			.padding(16.dp),
		expanded = expanded,
		button = {
			ToggleFloatingActionButton(
				checked = expanded,
				onCheckedChange = { expanded = it }
			) {
				Icon(
					imageVector = if(expanded) Icons.Default.Close else Icons.Default.Add,
					contentDescription = "FloatingActionButton",
					tint = if(expanded) {
						MaterialTheme.colorScheme.onPrimary
					} else {
						MaterialTheme.colorScheme.onPrimaryContainer
					}
				)
			}
		},
	) {
		items.forEach { item ->
			FloatingActionButtonMenuItem(
				onClick = {
					item.onClick()
					expanded = false
			    },
				text = { Text(item.label) },
				icon = {
					Icon(
						imageVector = item.icon,
						contentDescription = null
					)
				}
			)
		}
	}
}


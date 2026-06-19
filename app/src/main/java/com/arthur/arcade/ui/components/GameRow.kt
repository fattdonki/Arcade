package com.arthur.arcade.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.arthur.arcade.GameApp
import com.arthur.arcade.SettingsRepository
import com.arthur.arcade.applyProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
fun GameRow(game: GameApp, showDeleteButton: Boolean, onDelete: () -> Unit) {
	val context = LocalContext.current
	val posThreshold = 0.5f
	val velThreshold = 1500f

	val rotation = remember { Animatable(0f) }
	var rowWidthPx by remember { mutableFloatStateOf(1f) }
	val offsetAnim = remember { Animatable(0f) }
	val scope = rememberCoroutineScope()
	var showSheet by remember { mutableStateOf(false) }

	val fraction = (offsetAnim.value / rowWidthPx).coerceIn(0f, 1f)
	val cornerDp = (fraction * 150f).coerceAtMost(75f)
	val density = LocalDensity.current.density

	var renameValue by remember { mutableStateOf(game.name) }
	var profile by remember { mutableStateOf(game.profile) }

	LaunchedEffect(offsetAnim.value > 0f) {
		if (offsetAnim.value > 0f) {
			rotation.snapTo(0f)
			rotation.animateTo(360f, animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing))
		}
	}

	if (showSheet) {
		ModalBottomSheet(onDismissRequest = { showSheet = false }) {
			val keyboardController = LocalSoftwareKeyboardController.current
			val focusManager = LocalFocusManager.current

			Column(
				Modifier
					.fillMaxWidth()
					.padding(24.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {

				OutlinedTextField(
					value = renameValue,
					onValueChange = { renameValue = it },
					keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
					keyboardActions = KeyboardActions(
						onDone = {
							SettingsRepository.saveName(context, game.packageName, renameValue)
							keyboardController?.hide()
							focusManager.clearFocus()
						}
					),
					colors = TextFieldDefaults.colors(
						focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
						unfocusedContainerColor = MaterialTheme.colorScheme.surface,
						focusedIndicatorColor = MaterialTheme.colorScheme.primary,
						unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
					),
					shape = RoundedCornerShape(16.dp),
				)
				Spacer(modifier = Modifier.height(8.dp))
				RadioGroup("Do Not Disturb", profile.doNotDisturbOn) {
					profile = game.profile.copy(doNotDisturbOn = it)
					game.profile = profile
					SettingsRepository.saveProfile(context, game.packageName, profile)
				}
				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Text("Block Internet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
					Switch(
						checked = profile.blockInternet,
						onCheckedChange = {
							profile = profile.copy(blockInternet = it)
							game.profile = profile
							SettingsRepository.saveProfile(context, game.packageName, profile)
						}
					)
				}
				Spacer(Modifier.height(8.dp))
			}
		}
	}

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.onSizeChanged { rowWidthPx = it.width.toFloat() }
			.draggable(
				orientation = Orientation.Horizontal,
				state = rememberDraggableState { delta ->
					scope.launch {
						offsetAnim.snapTo((offsetAnim.value + delta).coerceAtLeast(0f))
					}
				},
				onDragStopped = { velocity ->
					scope.launch {
						val shouldOpen = fraction >= posThreshold || velocity >= velThreshold

						if (shouldOpen) {
							val remaining = rowWidthPx - offsetAnim.value
							val durationToEnd = (remaining / (velocity.coerceAtLeast(800f) * 0.8f) * 1000).toInt()
								.coerceIn(80, 220)

							offsetAnim.animateTo(rowWidthPx, tween(durationToEnd, easing = LinearOutSlowInEasing))

							showSheet = true

							offsetAnim.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
						} else {
							offsetAnim.animateTo(0f, tween(180))
						}
					}
				}
			)
	) {
		Box(
			Modifier
				.matchParentSize()
				.padding(vertical = 2.dp),
			contentAlignment = Alignment.CenterStart
		) {
			Box(
				Modifier
					.fillMaxHeight()
					.fillMaxWidth(fraction.coerceAtLeast(0f))
					.clip(RoundedCornerShape(cornerDp.coerceAtLeast(4f).dp))
					.background(MaterialTheme.colorScheme.tertiaryContainer)
			)
			Icon(
				imageVector = Icons.Default.Settings,
				contentDescription = "Settings",
				modifier = Modifier
					.offset {
						IntOffset(
							((offsetAnim.value / 2f).coerceAtLeast(35f * density) - 12f * density).toInt(),
							0
						)
					}
					.rotate(rotation.value)
					.requiredSize(24.dp),
				tint = MaterialTheme.colorScheme.onTertiaryContainer
			)
		}

		Card(
			modifier = Modifier
				.fillMaxWidth()
				.offset { IntOffset(offsetAnim.value.toInt(), 0) }
				.clickable {
					applyProfile(context, game)
					context.packageManager
						.getLaunchIntentForPackage(game.packageName)
						?.let { context.startActivity(it) }
				},
			shape = RoundedCornerShape(4.dp),
			colors = CardDefaults.cardColors(
				containerColor = MaterialTheme.colorScheme.secondaryContainer,
				contentColor = MaterialTheme.colorScheme.onSecondaryContainer
			)
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Image(
					bitmap = game.icon.toBitmap().asImageBitmap(),
					contentDescription = null,
					modifier = Modifier.size(56.dp)
						.clip(RoundedCornerShape(16.dp))
				)
				Spacer(modifier = Modifier.width(16.dp))
				Text(
					text = renameValue,
					style = MaterialTheme.typography.titleMedium,
					modifier = Modifier.weight(1f)
				)
				if(showDeleteButton){
					IconButton(
						onClick = onDelete
					) {
						Icon(
							imageVector = Icons.Default.Delete,
							contentDescription = "Delete",
							tint = Color.Red,
						)
					}
				}
			}
		}
	}
}
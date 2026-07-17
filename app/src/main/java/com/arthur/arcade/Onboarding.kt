package com.arthur.arcade

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun Onboarding(modifier: Modifier = Modifier, onContinue: () -> Unit) {
	Box(Modifier.fillMaxSize()) {
		Column(
			modifier = modifier
				.fillMaxSize()
				.padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = "Welcome",
				style = MaterialTheme.typography.displayMedium,
				fontWeight = FontWeight.Black
			)

			Spacer(modifier = Modifier.height(24.dp))

			Text(
				"Arcade lets you choose what happens when you launch a game, like turning on DND, " +
						"and blocking a game's internet access. To configure this for each game, access its " +
						"settings by sliding the game card to the right. Try it here:"
			)

			Spacer(modifier = Modifier.height(24.dp))


			val posThreshold = 0.5f
			val velThreshold = 1500f
			var swipeSuccessful by remember { mutableStateOf(false) }

			val rotation = remember { Animatable(0f) }
			var rowWidthPx by remember { mutableFloatStateOf(1f) }
			val offsetAnim = remember { Animatable(0f) }
			val scope = rememberCoroutineScope()

			val fraction = (offsetAnim.value / rowWidthPx).coerceIn(0f, 1f)
			val density = LocalDensity.current.density
			val cornerDp = (rowWidthPx / 2f / density).coerceAtMost(75f)


			LaunchedEffect(offsetAnim.value > 0f) {
				if (offsetAnim.value > 0f) {
					rotation.snapTo(0f)
					rotation.animateTo(
						360f,
						animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
					)
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
								val shouldOpen =
									fraction >= posThreshold || velocity >= velThreshold

								if (shouldOpen) {
									val remaining = rowWidthPx - offsetAnim.value
									val durationToEnd =
										(remaining / (velocity.coerceAtLeast(800f) * 0.8f) * 1000).toInt()
											.coerceIn(80, 220)

									offsetAnim.animateTo(
										rowWidthPx,
										tween(durationToEnd, easing = LinearOutSlowInEasing)
									)

									swipeSuccessful = true

									offsetAnim.animateTo(
										0f,
										tween(220, easing = FastOutSlowInEasing)
									)
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
						.offset { IntOffset(offsetAnim.value.toInt(), 0) },
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
						AsyncImage(
							model = R.mipmap.ic_launcher_round,
							contentDescription = null,
							modifier = Modifier
								.size(56.dp)
								.clip(RoundedCornerShape(16.dp))
						)
						Spacer(modifier = Modifier.width(16.dp))
						Text(
							text = "Game",
							style = MaterialTheme.typography.titleMedium,
							modifier = Modifier.weight(1f)
						)
					}
				}
			}

			Spacer(Modifier.weight(1f))

			Row(modifier = Modifier.fillMaxWidth()) {
				Spacer(Modifier.weight(1f))
				Button(
					onClick = onContinue,
					enabled = swipeSuccessful
				) {
					Text("Continue")
				}
			}
		}
	}
}
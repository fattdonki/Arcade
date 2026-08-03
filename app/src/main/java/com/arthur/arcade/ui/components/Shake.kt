package com.arthur.arcade.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.shake(
	enabled: Boolean,
	intensity: Dp = 4.dp
): Modifier {
	if (!enabled) return this

	val infiniteTransition = rememberInfiniteTransition("shakeTransition")

	val rawX by infiniteTransition.animateFloat(
		initialValue = -1f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(40, easing = LinearEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "SHAKE_X"
	)

	val rawY by infiniteTransition.animateFloat(
		initialValue = -1f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(30, easing = LinearEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "SHAKE_Y"
	)

	return this.offset {
		IntOffset(
			x = (intensity * rawX).value.toInt(),
			y = (intensity * rawY).value.toInt()
		)
	}

}
package com.arthur.arcade.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.arthur.arcade.Theme

private val DarkColorScheme = darkColorScheme(
	primary = Purple80,
	secondary = PurpleGrey80,
	tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
	primary = Purple40,
	secondary = PurpleGrey40,
	tertiary = Pink40

	/* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun ArcadeTheme(
	theme: Theme,
	// Dynamic color is available on Android 12+
	dynamicColor: Boolean = true,
	content: @Composable () -> Unit
) {
	val context = LocalContext.current

	val colorScheme = if (dynamicColor) {
		when (theme) {
			Theme.Light -> dynamicLightColorScheme(context)
			Theme.Dark -> dynamicDarkColorScheme(context)
			Theme.Oled -> dynamicDarkColorScheme(context).copy(background = Color.Black)
		}
	} else {
		when (theme) {
			Theme.Light -> LightColorScheme
			Theme.Dark -> DarkColorScheme
			Theme.Oled -> darkColorScheme(background = Color.Black)
		}
	}

	MaterialTheme(
		colorScheme = colorScheme,
		typography = Typography,
		content = content
	)
}
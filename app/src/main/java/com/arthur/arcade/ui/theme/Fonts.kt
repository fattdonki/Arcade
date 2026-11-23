package com.arthur.arcade.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.arthur.arcade.R

val JacquardFontFamily: FontFamily = FontFamily(
	Font(R.font.jacquard_24, FontWeight.Normal)
)

val IcebergFontFamily: FontFamily = FontFamily(
	Font(R.font.iceberg_regular, FontWeight.Normal)
)
val GlitchFontFamily: FontFamily = FontFamily(
	Font(R.font.rubik_glitch_regular, FontWeight.Normal)
)
val RyeFontFamily: FontFamily = FontFamily(
	Font(R.font.rye_regular, FontWeight.Normal)
)

enum class TitleFont {
	Default,
	Jacquard,
	Rye,
	Iceberg,
	Glitch
}

fun TitleFont.toFontFamily(): FontFamily {
	return when (this) {
		TitleFont.Default -> FontFamily.Default
		TitleFont.Jacquard -> JacquardFontFamily
		TitleFont.Rye -> RyeFontFamily
		TitleFont.Iceberg -> IcebergFontFamily
		TitleFont.Glitch -> GlitchFontFamily
	}
}

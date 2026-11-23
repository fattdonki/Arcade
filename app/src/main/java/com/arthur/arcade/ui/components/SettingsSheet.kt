package com.arthur.arcade.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arthur.arcade.util.IconPack
import com.arthur.arcade.util.IconProvider
import com.arthur.arcade.data.SettingsRepository
import com.arthur.arcade.util.findIconPacks
import com.arthur.arcade.ui.theme.Theme
import com.arthur.arcade.ui.theme.TitleFont
import com.arthur.arcade.util.iconPackLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
	theme: Theme,
	onThemeSelected: (Theme) -> Unit,
	fontFamily: TitleFont,
	onFontSelected: (TitleFont) -> Unit,
	isEasterEggFound: Boolean,
	iconProvider: IconProvider,
	onGamesUpdated: () -> Unit,
	closeSheet: () -> Unit
) {
	val context = LocalContext.current
	var iconPack by remember { mutableStateOf(SettingsRepository.loadIconPack(context)) }

	ModalBottomSheet(onDismissRequest = closeSheet) {
		Column(
			Modifier
				.fillMaxWidth()
				.padding(24.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Text(
				"Settings",
				style = MaterialTheme.typography.headlineMedium
			)
			val icons = listOf(
				Icons.Default.WbSunny,
				Icons.Default.DarkMode,
				Icons.Default.AutoAwesome
			)

			var selected by remember {
				mutableIntStateOf(
					when (theme) {
						Theme.Light -> 0
						Theme.Dark -> 1
						Theme.Oled -> 2
					}
				)
			}

			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text(
					"Theme",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold
				)
				SingleChoiceSegmentedButtonRow(space = 4.dp) {
					icons.forEachIndexed { index, vector ->
						SegmentedButton(
							selected = selected == index,
							onClick = {
								selected = index
								when (selected) {
									0 -> onThemeSelected(Theme.Light)
									1 -> onThemeSelected(Theme.Dark)
									2 -> onThemeSelected(Theme.Oled)
								}
							},
							shape = RoundedCornerShape(50),
							border = BorderStroke(0.dp, Color.Transparent),
							icon = {},
							label = {
								Icon(
									vector,
									contentDescription = "Theme Option"
								)
							}
						)
					}
				}
			}

			if (isEasterEggFound) {
				var showFontDialog by remember { mutableStateOf(false) }

				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Text(
						"Title Font",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.SemiBold
					)
					Row(
						modifier = Modifier
							.height(36.dp)
							.width(120.dp)
							.clip(RoundedCornerShape(18.dp))
							.background(MaterialTheme.colorScheme.surfaceVariant)
							.clickable { showFontDialog = true },
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.Center
					) {
						Icon(
							imageVector = Icons.Default.UnfoldMore,
							contentDescription = "Unfold"
						)
						Text(
							fontFamily.toString(),
						)
					}
				}

				if (showFontDialog) {
					var selectedFontIndex by remember {
						mutableIntStateOf(
							when (fontFamily) {
								TitleFont.Default -> 0
								TitleFont.Jacquard -> 1
								TitleFont.Rye -> 2
								TitleFont.Iceberg -> 3
								TitleFont.Glitch -> 4
							}
						)
					}

					val fonts = TitleFont.entries.map { it.toString() }

					AlertDialog(
						onDismissRequest = { showFontDialog = false },
						icon = {
							Icon(
								imageVector = Icons.Default.FontDownload,
								contentDescription = "Font"
							)
						},
						title = {
							Text("Select Title Font", style = MaterialTheme.typography.titleMedium)
						},
						text = {
							Column {
								fonts.forEachIndexed { index, string ->
									Row(
										verticalAlignment = Alignment.CenterVertically,
										modifier = Modifier.clickable {
											selectedFontIndex = index
										}
									) {
										RadioButton(
											selected = selectedFontIndex == index,
											onClick = { selectedFontIndex = index }
										)
										Spacer(modifier = Modifier.width(8.dp))
										Text(
											string,
											style = MaterialTheme.typography.bodyMedium
										)
									}
								}
							}
						},
						confirmButton = {
							TextButton(
								onClick = {
									val newFont = TitleFont.valueOf(fonts[selectedFontIndex])
									SettingsRepository.saveFont(context, newFont)
									onFontSelected(newFont)
									showFontDialog = false
								}
							) {
								Text("Done")
							}
						},
						dismissButton = {
							TextButton(
								onClick = { showFontDialog = false }
							) {
								Text("Cancel")
							}
						}
					)
				}
			}

			var showIconDialog by remember { mutableStateOf(false) }
			val pm = context.packageManager

			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text(
					"Icon Pack",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold
				)
				Row(
					modifier = Modifier
						.height(36.dp)
						.width(120.dp)
						.clip(RoundedCornerShape(18.dp))
						.background(MaterialTheme.colorScheme.surfaceVariant)
						.clickable { showIconDialog = true },
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.Center
				) {
					Icon(
						imageVector = Icons.Default.UnfoldMore,
						contentDescription = "Unfold"
					)

					val label = remember(iconPack) { iconPackLabel(pm, iconPack) }

					Text(
						text = label,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
				}
			}

			if (showIconDialog) {
				val options = remember {
					listOf<IconPack?>(null) + findIconPacks(pm)
				}

				var selectedPackIndex by remember {
					mutableIntStateOf(
						options.indexOfFirst { it?.packageName == iconPack }.coerceAtLeast(0)
					)
				}

				AlertDialog(
					onDismissRequest = { showIconDialog = false },
					icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Icon Pack") },
					title = { Text("Select Icon Pack", style = MaterialTheme.typography.titleMedium) },
					text = {
						Column {
							options.forEachIndexed { index, pack ->
								Row(
									verticalAlignment = Alignment.CenterVertically,
									modifier = Modifier.clickable { selectedPackIndex = index }
								) {
									RadioButton(
										selected = selectedPackIndex == index,
										onClick = { selectedPackIndex = index }
									)
									Spacer(modifier = Modifier.width(8.dp))
									Text(
										pack?.label ?: "Default",
										style = MaterialTheme.typography.bodyMedium
									)
								}
							}
						}
					},
					confirmButton = {
						TextButton(
							onClick = {
								val chosen = options[selectedPackIndex]?.packageName
								SettingsRepository.saveIconPack(context, chosen)
								iconPack = chosen
								iconProvider.setPack(chosen)
								onGamesUpdated()
								showIconDialog = false
							}
						) { Text("Done") }
					},
					dismissButton = {
						TextButton(onClick = { showIconDialog = false }) { Text("Cancel") }
					}
				)
			}
		}
	}
}

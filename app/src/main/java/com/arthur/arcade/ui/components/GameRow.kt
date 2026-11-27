package com.arthur.arcade.ui.components

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.arthur.arcade.ShortcutActivity
import com.arthur.arcade.data.GameApp
import com.arthur.arcade.data.IconDetails
import com.arthur.arcade.data.SettingsRepository
import com.arthur.arcade.firewall.PermissionManager
import com.arthur.arcade.firewall.ShizukuManager
import com.arthur.arcade.firewall.ShizukuState
import com.arthur.arcade.util.IconProvider
import com.arthur.arcade.util.Position
import com.arthur.arcade.util.applyProfile
import com.arthur.arcade.util.findIconPacks
import com.arthur.arcade.util.pushGameShortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameRow(
	game: GameApp,
	iconButton: @Composable () -> Unit,
	position: Position,
	enabled: Boolean,
	onSlideStarted: () -> Unit,
	onSlideStopped: () -> Unit,
) {
	val context = LocalContext.current
	val posThreshold = 0.5f
	val velThreshold = 1500f

	val rotation = remember { Animatable(0f) }
	var rowWidthPx by remember { mutableFloatStateOf(1f) }
	val offsetAnim = remember { Animatable(0f) }

	val scope = rememberCoroutineScope()
	var showSheet by remember { mutableStateOf(false) }
	val sheetState = rememberBottomSheetState(
		initialValue = SheetValue.Hidden,
	)

	val fraction = (offsetAnim.value / rowWidthPx).coerceIn(0f, 1f)
	val density = LocalDensity.current.density
	val cornerDp = (rowWidthPx / 2f / density).coerceAtMost(75f)

	var renameValue by remember(game.name) { mutableStateOf(game.name) }
	var profile by remember(game.profile) { mutableStateOf(game.profile) }
	var gameIcon by remember(game.icon) { mutableStateOf(game.icon) }

	val notificationsAllowed by PermissionManager.notificationsEnabled.collectAsStateWithLifecycle()
	val vpnAllowed by PermissionManager.vpnGranted.collectAsStateWithLifecycle()
	val shizukuState by PermissionManager.shizukuState.collectAsStateWithLifecycle()
	val firewallAvailable =
		(notificationsAllowed && vpnAllowed) || shizukuState == ShizukuState.Ready
	val showUpgradeToShizuku = vpnAllowed && shizukuState == ShizukuState.NeedsPermission
	val dndAvailable by PermissionManager.dndEnabled.collectAsStateWithLifecycle()
	val checkPermissions = remember { mutableStateListOf<Permissions>() }

	LaunchedEffect(offsetAnim.value > 0f) {
		if (offsetAnim.value > 0f) {
			rotation.snapTo(0f)
			rotation.animateTo(
				360f,
				animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
			)
		}
	}


	if (checkPermissions.isNotEmpty()) {
		CheckPermissions(context, checkPermissions) {
			checkPermissions.clear()
		}
	}

	if (showSheet) {
		ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
			var isSearch by remember { mutableStateOf(false) }

			BackHandler(enabled = isSearch) { isSearch = false }

			val keyboardController = LocalSoftwareKeyboardController.current
			val focusManager = LocalFocusManager.current
			var textFieldValue by remember { mutableStateOf(renameValue) }
			val textFieldState = rememberTextFieldState()
			val focusRequester = remember { FocusRequester() }

			Column(
				Modifier
					.fillMaxWidth()
					.padding(24.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				SharedTransitionLayout {
					AnimatedContent(targetState = isSearch, label = "iconSearch") { searching ->
						val progress by animateFloatAsState(
							if (isSearch) 1f else 0f,
							label = "morph"
						)
						val corner = lerp(16.dp, 28.dp, progress)
						val container =
							MaterialTheme.colorScheme.surfaceVariant.copy(alpha = progress)
						val shape = RoundedCornerShape(corner)

						val morph = Modifier.sharedBounds(
							sharedContentState = rememberSharedContentState(key = "iconSearch"),
							animatedVisibilityScope = this,
							resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
							clipInOverlayDuringTransition = OverlayClip(shape)
						)

						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.spacedBy(8.dp)
						) {
							if (!searching) {
								Box(
									contentAlignment = Alignment.Center,
									modifier = Modifier
										.clickable {
											isSearch = true
										}
								) {
									Image(
										bitmap = gameIcon.toBitmap().asImageBitmap(),
										contentDescription = null,
										colorFilter = ColorFilter
											.tint(
												Color.Black.copy(alpha = 0.5f),
												blendMode = BlendMode.SrcAtop
											),
										modifier = Modifier
											.size(56.dp)
											.clip(RoundedCornerShape(16.dp))
									)

									Icon(
										imageVector = Icons.Default.Edit,
										contentDescription = "edit",
										tint = Color.White
									)
								}

								OutlinedTextField(
									value = textFieldValue,
									onValueChange = { textFieldValue = it },
									keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
									keyboardActions = KeyboardActions(
										onDone = {
											renameValue = textFieldValue
											SettingsRepository.saveName(
												context,
												game.packageName,
												renameValue
											)
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
									modifier = Modifier.onFocusChanged { focusState ->
										if (focusState.isFocused) {
											scope.launch {
												sheetState.expand()
											}
										}
									}
								)
							} else {
								Row(
									verticalAlignment = Alignment.CenterVertically,
									modifier = morph
										.fillMaxWidth()
										.height(56.dp)
										.clip(shape)
										.background(container)
								) {
									Icon(
										Icons.Default.Search,
										contentDescription = "Search",
										modifier = Modifier.padding(start = 16.dp)
									)
									TextField(
										state = textFieldState,
										lineLimits = TextFieldLineLimits.SingleLine,
										placeholder = { Text("Search icons") },
										colors = TextFieldDefaults.colors(
											focusedContainerColor = Color.Transparent,
											unfocusedContainerColor = Color.Transparent,
											focusedIndicatorColor = Color.Transparent,
											unfocusedIndicatorColor = Color.Transparent,
										),
										modifier = Modifier
											.weight(1f)
											.focusRequester(focusRequester)
									)
									IconButton(onClick = {
										textFieldState.clearText(); isSearch = false
									}) {
										Icon(
											Icons.Default.Close,
											contentDescription = "Close search"
										)
									}
								}
							}
						}
					}
				}

				Spacer(modifier = Modifier.height(8.dp))

				if (!isSearch) {
					if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
						val size = ShortcutManagerCompat.getIconMaxWidth(context)
						val bitmap = IconProvider(context.packageManager)
							.iconFor(game.packageName)
							.toBitmap(size, size)

						Row(
							horizontalArrangement = Arrangement.SpaceBetween,
							verticalAlignment = Alignment.CenterVertically,
							modifier = Modifier.fillMaxWidth()
						) {
							Text(
								"Add shortcut to homescreen",
								style = MaterialTheme.typography.titleMedium,
								fontWeight = FontWeight.SemiBold,
							)
							IconButton(
								onClick = {
									val pinShortcutInfo = ShortcutInfoCompat
										.Builder(context, game.packageName)
										.setShortLabel(game.name)
										.setIcon(IconCompat.createWithBitmap(bitmap))
										.setIntent(
											Intent(context, ShortcutActivity::class.java).apply {
												action = ShortcutActivity.ACTION_LAUNCH_GAME
												putExtra(
													ShortcutActivity.EXTRA_PACKAGE,
													game.packageName
												)
											}
										)
										.build()

									ShortcutManagerCompat.requestPinShortcut(
										context,
										pinShortcutInfo,
										null
									)
								}
							) {
								Icon(
									imageVector = Icons.AutoMirrored.Filled.AddToHomeScreen,
									contentDescription = "Add to home screen"
								)
							}
						}
					}
					Spacer(modifier = Modifier.height(8.dp))

					Box {
						val options = listOf(
							null to "Don't change",
							true to "Always turn on",
							false to "Always turn off"
						)
						Column(Modifier.fillMaxWidth()) {

							Text(
								text = "Do Not Disturb",
								style = MaterialTheme.typography.titleMedium,
								fontWeight = FontWeight.SemiBold,
								color = if (!dndAvailable) {
									MaterialTheme.colorScheme.onSurfaceVariant
								} else {
									MaterialTheme.colorScheme.onSurface
								}
							)

							Spacer(modifier = Modifier.height(4.dp))
							options.forEach { (optionValue, optionLabel) ->
								val onSelectOption = {
									profile = game.profile.copy(doNotDisturbOn = optionValue)
									game.profile = profile
									SettingsRepository.saveProfile(
										context,
										game.packageName,
										profile
									)
								}

								Row(
									modifier = Modifier
										.fillMaxWidth()
										.clickable(onClick = onSelectOption)
										.padding(vertical = 4.dp),
									verticalAlignment = Alignment.CenterVertically
								) {
									RadioButton(
										selected = profile.doNotDisturbOn == optionValue,
										onClick = onSelectOption,
										enabled = dndAvailable
									)
									Spacer(modifier = Modifier.width(8.dp))
									Text(
										text = optionLabel,
										style = MaterialTheme.typography.bodyMedium,
										color = if (!dndAvailable) {
											MaterialTheme.colorScheme.onSurfaceVariant
										} else {
											MaterialTheme.colorScheme.onSurface
										}
									)
								}

							}
						}
						if (!dndAvailable) {
							Box(
								modifier = Modifier
									.matchParentSize()
									.clickable(
										onClick = { checkPermissions.add(Permissions.DND) }
									)
							)
						}
					}
					Box {
						Row(
							modifier = Modifier
								.fillMaxWidth(),
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.SpaceBetween
						) {
							Row(
								verticalAlignment = Alignment.CenterVertically,
							) {
								Text(
									"Block Internet",
									style = MaterialTheme.typography.titleMedium,
									fontWeight = FontWeight.SemiBold,
									color = if (!firewallAvailable) {
										MaterialTheme.colorScheme.onSurfaceVariant
									} else {
										MaterialTheme.colorScheme.onSurface
									}
								)
								if (showUpgradeToShizuku) {
									IconButton(
										onClick = {
											checkPermissions.add(Permissions.UpgradeToShizuku)
										}
									) {
										Icon(
											imageVector = Icons.Default.Upgrade,
											contentDescription = "Upgrade"
										)
									}
								}

							}
							Switch(
								enabled = firewallAvailable,
								checked = profile.blockInternet,
								onCheckedChange = {
									profile = profile.copy(blockInternet = it)
									game.profile = profile
									SettingsRepository.saveProfile(
										context,
										game.packageName,
										profile
									)
									if (ShizukuManager.isShizukuRunning) {
										if (it) ShizukuManager.block(game.packageName)
										else ShizukuManager.unblock(game.packageName)
									}
								}
							)
						}
						if (!firewallAvailable) {
							Box(
								modifier = Modifier
									.matchParentSize()
									.clickable(
										onClick = {
											if (shizukuState == ShizukuState.NotRunning) {
												checkPermissions.addAll(
													listOf(
														Permissions.VPN,
														Permissions.Notifications
													)
												)
											} else {
												checkPermissions.add(Permissions.Shizuku)
											}
										}
									)
							)
						}
					}
					Spacer(Modifier.height(8.dp))
				} else {
					val pm = context.packageManager
					val iconProvider by remember { mutableStateOf(IconProvider(pm)) }
					val packs = remember { findIconPacks(pm) }


					val settingsPack = remember { SettingsRepository.loadIconPack(context) }
					var selectedPack by remember {
						mutableStateOf(packs.firstOrNull { it.packageName == settingsPack })
					}

					var iconNames by remember { mutableStateOf(emptyList<String>()) }
					var isLoading by remember { mutableStateOf(false) }

					LaunchedEffect(selectedPack) {
						iconNames = emptyList()
						val pack = selectedPack ?: return@LaunchedEffect
						isLoading = true
						try {
							iconNames = withContext(Dispatchers.IO) {
								iconProvider.setPack(pack.packageName)
								iconProvider.packIconNames()
							}
						} finally {
							isLoading = false
						}
					}

					val filteredIcons = remember(iconNames, textFieldState.text) {
						if (textFieldState.text.isBlank()) iconNames
						else iconNames.filter {
							it.contains( textFieldState.text, ignoreCase = true )
						}
					}

					LazyRow(
						horizontalArrangement = Arrangement.spacedBy(8.dp)
					) {
						items(packs) { pack ->
							FilterChip(
								selected = selectedPack == pack,
								onClick = {
									selectedPack = pack
								},
								label = { Text(pack.label) }
							)
						}
					}

					if (selectedPack == null) {
						Text("Choose an icon pack above")
					} else if (!isLoading){
						LazyVerticalGrid(
							columns = GridCells.Adaptive(72.dp),
							modifier = Modifier
								.fillMaxWidth()
								.weight(1f),
							horizontalArrangement = Arrangement.spacedBy(8.dp),
							verticalArrangement = Arrangement.spacedBy(8.dp)
						) {
							items(filteredIcons, key = { it }) { name ->
								var icon by remember(name) { mutableStateOf<Drawable?>(null) }

								LaunchedEffect(name) {
									icon = withContext(Dispatchers.IO) {
										iconProvider.packDrawable(name)
									}
								}

								Column(
									horizontalAlignment = Alignment.CenterHorizontally,
									modifier = Modifier
										.clip(RoundedCornerShape(16.dp))
										.clickable {
											isSearch = false
											SettingsRepository.saveIcon(
												context,
												game.packageName,
												IconDetails(
													name = name,
													pack = selectedPack!!.packageName
												)
											)
											gameIcon = icon!!
										}
										.padding(4.dp)
								) {
									AsyncImage(
										model = icon,
										contentDescription = name,
										modifier = Modifier
											.size(56.dp)
											.clip(RoundedCornerShape(16.dp))
									)
									Text(
										text = name.replace('_', ' '),
										style = MaterialTheme.typography.labelSmall,
										maxLines = 1,
										overflow = TextOverflow.Ellipsis
									)
								}

							}
						}
					} else {
						Box(
							Modifier
								.fillMaxWidth()
								.weight(1f),
							contentAlignment = Alignment.Center
						) {
							LoadingIndicator()
						}
					}
				}
			}
		}
	}

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.onSizeChanged { rowWidthPx = it.width.toFloat() }
			.draggable(
				enabled = enabled,
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
							val durationToEnd =
								(remaining / (velocity.coerceAtLeast(800f) * 0.8f) * 1000).toInt()
									.coerceIn(80, 220)

							offsetAnim.animateTo(
								rowWidthPx,
								tween(durationToEnd, easing = LinearOutSlowInEasing)
							)

							showSheet = true

							offsetAnim.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
						} else {
							offsetAnim.animateTo(0f, tween(180))
						}

						onSlideStopped()
					}
				},
				onDragStarted = { onSlideStarted() }
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
							((offsetAnim.value / 2f).coerceAtLeast(35f * density) - 12f * density)
								.toInt(),
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
				.clickable(
					enabled = enabled,
					onClick = {
						pushGameShortcut(
							context,
							IconProvider(context.packageManager),
							game.packageName,
							game.name
						)
						applyProfile(context, game.packageName, game.profile)
						context.packageManager
							.getLaunchIntentForPackage(game.packageName)
							?.let { context.startActivity(it) }
					}
				),
			shape = RoundedCornerShape(
				topStart = position.topCr, topEnd = position.topCr,
				bottomStart = position.bottomCr, bottomEnd = position.bottomCr
			),
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
					bitmap = gameIcon.toBitmap().asImageBitmap(),
					contentDescription = null,
					modifier = Modifier
						.size(56.dp)
						.clip(RoundedCornerShape(16.dp))
				)
				Spacer(modifier = Modifier.width(16.dp))
				Text(
					text = renameValue,
					style = MaterialTheme.typography.titleMedium,
					modifier = Modifier.weight(1f)
				)
				iconButton()
			}
		}
	}
}
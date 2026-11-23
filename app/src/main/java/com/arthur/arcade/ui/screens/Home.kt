package com.arthur.arcade.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arthur.arcade.util.IconProvider
import com.arthur.arcade.data.AppListViewModel
import com.arthur.arcade.data.GameApp
import com.arthur.arcade.data.SettingsRepository
import com.arthur.arcade.ui.components.AddAppSheet
import com.arthur.arcade.ui.components.GameRow
import com.arthur.arcade.ui.components.SettingsSheet
import com.arthur.arcade.ui.components.pointerInputIf
import com.arthur.arcade.ui.components.shake
import com.arthur.arcade.ui.theme.Theme
import com.arthur.arcade.ui.theme.TitleFont
import com.arthur.arcade.ui.theme.toFontFamily
import com.arthur.arcade.util.getGames
import com.arthur.arcade.util.getGroupEndIndex
import com.arthur.arcade.util.getInitialCollapsedHeaders
import com.arthur.arcade.util.isGameVisible
import com.arthur.arcade.util.resolveCardPosition
import com.arthur.arcade.util.startVibrating
import com.arthur.arcade.util.stopVibrating
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun Home(modifier: Modifier = Modifier, theme: Theme, onThemeSelected: (Theme) -> Unit) {
	val context = LocalContext.current

	var isEasterEggFound by remember {
		mutableStateOf(SettingsRepository.hasFoundEasterEgg(context))
	}
	var isCharging by remember { mutableStateOf(false) }
	var fontFamily by remember { mutableStateOf(SettingsRepository.loadFont(context)) }
	val iconProvider by remember { mutableStateOf(IconProvider(context.packageManager)
		.apply {
			setPack(SettingsRepository.loadIconPack(context))
		}) }

	var slidePermission: String? by remember { mutableStateOf(null) }

	var games by remember { mutableStateOf(getGames(context, iconProvider)) }
	var spacerIndices by remember { mutableStateOf(SettingsRepository.loadIndices(context)) }
	var collapsedGroupHeaders by remember(spacerIndices) {
		mutableStateOf(getInitialCollapsedHeaders(games, spacerIndices ?: emptyList()))
	}
	val isCategorised = remember(spacerIndices) { spacerIndices?.isNotEmpty() ?: false }

	var showAddAppSheet by remember { mutableStateOf(false) }
	var showSettingsSheet by remember { mutableStateOf(false) }
	var showDeleteButton by remember { mutableStateOf(false) }
	var showDraggableButton by remember { mutableStateOf(false) }

	var isAnyItemDragging by remember { mutableStateOf(false) }
	var wasDragging by remember { mutableStateOf(false) }
	var showDropZoneContent by remember { mutableStateOf(true) }
	var dropZoneIndex by remember { mutableIntStateOf(games.size) }

	val visibleGames = remember(games, spacerIndices, collapsedGroupHeaders, showDraggableButton, showDeleteButton) {
		if (showDraggableButton || showDeleteButton) games
		else games.filterIndexed { index, _ ->
			isGameVisible(index, games, spacerIndices ?: emptyList(), collapsedGroupHeaders)
		}
	}

	val renderList: List<GameApp?> = remember(games, dropZoneIndex, showDraggableButton, collapsedGroupHeaders, showDeleteButton) {
		if (!showDraggableButton) visibleGames
		else visibleGames.toMutableList<GameApp?>()
			.apply { add(dropZoneIndex.coerceIn(0, size), null) }
	}

	LaunchedEffect(isAnyItemDragging) {
		if (wasDragging && !isAnyItemDragging) {

			if (dropZoneIndex != games.size) {

				val boundary = (dropZoneIndex - 1).coerceAtLeast(0)
				spacerIndices = ((spacerIndices ?: emptyList()) + boundary).distinct().sorted()

				showDropZoneContent = false
				delay(300.milliseconds)
				dropZoneIndex = games.size
				showDropZoneContent = true
			}
		}
		wasDragging = isAnyItemDragging
	}

	val lazyListState = rememberLazyListState()
	val reorderableLazyListState = rememberReorderableLazyListState(
		lazyListState
	) { from, to ->

		if (from.index == to.index) return@rememberReorderableLazyListState

		val working = games.toMutableList<GameApp?>().apply {
			add(dropZoneIndex.coerceIn(0, size), null)
		}

		working.add(to.index, working.removeAt(from.index))

		games = working.filterNotNull()
		dropZoneIndex = working.indexOf(null)

		spacerIndices = (spacerIndices ?: emptyList()).map { spacerIdx ->
			when {
				from.index <= spacerIdx && to.index > spacerIdx -> spacerIdx - 1
				from.index > spacerIdx && to.index <= spacerIdx -> spacerIdx + 1
				else -> spacerIdx
			}
		}.distinct().filter { it in 0 until games.lastIndex }.sorted()
	}

	BackHandler(showDeleteButton) { showDeleteButton = false }
	BackHandler(showDraggableButton) { showDraggableButton = false }

	Box(Modifier.fillMaxSize()) {
		Column(
			modifier = modifier
				.fillMaxSize()
				.padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = "Arcade",
				style = MaterialTheme.typography.displayMedium,
				fontWeight = FontWeight.Black,
				fontFamily = fontFamily.toFontFamily(),
				modifier = Modifier
					.shake(isCharging)
					.pointerInputIf(!isEasterEggFound) {
						awaitEachGesture {
							awaitFirstDown(requireUnconsumed = false)

							isCharging = true
							startVibrating(context)

							var completedHold = false

							withTimeoutOrNull(3000L) {
								waitForUpOrCancellation()

								completedHold = false
							} ?: run {
								completedHold = true
							}

							isCharging = false
							stopVibrating(context)

							if (completedHold) {
								isEasterEggFound = true
								SettingsRepository.setHasFoundEasterEgg(context)
								fontFamily = TitleFont.Jacquard
								SettingsRepository.saveFont(context, TitleFont.Jacquard)
							}
						}
					}
			)

			Spacer(modifier = Modifier.height(24.dp))

			Box(modifier = Modifier.clip(RoundedCornerShape(24.dp))) {
				LazyColumn(
					verticalArrangement = Arrangement.spacedBy(4.dp),
					state = lazyListState
				) {
					itemsIndexed(
						items = renderList,
						key = { _, g -> g?.packageName ?: "BOTTOM_BOX" }
					) { _, gameOrNull ->
						ReorderableItem(
							reorderableLazyListState,
							key = gameOrNull?.packageName ?: "BOTTOM_BOX"
						) { isDragging ->
							Column(modifier = Modifier.animateItem()) {
								if (gameOrNull == null) {
									Spacer(Modifier.height(4.dp))
									AnimatedVisibility(
										visible = showDropZoneContent,
										enter = fadeIn(tween(200)),
										exit = fadeOut(tween(150))
									) {
										Box(
											modifier = Modifier
												.fillMaxWidth()
												.height(80.dp)
												.clip(RoundedCornerShape(24.dp))
												.background(MaterialTheme.colorScheme.tertiaryContainer)
												.padding(horizontal = 24.dp, vertical = 12.dp),
										) {
											Row(
												Modifier.fillMaxSize(),
												Arrangement.SpaceBetween,
												Alignment.CenterVertically
											) {
												Text(
													"Drop game below to create a new category",
													color = MaterialTheme.colorScheme.onTertiaryContainer,
													maxLines = 2,
													overflow = TextOverflow.Ellipsis,
													modifier = Modifier
														.weight(1f, fill = true)
												)
												Icon(
													imageVector = Icons.Default.ArrowDownward,
													contentDescription = "Downward Arrow",
													tint = MaterialTheme.colorScheme.onTertiaryContainer,
												)
											}
										}

									}
								} else {
									val rawGameIndex = games.indexOf(gameOrNull)
									val isGroupHeader = rawGameIndex == 0 || (spacerIndices?.contains(rawGameIndex - 1) == true)
									val isGroupExpanded = !collapsedGroupHeaders.contains(gameOrNull.packageName)
									val groupEndIndex = getGroupEndIndex(rawGameIndex, games.size, spacerIndices ?: emptyList())

									val isCollapsibleGroup = isGroupHeader && (groupEndIndex > rawGameIndex)

									GameRow(
										game = gameOrNull
											.resolveName(
												SettingsRepository.loadName(
													context,
													gameOrNull.packageName
												)
											)
											.resolveProfile(
												SettingsRepository.loadProfile(
													context,
													gameOrNull.packageName
												)
											),

										iconButton = @Composable {
											when {
												showDraggableButton -> {
													IconButton(
														modifier = Modifier.draggableHandle(
															onDragStarted = {
																isAnyItemDragging = true
															},
															onDragStopped = {
																isAnyItemDragging = false
															}
														),
														onClick = {},
													) {
														Icon(
															Icons.Default.DragIndicator,
															contentDescription = "Reorder"
														)
													}
												}
												showDeleteButton -> {
													IconButton(
														onClick = {
															SettingsRepository.removeGame(
																context,
																gameOrNull.packageName
															)
															games = getGames(context, iconProvider)
															spacerIndices = (spacerIndices ?: emptyList())
																.map {
																	if (it >= rawGameIndex) it - 1
																	else it
																}
																.distinct().filter { it in 0 until games.lastIndex }.sorted()
														},
													) {
														Icon(
															imageVector = Icons.Default.Delete,
															contentDescription = "Delete",
															tint = MaterialTheme.colorScheme.error,
														)
													}
												}
												isGroupHeader && isCollapsibleGroup && isCategorised -> {
													IconButton(
														onClick = {
															collapsedGroupHeaders = if (isGroupExpanded) {
																collapsedGroupHeaders + gameOrNull.packageName
															} else {
																collapsedGroupHeaders - gameOrNull.packageName
															}
														}
													) {
														Icon(
															imageVector = if (isGroupExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
															contentDescription = if (isGroupExpanded) "Collapse Group" else "Expand Group"
														)
													}
												}
											}
										},

										position = resolveCardPosition(
											rawGameIndex,
											games.size,
											spacerIndices ?: listOf(),
											isDragging,
											isGroupHeader,
											!isGroupExpanded && !showDeleteButton && !showDraggableButton
										),

										enabled = !showDeleteButton && !showDraggableButton &&
												(slidePermission == null || slidePermission == gameOrNull.packageName),


										onSlideStarted = {
											slidePermission = gameOrNull.packageName
										},

										onSlideStopped = {
											slidePermission = null
										}
									)

									if (
										spacerIndices?.contains(rawGameIndex) == true
										|| (isGroupHeader && !isGroupExpanded && !showDeleteButton && !showDraggableButton)
									) {
										Spacer(modifier = Modifier.height(16.dp))
									}
								}
							}
						}
					}
					item {
						Spacer(Modifier.height(88.dp))
					}
				}
			}
		}

		Box(
			modifier = Modifier
				.align(Alignment.BottomEnd)
				.navigationBarsPadding()
				.padding(16.dp)
		) {
			var expanded by remember { mutableStateOf(false) }

			BackHandler(expanded) { expanded = false }

			FloatingActionButtonMenu(
				expanded = expanded,
				button = {
					ToggleFloatingActionButton(
						checked = expanded,
						onCheckedChange = { checked ->
							if (showDeleteButton) {
								showDeleteButton = false
								SettingsRepository.setCustomOrder(
									context,
									games.map { it.packageName })
								SettingsRepository.setIndices(context, spacerIndices ?: emptyList())

							} else if (showDraggableButton) {
								showDraggableButton = false
								SettingsRepository.setCustomOrder(
									context,
									games.map { it.packageName })
								SettingsRepository.setIndices(context, spacerIndices ?: emptyList())
							} else {
								expanded = checked
							}
						}
					) {
						Icon(
							imageVector = when {
								showDeleteButton -> Icons.Default.Done
								showDraggableButton -> Icons.Default.Done
								expanded -> Icons.Default.Close
								else -> Icons.Default.Add
							},
							contentDescription = if (showDeleteButton) "Done" else "Menu Toggle",
							tint = if (expanded && !showDeleteButton) {
								MaterialTheme.colorScheme.onPrimary
							} else {
								MaterialTheme.colorScheme.onPrimaryContainer
							}
						)
					}
				}
			) {
				FloatingActionButtonMenuItem(
					onClick = {
						showSettingsSheet = true
						expanded = false
					},
					text = { Text("Settings") },
					icon = {
						Icon(
							Icons.Default.Settings,
							contentDescription = "Settings"
						)
					}
				)
				FloatingActionButtonMenuItem(
					onClick = {
						showDraggableButton = true
						expanded = false
					},
					text = { Text("Organise Games") },
					icon = {
						Icon(
							Icons.Default.DragIndicator,
							contentDescription = "Drag Indicator"
						)
					}
				)
				FloatingActionButtonMenuItem(
					onClick = {
						showDeleteButton = true
						expanded = false
					},
					text = { Text("Remove Game") },
					icon = {
						Icon(
							Icons.Default.Delete,
							contentDescription = "Delete"
						)
					}
				)
				FloatingActionButtonMenuItem(
					onClick = {
						showAddAppSheet = true
						expanded = false
					},
					text = { Text("Add Game") },
					icon = {
						Icon(
							Icons.Default.Add,
							contentDescription = "Add"
						)
					}
				)
			}
		}
	}

	if (showAddAppSheet) {
		val viewModel: AppListViewModel = viewModel()

		LaunchedEffect(showAddAppSheet) {
			if (showAddAppSheet) {
				viewModel.loadInstalledApps()
			}
		}

		AddAppSheet(
			viewModel,
			closeSheet = { showAddAppSheet = false },
			updateGames = { games = getGames(context, iconProvider) }
		)
	} else if (showSettingsSheet) {
		SettingsSheet(
			theme = theme,
			onThemeSelected = onThemeSelected,
			fontFamily = fontFamily,
			onFontSelected = { fontFamily = it },
			isEasterEggFound = isEasterEggFound,
			iconProvider = iconProvider,
			onGamesUpdated = { games = getGames(context, iconProvider) },
			closeSheet = { showSettingsSheet = false }
		)
	}
}
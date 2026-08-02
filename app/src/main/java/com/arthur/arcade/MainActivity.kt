package com.arthur.arcade

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.arthur.arcade.ui.components.CheckPermissions
import com.arthur.arcade.ui.components.GameRow
import com.arthur.arcade.ui.theme.ArcadeTheme
import com.arthur.arcade.vpn.VpnHandler
import com.arthur.arcade.vpn.VpnHandlerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity(), VpnHandler by VpnHandlerImpl() {
	val vpnPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { }

	val notificationPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { }

	val dndPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		initVpnHandler(this)
		setContent {
			ArcadeTheme {
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
					val context = LocalContext.current

					var showOnboarding by remember {
						mutableStateOf(!SettingsRepository.hasSeenOnboarding(context))
					}

					if (showOnboarding) {
						Onboarding(
							Modifier.padding(innerPadding),
							onContinue = {
								SettingsRepository.setHasSeenOnboarding(context)
								showOnboarding = false
							}
						)
					} else {
						CheckPermissions(context)

						Home(Modifier.padding(innerPadding))
					}
				}
			}
		}
	}
}

data class GameProfile(
	var doNotDisturbOn: Boolean? = null,
	var blockInternet: Boolean = false,
)

data class GameApp(
	var name: String,
	val packageName: String,
	val icon: Drawable,
	var profile: GameProfile
) {
	fun resolveName(customName: String?): GameApp {
		return this.copy(name = customName ?: name)
	}

	fun resolveProfile(profile: GameProfile): GameApp {
		return this.copy(profile = profile)
	}
}

data class App(
	val name: String,
	var isChecked: Boolean = false,
	val info: ApplicationInfo
)

class AppListViewModel(application: Application) : AndroidViewModel(application) {
	var appsState = mutableStateOf<List<App>>(emptyList())
		private set

	var isLoading = mutableStateOf(false)
		private set

	fun loadInstalledApps() {
		if (appsState.value.isNotEmpty()) return

		viewModelScope.launch(Dispatchers.IO) {
			isLoading.value = true

			val context = getApplication<Application>()
			val intent = Intent(Intent.ACTION_MAIN).apply {
				addCategory(Intent.CATEGORY_LAUNCHER)
			}

			val flags = PackageManager.ResolveInfoFlags
				.of(PackageManager.GET_META_DATA.toLong())
			val resolveInfos = context.packageManager
				.queryIntentActivities(intent, flags)

			val appList = resolveInfos.map { info ->
				App(
					name = info.loadLabel(context.packageManager).toString(),
					info = info.activityInfo.applicationInfo,
				)
			}
				.sortedBy { it.name.lowercase() }
				.filter { app ->
					!isAGame(app.info, context)
							&& app.info.packageName != "com.arthur.arcade"
				}

			withContext(Dispatchers.Main) {
				appsState.value = appList
				isLoading.value = false
			}
		}
	}

	fun toggleAppChecked(packageName: String) {
		appsState.value = appsState.value.map {
			if (it.info.packageName == packageName) it.copy(isChecked = !it.isChecked) else it
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(modifier: Modifier = Modifier) {
	val context = LocalContext.current

	var slidePermission: String? by remember { mutableStateOf(null) }

	var games by remember { mutableStateOf(getGames(context)) }
	var spacerIndices by remember { mutableStateOf(SettingsRepository.loadIndices(context)) }
	var collapsedGroupHeaders by remember(spacerIndices) {
		mutableStateOf(getInitialCollapsedHeaders(games, spacerIndices ?: emptyList()))
	}
	val isCategorised = remember(spacerIndices) { spacerIndices?.isNotEmpty() ?: false }

	var showAddAppSheet by remember { mutableStateOf(false) }
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
				fontWeight = FontWeight.Black
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
															games = getGames(context)
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

										slideEnabled = !showDeleteButton && !showDraggableButton &&
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

					if (showDeleteButton or showDraggableButton) {
						item {
							Spacer(Modifier.height(88.dp))
						}
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
				expanded = expanded && !showDeleteButton,
				button = {
					ToggleFloatingActionButton(
						checked = if (showDeleteButton) false else expanded,
						onCheckedChange = { checked ->
							if (showDeleteButton) {
								showDeleteButton = false
							} else if (showDraggableButton) {
								showDraggableButton = false
								SettingsRepository.setCustomOrder(
									context,
									games.map { it.packageName })
								SettingsRepository.setIndices(context, spacerIndices ?: listOf())
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
				if (!showDeleteButton) {
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
	}

	if (showAddAppSheet) {
		val viewModel: AppListViewModel = viewModel()
		val apps by viewModel.appsState
		val isLoading by viewModel.isLoading

		LaunchedEffect(showAddAppSheet) {
			if (showAddAppSheet) {
				viewModel.loadInstalledApps()
			}
		}

		ModalBottomSheet(onDismissRequest = { showAddAppSheet = false }) {
			if (isLoading) {
				Box(
					Modifier
						.fillMaxWidth()
						.height(200.dp), contentAlignment = Alignment.Center
				) {
					CircularProgressIndicator()
				}
			} else {
				Box(
					Modifier
						.fillMaxSize()
				) {
					LazyColumn(
						Modifier
							.fillMaxWidth()
							.padding(24.dp),
						verticalArrangement = Arrangement.spacedBy(8.dp)
					) {
						item {
							Text(
								"Add app",
								style = MaterialTheme.typography.headlineMedium
							)
						}
						items(apps, key = { it.info.packageName }) { app ->
							Row(
								modifier = Modifier.fillMaxWidth(),
								verticalAlignment = Alignment.CenterVertically,
								horizontalArrangement = Arrangement.spacedBy(4.dp)
							) {
								var appIcon by remember(app.info.packageName) {
									mutableStateOf<Drawable?>(null)
								}

								LaunchedEffect(app.info.packageName) {
									withContext(Dispatchers.IO) {
										try {
											appIcon = context.packageManager
												.getApplicationIcon(app.info.packageName)
										} catch (_: Exception) {
										}
									}
								}

								AsyncImage(
									model = appIcon,
									contentDescription = "App Icon",
									modifier = Modifier
										.size(40.dp)
										.clip(RoundedCornerShape(16.dp))
								)

								Text(app.name)

								Spacer(Modifier.weight(1f))

								Checkbox(
									checked = app.isChecked,
									onCheckedChange = { viewModel.toggleAppChecked(app.info.packageName) }
								)
							}
						}
						item {
							Spacer(Modifier.height(88.dp))
						}
					}
					Box(
						modifier = Modifier
							.align(Alignment.BottomEnd)
							.padding(16.dp)
					) {
						FloatingActionButtonMenu(
							expanded = false,
							button = {
								ToggleFloatingActionButton(
									checked = false,
									onCheckedChange = {
										viewModel.appsState.value
											.filter { it.isChecked }
											.map { it.info.packageName }
											.forEach {
												SettingsRepository.addNonGameApps(
													context,
													it
												)
											}
										games = getGames(context)
										showAddAppSheet = false
									}
								) {
									Icon(
										imageVector = Icons.Default.Done,
										contentDescription = if (showDeleteButton) "Done" else "Menu Toggle",
										tint = MaterialTheme.colorScheme.onPrimaryContainer
									)
								}
							}
						) {}
					}
				}
			}
		}
	}
}

enum class Position(val topCr: Dp, val bottomCr: Dp) {
	Top(24.dp, 4.dp),
	Middle(4.dp, 4.dp),
	Bottom(4.dp, 24.dp),
	Floating(24.dp, 24.dp)
}


fun resolveCardPosition(
	gameIndex: Int,
	totalGames: Int,
	spacerIndices: List<Int>,
	isDragging: Boolean,
	isGroupHeader: Boolean,
	isGroupCollapsed: Boolean,
): Position {
	if (isDragging) return Position.Floating
	if (isGroupHeader && isGroupCollapsed) return Position.Floating

	val isFirstInList = gameIndex == 0
	val isLastInList = gameIndex == totalGames - 1

	val isBucketTop = isFirstInList || spacerIndices.contains(gameIndex - 1)

	val isBucketBottom = isLastInList || spacerIndices.contains(gameIndex)

	return when {
		isBucketTop && isBucketBottom -> Position.Floating
		isBucketTop -> Position.Top
		isBucketBottom -> Position.Bottom
		else -> Position.Middle
	}
}

fun isGameVisible(
	gameIndex: Int,
	games: List<GameApp>,
	spacerIndices: List<Int>,
	collapsedHeaders: Set<String>
): Boolean {
	val lastSpacerBefore = spacerIndices.filter { it < gameIndex }.maxOrNull()
	val groupHeaderIndex = if (lastSpacerBefore != null) lastSpacerBefore + 1 else 0

	if (gameIndex == groupHeaderIndex) return true

	val headerGame = games.getOrNull(groupHeaderIndex) ?: return true
	return !collapsedHeaders.contains(headerGame.packageName)
}

fun getGroupEndIndex(headerIndex: Int, totalGames: Int, spacerIndices: List<Int>): Int {
	val nextSpacer = spacerIndices.filter { it >= headerIndex }.minOrNull()
	return nextSpacer ?: (totalGames - 1)
}

fun getInitialCollapsedHeaders(
	games: List<GameApp>,
	spacerIndices: List<Int>
): Set<String> {
	if (spacerIndices.isEmpty() || games.isEmpty()) return emptySet()

	val headers = mutableSetOf<String>()

	games.firstOrNull()?.let { headers.add(it.packageName) }

	spacerIndices.forEach { spacerIndex ->
		val headerIndex = spacerIndex + 1
		games.getOrNull(headerIndex)?.let { game ->
			headers.add(game.packageName)
		}
	}

	return headers
}

fun getGames(context: Context): List<GameApp> {
	val packageManager = context.packageManager
	val order = SettingsRepository.loadCustomOrder(context)

	val intent = Intent(Intent.ACTION_MAIN).apply {
		addCategory(Intent.CATEGORY_LAUNCHER)
	}

	val flags = PackageManager.ResolveInfoFlags
		.of(PackageManager.GET_META_DATA.toLong())

	val resolveInfos = packageManager.queryIntentActivities(intent, flags)

	return resolveInfos.mapNotNull { info ->
		val appInfo = info.activityInfo.applicationInfo

		if (isAGame(appInfo, context)) {
			GameApp(
				name = info.loadLabel(packageManager).toString(),
				packageName = appInfo.packageName,
				icon = info.loadIcon(packageManager),
				profile = GameProfile()
			)
		} else null
	}.sortedWith { a, b ->
		if (order.isNullOrEmpty()) {
			a.name.compareTo(b.name)
		} else {
			val indexA = order.indexOf(a.packageName).let {
				if (it != -1) it else Int.MAX_VALUE
			}

			val indexB = order.indexOf(b.packageName).let {
				if (it != -1) it else Int.MAX_VALUE
			}

			indexA.compareTo(indexB)
		}
	}
}

fun isAGame(appInfo: ApplicationInfo, context: Context): Boolean {
	val nonGameApps = SettingsRepository.loadNonGameApps(context)
	val removedGames = SettingsRepository.loadRemovedGames(context)

	return if (removedGames.contains(appInfo.packageName)) {
		false
	} else if (nonGameApps.contains(appInfo.packageName)) {
		true
	} else if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
		true
	} else {
		false
	}
}

object SettingsRepository {
	private const val FILE_NAME = "settings.json"

	private fun loadFile(context: Context): JSONObject {
		val file = File(context.filesDir, FILE_NAME)
		if (!file.exists()) return JSONObject()
		return try {
			JSONObject(file.readText())
		} catch (_: Exception) {
			JSONObject()
		}
	}

	private fun saveFile(context: Context, json: JSONObject) {
		File(context.filesDir, FILE_NAME).writeText(json.toString())
	}

	fun loadName(context: Context, packageName: String): String? {
		val entry = loadFile(context).optJSONObject(packageName) ?: return null
		return if (entry.has("name")) entry.getString("name") else null
	}

	fun loadProfile(context: Context, packageName: String): GameProfile {
		val entry = loadFile(context).optJSONObject(packageName) ?: return GameProfile()
		return GameProfile(
			doNotDisturbOn = if (entry.has("doNotDisturbOn")) entry.getBoolean("doNotDisturbOn") else null,
			blockInternet = entry.optBoolean("blockInternet", false),
		)
	}

	fun saveName(context: Context, packageName: String, name: String) {
		val root = loadFile(context)
		val entry = root.optJSONObject(packageName) ?: JSONObject()
		entry.put("name", name)
		root.put(packageName, entry)
		saveFile(context, root)
	}

	fun saveProfile(context: Context, packageName: String, profile: GameProfile) {
		val root = loadFile(context)
		val entry = root.optJSONObject(packageName) ?: JSONObject()
		if (profile.doNotDisturbOn != null) {
			entry.put("doNotDisturbOn", profile.doNotDisturbOn)
		} else {
			entry.remove("doNotDisturbOn")
		}
		entry.put("blockInternet", profile.blockInternet)
		root.put(packageName, entry)
		saveFile(context, root)
	}

	fun loadNonGameApps(context: Context): List<String> {
		val jsonArray = loadFile(context).optJSONArray("nonGameApps") ?: JSONArray()

		return (0 until jsonArray.length()).map { index ->
			jsonArray.getString(index)
		}
	}

	fun addNonGameApps(context: Context, packageName: String) {
		val root = loadFile(context)
		val removedGames = root.optJSONArray("removedGames") ?: JSONArray()
		val addedGames = root.optJSONArray("nonGameApps") ?: JSONArray()

		val targetIndex = removedGames.indexOfString(packageName)

		if (targetIndex != -1) {
			removedGames.remove(targetIndex)
		} else {
			addedGames.put(packageName)
		}

		root.put("removedGames", removedGames)
		root.put("nonGameApps", addedGames)
		saveFile(context, root)
	}

	fun loadRemovedGames(context: Context): List<String> {
		val jsonArray = loadFile(context).optJSONArray("removedGames") ?: JSONArray()

		return (0 until jsonArray.length()).map { index ->
			jsonArray.getString(index)
		}
	}

	fun removeGame(context: Context, packageName: String) {
		val root = loadFile(context)
		val removedGames = root.optJSONArray("removedGames") ?: JSONArray()
		val addedGames = root.optJSONArray("nonGameApps") ?: JSONArray()

		val targetIndex = addedGames.indexOfString(packageName)
		if (targetIndex != -1) {
			addedGames.remove(targetIndex)
		} else {
			if (removedGames.indexOfString(packageName) == -1) {
				removedGames.put(packageName)
			}
		}

		root.put("removedGames", removedGames)
		root.put("nonGameApps", addedGames)
		saveFile(context, root)
	}

	fun hasSeenOnboarding(context: Context): Boolean {
		return loadFile(context).optBoolean("hasSeenOnboarding", false)
	}

	fun setHasSeenOnboarding(context: Context) {
		val root = loadFile(context)
		root.put("hasSeenOnboarding", true)
		saveFile(context, root)
	}

	fun loadCustomOrder(context: Context): List<String>? {
		val jsonArray = loadFile(context).optJSONArray("customOrder") ?: return null

		return (0 until jsonArray.length()).map { index ->
			jsonArray.getString(index)
		}
	}

	fun setCustomOrder(context: Context, order: List<String>) {
		val root: JSONObject = loadFile(context)

		root.put("customOrder", JSONArray(order))
		saveFile(context, root)
	}

	fun loadIndices(context: Context): List<Int>? {
		val jsonArray = loadFile(context).optJSONArray("spacerIndices") ?: return null

		return (0 until jsonArray.length()).map { index ->
			jsonArray.getInt(index)
		}
	}

	fun setIndices(context: Context, indices: List<Int>) {
		val root: JSONObject = loadFile(context)

		root.put("spacerIndices", JSONArray(indices))
		saveFile(context, root)
	}

	private fun JSONArray.indexOfString(target: String): Int {
		for (i in 0 until this.length()) {
			if (this.optString(i) == target) {
				return i
			}
		}
		return -1
	}
}

fun applyProfile(context: Context, game: GameApp) {
	val notificationManager =
		context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

	game.profile.doNotDisturbOn?.let { turnOn ->
		if (notificationManager.isNotificationPolicyAccessGranted) {
			val filter = if (turnOn) NotificationManager.INTERRUPTION_FILTER_ALARMS
			else NotificationManager.INTERRUPTION_FILTER_ALL
			notificationManager.setInterruptionFilter(filter)
		}
	}

	if (game.profile.blockInternet) {
		if (context is VpnHandler) {
			context.checkAndStartVpn(game.packageName)
		}
	}
}
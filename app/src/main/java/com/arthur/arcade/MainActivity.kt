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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.arthur.arcade.ui.components.CheckPermissions
import com.arthur.arcade.ui.components.FabMenu
import com.arthur.arcade.ui.components.FabMenuItem
import com.arthur.arcade.ui.components.GameRow
import com.arthur.arcade.ui.theme.ArcadeTheme
import com.arthur.arcade.vpn.VpnHandler
import com.arthur.arcade.vpn.VpnHandlerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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
					CheckPermissions(LocalContext.current)

					Home(
						modifier = Modifier.padding(innerPadding)
					)
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

			val appList = resolveInfos.map{ info ->
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
@Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
fun Home(modifier: Modifier = Modifier) {
	val context = LocalContext.current
	var games by remember { mutableStateOf(getGames(context)) }

	var showAddAppSheet by remember { mutableStateOf(false) }
	var showDeleteButtons by remember { mutableStateOf(false) }

	BackHandler(showDeleteButtons) { showDeleteButtons = false }

	Box(Modifier.fillMaxSize()){
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
				Column(
					verticalArrangement = Arrangement.spacedBy(12.dp)
				){
					LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
						games.forEach { game ->
							item {
								GameRow(
									game
										.resolveName(
											SettingsRepository.loadName(
												context,
												game.packageName
											)
										)
										.resolveProfile(
											SettingsRepository.loadProfile(
												context,
												game.packageName
											)
										),
									showDeleteButtons,
								) {
									SettingsRepository.removeGame(context, game.packageName)
									games = getGames(context)
								}
							}
						}
						if (showDeleteButtons) {
							item {
								Button(
									modifier = Modifier.fillMaxWidth(),
									content = { Text("Done") },
									onClick = { showDeleteButtons = false }
								)
							}
						}
					}
				}
			}

		}

		if (!showDeleteButtons){
			FabMenu(
				listOf(
					FabMenuItem(
						"Remove Game",
						Icons.Default.Delete
					) { showDeleteButtons = true },
					FabMenuItem(
						"Add Game",
						Icons.Default.Add
					) { showAddAppSheet = true },
				)
			)
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

		ModalBottomSheet(onDismissRequest = { showAddAppSheet = false} ) {
			if (isLoading) {
				Box(Modifier
					.fillMaxWidth()
					.height(200.dp), contentAlignment = Alignment.Center) {
					CircularProgressIndicator()
				}
			} else {
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
									} catch (_: Exception) {}
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
						Button(
							modifier = Modifier.fillMaxWidth(),
							content = { Text("Save") },
							onClick = {
								viewModel.appsState.value
									.filter { it.isChecked }
									.map { it.info.packageName }
									.forEach { SettingsRepository.addNonGameApps(context, it) }
								games = getGames(context)
								showAddAppSheet = false
							}
						)
					}
				}
			}
		}
	}
}

fun getGames(context: Context): List<GameApp> {
	val packageManager = context.packageManager

	val intent = Intent(Intent.ACTION_MAIN).apply {
		addCategory(Intent.CATEGORY_LAUNCHER)
	}

	val flags = PackageManager.ResolveInfoFlags
		.of(PackageManager.GET_META_DATA.toLong())

	val resolveInfos = packageManager.queryIntentActivities(intent, flags)

	return resolveInfos.mapNotNull { info ->
		val appInfo = info.activityInfo.applicationInfo

		if (isAGame(appInfo, context)) {
			GameApp (
				name = info.loadLabel(packageManager).toString(),
				packageName = appInfo.packageName,
				icon = info.loadIcon(packageManager),
				profile = GameProfile()
			)
		} else null
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
		return try { JSONObject(file.readText()) } catch (_: Exception) { JSONObject() }
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
	val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
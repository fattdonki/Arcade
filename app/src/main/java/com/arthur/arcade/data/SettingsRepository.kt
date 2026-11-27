package com.arthur.arcade.data

import android.content.Context
import com.arthur.arcade.ui.theme.Theme
import com.arthur.arcade.ui.theme.TitleFont
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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

	fun loadIcon(context: Context, packageName: String): IconDetails? {
		val entry = loadFile(context).optJSONObject(packageName) ?: return null
		val name = entry.optString("iconName").ifEmpty { return null }
		val pack = entry.optString("iconPack").ifEmpty { loadIconPack(context) ?: return null }
		return IconDetails(pack = pack, name = name)
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

	fun saveIcon(context: Context, packageName: String, iconDetails: IconDetails) {
		val root = loadFile(context)
		val entry = root.optJSONObject(packageName) ?: JSONObject()

		entry.put("iconName", iconDetails.name)
		if (iconDetails.pack == loadIconPack(context)) {
			entry.remove("iconPack")
		} else {
			entry.put("iconPack", iconDetails.pack)
		}

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

	fun hasFoundEasterEgg(context: Context): Boolean {
		return loadFile(context).optBoolean("hasFoundEasterEgg", false)
	}

	fun setHasFoundEasterEgg(context: Context) {
		val root = loadFile(context)
		root.put("hasFoundEasterEgg", true)
		saveFile(context, root)
	}

	fun loadFont(context: Context): TitleFont {
		val root = loadFile(context)
		return TitleFont.valueOf(root.optString("titleFont", "Default"))
	}

	fun saveFont(context: Context, font: TitleFont) {
		val root = loadFile(context)
		root.put("titleFont", font)
		saveFile(context, root)
	}

	fun loadIconPack(context: Context): String? {
		val root = loadFile(context)
		return if (root.has("iconPack")) root.optString("iconPack").ifEmpty { null } else null
	}

	fun saveIconPack(context: Context, iconPackPackage: String?) {
		val root = loadFile(context)
		if (iconPackPackage == null) {
			root.remove("iconPack")
		} else {
			root.put("iconPack", iconPackPackage)
		}
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

	fun loadAllBlockedPackages(context: Context): List<String> {
		val root = loadFile(context)
		return root.keys().asSequence()
			.filter { it.contains(".") }
			.filter { root.optJSONObject(it)?.optBoolean("blockInternet", false) == true }
			.toList()
	}

	fun loadTheme(context: Context, system: Theme): Theme {
		val root = loadFile(context)
		return runCatching {
			Theme.valueOf(root.optString("theme"))
		}.getOrDefault(system)
	}

	fun saveTheme(context: Context, theme: Theme) {
		val root = loadFile(context)
		root.put("theme", theme)
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
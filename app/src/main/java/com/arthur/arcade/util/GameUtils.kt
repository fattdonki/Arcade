package com.arthur.arcade.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.arthur.arcade.ShortcutActivity
import com.arthur.arcade.data.GameApp
import com.arthur.arcade.data.GameProfile
import com.arthur.arcade.data.SettingsRepository
import com.arthur.arcade.firewall.PermissionManager
import com.arthur.arcade.firewall.ShizukuState
import com.arthur.arcade.firewall.VpnHandler
import kotlin.jvm.java

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

fun getGames(context: Context, iconProvider: IconProvider): List<GameApp> {
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
		val iconOverload = SettingsRepository.loadIcon(context, appInfo.packageName)
		val name = SettingsRepository.loadName(context, appInfo.packageName)
			?: info.loadLabel(packageManager).toString()
		val profile = SettingsRepository.loadProfile(context, appInfo.packageName)

		val icon = if (iconOverload != null) {
			IconProvider(packageManager).apply {
				setPack(iconOverload.pack)
			}
				.packDrawable(iconOverload.name)
				?: iconProvider.iconFor(appInfo.packageName)
		} else {
			iconProvider.iconFor(appInfo.packageName)
		}

		if (isAGame(appInfo, context)) {
			GameApp(
				name,
				packageName = appInfo.packageName,
				icon,
				profile = profile
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

	return !removedGames.contains(appInfo.packageName)
			&& (nonGameApps.contains(appInfo.packageName)
			|| appInfo.category == ApplicationInfo.CATEGORY_GAME)
}

fun iconPackLabel(pm: PackageManager, pack: String?): String {
	if (pack == null) return "System"
	return try {
		pm.getApplicationLabel(pm.getApplicationInfo(pack, 0)).toString()
	} catch (_: PackageManager.NameNotFoundException) {
		"System"
	}
}

fun applyProfile(context: Context, packageName: String, profile: GameProfile) {
	val notificationManager =
		context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

	profile.doNotDisturbOn?.let { turnOn ->
		if (notificationManager.isNotificationPolicyAccessGranted) {
			val filter = if (turnOn) NotificationManager.INTERRUPTION_FILTER_ALARMS
			else NotificationManager.INTERRUPTION_FILTER_ALL
			notificationManager.setInterruptionFilter(filter)
		}
	}

	if (PermissionManager.shizukuState.value != ShizukuState.Ready) {
		if (profile.blockInternet) {
			if (context is VpnHandler) {
				context.checkAndStartVpn(packageName)
			}
		}
	}
}

fun pushGameShortcut(
	context: Context,
	iconProvider: IconProvider,
	packageName: String,
	label: String
) {
	val intent = Intent(context, ShortcutActivity::class.java).apply {
		action = ShortcutActivity.ACTION_LAUNCH_GAME
		putExtra(ShortcutActivity.EXTRA_PACKAGE, packageName)
	}

	val size = ShortcutManagerCompat.getIconMaxWidth(context)
	val bitmap = iconProvider.iconFor(packageName).toBitmap(size, size)

	val shortcut = ShortcutInfoCompat.Builder(context, "game_$packageName")
		.setShortLabel(label)
		.setIcon(IconCompat.createWithBitmap(bitmap))
		.setIntent(intent)
		.build()

	ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
}
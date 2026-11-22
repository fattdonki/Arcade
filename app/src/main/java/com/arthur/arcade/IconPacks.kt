package com.arthur.arcade

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class IconPack(val packageName: String, val label: String)

private val PACK_ACTIONS = listOf(
	"org.adw.launcher.THEMES",
	"com.novalauncher.THEME",
	"com.anddoes.launcher.THEME",
	"com.gau.go.launcherex.theme",
)

fun findIconPacks(pm: PackageManager): List<IconPack> {
	return PACK_ACTIONS
		.asSequence()
		.flatMap { action ->
			pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA)
		}
		.map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
		.distinctBy { it.first }
		.map { IconPack(it.first, it.second) }
		.sortedBy { it.label.lowercase() }
		.toList()
}

fun loadAppFilter(pm: PackageManager, pack: String): Map<String, String> {
	val res = pm.getResourcesForApplication(pack)
	val xmlId = res.getIdentifier("appfilter", "xml", pack)
	if (xmlId != 0) {
		return res.getXml(xmlId).use { parseItems(it) }
	}

	val parser = XmlPullParserFactory.newInstance().newPullParser()
	return res.assets.open("appfilter.xml").use { stream ->
		parser.setInput(stream, null)
		parseItems(parser)
	}
}

fun parseItems(parser: XmlPullParser): Map<String, String> {
	val map = HashMap<String, String>()
	var event = parser.eventType
	while (event != XmlPullParser.END_DOCUMENT) {
		if (event == XmlPullParser.START_TAG && parser.name == "item") {
			val component = parser.getAttributeValue(null, "component")
			val drawable = parser.getAttributeValue(null, "drawable")
			if (component != null && drawable != null) map[component] = drawable
		}
		event = parser.next()
	}
	return map
}

class IconProvider(private val pm: PackageManager) {
	private var packName: String? = null
	private var mapping: Map<String, String> = emptyMap()
	private var packRes: Resources? = null

	fun setPack(pack: String?) {
		packName = pack
		if (pack == null ) {
			mapping = emptyMap()
			packRes = null
			return
		}
		mapping = loadAppFilter(pm, pack)
		packRes = pm.getResourcesForApplication(pack)
	}

	fun iconFor(appPackage: String): Drawable {
		fromPack(appPackage)?.let { return it }
		return pm.getApplicationIcon(appPackage)
	}
	private fun fromPack(appPackage: String): Drawable? {
		val pack = packName ?: return null
		val res  = packRes ?: return null
		val component = pm.getLaunchIntentForPackage(appPackage)?.component ?: return null
		val key = "ComponentInfo{${component.packageName}/${component.className}}"
		val drawableName = mapping[key] ?: return null
		val id = res.getIdentifier(drawableName, "drawable", pack)
		if (id == 0) return null
		return ResourcesCompat.getDrawable(res, id, null)
	}
}


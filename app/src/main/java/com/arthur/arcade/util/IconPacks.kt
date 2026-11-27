package com.arthur.arcade.util

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException

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

@SuppressLint("DiscouragedApi")
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

	private val resIdMap = HashMap<String, Int>()
	private val iconCache = android.util.LruCache<String, Drawable>(300)

	private var cachedNames: List<String>? = null


	fun setPack(pack: String?) {
		if (packName == pack) return
		packName = pack
		cachedNames = null
		resIdMap.clear()
		iconCache.evictAll()
		if (pack.isNullOrEmpty()) {
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

	@SuppressLint("DiscouragedApi")
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

	private fun collectDrawableNames(parser: XmlPullParser, out: MutableSet<String>) {
		var event = parser.eventType
		while (event != XmlPullParser.END_DOCUMENT) {
			if (event == XmlPullParser.START_TAG && parser.name == "item") {
				parser.getAttributeValue(null, "drawable")?.let { out.add(it) }
			}
			event = parser.next()
		}
	}

	@SuppressLint("DiscouragedApi")
	private fun readDrawableNames(res: Resources, pack: String): Set<String> {
		val names = LinkedHashSet<String>()
		val xmlId = res.getIdentifier("drawable", "xml", pack)
		if (xmlId != 0) {
			val parser = res.getXml(xmlId)
			parser.use { parser ->
				collectDrawableNames(parser, names)
			}
		} else {
			try {
				res.assets.open("drawable.xml").use { stream ->
					val parser = XmlPullParserFactory.newInstance().newPullParser()
					parser.setInput(stream, null)
					collectDrawableNames(parser, names)
				}
			} catch (_: IOException) { }
		}
		return names
	}

	fun packIconNames(): List<String> {
		val pack = packName ?: return emptyList()
		if (cachedNames != null) return cachedNames!!

		val res = packRes ?: return emptyList()
		val names = readDrawableNames(res, pack).sorted()
		cachedNames = names

		return names
	}

	@SuppressLint("DiscouragedApi")
	fun packDrawable(name: String): Drawable? {
		val pack = packName ?: return null
		val res = packRes ?: return null

		iconCache.get(name)?.let {return it}

		val id = resIdMap[name] ?: res.getIdentifier(name, "drawable", pack).also {
			if (it != 0 ) resIdMap[name] = it
		}
		if (id <= 0) return null

		val drawable = ResourcesCompat.getDrawable(res, id, null)
		if (drawable != null) {
			iconCache.put(name, drawable)
		}
		return drawable
	}
}


package com.arthur.arcade.data

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arthur.arcade.util.isAGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
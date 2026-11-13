package com.vitek.tvlauncher

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vitek.tvlauncher.data.TvApp
import com.vitek.tvlauncher.data.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val pm = application.packageManager
    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)

    private val _apps = MutableStateFlow<List<TvApp>>(emptyList())
    val apps: StateFlow<List<TvApp>> = _apps.asStateFlow()

    private val _pins = MutableStateFlow(loadPins())
    val pins: StateFlow<List<String>> = _pins.asStateFlow()

    init {
        refresh()
    }

    /** (Re)loads the list of launchable apps installed on the device. */
    fun refresh() {
        viewModelScope.launch {
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            val ownPackage = getApplication<Application>().packageName

            val infos = (pm.queryIntentActivities(launcher, 0) +
                pm.queryIntentActivities(leanback, 0))
                .distinctBy { it.activityInfo.packageName }
                .filter { it.activityInfo.packageName != ownPackage }

            _apps.value = infos.mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                val label = info.loadLabel(pm).toString()
                val icon = try {
                    info.loadIcon(pm).toBitmap()
                } catch (e: Exception) {
                    null
                } ?: return@mapNotNull null
                TvApp(packageName = pkg, label = label, icon = icon)
            }.sortedBy { it.label.lowercase() }
        }
    }

    fun isPinned(packageName: String): Boolean = _pins.value.contains(packageName)

    /** Pins/unpins an app in the Top Shelf row. Order is preserved. */
    fun togglePin(packageName: String) {
        val current = _pins.value.toMutableList()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _pins.value = current
        prefs.edit().putString(KEY_PINS, current.joinToString(SEPARATOR)).apply()
    }

    private fun loadPins(): List<String> =
        prefs.getString(KEY_PINS, "")
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    private companion object {
        const val PREFS_NAME = "tv_launcher"
        const val KEY_PINS = "pins"
        const val SEPARATOR = "|"
    }
}

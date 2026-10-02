package com.middes.launcher

import android.content.Context

class LauncherStore(context: Context) {
    private val prefs = context.getSharedPreferences("middes", Context.MODE_PRIVATE)

    var scene: String
        get() = prefs.getString(KEY_SCENE, "Normal") ?: "Normal"
        set(value) { prefs.edit().putString(KEY_SCENE, value).apply() }

    var dimWallpaper: Boolean
        get() = prefs.getBoolean(KEY_DIM, true)
        set(value) { prefs.edit().putBoolean(KEY_DIM, value).apply() }

    var wallpaperUri: String?
        get() = prefs.getString(KEY_WALLPAPER, null)
        set(value) { prefs.edit().apply {
            if (value == null) remove(KEY_WALLPAPER) else putString(KEY_WALLPAPER, value)
        }.apply() }

    var nexaEnabled: Boolean
        get() = prefs.getBoolean(KEY_NEXA, false)
        set(value) { prefs.edit().putBoolean(KEY_NEXA, value).apply() }

    fun sceneApps(scene: String, defaults: List<String>): List<String> {
        val key = sceneAppsKey(scene)
        if (!prefs.contains(key)) return defaults
        return prefs.getStringSet(key, emptySet()).orEmpty().toList()
    }

    fun setSceneApps(scene: String, packages: Collection<String>) {
        prefs.edit().putStringSet(sceneAppsKey(scene), packages.toSet()).apply()
    }

    fun restoreSceneApps(scene: String) {
        prefs.edit().remove(sceneAppsKey(scene)).apply()
    }

    fun favorites(): List<String> =
        prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toList()

    fun setFavorites(packages: Collection<String>) {
        prefs.edit().putStringSet(KEY_FAVORITES, packages.distinct().toSet()).apply()
    }

    fun toggleFavorite(packageName: String): Boolean {
        val current = favorites().toMutableSet()
        val added = current.add(packageName)
        if (!added) current.remove(packageName)
        setFavorites(current)
        return added
    }

    fun recentApps(): List<String> =
        prefs.getString(KEY_RECENTS, "").orEmpty().split("|").filter { it.isNotBlank() }

    fun recordLaunch(packageName: String) {
        val updated = listOf(packageName) + recentApps().filterNot { it == packageName }
        prefs.edit().putString(KEY_RECENTS, updated.take(12).joinToString("|")).apply()
    }

    private fun sceneAppsKey(scene: String): String =
        "scene_apps_" + scene.lowercase().replace("ã", "a").replace("ç", "c").replace("é", "e")

    private companion object {
        const val KEY_SCENE = "scene"
        const val KEY_DIM = "dim"
        const val KEY_WALLPAPER = "wallpaper"
        const val KEY_NEXA = "nexa_enabled"
        const val KEY_FAVORITES = "favorites"
        const val KEY_RECENTS = "recent_apps"
    }
}
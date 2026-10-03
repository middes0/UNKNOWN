package com.middes.launcher

import android.content.Context

class LauncherStore(context: Context) {
    private val prefs = context.getSharedPreferences("middes", Context.MODE_PRIVATE)

    data class SceneProfile(
        val focusMinutes: Int,
        val brightnessPercent: Int,
        val mediaVolumePercent: Int,
        val dndEnabled: Boolean,
        val dimWallpaper: Boolean,
        val autoLaunch: Boolean,
        val autoLaunchPackage: String?
    )

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

    fun sceneProfile(scene: String): SceneProfile {
        val defaults = defaultSceneProfile(scene)
        return SceneProfile(
            focusMinutes = prefs.getInt(profileKey(scene, "focus"), defaults.focusMinutes).coerceIn(5, 180),
            brightnessPercent = prefs.getInt(profileKey(scene, "brightness"), defaults.brightnessPercent).coerceIn(0, 100),
            mediaVolumePercent = prefs.getInt(profileKey(scene, "volume"), defaults.mediaVolumePercent).coerceIn(-1, 100),
            dndEnabled = prefs.getBoolean(profileKey(scene, "dnd"), defaults.dndEnabled),
            dimWallpaper = prefs.getBoolean(profileKey(scene, "dim"), defaults.dimWallpaper),
            autoLaunch = prefs.getBoolean(profileKey(scene, "autolaunch"), defaults.autoLaunch),
            autoLaunchPackage = prefs.getString(profileKey(scene, "launchpkg"), defaults.autoLaunchPackage)
        )
    }

    fun setSceneProfile(scene: String, profile: SceneProfile) {
        prefs.edit()
            .putInt(profileKey(scene, "focus"), profile.focusMinutes.coerceIn(5, 180))
            .putInt(profileKey(scene, "brightness"), profile.brightnessPercent.coerceIn(0, 100))
            .putInt(profileKey(scene, "volume"), profile.mediaVolumePercent.coerceIn(-1, 100))
            .putBoolean(profileKey(scene, "dnd"), profile.dndEnabled)
            .putBoolean(profileKey(scene, "dim"), profile.dimWallpaper)
            .putBoolean(profileKey(scene, "autolaunch"), profile.autoLaunch)
            .putString(profileKey(scene, "launchpkg"), profile.autoLaunchPackage)
            .apply()
    }

    fun restoreSceneProfile(scene: String) {
        prefs.edit()
            .remove(profileKey(scene, "focus"))
            .remove(profileKey(scene, "brightness"))
            .remove(profileKey(scene, "volume"))
            .remove(profileKey(scene, "dnd"))
            .remove(profileKey(scene, "dim"))
            .remove(profileKey(scene, "autolaunch"))
            .remove(profileKey(scene, "launchpkg"))
            .apply()
    }

    fun sceneWallpaperUri(scene: String): String? =
        prefs.getString(sceneWallpaperKey(scene), null)

    fun setSceneWallpaperUri(scene: String, uri: String?) {
        prefs.edit().apply {
            if (uri == null) remove(sceneWallpaperKey(scene))
            else putString(sceneWallpaperKey(scene), uri)
        }.apply()
    }

    fun effectiveWallpaperUri(scene: String): String? =
        sceneWallpaperUri(scene) ?: wallpaperUri

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

    private fun profileKey(scene: String, field: String): String =
        "scene_profile_" + sceneKey(scene) + "_" + field

    private fun sceneWallpaperKey(scene: String): String =
        "scene_wallpaper_" + sceneKey(scene)

    private fun sceneKey(scene: String): String =
        scene.lowercase()
            .replace("ã", "a")
            .replace("ç", "c")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")

    private fun defaultSceneProfile(scene: String): SceneProfile {
        val defaultDim = dimWallpaper
        return when (scene) {
            "Noite" -> SceneProfile(25, 18, -1, true, defaultDim, false, null)
            "Estudo" -> SceneProfile(25, 0, -1, true, defaultDim, false, null)
            "Gaming" -> SceneProfile(25, 0, -1, true, false, false, null)
            "Música" -> SceneProfile(25, 0, -1, false, false, false, null)
            else -> SceneProfile(25, 0, -1, false, defaultDim, false, null)
        }
    }

    private companion object {
        const val KEY_SCENE = "scene"
        const val KEY_DIM = "dim"
        const val KEY_WALLPAPER = "wallpaper"
        const val KEY_NEXA = "nexa_enabled"
        const val KEY_FAVORITES = "favorites"
        const val KEY_RECENTS = "recent_apps"
    }
}
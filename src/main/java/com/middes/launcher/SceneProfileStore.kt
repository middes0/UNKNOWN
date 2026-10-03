package com.middes.launcher

import android.content.Context

data class SceneProfile(
    val focusMinutes: Int = 25,
    val brightnessPercent: Int = -1,
    val volumePercent: Int = -1,
    val dndEnabled: Boolean = false,
    val autoStartFocus: Boolean = false,
    val autoLaunchPackage: String? = null,
    val preferredPlayerPackage: String? = null,
    val wallpaperUri: String? = null
) {
    companion object {
        fun defaults(scene: String): SceneProfile = SceneProfile(
            focusMinutes = 25,
            brightnessPercent = if (scene == "Noite") 18 else -1,
            volumePercent = -1,
            dndEnabled = scene == "Estudo" || scene == "Noite" || scene == "Gaming",
            autoStartFocus = false
        )
    }
}

class SceneProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("middes_scene_profiles", Context.MODE_PRIVATE)

    fun profile(scene: String): SceneProfile {
        val defaults = SceneProfile.defaults(scene)
        val prefix = keyPrefix(scene)
        return SceneProfile(
            focusMinutes = prefs.getInt(prefix + "focus_minutes", defaults.focusMinutes).coerceIn(1, 240),
            brightnessPercent = prefs.getInt(prefix + "brightness", defaults.brightnessPercent).let {
                if (it == -1) -1 else it.coerceIn(0, 100)
            },
            volumePercent = prefs.getInt(prefix + "volume", defaults.volumePercent).let {
                if (it == -1) -1 else it.coerceIn(0, 100)
            },
            dndEnabled = prefs.getBoolean(prefix + "dnd", defaults.dndEnabled),
            autoStartFocus = prefs.getBoolean(prefix + "auto_focus", defaults.autoStartFocus),
            autoLaunchPackage = prefs.getString(prefix + "auto_launch", null),
            preferredPlayerPackage = prefs.getString(prefix + "player", null),
            wallpaperUri = prefs.getString(prefix + "wallpaper", null)
        )
    }

    fun save(scene: String, profile: SceneProfile) {
        val prefix = keyPrefix(scene)
        prefs.edit()
            .putInt(prefix + "focus_minutes", profile.focusMinutes.coerceIn(1, 240))
            .putInt(prefix + "brightness", profile.brightnessPercent.coerceIn(-1, 100).let {
                if (it == -1) -1 else it
            })
            .putInt(prefix + "volume", profile.volumePercent.coerceIn(-1, 100).let {
                if (it == -1) -1 else it
            })
            .putBoolean(prefix + "dnd", profile.dndEnabled)
            .putBoolean(prefix + "auto_focus", profile.autoStartFocus)
            .apply {
                if (profile.autoLaunchPackage.isNullOrBlank()) remove(prefix + "auto_launch")
                else putString(prefix + "auto_launch", profile.autoLaunchPackage)
                if (profile.preferredPlayerPackage.isNullOrBlank()) remove(prefix + "player")
                else putString(prefix + "player", profile.preferredPlayerPackage)
                if (profile.wallpaperUri.isNullOrBlank()) remove(prefix + "wallpaper")
                else putString(prefix + "wallpaper", profile.wallpaperUri)
            }
            .apply()
    }

    fun reset(scene: String) {
        val prefix = keyPrefix(scene)
        prefs.edit()
            .remove(prefix + "focus_minutes")
            .remove(prefix + "brightness")
            .remove(prefix + "volume")
            .remove(prefix + "dnd")
            .remove(prefix + "auto_focus")
            .remove(prefix + "auto_launch")
            .remove(prefix + "player")
            .remove(prefix + "wallpaper")
            .apply()
    }

    fun setWallpaper(scene: String, uri: String?) {
        val prefix = keyPrefix(scene)
        prefs.edit().apply {
            if (uri.isNullOrBlank()) remove(prefix + "wallpaper")
            else putString(prefix + "wallpaper", uri)
        }.apply()
    }

    fun resetWallpaper(scene: String) {
        setWallpaper(scene, null)
    }

    private fun keyPrefix(scene: String): String =
        "scene_" + scene.lowercase()
            .replace("ã", "a")
            .replace("ç", "c")
            .replace("é", "e")
            .replace(" ", "_") + "_"
}

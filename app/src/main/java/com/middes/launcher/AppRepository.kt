package com.middes.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import java.text.Normalizer
import java.util.Locale

data class InstalledApp(
    val packageName: String,
    val label: String,
    val resolveInfo: ResolveInfo
)

class AppRepository(private val context: Context) {
    private val pm = context.packageManager

    fun getLaunchableApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), it) }
            .distinctBy { it.packageName }
            .sortedBy { normalize(it.label) }
            .toList()
    }

    fun findByPackage(packageName: String): InstalledApp? =
        getLaunchableApps().firstOrNull { it.packageName == packageName }

    fun findBySpokenName(spokenName: String): InstalledApp? {
        val wanted = normalize(spokenName)
            .removePrefix("abrir ")
            .removePrefix("abra ")
            .removePrefix("abre ")
            .removePrefix("iniciar ")
            .removePrefix("inicie ")
            .removePrefix("inicia ")
            .removePrefix("aplicativo ")
            .removePrefix("app ")
            .trim()
        if (wanted.isBlank()) return null

        return getLaunchableApps()
            .filter {
                val label = normalize(it.label)
                label == wanted || label.contains(wanted) || wanted.contains(label)
            }
            .sortedBy { if (normalize(it.label) == wanted) 0 else 1 }
            .firstOrNull()
    }

    fun firstInstalled(candidates: List<String>): List<String> =
        candidates.filter { packageName ->
            try { pm.getLaunchIntentForPackage(packageName) != null } catch (_: Exception) { false }
        }

    fun icon(packageName: String) =
        try { pm.getApplicationIcon(packageName) } catch (_: Exception) { null }

    fun label(packageName: String): String? =
        try { pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() }
        catch (_: Exception) { null }

    fun launch(packageName: String): Boolean =
        try {
            val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }

    fun isSystemOrLaunchable(app: InstalledApp): Boolean =
        try {
            val info = pm.getApplicationInfo(app.packageName, 0)
            (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                pm.getLaunchIntentForPackage(app.packageName) != null
        } catch (_: Exception) {
            false
        }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
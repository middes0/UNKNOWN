package com.middes.launcher

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.BatteryManager

/**
 * Snapshot pequeno e seguro do estado que a NEXA consegue conhecer dentro do launcher.
 *
 * A classe não tenta adivinhar qual app está em primeiro plano. "Último app"
 * significa o último aplicativo que o MIDDES abriu e registrou.
 */
class NexaSystemContext(private val context: Context) {

    data class Snapshot(
        val scene: String,
        val batteryPercent: Int,
        val dndStatus: String,
        val studyRunning: Boolean,
        val studyRemainingSeconds: Int,
        val nexaState: NexaState,
        val screen: String,
        val recentApps: List<String>,
        val wallpaperPersonalized: Boolean,
        val volumePercent: Int
    )

    fun capture(
        scene: String,
        studyRunning: Boolean,
        studyRemainingSeconds: Int,
        nexaState: NexaState,
        screen: String,
        recentApps: List<String>,
        wallpaperPersonalized: Boolean
    ): Snapshot {
        val battery = try {
            (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                .coerceIn(0, 100)
        } catch (_: Exception) {
            -1
        }

        val dnd = try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            when {
                !manager.isNotificationPolicyAccessGranted -> "Acesso necessário"
                manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE ->
                    "Não perturbe ativo"
                else -> "Disponível"
            }
        } catch (_: Exception) {
            "Indisponível"
        }

        val volume = try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) ((current * 100f) / max).toInt().coerceIn(0, 100) else -1
        } catch (_: Exception) {
            -1
        }

        return Snapshot(
            scene = scene,
            batteryPercent = battery,
            dndStatus = dnd,
            studyRunning = studyRunning,
            studyRemainingSeconds = studyRemainingSeconds,
            nexaState = nexaState,
            screen = screen,
            recentApps = recentApps.take(5),
            wallpaperPersonalized = wallpaperPersonalized,
            volumePercent = volume
        )
    }

    fun describe(snapshot: Snapshot): String {
        val parts = mutableListOf<String>()
        parts += "Modo " + snapshot.scene.lowercase() + "."
        if (snapshot.batteryPercent >= 0) {
            parts += "Bateria em " + snapshot.batteryPercent + "%."
        }

        if (snapshot.scene == "Estudo") {
            val time = formatSeconds(snapshot.studyRemainingSeconds)
            parts += if (snapshot.studyRunning) {
                "Foco ativo, restam " + time + "."
            } else {
                "Foco pausado, restam " + time + "."
            }
        }

        parts += when (snapshot.dndStatus) {
            "Não perturbe ativo", "Alertas silenciados" -> "Proteção contra interrupções ativa."
            "Acesso necessário" -> "O acesso a Não perturbe ainda não foi autorizado."
            else -> "Proteção contra interrupções disponível."
        }

        if (snapshot.volumePercent >= 0) {
            parts += "Volume de mídia em " + snapshot.volumePercent + "%."
        }

        return parts.joinToString(" ")
    }

    fun describeRecentApps(snapshot: Snapshot, labels: (String) -> String?): String {
        if (snapshot.recentApps.isEmpty()) {
            return "Ainda não há aplicativos recentes registrados pelo MIDDES."
        }

        val names = snapshot.recentApps.mapNotNull { packageName ->
            labels(packageName)?.takeIf { it.isNotBlank() }
        }.distinct().take(5)

        return if (names.isEmpty()) {
            "Ainda não consegui identificar os aplicativos recentes."
        } else {
            "Os aplicativos mais recentes registrados pelo MIDDES são: " + names.joinToString(", ") + "."
        }
    }

    fun describeLastApp(snapshot: Snapshot, labels: (String) -> String?): String {
        val packageName = snapshot.recentApps.firstOrNull()
            ?: return "Nenhum aplicativo foi aberto pelo MIDDES ainda."

        val label = labels(packageName).orEmpty().ifBlank { packageName }
        return "O último aplicativo lançado pelo MIDDES foi " + label + "."
    }

    fun describeMode(snapshot: Snapshot): String = when (snapshot.scene) {
        "Estudo" -> if (snapshot.studyRunning) "O modo estudo está ativo com foco em andamento." else "O modo estudo está ativo, mas o foco está pausado."
        "Música" -> "O modo música está ativo."
        "Noite" -> "O modo noite está ativo."
        "Gaming" -> "O modo gaming está ativo."
        else -> "O modo normal está ativo."
    }

    private fun formatSeconds(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val minutes = safe / 60
        val seconds = safe % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
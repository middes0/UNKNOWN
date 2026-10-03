package com.middes.launcher

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import kotlin.math.roundToInt

/**
 * Centraliza os efeitos reais dos protocolos de cena.
 *
 * O protocolo não apaga notificações. Ele controla as interrupções do Android
 * (Não perturbe) enquanto a cena está ativa e restaura o estado anterior ao sair.
 */
class SceneProtocolController(
    private val context: Context,
    private val window: Window
) {
    enum class Result {
        APPLIED,
        MISSING_DND_ACCESS,
        FAILED
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var protocolActive = false
    private var savedBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    private var savedInterruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL
    private var changedDnd = false
    private var savedMediaVolume = 0

    fun applyScene(scene: String, profile: LauncherStore.SceneProfile): Result {
        return try {
            if (scene == "Normal") {
                restore()
                return Result.APPLIED
            }

            if (!protocolActive) {
                savedBrightness = window.attributes.screenBrightness
                savedInterruptionFilter = notificationManager.currentInterruptionFilter
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                savedMediaVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                protocolActive = true
                changedDnd = false
            }

            val dndResult = applyDnd(profile.dndEnabled)
            applyBrightness(profile.brightnessPercent)
            applyMediaVolume(profile.mediaVolumePercent)

            if (profile.dndEnabled && !dndResult) Result.MISSING_DND_ACCESS else Result.APPLIED
        } catch (_: Exception) {
            Result.FAILED
        }
    }

    fun hasDndAccess(): Boolean =
        try {
            notificationManager.isNotificationPolicyAccessGranted
        } catch (_: Exception) {
            false
        }

    fun dndDescription(): String = when {
        !hasDndAccess() -> "Acesso necessário"
        changedDnd -> "Alertas silenciados"
        notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL ->
            "Não perturbe ativo"
        else -> "Disponível"
    }

    fun openDndSettings() {
        try {
            context.startActivity(IntentFactory.notificationPolicyAccess())
        } catch (_: Exception) {
            try {
                context.startActivity(android.content.Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
                // Sem ação adicional: o sistema não disponibilizou a tela.
            }
        }
    }

    fun restore() {
        if (!protocolActive) return

        try {
            if (changedDnd && hasDndAccess()) {
                notificationManager.setInterruptionFilter(savedInterruptionFilter)
            }
        } catch (_: Exception) {
            // Não interrompe a troca de cena se o sistema não aceitar a restauração.
        }

        try {
            val params = window.attributes
            params.screenBrightness = savedBrightness
            window.attributes = params
        } catch (_: Exception) {
            // Mantém o comportamento normal do Android.
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(
                android.media.AudioManager.STREAM_MUSIC,
                savedMediaVolume.coerceIn(0, max),
                0
            )
        } catch (_: Exception) {
            // Mantém o volume atual se o Android não aceitar a restauração.
        }

        changedDnd = false
        savedMediaVolume = 0
        protocolActive = false
    }

    private fun applyDnd(enabled: Boolean): Boolean {
        if (!enabled) {
            try {
                if (changedDnd && hasDndAccess()) {
                    notificationManager.setInterruptionFilter(savedInterruptionFilter)
                    changedDnd = false
                }
                return true
            } catch (_: Exception) {
                return false
            }
        }

        if (!hasDndAccess()) return false

        return try {
            if (notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_NONE) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                changedDnd = true
            }
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun applyBrightness(percent: Int) {
        try {
            val params = window.attributes
            params.screenBrightness =
                if (percent in 1..100) percent / 100f else savedBrightness
            window.attributes = params
        } catch (_: Exception) {
            // O restante do protocolo continua mesmo sem controle de brilho.
        }
    }

    private fun applyMediaVolume(percent: Int) {
        if (percent == -1) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(
                    android.media.AudioManager.STREAM_MUSIC,
                    savedMediaVolume.coerceIn(0, max),
                    0
                )
            } catch (_: Exception) {}
            return
        }
        if (percent !in 0..100) return
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val target = ((max * percent) / 100f).roundToInt().coerceIn(0, max)
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, target, 0)
        } catch (_: Exception) {
            // O restante do protocolo continua mesmo sem controle de volume.
        }
    }

    private object IntentFactory {
        fun notificationPolicyAccess(): android.content.Intent =
            android.content.Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }
}

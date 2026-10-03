package com.middes.launcher

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import android.view.Window
import android.view.WindowManager

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
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

    private var protocolActive = false
    private var savedBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    private var savedInterruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL
    private var savedMusicVolume = -1
    private var changedDnd = false
    private var changedVolume = false

    fun applyScene(scene: String, profile: SceneProfile = SceneProfile.defaults(scene)): Result {
        return try {
            val requiresProtocol =
                profile.dndEnabled ||
                    profile.brightnessPercent in 0..100 ||
                    profile.volumePercent in 0..100

            if (!requiresProtocol) {
                restore()
                return Result.APPLIED
            }

            if (!protocolActive) {
                savedBrightness = window.attributes.screenBrightness
                savedInterruptionFilter = notificationManager.currentInterruptionFilter
                savedMusicVolume = try {
                    audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                } catch (_: Exception) {
                    -1
                }
                protocolActive = true
                changedDnd = false
                changedVolume = false
            }

            val dndResult = if (profile.dndEnabled) {
                enableDnd()
            } else {
                restoreDndIfChanged()
                true
            }

            applyBrightness(scene, profile)
            applyVolume(profile)

            if (!dndResult) Result.MISSING_DND_ACCESS else Result.APPLIED
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

        restoreDndIfChanged()

        try {
            val params = window.attributes
            params.screenBrightness = savedBrightness
            window.attributes = params
        } catch (_: Exception) {
            // Mantém o comportamento normal do Android.
        }

        if (changedVolume && savedMusicVolume >= 0) {
            try {
                audioManager.setStreamVolume(
                    android.media.AudioManager.STREAM_MUSIC,
                    savedMusicVolume,
                    0
                )
            } catch (_: Exception) {
                // O sistema pode bloquear a restauração do volume.
            }
        }

        changedDnd = false
        changedVolume = false
        savedMusicVolume = -1
        protocolActive = false
    }

    private fun enableDnd(): Boolean {
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

    private fun restoreDndIfChanged() {
        try {
            if (changedDnd && hasDndAccess()) {
                notificationManager.setInterruptionFilter(savedInterruptionFilter)
            }
        } catch (_: Exception) {
            // O restante do protocolo continua mesmo sem restaurar notificações.
        }
        changedDnd = false
    }

    private fun applyBrightness(scene: String, profile: SceneProfile) {
        try {
            val params = window.attributes
            params.screenBrightness =
                if (profile.brightnessPercent in 0..100) {
                    profile.brightnessPercent / 100f
                } else if (scene == "Noite") {
                    0.18f
                } else {
                    savedBrightness
                }
            window.attributes = params
        } catch (_: Exception) {
            // O restante do protocolo continua mesmo sem controle de brilho.
        }
    }

    private fun applyVolume(profile: SceneProfile) {
        if (profile.volumePercent !in 0..100) {
            if (changedVolume && savedMusicVolume >= 0) {
                try {
                    audioManager.setStreamVolume(
                        android.media.AudioManager.STREAM_MUSIC,
                        savedMusicVolume,
                        0
                    )
                } catch (_: Exception) {}
            }
            changedVolume = false
            return
        }

        try {
            val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            if (max <= 0 || savedMusicVolume < 0) return
            val target = kotlin.math.round((profile.volumePercent / 100f) * max).toInt().coerceIn(0, max)
            val current = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            if (current != target) {
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, target, 0)
                changedVolume = true
            }
        } catch (_: Exception) {
            // O sistema pode bloquear o controle de volume.
        }
    }

    private object IntentFactory {
        fun notificationPolicyAccess(): android.content.Intent =
            android.content.Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }
}

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

    private var protocolActive = false
    private var savedBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    private var savedInterruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL
    private var changedDnd = false

    fun applyScene(scene: String): Result {
        return try {
            val usesDnd = scene == "Estudo" || scene == "Noite" || scene == "Gaming"

            if (!usesDnd) {
                restore()
                protocolActive = false
                return Result.APPLIED
            }

            if (!protocolActive) {
                savedBrightness = window.attributes.screenBrightness
                savedInterruptionFilter = notificationManager.currentInterruptionFilter
                protocolActive = true
                changedDnd = false
            }

            val dndResult = enableDnd()
            applyBrightness(scene)

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

        changedDnd = false
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

    private fun applyBrightness(scene: String) {
        try {
            val params = window.attributes
            params.screenBrightness =
                if (scene == "Noite") 0.18f else savedBrightness
            window.attributes = params
        } catch (_: Exception) {
            // O restante do protocolo continua mesmo sem controle de brilho.
        }
    }

    private object IntentFactory {
        fun notificationPolicyAccess(): android.content.Intent =
            android.content.Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }
}

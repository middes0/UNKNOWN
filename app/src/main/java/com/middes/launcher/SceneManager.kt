package com.middes.launcher

import android.app.UiModeManager
import android.content.Context
import android.view.Window
import android.view.WindowManager

object SceneManager {
    val scenes = listOf("Normal", "Estudo", "Música", "Noite", "Gaming")

    fun accent(scene: String): Int = when (scene) {
        "Estudo" -> MiddesColors.study
        "Música" -> MiddesColors.music
        "Noite" -> MiddesColors.night
        "Gaming" -> MiddesColors.gaming
        else -> MiddesColors.purpleBright
    }

    fun applySystem(context: Context, window: Window, scene: String) {
        // Os protocolos do MIDDES não alteram o modo global do Android.
        // Isso evita que o Android recrie a Activity durante comandos da NEXA.
        try {
            val params = window.attributes
            params.screenBrightness =
                if (scene == "Noite") 0.18f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = params
        } catch (_: Exception) {
            // O protocolo continua funcionando mesmo se o sistema bloquear o brilho.
        }
    }

    fun description(scene: String, hour: Int): Pair<String, String> = when (scene) {
        "Estudo" -> "Foco ativo" to "Um espaço calmo para estudar e manter a concentração."
        "Música" -> "Seu espaço sonoro" to "Players e aplicativos de áudio ficam a um toque."
        "Noite" -> "Modo noturno" to "Interface discreta e brilho reduzido nesta janela."
        "Gaming" -> "Gaming" to "Ambiente dedicado para seus jogos e atalhos."
        else -> {
            val greeting = when (hour) {
                in 5..11 -> "Bom dia."
                in 12..17 -> "Boa tarde."
                else -> "Boa noite."
            }
            "Seu espaço" to "$greeting Tudo pronto para o que vem agora."
        }
    }
}
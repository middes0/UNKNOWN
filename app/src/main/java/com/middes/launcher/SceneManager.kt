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

    @Deprecated("Use SceneProtocolController from MainActivity.")
    fun applySystem(context: Context, window: Window, scene: String) {
        // Mantido por compatibilidade com versões anteriores. Os efeitos reais
        // de cena agora ficam centralizados no SceneProtocolController.
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
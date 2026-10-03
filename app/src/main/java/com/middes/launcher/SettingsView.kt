package com.middes.launcher

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.content.ContextCompat

class SettingsView(
    context: android.content.Context,
    private val store: LauncherStore,
    private val scenes: List<String>,
    private val onBack: () -> Unit,
    private val onWallpaper: () -> Unit,
    private val onSceneApps: () -> Unit,
    private val onOpenProfiles: () -> Unit,
    private val onOpenDrawer: () -> Unit,
    private val onOpenFlow: () -> Unit,
    private val onToggleNexa: () -> Unit,
    private val nexaEnabled: () -> Boolean,
    private val onAndroidSettings: () -> Unit,
    private val onMicrophoneSettings: () -> Unit,
    private val dndStatus: () -> String,
    private val onOpenDndSettings: () -> Unit,
) : ScrollView(context) {

    private val list = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(MiddesUi.dp(context, 20f), MiddesUi.dp(context, 20f), MiddesUi.dp(context, 20f), MiddesUi.dp(context, 30f))
    }

    init {
        setBackgroundColor(MiddesColors.background)
        addView(list)
        build()
    }

    fun rebuild() {
        list.removeAllViews()
        build()
    }

    private fun build() {
        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(MiddesUi.text(context, "‹", 38f, MiddesColors.white).apply {
            gravity = Gravity.CENTER
            setOnClickListener { onBack() }
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 46f), MiddesUi.dp(context, 52f)))
        val title = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        title.addView(MiddesUi.text(context, "Middes", 25f, MiddesColors.white, true),
            LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 30f)))
        title.addView(MiddesUi.text(context, "Personalização e sistema", 10.5f, MiddesColors.muted),
            LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 20f)))
        top.addView(title, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 52f), 1f))
        list.addView(top)

        section("Tela inicial")
        row("Papel de parede", if (store.wallpaperUri != null) "Personalizado" else "Padrão", "Escolher uma imagem", onWallpaper)
        row("Middes Flow", "Disponível", "Espaço dinâmico por contexto", onOpenFlow)
        row("Aplicativos", "Drawer", "Pesquisa, recentes, favoritos e gerenciamento", onOpenDrawer)

        section("Protocolos")
        row("Ambientes da NEXA", "Ocultos", "Estudo, Música, Noite e Gaming são ativados por voz", onSceneApps)
        row("Perfis dos modos", "Configuráveis", "Brilho, volume, foco, DND, wallpaper e app de entrada", onOpenProfiles)
        row("Proteção contra notificações", dndStatus(), "Permite que os protocolos silenciem alertas e chamadas e restaurem o estado anterior", onOpenDndSettings)

        section("NEXA")
        row("NEXA", if (nexaEnabled()) "Ativada" else "Desativada", "Wake word e comandos do launcher", onToggleNexa)
        val microphone = if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        ) "Autorizado" else "Precisa autorizar"
        row("Microfone", microphone, "Permissão controlada pelo Android", onMicrophoneSettings)

        section("Gestos")
        row("Deslizar para cima", "Apps", "Na tela inicial")
        row("Deslizar para baixo", "Busca", "Na tela inicial")

        section("Sistema")
        row("Configurações do Android", "Abrir", "Wi‑Fi, Bluetooth, tela e sistema", onAndroidSettings)
        row("Versão", "2.3", "SCI-FI HUD / NEXA CORE")
        row("Gaming", "Dados reais", "Sem FPS ou desempenho inventados")
    }

    private fun section(title: String) {
        list.addView(MiddesUi.text(context, title.uppercase(), 8.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.15f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)).apply {
            setMargins(MiddesUi.dp(context, 2f), MiddesUi.dp(context, 18f), 0, MiddesUi.dp(context, 6f))
        })
    }

    private fun row(title: String, value: String, subtitle: String? = null, action: (() -> Unit)? = null) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 18f), MiddesUi.dp(context, 13f), MiddesUi.dp(context, 18f), MiddesUi.dp(context, 13f))
            background = MiddesUi.rounded(context, MiddesColors.surfaceRaised, 22f)
            if (action != null) setOnClickListener { action() }
        }
        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(MiddesUi.text(context, title, 14.5f, MiddesColors.white, true),
            LinearLayout.LayoutParams(0, MiddesUi.dp(context, 25f), 1f))
        top.addView(MiddesUi.text(context, value, 10.5f, MiddesColors.purpleBright, true).apply {
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 105f), MiddesUi.dp(context, 26f)))
        card.addView(top)
        if (!subtitle.isNullOrBlank()) {
            card.addView(MiddesUi.text(context, subtitle, 10f, MiddesColors.muted),
                LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 23f)))
        }
        list.addView(card, LinearLayout.LayoutParams(-1, if (subtitle.isNullOrBlank()) MiddesUi.dp(context, 58f) else MiddesUi.dp(context, 75f)).apply {
            setMargins(0, 0, 0, MiddesUi.dp(context, 8f))
        })
    }
}

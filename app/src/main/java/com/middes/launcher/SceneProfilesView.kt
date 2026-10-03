package com.middes.launcher

import android.app.AlertDialog
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch

class SceneProfilesView(
    context: android.content.Context,
    private val store: LauncherStore,
    private val repo: AppRepository,
    private val scenes: List<String>,
    private val onBack: () -> Unit,
    private val onSceneWallpaper: (String) -> Unit,
    private val onChanged: () -> Unit
) : ScrollView(context) {

    private val list = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(MiddesUi.dp(context, 18f), MiddesUi.dp(context, 18f), MiddesUi.dp(context, 18f), MiddesUi.dp(context, 30f))
    }

    init {
        setBackgroundColor(MiddesColors.background)
        addView(list)
        rebuild()
    }

    fun rebuild() {
        list.removeAllViews()
        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(MiddesUi.text(context, "‹", 38f, MiddesColors.white).apply {
            gravity = Gravity.CENTER
            contentDescription = "Voltar"
            setOnClickListener { onBack() }
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 46f), MiddesUi.dp(context, 52f)))
        val title = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        title.addView(MiddesUi.text(context, "Perfis dos modos", 24f, MiddesColors.white, true), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 31f)))
        title.addView(MiddesUi.text(context, "Configure o comportamento de cada ambiente.", 10.5f, MiddesColors.muted), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 21f)))
        top.addView(title, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 52f), 1f))
        list.addView(top)

        list.addView(MiddesUi.text(context, "FOCO • BRILHO • VOLUME • NÃO PERTURBE • WALLPAPER • ABERTURA AUTOMÁTICA", 8f, MiddesColors.muted, true).apply { letterSpacing = 0.08f },
            LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 36f)).apply { setMargins(0, MiddesUi.dp(context, 12f), 0, MiddesUi.dp(context, 4f)) })

        scenes.forEach { addProfileCard(it) }

        val restoreAll = Button(context).apply {
            text = "RESTAURAR TODOS OS PERFIS"
            setTextColor(MiddesColors.muted)
            setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Restaurar perfis")
                    .setMessage("As configurações personalizadas dos modos serão apagadas e os padrões voltarão.")
                    .setPositiveButton("Restaurar") { _, _ ->
                        scenes.forEach { store.restoreSceneProfile(it) }
                        rebuild()
                        onChanged()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
        list.addView(restoreAll, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 50f)).apply { setMargins(0, MiddesUi.dp(context, 8f), 0, 0) })
    }

    private fun addProfileCard(scene: String) {
        val p = store.sceneProfile(scene)
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 17f), MiddesUi.dp(context, 14f), MiddesUi.dp(context, 17f), MiddesUi.dp(context, 14f))
            background = MiddesUi.rounded(context, MiddesColors.surfaceRaised, 22f)
            setOnClickListener { editProfile(scene) }
        }
        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(MiddesUi.text(context, scene.uppercase(), 15f, SceneManager.accent(scene), true), LinearLayout.LayoutParams(0, MiddesUi.dp(context, 25f), 1f))
        top.addView(MiddesUi.text(context, if (scene == store.scene) "ATIVO" else "EDITAR", 8.5f, MiddesColors.muted, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(MiddesUi.dp(context, 70f), MiddesUi.dp(context, 24f)))
        card.addView(top)
        val summary = p.focusMinutes.toString() + " min de foco  •  " +
            (if (p.brightnessPercent > 0) p.brightnessPercent.toString() + "% brilho" else "brilho do sistema") + "  •  " +
            (if (p.mediaVolumePercent >= 0) p.mediaVolumePercent.toString() + "% volume" else "volume sem alteração") + "  •  " +
            (if (p.dndEnabled) "DND" else "DND off")
        card.addView(MiddesUi.text(context, summary, 9.5f, MiddesColors.muted).apply { maxLines = 2 }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 38f)))
        val secondary = (if (p.dimWallpaper) "Wallpaper escurecido" else "Wallpaper normal") +
            if (p.autoLaunch) "  •  Abrir: " + (p.autoLaunchPackage?.let { safeLabel(it) } ?: "app") else ""
        card.addView(MiddesUi.text(context, secondary, 9.5f, MiddesColors.muted).apply { maxLines = 2 }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 28f)))
        list.addView(card, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 116f)).apply { setMargins(0, 0, 0, MiddesUi.dp(context, 8f)) })
    }

    private fun editProfile(scene: String) {
        val original = store.sceneProfile(scene)
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 4f), MiddesUi.dp(context, 2f), MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f))
        }

        val focusValue = MiddesUi.text(context, "", 11f, SceneManager.accent(scene), true)
        box.addView(MiddesUi.text(context, "Tempo de foco", 13f, MiddesColors.white, true))
        box.addView(focusValue, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 24f)))
        val focusSeek = seek(5, 180, original.focusMinutes) { value -> focusValue.text = if (value < 60) "$value minutos" else (value / 60).toString() + "h " + (value % 60).toString() + "min" }
        box.addView(focusSeek, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 38f)))

        box.addView(MiddesUi.text(context, "Brilho da tela", 13f, MiddesColors.white, true), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 25f)).apply { setMargins(0, MiddesUi.dp(context, 8f), 0, 0) })
        val brightnessEnabled = Switch(context).apply { text = "Usar brilho personalizado"; setTextColor(MiddesColors.muted); isChecked = original.brightnessPercent > 0 }
        box.addView(brightnessEnabled)
        val brightnessValue = MiddesUi.text(context, "", 11f, SceneManager.accent(scene), true)
        box.addView(brightnessValue, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)))
        val brightnessSeek = seek(5, 100, original.brightnessPercent.coerceAtLeast(5)) { value -> brightnessValue.text = value.toString() + "%" }
        box.addView(brightnessSeek, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 36f)))
        brightnessSeek.visibility = if (brightnessEnabled.isChecked) VISIBLE else GONE
        brightnessValue.visibility = brightnessSeek.visibility
        brightnessEnabled.setOnCheckedChangeListener { _, checked -> brightnessSeek.visibility = if (checked) VISIBLE else GONE; brightnessValue.visibility = brightnessSeek.visibility }

        val volumeEnabled = Switch(context).apply { text = "Alterar volume de mídia"; setTextColor(MiddesColors.muted); isChecked = original.mediaVolumePercent >= 0 }
        box.addView(volumeEnabled, LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = MiddesUi.dp(context, 6f) })
        val volumeValue = MiddesUi.text(context, "", 11f, SceneManager.accent(scene), true)
        box.addView(volumeValue, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)))
        val volumeStart = if (original.mediaVolumePercent >= 0) original.mediaVolumePercent else 50
        val volumeSeek = seek(0, 100, volumeStart) { value -> volumeValue.text = value.toString() + "%" }
        box.addView(volumeSeek, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 36f)))
        volumeSeek.visibility = if (volumeEnabled.isChecked) VISIBLE else GONE
        volumeValue.visibility = volumeSeek.visibility
        volumeEnabled.setOnCheckedChangeListener { _, checked -> volumeSeek.visibility = if (checked) VISIBLE else GONE; volumeValue.visibility = volumeSeek.visibility }

        val dndSwitch = Switch(context).apply { text = "Silenciar notificações e chamadas"; setTextColor(MiddesColors.muted); isChecked = original.dndEnabled }
        box.addView(dndSwitch, LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = MiddesUi.dp(context, 5f) })
        val dimSwitch = Switch(context).apply { text = "Escurecer wallpaper"; setTextColor(MiddesColors.muted); isChecked = original.dimWallpaper }
        box.addView(dimSwitch, LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT))
        val autoSwitch = Switch(context).apply { text = "Abrir um app automaticamente"; setTextColor(MiddesColors.muted); isChecked = original.autoLaunch && !original.autoLaunchPackage.isNullOrBlank() }
        box.addView(autoSwitch, LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT))

        var selectedPackage: String? = original.autoLaunchPackage
        val appButton = Button(context).apply {
            text = selectedPackage?.let { "App: " + safeLabel(it) } ?: "Escolher app"
            setOnClickListener { showAppPicker(scene) { pkg -> selectedPackage = pkg; text = if (pkg == null) "Nenhum app selecionado" else "App: " + safeLabel(pkg); autoSwitch.isChecked = pkg != null } }
        }
        box.addView(appButton, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 44f)))

        val wallpaperButton = Button(context).apply {
            text = if (store.sceneWallpaperUri(scene) == null) "Wallpaper: usar padrão/global" else "Wallpaper: personalizado"
            setOnClickListener { onSceneWallpaper(scene) }
        }
        box.addView(wallpaperButton, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 44f)))

        val dialog = AlertDialog.Builder(context)
            .setTitle("Perfil • " + scene)
            .setView(ScrollView(context).apply { addView(box) })
            .setPositiveButton("Salvar", null)
            .setNeutralButton("Restaurar padrão", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val brightness = if (brightnessEnabled.isChecked) brightnessSeek.progress.coerceIn(0, 95) + 5 else 0
                val volume = if (volumeEnabled.isChecked) volumeSeek.progress.coerceIn(0, 100) else -1
                store.setSceneProfile(scene, LauncherStore.SceneProfile(focusSeek.progress.coerceIn(0, 175) + 5, brightness, volume, dndSwitch.isChecked, dimSwitch.isChecked, autoSwitch.isChecked && selectedPackage != null, selectedPackage))
                dialog.dismiss()
                rebuild()
                onChanged()
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                store.restoreSceneProfile(scene)
                dialog.dismiss()
                rebuild()
                onChanged()
            }
        }
        dialog.show()
    }

    private fun showAppPicker(scene: String, onSelected: (String?) -> Unit) {
        val apps = repo.getLaunchableApps()
        val labels = arrayOf("Nenhum app") + apps.map { it.label }.toTypedArray()
        AlertDialog.Builder(context).setTitle("Abrir automaticamente em " + scene).setItems(labels) { dialog, which -> onSelected(if (which == 0) null else apps[which - 1].packageName); dialog.dismiss() }.setNegativeButton("Cancelar", null).show()
    }

    private fun seek(min: Int, max: Int, initial: Int, listener: (Int) -> Unit): SeekBar =
        SeekBar(context).apply {
            this.max = (max - min).coerceAtLeast(1)
            progress = (initial.coerceIn(min, max) - min).coerceIn(0, this.max)
            post { listener(min + progress) }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) { listener(min + progress) }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }

    private fun safeLabel(packageName: String): String = try { repo.label(packageName) ?: packageName.substringAfterLast('.') } catch (_: Exception) { packageName.substringAfterLast('.') }
}
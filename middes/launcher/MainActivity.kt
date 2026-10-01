package com.middes.launcher

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var root: FrameLayout
    private lateinit var wallpaperView: ImageView
    private lateinit var overlay: View
    private lateinit var homeView: FrameLayout
    private lateinit var drawerView: LinearLayout
    private lateinit var deskView: LinearLayout
    private lateinit var settingsView: LinearLayout
    private lateinit var appsContainer: LinearLayout
    private lateinit var search: EditText
    private lateinit var clock: TextView
    private lateinit var dateText: TextView
    private lateinit var batteryText: TextView
    private lateinit var greetingText: TextView
    private lateinit var sceneStrip: LinearLayout
    private lateinit var sidebar: LinearLayout
    private lateinit var dock: LinearLayout
private lateinit var gameModeView: GameModeView

    private val prefs by lazy { getSharedPreferences("middes", Context.MODE_PRIVATE) }

    private val black = Color.rgb(6, 6, 9)
    private val purple = Color.rgb(125, 78, 190)
    private val purpleBright = Color.rgb(168, 112, 240)
    private val white = Color.WHITE
    private val gray = Color.rgb(178, 178, 190)
    private val muted = Color.rgb(112, 108, 124)
    private val panel = Color.argb(178, 16, 13, 24)
    private val panelStrong = Color.argb(220, 16, 13, 24)

    private var currentScene = "Normal"

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var speechIntent: Intent
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private var listening = false
    private var voiceEnabled = false
    private var speaking = false
    private var bargeInListening = false
    private val voiceHandler = Handler(Looper.getMainLooper())

    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceEnabled = true
            speak("Pronto.")
        } else {
            voiceEnabled = false
            Toast.makeText(this, "Permissão do microfone necessária para a NEXA.", Toast.LENGTH_LONG).show()
        }
    }

    private val wallpaperPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            prefs.edit().putString("wallpaper", uri.toString()).apply()
            applyWallpaper()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        currentScene = prefs.getString("scene", "Normal") ?: "Normal"
        // Gaming é uma sessão visual temporária: ao reabrir o launcher, volta para Normal.
        if (currentScene == "Gaming") {
            currentScene = "Normal"
            prefs.edit().putString("scene", "Normal").apply()
        }
        setupNexa()
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::clock.isInitialized) {
            updateClock()
            applyWallpaper()
            applyScene(currentScene, false)
        }
        if (voiceEnabled && hasMicrophonePermission()) startNexaListening()
    }

    override fun onDestroy() {
        stopNexaListening()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    override fun onBackPressed() {
        when {
            currentScene == "Gaming" -> applyScene("Normal", true)
            ::drawerView.isInitialized && drawerView.visibility == View.VISIBLE -> closeDrawer()
            ::deskView.isInitialized && deskView.visibility == View.VISIBLE -> closeDesk()
            ::settingsView.isInitialized && settingsView.visibility == View.VISIBLE -> closeSettings()
            else -> super.onBackPressed()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Float = 24f, strokeColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius.toInt()).toFloat()
            if (strokeColor != null) setStroke(dp(1), strokeColor)
        }

    private fun textView(
        text: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false
    ) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun buildUi() {
        root = FrameLayout(this)
        root.setBackgroundColor(black)

        wallpaperView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(black)
        }
        root.addView(wallpaperView, FrameLayout.LayoutParams(-1, -1))

        overlay = View(this).apply {
            setBackgroundColor(Color.argb(105, 0, 0, 0))
        }
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))

        homeView = buildHome()
        root.addView(homeView, FrameLayout.LayoutParams(-1, -1))

        drawerView = buildDrawer()
        drawerView.visibility = View.GONE
        root.addView(drawerView, FrameLayout.LayoutParams(-1, -1))

        deskView = buildDesk()
        deskView.visibility = View.GONE
        root.addView(deskView, FrameLayout.LayoutParams(-1, -1))

        settingsView = buildSettings()
        settingsView.visibility = View.GONE
        root.addView(settingsView, FrameLayout.LayoutParams(-1, -1))

        gameModeView = GameModeView(this)
        gameModeView.visibility = View.GONE
        root.addView(gameModeView, FrameLayout.LayoutParams(-1, -1))

        setContentView(root)
        applyWallpaper()
        applyScene(currentScene, false)
        updateClock()
    }

    private fun buildHome(): FrameLayout {
        val frame = FrameLayout(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(28), dp(18), dp(14))
        }
        frame.addView(content, FrameLayout.LayoutParams(-1, -1))

        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = textView("MIDDES", 13f, white, true).apply {
            letterSpacing = 0.22f
        }
        top.addView(logo, LinearLayout.LayoutParams(0, dp(34), 1f))

        batteryText = textView("100%", 11f, gray).apply {
            gravity = Gravity.CENTER
        }
        top.addView(batteryText, LinearLayout.LayoutParams(dp(52), dp(34)))

        val settings = textView("⚙", 23f, white).apply {
            gravity = Gravity.CENTER
            setOnClickListener { openSettings() }
        }
        top.addView(settings, LinearLayout.LayoutParams(dp(42), dp(34)))
        content.addView(top)

        val timeBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(20), 0, 0)
        }

        clock = textView("", 68f, white).apply {
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.02f
        }
        timeBox.addView(clock, LinearLayout.LayoutParams(-1, dp(82)))

        dateText = textView("", 12f, gray).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.04f
        }
        timeBox.addView(dateText, LinearLayout.LayoutParams(-1, dp(28)))
        content.addView(timeBox)

        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = rounded(panelStrong, 24f)
        }

        val summaryTitle = textView("SEU RESUMO", 10f, muted, true).apply {
            letterSpacing = 0.12f
        }
        summary.addView(summaryTitle, LinearLayout.LayoutParams(-1, dp(20)))

        greetingText = textView("", 17f, white, true)
        summary.addView(greetingText, LinearLayout.LayoutParams(-1, dp(28)))

        val summaryLine = textView(
            "Acesso rápido aos seus apps, cenas e espaço de trabalho.",
            11f, gray
        )
        summary.addView(summaryLine, LinearLayout.LayoutParams(-1, dp(26)))

        content.addView(summary, LinearLayout.LayoutParams(-1, dp(88)).apply {
            setMargins(0, dp(12), 0, dp(12))
        })

        val sceneLabel = textView("CENA ATUAL", 10f, muted, true).apply {
            letterSpacing = 0.12f
        }
        content.addView(sceneLabel, LinearLayout.LayoutParams(-1, dp(20)))

        sceneStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        content.addView(sceneStrip, LinearLayout.LayoutParams(-1, dp(52)).apply {
            setMargins(0, dp(3), 0, dp(10))
        })
        buildSceneButtons()

        val spacer = Space(this)
        content.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))

        sidebar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        content.addView(sidebar, LinearLayout.LayoutParams(-1, dp(68)).apply {
            setMargins(0, 0, 0, dp(10))
        })
        buildQuickApps()

        dock = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.argb(215, 10, 9, 15), 26f)
            setPadding(dp(6), dp(5), dp(6), dp(5))
        }
        content.addView(dock, LinearLayout.LayoutParams(-1, dp(64)))
        buildDock()
        return frame
    }

    private fun buildSceneButtons() {
        sceneStrip.removeAllViews()
        listOf("Normal", "Gaming", "Estudo", "Trabalho", "Noite").forEach { scene ->
            val b = textView(scene, 10.5f, gray, true).apply {
                gravity = Gravity.CENTER
                setPadding(dp(10), 0, dp(10), 0)
                setOnClickListener { applyScene(scene, true) }
            }
            sceneStrip.addView(b, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            })
        }
    }

    private fun buildQuickApps() {
        sidebar.removeAllViews()
        val pm = packageManager
        val defaultCandidates = listOf(
            "com.whatsapp",
            "com.google.android.youtube",
            "com.android.chrome",
            "com.google.android.googlequicksearchbox"
        )
        val selectedPackages = getSceneAppPackages(currentScene, defaultCandidates)

        selectedPackages.forEach { pkg ->
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                val label = info.loadLabel(pm).toString()
                val button = FrameLayout(this).apply {
                    background = rounded(Color.argb(205, 15, 13, 22), 19f)
                    setOnClickListener { launchPackage(pkg) }
                    contentDescription = label
                }
                button.addView(ImageView(this).apply {
                    setImageDrawable(info.loadIcon(pm))
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    contentDescription = label
                }, FrameLayout.LayoutParams(dp(60), dp(60), Gravity.CENTER))
                sidebar.addView(button, LinearLayout.LayoutParams(dp(62), dp(62)).apply {
                    setMargins(0, 0, dp(8), 0)
                })
            } catch (_: Exception) {}
        }

        val apps = FrameLayout(this).apply {
            background = rounded(Color.argb(205, 15, 13, 22), 19f)
            setOnClickListener { openDrawer() }
        }
        apps.addView(textView("•••", 20f, white, true).apply {
            gravity = Gravity.CENTER
        }, FrameLayout.LayoutParams(dp(60), dp(60), Gravity.CENTER))
        sidebar.addView(apps, LinearLayout.LayoutParams(dp(62), dp(62)))
    }

    private fun sceneAppsKey(scene: String): String =
        "scene_apps_" + normalizeSceneName(scene)

    private fun normalizeSceneName(scene: String): String =
        scene.lowercase(Locale.getDefault()).replace("ã", "a").replace("ç", "c")

    private fun getSceneAppPackages(scene: String, defaults: List<String>): List<String> {
        if (!prefs.contains(sceneAppsKey(scene))) return defaults
        return prefs.getStringSet(sceneAppsKey(scene), emptySet()).orEmpty().toList()
    }

    private fun showSceneAppsChooser() {
        val scenes = arrayOf("Normal", "Gaming", "Estudo", "Trabalho", "Noite")
        val sceneLabels = scenes.map { scene ->
            val count = getSceneAppPackages(scene, emptyList()).size
            if (prefs.contains(sceneAppsKey(scene))) "$scene ($count apps)" else "$scene (padrão)"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Aplicativos das cenas")
            .setItems(sceneLabels) { _, which ->
                showAppsForScene(scenes[which])
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun showAppsForScene(scene: String) {
        val pm = packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = pm.queryIntentActivities(launchIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.getDefault()) }

        val defaultCandidates = listOf(
            "com.whatsapp",
            "com.google.android.youtube",
            "com.android.chrome",
            "com.google.android.googlequicksearchbox"
        )
        val selected = getSceneAppPackages(scene, defaultCandidates).toMutableSet()
        val checked = apps.map { selected.contains(it.activityInfo.packageName) }.toBooleanArray()
        val labels = apps.map { it.loadLabel(pm).toString() }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Apps no modo $scene")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val pkg = apps[which].activityInfo.packageName
                if (isChecked) selected.add(pkg) else selected.remove(pkg)
            }
            .setPositiveButton("Salvar") { _, _ ->
                prefs.edit().putStringSet(sceneAppsKey(scene), selected).apply()
                if (currentScene == scene && ::sidebar.isInitialized) buildQuickApps()
                Toast.makeText(this, "Apps do modo $scene atualizados.", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Restaurar padrão") { _, _ ->
                prefs.edit().remove(sceneAppsKey(scene)).apply()
                if (currentScene == scene && ::sidebar.isInitialized) buildQuickApps()
                Toast.makeText(this, "Padrão restaurado.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun buildDock() {
        dock.removeAllViews()

        val home = dockButton("HOME", true) {}
        val desk = dockButton("DESK", false) { openDesk() }
        val apps = dockButton("APPS", false) { openDrawer() }
        val voice = dockButton("NEXA", false) {
            requestNexaMicrophone()
        }

        dock.addView(home, LinearLayout.LayoutParams(0, dp(52), 1f))
        dock.addView(desk, LinearLayout.LayoutParams(0, dp(52), 1f))
        dock.addView(apps, LinearLayout.LayoutParams(0, dp(52), 1f))
        dock.addView(voice, LinearLayout.LayoutParams(0, dp(52), 1f))
    }

    private fun dockButton(label: String, active: Boolean, action: () -> Unit): TextView =
        textView(label, 9.5f, if (active) purpleBright else gray, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.1f
            setOnClickListener { action() }
        }

    private fun buildDrawer(): LinearLayout {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(28), dp(18), dp(14))
            setBackgroundColor(Color.argb(245, 7, 6, 10))
        }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }

        val back = textView("‹", 38f, white).apply {
            gravity = Gravity.CENTER
            setOnClickListener { closeDrawer() }
        }
        top.addView(back, LinearLayout.LayoutParams(dp(48), dp(52)))

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleBox.addView(textView("Aplicativos", 23f, white, true), LinearLayout.LayoutParams(-1, dp(32)))
        titleBox.addView(textView("Tudo instalado neste dispositivo", 11f, gray), LinearLayout.LayoutParams(-1, dp(22)))
        top.addView(titleBox, LinearLayout.LayoutParams(0, dp(52), 1f))

        val gear = textView("⚙", 22f, white).apply {
            gravity = Gravity.CENTER
            setOnClickListener { openSettings() }
        }
        top.addView(gear, LinearLayout.LayoutParams(dp(44), dp(52)))
        view.addView(top)

        search = EditText(this).apply {
            hint = "Buscar aplicativo"
            setHintTextColor(Color.rgb(115, 110, 125))
            setTextColor(white)
            textSize = 14f
            setSingleLine(true)
            setPadding(dp(20), 0, dp(20), 0)
            background = rounded(Color.rgb(22, 19, 29), 22f)
        }
        view.addView(search, LinearLayout.LayoutParams(-1, dp(52)).apply {
            setMargins(0, dp(14), 0, dp(12))
        })

        val deskButton = textView("MY DESK   ›", 11f, white, true).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), 0, dp(18), 0)
            background = rounded(Color.rgb(24, 20, 32), 20f)
            setOnClickListener { openDesk() }
        }
        view.addView(deskButton, LinearLayout.LayoutParams(-1, dp(48)).apply {
            setMargins(0, 0, 0, dp(10))
        })

        val scroll = ScrollView(this)
        appsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(12))
        }
        scroll.addView(appsContainer, FrameLayout.LayoutParams(-1, -2))
        view.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                loadApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(e: android.text.Editable?) = Unit
        })
        return view
    }

    private fun buildDesk(): LinearLayout {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(30), dp(20), dp(18))
            setBackgroundColor(Color.argb(248, 8, 7, 12))
        }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(textView("‹", 38f, white).apply {
            gravity = Gravity.CENTER
            setOnClickListener { closeDesk() }
        }, LinearLayout.LayoutParams(dp(48), dp(52)))

        val title = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        title.addView(textView("MY DESK", 22f, white, true), LinearLayout.LayoutParams(-1, dp(30)))
        title.addView(textView("Seu espaço de controle do Middes", 11f, gray), LinearLayout.LayoutParams(-1, dp(22)))
        top.addView(title, LinearLayout.LayoutParams(0, dp(52), 1f))
        view.addView(top)

        val cards = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        deskCard(cards, "CENA ATUAL", currentScene, "Toque para trocar") {
            openSettings()
        }
        deskCard(cards, "PAPEL DE PAREDE", if (prefs.contains("wallpaper")) "Personalizado" else "Padrão escuro", "Toque para alterar") {
            chooseWallpaper()
        }
        deskCard(cards, "APLICATIVOS", "Abrir gaveta", "Todos os seus aplicativos") {
            openDrawer()
        }
        deskCard(cards, "NEXA", "Assistente", "Comandos de voz em breve") {
            Toast.makeText(this, "A NEXA será conectada às ações do Middes.", Toast.LENGTH_SHORT).show()
        }

        view.addView(cards, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            setMargins(0, dp(18), 0, 0)
        })
        return view
    }

    private fun deskCard(parent: LinearLayout, title: String, value: String, sub: String, action: () -> Unit) {
        val cardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(13), dp(18), dp(13))
            background = rounded(Color.rgb(20, 17, 27), 22f)
            setOnClickListener { action() }
        }
        cardView.addView(textView(title, 9.5f, muted, true), LinearLayout.LayoutParams(-1, dp(20)))
        cardView.addView(textView(value, 17f, white, true), LinearLayout.LayoutParams(-1, dp(28)))
        cardView.addView(textView(sub, 10.5f, gray), LinearLayout.LayoutParams(-1, dp(20)))
        parent.addView(cardView, LinearLayout.LayoutParams(-1, dp(82)).apply {
            setMargins(0, 0, 0, dp(10))
        })
    }

    private fun buildSettings(): LinearLayout {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(30), dp(20), dp(18))
            setBackgroundColor(Color.rgb(8, 7, 12))
        }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(textView("‹", 38f, white).apply {
            gravity = Gravity.CENTER
            setOnClickListener { closeSettings() }
        }, LinearLayout.LayoutParams(dp(48), dp(52)))

        val title = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        title.addView(textView("Middes", 22f, white, true), LinearLayout.LayoutParams(-1, dp(30)))
        title.addView(textView("Personalização do launcher", 11f, gray), LinearLayout.LayoutParams(-1, dp(22)))
        top.addView(title, LinearLayout.LayoutParams(0, dp(52), 1f))
        view.addView(top)

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, dp(20))
        }

        settingsRow(list, "Papel de parede", "Escolher uma imagem da galeria") { chooseWallpaper() }
        settingsRow(list, "Cena", "Normal, Gaming, Estudo, Trabalho ou Noite") { showSceneChooser() }
        settingsRow(list, "Aplicativos das cenas", "Escolha quais apps aparecem em cada modo") { showSceneAppsChooser() }
        settingsRow(list, "My Desk", "Central de atalhos e informações") { openDesk() }
        settingsRow(list, "Aplicativos", "Abrir a gaveta e pesquisar apps") { openDrawer() }
        settingsRow(list, "Modo escuro do fundo", "Reaplicar a camada de escurecimento") {
            val enabled = !prefs.getBoolean("dim", true)
            prefs.edit().putBoolean("dim", enabled).apply()
            applyWallpaper()
        }
        settingsRow(list, "Abrir configurações do Android", "Wi-Fi, Bluetooth, tela e sistema") {
            try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) {}
        }

        scroll.addView(list, FrameLayout.LayoutParams(-1, -2))
        view.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        return view
    }

    private fun settingsRow(parent: LinearLayout, title: String, subtitle: String, action: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = rounded(Color.rgb(20, 17, 27), 22f)
            setOnClickListener { action() }
        }
        row.addView(textView(title, 15f, white, true), LinearLayout.LayoutParams(-1, dp(26)))
        row.addView(textView(subtitle, 10.5f, gray), LinearLayout.LayoutParams(-1, dp(24)))
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(70)).apply {
            setMargins(0, 0, 0, dp(10))
        })
    }

    private fun setupNexa() {
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts.language = Locale("pt", "BR")
                tts.setSpeechRate(1.05f)
                tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        speaking = true
                        bargeInListening = false
                        voiceHandler.post {
                            try { speechRecognizer?.cancel() } catch (_: Exception) {}
                            if (voiceEnabled) {
                                voiceHandler.postDelayed({
                                    if (voiceEnabled && speaking) {
                                        bargeInListening = true
                                        startNexaListening(true)
                                    }
                                }, 180)
                            }
                        }
                    }
                    override fun onDone(utteranceId: String?) {
                        speaking = false
                        bargeInListening = false
                        if (voiceEnabled) restartNexaListening(120)
                    }
                    override fun onError(utteranceId: String?) {
                        speaking = false
                        bargeInListening = false
                        if (voiceEnabled) restartNexaListening(120)
                    }
                })
            }
        }

        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { listening = true }
                override fun onBeginningOfSpeech() { listening = true }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { listening = false }
                override fun onError(error: Int) {
                    listening = false
                    if (voiceEnabled) {
                        if (speaking && bargeInListening) restartNexaListening(120)
                        else restartNexaListening(250)
                    }
                }
                override fun onResults(results: Bundle?) {
                    listening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val phrase = matches.firstOrNull().orEmpty()

                    if (speaking && bargeInListening) {
                        val normalized = normalizeVoice(phrase)
                        if (normalized.contains("nexa") || normalized.contains("nessa")) {
                            bargeInListening = false
                            speaking = false
                            try { tts.stop() } catch (_: Exception) {}
                            handleNexaCommand(phrase)
                        } else if (voiceEnabled) {
                            restartNexaListening(120)
                        }
                    } else {
                        if (phrase.isNotBlank()) handleNexaCommand(phrase)
                        if (voiceEnabled && !speaking) restartNexaListening(120)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun hasMicrophonePermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun requestNexaMicrophone() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Reconhecimento de voz não disponível neste aparelho.", Toast.LENGTH_LONG).show()
            return
        }
        if (!hasMicrophonePermission()) {
            microphonePermission.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }
        voiceEnabled = true
        speak("Estou ouvindo.")
    }

    private fun startNexaListening(allowWhileSpeaking: Boolean = false) {
        if (!voiceEnabled || !hasMicrophonePermission() || isFinishing || isDestroyed) return
        if (speaking && !allowWhileSpeaking) return
        if (listening) return
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.startListening(speechIntent)
        } catch (_: Exception) {
            restartNexaListening(700)
        }
    }

    private fun stopNexaListening() {
        voiceEnabled = false
        listening = false
        bargeInListening = false
        voiceHandler.removeCallbacksAndMessages(null)
        try { speechRecognizer?.cancel() } catch (_: Exception) {}
    }

    private fun restartNexaListening(delay: Long) {
        voiceHandler.removeCallbacksAndMessages(null)
        if (!voiceEnabled) return
        voiceHandler.postDelayed({ startNexaListening() }, delay)
    }

    private fun speak(text: String) {
        if (!ttsReady) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nexa-response")
    }

    private fun normalizeVoice(text: String): String {
        return java.text.Normalizer.normalize(text.lowercase(Locale("pt", "BR")), java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun handleNexaCommand(raw: String) {
        val text = normalizeVoice(raw)
        if (!text.contains("nexa")) return

        val wakeWords = listOf("nexa", "nessa", "nexa")
        val wake = wakeWords.firstOrNull { text.contains(it) } ?: return
        val command = text.substringAfter(wake, "").trim().trim(',', '.', ':', ';')
        if (command.isBlank()) {
            speak("Estou ouvindo.")
            return
        }

        when {
            command.contains("modo estudo") ||
                command.contains("modo de estudo") ||
                command.contains("modo estudar") ||
                command.contains("ativar modo estudo") ||
                command.contains("ativa modo estudo") ||
                command.contains("ativar estudo") ||
                command.contains("ativa estudo") ||
                command == "estudo" ||
                command == "estudar" -> {
                applyScene("Estudo", true)
                speak("Modo estudo ativado.")
            }
            command.contains("modo gaming") ||
                command.contains("modo game") ||
                command.contains("modo jogo") ||
                command.contains("ativar modo gaming") ||
                command.contains("ativar modo game") ||
                command.contains("ativa modo gaming") ||
                command.contains("ativa modo game") ||
                command.contains("ativar gaming") ||
                command.contains("ativar game") ||
                command.contains("ativa gaming") ||
                command.contains("ativa game") ||
                command == "gaming" ||
                command == "game" -> {
                applyScene("Gaming", true)
                speak("Modo gaming ativado.")
            }
            command.contains("modo trabalho") ||
                command.contains("modo de trabalho") ||
                command.contains("ativar modo trabalho") ||
                command.contains("ativa modo trabalho") ||
                command.contains("ativar trabalho") ||
                command.contains("ativa trabalho") ||
                command == "trabalho" -> {
                applyScene("Trabalho", true)
                speak("Modo trabalho ativado.")
            }
            command.contains("modo noite") ||
                command.contains("modo noturno") ||
                command.contains("ativar modo noite") ||
                command.contains("ativa modo noite") ||
                command.contains("ativar modo noturno") ||
                command.contains("ativa modo noturno") ||
                command.contains("ativar noite") ||
                command.contains("ativa noite") ||
                command == "noite" ||
                command == "noturno" -> {
                applyScene("Noite", true)
                speak("Modo noite ativado.")
            }
            command.contains("modo normal") ||
                command.contains("modo padrao") ||
                command.contains("voltar ao normal") ||
                command.contains("voltar pro normal") ||
                command.contains("voltar para o normal") ||
                command.contains("ativar modo normal") ||
                command.contains("ativa modo normal") ||
                command.contains("ativar normal") ||
                command.contains("ativa normal") ||
                command == "normal" ||
                command == "padrao" -> {
                applyScene("Normal", true)
                speak("Modo normal ativado.")
            }
            command.contains("abrir whatsapp") || command.contains("abre whatsapp") || command.contains("abrir whats") || command.contains("abre whats") || command.contains("abrir zap") || command.contains("abre zap") || command.contains("abrir wpp") || command.contains("abre wpp") -> {
                openVoiceApp("com.whatsapp", "WhatsApp")
            }
            command.contains("abrir youtube") || command.contains("abre youtube") -> openVoiceApp("com.google.android.youtube", "YouTube")
            command.contains("abrir chrome") || command.contains("abre chrome") || command.contains("abrir navegador") || command.contains("abre navegador") -> openVoiceApp("com.android.chrome", "Chrome")
            command.contains("abrir configuracoes") || command.contains("abre configuracoes") || command.contains("abrir configuracao") || command.contains("abre configuracao") -> {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                    speak("Abrindo as configurações.")
                } catch (_: Exception) {
                    speak("Não consegui abrir as configurações.")
                }
            }
            command.contains("abrir aplicativos") || command.contains("abrir apps") || command.contains("gaveta") -> {
                openDrawer()
                speak("Abrindo aplicativos.")
            }
            else -> {
                // Última tentativa: procurar pelo nome real de qualquer app instalado.
                // Assim comandos como "Nexa, abra o relógio" não dependem de pacote fixo.
                val appCommand = command
                    .removePrefix("abrir ")
                    .removePrefix("abra ")
                    .removePrefix("abre ")
                    .removePrefix("aplicativo ")
                    .removePrefix("app ")
                    .trim()

                if (command.startsWith("abrir ") || command.startsWith("abra ") || command.startsWith("abre ")) {
                    openInstalledAppByName(appCommand)
                }
                // Se não for um comando de abertura, simplesmente continua ouvindo.
            }
        }
    }

    private fun openVoiceApp(packageName: String, label: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                launchVoiceIntent(intent, label)
            } else {
                speak("O $label não está instalado.")
            }
        } catch (_: Exception) {
            speak("Não consegui abrir o $label.")
        }
    }

    private fun launchVoiceIntent(intent: Intent, label: String) {
        try {
            // Abre primeiro e fala depois. Assim a fala da NEXA não atrasa a troca de tela.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(intent)
            voiceHandler.postDelayed({ speak("Abrindo $label.") }, 120)
        } catch (_: Exception) {
            // Uma segunda tentativa rápida resolve alguns casos em que o launcher ainda está retomando.
            voiceHandler.postDelayed({
                try {
                    startActivity(intent)
                    speak("Abrindo $label.")
                } catch (_: Exception) {
                    speak("Não consegui abrir o $label.")
                }
            }, 180)
        }
    }

    private fun openInstalledAppByName(spokenName: String) {
        val wanted = normalizeVoice(spokenName)
            .replace("abrir ", "")
            .replace("abra ", "")
            .replace("abre ", "")
            .replace("aplicativo ", "")
            .replace("app ", "")
            .trim()

        if (wanted.isBlank()) return

        try {
            val apps = packageManager.getInstalledApplications(0)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || packageManager.getLaunchIntentForPackage(it.packageName) != null }

            val match = apps
                .mapNotNull { app ->
                    val label = normalizeVoice(packageManager.getApplicationLabel(app).toString())
                    if (label == wanted || label.contains(wanted) || wanted.contains(label)) {
                        val launch = packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launch != null) Triple(app, label, launch) else null
                    } else null
                }
                .sortedBy { if (normalizeVoice(packageManager.getApplicationLabel(it.first).toString()) == wanted) 0 else 1 }
                .firstOrNull()

            if (match != null) {
                val display = packageManager.getApplicationLabel(match.first).toString()
                launchVoiceIntent(match.third, display)
            } else {
                speak("Não encontrei esse aplicativo.")
            }
        } catch (_: Exception) {
            speak("Não consegui acessar seus aplicativos.")
        }
    }

    private fun chooseWallpaper() {
        wallpaperPicker.launch(arrayOf("image/*"))
    }

    private fun applyWallpaper() {
        val value = prefs.getString("wallpaper", null)
        if (value == null) {
            wallpaperView.setImageDrawable(null)
            wallpaperView.setBackgroundColor(black)
        } else {
            try {
                wallpaperView.setImageURI(Uri.parse(value))
            } catch (_: Exception) {
                wallpaperView.setImageDrawable(null)
            }
        }
        overlay.setBackgroundColor(
            if (prefs.getBoolean("dim", true)) Color.argb(105, 0, 0, 0) else Color.argb(45, 0, 0, 0)
        )
    }

    private fun showGameMode(animate: Boolean = true) {
        if (!::gameModeView.isInitialized) return
        gameModeView.setSceneApps(getSceneAppPackages("Gaming", listOf(
            "com.whatsapp",
            "com.google.android.youtube",
            "com.android.chrome",
            "com.google.android.googlequicksearchbox"
        )))
        gameModeView.visibility = View.VISIBLE
        gameModeView.bringToFront()
        gameModeView.startGameAnimation(animate)
    }

    private fun hideGameMode() {
        if (::gameModeView.isInitialized) {
            gameModeView.stopGameAnimation()
            gameModeView.visibility = View.GONE
            homeView.visibility = View.VISIBLE
            drawerView.visibility = View.GONE
            deskView.visibility = View.GONE
            settingsView.visibility = View.GONE
        }
    }

    private inner class GameModeView(context: Context) : FrameLayout(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val handler = Handler(Looper.getMainLooper())
        private var orbitAngle = 0f
        private var orbitRadius = 0f
        private var pulse = 0f
        private var running = false
        private var entrance = true
        private var booting = false
        private var bootStart = 0L
        private val iconViews = mutableListOf<Pair<String, ImageView>>()
        private val cyan = Color.rgb(110, 210, 255)
        private val violet = Color.rgb(155, 95, 255)

        private val ticker = object : Runnable {
            override fun run() {
                if (!running) return
                if (booting) {
                    invalidate()
                    handler.postDelayed(this, 16L)
                    return
                }
                orbitAngle = (orbitAngle + 0.65f) % 360f
                pulse += 0.08f
                if (entrance && orbitRadius < dp(116).toFloat()) {
                    orbitRadius += dp(5).toFloat()
                } else {
                    entrance = false
                    orbitRadius = dp(116).toFloat()
                }
                positionIcons()
                invalidate()
                handler.postDelayed(this, 16L)
            }
        }

        init {
            setWillNotDraw(false)
            setBackgroundColor(Color.rgb(3, 4, 8))
            alpha = 0f
            scaleX = 1.035f
            scaleY = 1.035f

            // Controle manual para nunca deixar o usuário preso no Gaming Mode.
            val exit = textView("SAIR  ×", 10f, Color.argb(220, 210, 220, 235), true).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), 0, dp(12), 0)
                background = rounded(Color.argb(150, 14, 18, 28), 18f, Color.argb(100, 110, 210, 255))
                setOnClickListener { applyScene("Normal", true) }
                contentDescription = "Sair do modo gaming"
            }
            addView(exit, LayoutParams(dp(82), dp(42), Gravity.TOP or Gravity.END).apply {
                setMargins(0, dp(24), dp(16), 0)
            })
        }

        fun setSceneApps(packages: List<String>) {
            iconViews.forEach { removeView(it.second) }
            iconViews.clear()
            val pm = packageManager
            packages.distinct().take(10).forEach { pkg ->
                try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    val icon = ImageView(this@MainActivity).apply {
                        setImageDrawable(info.loadIcon(pm))
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        setPadding(dp(11), dp(11), dp(11), dp(11))
                        background = rounded(Color.argb(225, 13, 15, 24), 22f, Color.argb(120, 110, 210, 255))
                        contentDescription = info.loadLabel(pm).toString()
                        setOnClickListener { launchPackage(pkg) }
                        alpha = 0f
                        scaleX = 0.2f
                        scaleY = 0.2f
                    }
                    addView(icon, LayoutParams(dp(62), dp(62)))
                    iconViews.add(pkg to icon)
                } catch (_: Exception) {}
            }
            requestLayout()
        }

        fun startGameAnimation(animate: Boolean) {
            stopGameAnimation()
            running = true
            entrance = animate
            booting = animate
            bootStart = System.currentTimeMillis()
            orbitRadius = if (animate) 0f else dp(116).toFloat()
            alpha = if (animate) 0f else 1f
            scaleX = if (animate) 1.035f else 1f
            scaleY = scaleX

            iconViews.forEach { pair ->
                pair.second.alpha = if (animate) 0f else 1f
                pair.second.scaleX = if (animate) 0.2f else 1f
                pair.second.scaleY = pair.second.scaleX
            }

            if (animate) {
                // Primeiro a tela normal escurece; depois o sistema é inicializado.
                animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(420L).start()

                handler.postDelayed({
                    if (!running) return@postDelayed
                    booting = false
                    entrance = true
                    orbitRadius = 0f
                    iconViews.forEachIndexed { index, pair ->
                        pair.second.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setStartDelay(index * 90L)
                            .setDuration(520L)
                            .start()
                    }
                    invalidate()
                }, 1350L)
            }

            handler.post(ticker)
        }

        fun stopGameAnimation() {
            running = false
            handler.removeCallbacks(ticker)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f - dp(12)

            if (booting) {
                paint.style = Paint.Style.FILL
                paint.color = Color.rgb(3, 4, 8)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                val elapsed = (System.currentTimeMillis() - bootStart).coerceAtLeast(0L)
                val pulse = (kotlin.math.sin(elapsed / 170.0) * 0.5 + 0.5).toFloat()

                paint.color = Color.argb((45 + pulse * 45).toInt(), 110, 210, 255)
                canvas.drawCircle(cx, cy, dp(20) + dp(8) * pulse, paint)

                paint.color = Color.rgb(110, 210, 255)
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                paint.textSize = dp(16).toFloat()
                canvas.drawText("NEXA", cx, cy - dp(8), paint)

                paint.color = Color.argb(205, 190, 205, 220)
                paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                paint.textSize = dp(10).toFloat()
                canvas.drawText("INICIANDO MODO GAMING", cx, cy + dp(22), paint)

                paint.color = Color.argb(110, 110, 210, 255)
                paint.textSize = dp(8).toFloat()
                canvas.drawText("CARREGANDO SISTEMA  //  ONLINE", cx, cy + dp(40), paint)
                return
            }

            val r = if (orbitRadius > 0f) orbitRadius else dp(116).toFloat()

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1).toFloat()
            paint.color = Color.argb(80, 110, 210, 255)
            canvas.drawCircle(cx, cy, r + dp(30), paint)
            paint.color = Color.argb(55, 155, 95, 255)
            canvas.drawCircle(cx, cy, r + dp(52), paint)

            for (i in 0..2) {
                paint.color = Color.argb(38 + i * 12, 110, 210, 255)
                canvas.drawCircle(cx, cy, dp(58 + i * 8) + kotlin.math.sin(pulse.toDouble()).toFloat() * dp(2), paint)
            }

            paint.style = Paint.Style.FILL
            val glow = dp(40) + kotlin.math.sin(pulse.toDouble()).toFloat() * dp(3)
            paint.color = Color.argb(28, 110, 210, 255)
            canvas.drawCircle(cx, cy, glow + dp(18), paint)
            paint.color = Color.argb(70, 110, 210, 255)
            canvas.drawCircle(cx, cy, glow, paint)
            paint.color = Color.rgb(9, 15, 24)
            canvas.drawCircle(cx, cy, dp(28).toFloat(), paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(2).toFloat()
            paint.color = cyan
            canvas.drawCircle(cx, cy, dp(28).toFloat(), paint)

            paint.color = Color.argb(180, 110, 210, 255)
            paint.strokeWidth = dp(1).toFloat()
            canvas.drawLine(cx - dp(48), cy, cx + dp(48), cy, paint)
            canvas.drawLine(cx, cy - dp(48), cx, cy + dp(48), paint)

            paint.style = Paint.Style.FILL
            paint.color = white
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            paint.textSize = dp(12).toFloat()
            canvas.drawText("NEXA", cx, cy + dp(4), paint)

            paint.color = Color.argb(210, 190, 205, 220)
            paint.textSize = dp(10).toFloat()
            canvas.drawText("GAMING SYSTEM", cx, cy + dp(176), paint)
            paint.color = Color.argb(100, 110, 210, 255)
            paint.textSize = dp(8).toFloat()
            canvas.drawText("ONLINE  //  ORBITAL INTERFACE", cx, cy + dp(192), paint)
        }

        private fun positionIcons() {
            val cx = width / 2f
            val cy = height / 2f - dp(12)
            val count = iconViews.size
            if (count == 0 || width == 0 || height == 0) return
            iconViews.forEachIndexed { index, pair ->
                val angle = Math.toRadians((orbitAngle + index * (360.0 / count)) - 90.0)
                val x = cx + kotlin.math.cos(angle).toFloat() * orbitRadius - dp(31)
                val y = cy + kotlin.math.sin(angle).toFloat() * orbitRadius - dp(31)
                pair.second.x = x
                pair.second.y = y
                pair.second.rotation = (orbitAngle * -0.35f + index * 36f) % 360f
            }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            positionIcons()
        }
    }

    private fun applyScene(scene: String, save: Boolean) {
        currentScene = scene
        if (save) prefs.edit().putString("scene", scene).apply()

        val sceneColor = when (scene) {
            "Gaming" -> Color.rgb(150, 75, 220)
            "Estudo" -> Color.rgb(105, 125, 205)
            "Trabalho" -> Color.rgb(150, 150, 170)
            "Noite" -> Color.rgb(92, 65, 145)
            else -> purpleBright
        }

        greetingText.text = when (scene) {
            "Gaming" -> "Tudo pronto para jogar."
            "Estudo" -> "Foco ativado."
            "Trabalho" -> "Área de trabalho pronta."
            "Noite" -> "Modo noturno ativado."
            else -> "Tudo pronto para você."
        }

        if (::sceneStrip.isInitialized) {
            for (i in 0 until sceneStrip.childCount) {
                val b = sceneStrip.getChildAt(i) as TextView
                val active = b.text.toString() == scene
                b.setTextColor(if (active) white else gray)
                b.background = rounded(
                    if (active) Color.argb(210, Color.red(sceneColor), Color.green(sceneColor), Color.blue(sceneColor))
                    else Color.argb(145, 18, 15, 25),
                    18f
                )
            }
        }

        if (::dock.isInitialized) {
            dock.setBackgroundColor(Color.argb(215, 10, 9, 15))
        }
        if (::sidebar.isInitialized) buildQuickApps()

        if (scene == "Gaming") {
            showGameMode(save)
        } else {
            hideGameMode()
        }
    }

    private fun showSceneChooser() {
        val options = arrayOf("Normal", "Gaming", "Estudo", "Trabalho", "Noite")
        AlertDialog.Builder(this)
            .setTitle("Escolher cena")
            .setSingleChoiceItems(options, options.indexOf(currentScene)) { dialog, which ->
                applyScene(options[which], true)
                dialog.dismiss()
                closeSettings()
            }
            .show()
    }

    private fun openDrawer() {
        homeView.visibility = View.GONE
        deskView.visibility = View.GONE
        settingsView.visibility = View.GONE
        drawerView.visibility = View.VISIBLE
        search.setText("")
        loadApps("")
    }

    private fun closeDrawer() {
        drawerView.visibility = View.GONE
        homeView.visibility = View.VISIBLE
    }

    private fun openDesk() {
        drawerView.visibility = View.GONE
        settingsView.visibility = View.GONE
        homeView.visibility = View.GONE
        deskView.visibility = View.VISIBLE
    }

    private fun closeDesk() {
        deskView.visibility = View.GONE
        homeView.visibility = View.VISIBLE
    }

    private fun openSettings() {
        drawerView.visibility = View.GONE
        deskView.visibility = View.GONE
        homeView.visibility = View.GONE
        settingsView.visibility = View.VISIBLE
    }

    private fun closeSettings() {
        settingsView.visibility = View.GONE
        homeView.visibility = View.VISIBLE
    }

    private fun updateClock() {
        val now = Date()
        clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        dateText.text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
            .format(now).replaceFirstChar { it.uppercaseChar() }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        greetingText.text = when (currentScene) {
            "Gaming" -> "Tudo pronto para jogar."
            "Estudo" -> "Foco ativado."
            "Trabalho" -> "Área de trabalho pronta."
            "Noite" -> "Modo noturno ativado."
            else -> when (hour) {
                in 5..11 -> "Bom dia."
                in 12..17 -> "Boa tarde."
                else -> "Boa noite."
            }
        }

        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        batteryText.text = if (battery >= 0) "$battery%" else "—"
    }

    private fun launchPackage(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun loadApps(query: String) {
        appsContainer.removeAllViews()
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .filter { it.loadLabel(pm).toString().contains(query, ignoreCase = true) }
            .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.getDefault()) }

        apps.forEachIndexed { index, info ->
            val row = if (index % 4 == 0) {
                LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                }.also {
                    appsContainer.addView(it, LinearLayout.LayoutParams(-1, dp(108)))
                }
            } else {
                appsContainer.getChildAt(appsContainer.childCount - 1) as LinearLayout
            }

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                setPadding(dp(4), dp(6), dp(4), dp(6))
                setOnClickListener {
                    val launch = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setClassName(info.activityInfo.packageName, info.activityInfo.name)
                    }
                    try { startActivity(launch) } catch (_: Exception) {}
                }
            }

            val iconBox = FrameLayout(this).apply {
                background = rounded(Color.rgb(22, 19, 29), 18f)
                addView(ImageView(this@MainActivity).apply {
                    setImageDrawable(info.loadIcon(pm))
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                }, FrameLayout.LayoutParams(dp(58), dp(58), Gravity.CENTER))
            }
            item.addView(iconBox, LinearLayout.LayoutParams(dp(70), dp(66)))

            val label = textView(info.loadLabel(pm).toString(), 10.5f, white).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            item.addView(label, LinearLayout.LayoutParams(-1, dp(28)))
            row.addView(item, LinearLayout.LayoutParams(0, dp(104), 1f))
        }
    }

}

package com.middes.launcher

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
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
    private lateinit var sceneText: TextView
    private lateinit var batteryText: TextView
    private lateinit var greetingText: TextView
    private lateinit var sceneStrip: LinearLayout
    private lateinit var sidebar: LinearLayout
    private lateinit var dock: LinearLayout

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
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::clock.isInitialized) {
            updateClock()
            applyWallpaper()
            applyScene(currentScene, false)
        }
    }

    override fun onBackPressed() {
        when {
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
        val candidates = listOf(
            "com.whatsapp",
            "com.google.android.youtube",
            "com.android.chrome",
            "com.google.android.googlequicksearchbox"
        )

        val installed = candidates.mapNotNull { pkg ->
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                Triple(pkg, info.loadIcon(pm), info.loadLabel(pm).toString())
            } catch (_: Exception) { null }
        }

        installed.forEach { (pkg, icon, label) ->
            val button = FrameLayout(this).apply {
                background = rounded(Color.argb(205, 15, 13, 22), 19f)
                setOnClickListener { launchPackage(pkg) }
            }
            button.addView(ImageView(this).apply {
                setImageDrawable(icon)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(12), dp(12), dp(12), dp(12))
                contentDescription = label
            }, FrameLayout.LayoutParams(dp(60), dp(60), Gravity.CENTER))
            sidebar.addView(button, LinearLayout.LayoutParams(dp(62), dp(62)).apply {
                setMargins(0, 0, dp(8), 0)
            })
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

    private fun buildDock() {
        dock.removeAllViews()

        val home = dockButton("HOME", true) {}
        val desk = dockButton("DESK", false) { openDesk() }
        val apps = dockButton("APPS", false) { openDrawer() }
        val voice = dockButton("NEXA", false) {
            Toast.makeText(this, "A NEXA será integrada nesta base.", Toast.LENGTH_SHORT).show()
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

        sceneText.text = scene
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
                    if (active) Color.argb(210, sceneColor.red(), sceneColor.green(), sceneColor.blue())
                    else Color.argb(145, 18, 15, 25),
                    18f
                )
            }
        }

        if (::dock.isInitialized) {
            dock.setBackgroundColor(Color.argb(215, 10, 9, 15))
        }
        if (::sidebar.isInitialized) buildQuickApps()
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
            .format(now).replaceFirstChar { it.uppercase() }

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
        sceneText.text = currentScene
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

    private fun Int.red(): Int = Color.red(this)
    private fun Int.green(): Int = Color.green(this)
    private fun Int.blue(): Int = Color.blue(this)
}

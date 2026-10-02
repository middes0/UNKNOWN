package com.middes.launcher

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.util.Log
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import java.text.Normalizer
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var root: FrameLayout
    private lateinit var wallpaperView: android.widget.ImageView
    private lateinit var dimView: View
    private lateinit var homeView: HomeView
    private lateinit var drawerView: DrawerView
    private lateinit var settingsView: SettingsView
    private lateinit var flowView: MiddesFlowView
    private lateinit var gameView: GameModeView
    private lateinit var nexaView: NexaView
    private lateinit var sceneView: SceneModeView
    private lateinit var hudView: MiddesHudView
    private lateinit var store: LauncherStore
    private lateinit var repo: AppRepository
    private lateinit var nexa: NexaController

    private val handler = Handler(Looper.getMainLooper())
    private var screen = Screen.HOME
    private var nexaState = NexaState.OFF
    private var studyRunning = false
    private var studyRemainingSeconds = 25 * 60

    private val clockTicker = object : Runnable {
        override fun run() {
            when (screen) {
                Screen.HOME -> refreshHome()
                Screen.NEXA -> if (::nexaView.isInitialized) nexaView.refresh()
                Screen.SCENE -> if (::sceneView.isInitialized) refreshSceneView()
                else -> Unit
            }
            handler.postDelayed(this, 1_000L)
        }
    }

    private val studyTicker = object : Runnable {
        override fun run() {
            if (!studyRunning) return
            studyRemainingSeconds = (studyRemainingSeconds - 1).coerceAtLeast(0)
            if (studyRemainingSeconds == 0) {
                studyRunning = false
                nexa.speak("Tempo de foco concluído.")
            }
            when (screen) {
                Screen.HOME -> refreshHome()
                Screen.SCENE -> refreshSceneView()
                else -> Unit
            }
            if (studyRunning) handler.postDelayed(this, 1_000L)
        }
    }

    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) nexa.enableAfterPermission()
        else Toast.makeText(this, "O microfone é necessário para a NEXA.", Toast.LENGTH_LONG).show()
        if (::settingsView.isInitialized) settingsView.rebuild()
        refreshHome()
    }

    private val wallpaperPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            store.wallpaperUri = uri.toString()
            applyWallpaper()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        store = LauncherStore(this)
        repo = AppRepository(this)
        if (store.scene == "Gaming") store.scene = "Normal"

        setupNexa()
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        applyWallpaper()
        SceneManager.applySystem(this, window, store.scene)
        nexa.onResume()
        handler.removeCallbacks(clockTicker)
        handler.post(clockTicker)
        refreshHome()
    }

    override fun onPause() {
        nexa.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockTicker)
        handler.removeCallbacks(studyTicker)
        nexa.destroy()
        if (::flowView.isInitialized) flowView.stop()
        if (::gameView.isInitialized) gameView.stop()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when (screen) {
            Screen.HOME -> Unit
            Screen.DRAWER, Screen.SETTINGS, Screen.FLOW, Screen.NEXA -> showHome()
            Screen.SCENE, Screen.GAME -> applyScene("Normal", true)
        }
    }

    private fun setupNexa() {
        nexa = NexaController(
            context = this,
            enabledProvider = { store.nexaEnabled },
            setEnabled = { store.nexaEnabled = it },
            onNeedPermission = { microphonePermission.launch(Manifest.permission.RECORD_AUDIO) },
            onCommand = { raw ->
                runOnUiThread {
                    if (::nexaView.isInitialized) nexaView.addCommand(raw)
                    try {
                        handleNexaCommand(raw)
                    } catch (error: Exception) {
                        Log.e("NEXA", "Falha ao executar comando: $raw", error)
                        nexa.speak("Não consegui executar esse comando.")
                    }
                }
            },
            onStateChanged = { state ->
                runOnUiThread {
                    nexaState = state
                    if (::homeView.isInitialized) refreshHome()
                    if (::nexaView.isInitialized) nexaView.refresh()
                }
            }
        )
    }

    private fun buildUi() {
        root = FrameLayout(this).apply { setBackgroundColor(MiddesColors.background) }

        wallpaperView = android.widget.ImageView(this).apply {
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(MiddesColors.background)
        }
        root.addView(wallpaperView, FrameLayout.LayoutParams(-1, -1))

        dimView = View(this)
        root.addView(dimView, FrameLayout.LayoutParams(-1, -1))

        hudView = MiddesHudView(this)
        root.addView(hudView, FrameLayout.LayoutParams(-1, -1))

        homeView = HomeView(
            this,
            onOpenDrawer = { showDrawer(false) },
            onOpenDrawerSearch = { showDrawer(true) },
            onOpenSettings = { showSettings() },
            onOpenFlow = { showFlow() },
            onOpenNexa = { showNexa() },
            onLaunchApp = { launchPackage(it) },
            appLabel = { repo.label(it) },
            appIcon = { repo.icon(it) },
            favoritePackages = { homeFavorites() },
            nexaState = { nexaState }
        )
        root.addView(homeView, FrameLayout.LayoutParams(-1, -1))

        drawerView = DrawerView(
            this, repo, store,
            onClose = { showHome() },
            onOpenSettings = { showSettings() },
            onLaunch = { launchPackage(it) },
            onLongPress = { showAppMenu(it) }
        )
        root.addView(drawerView, FrameLayout.LayoutParams(-1, -1))

        settingsView = SettingsView(
            this, store, SceneManager.scenes,
            onBack = { showHome() },
            onWallpaper = { chooseWallpaper() },
            onSceneApps = { showSceneAppsChooser() },
            onOpenDrawer = { showDrawer(false) },
            onOpenFlow = { showFlow() },
            onToggleNexa = { toggleNexa() },
            nexaEnabled = { store.nexaEnabled },
            onAndroidSettings = { openAndroidSettings() },
            onMicrophoneSettings = { openMicrophoneSettings() }
        )
        root.addView(settingsView, FrameLayout.LayoutParams(-1, -1))

        nexaView = NexaView(
            this,
            onClose = { showHome() },
            onToggle = { toggleNexa() },
            onApps = { showDrawer(false) },
            onFlow = { showFlow() },
            enabled = { store.nexaEnabled },
            state = { nexaState }
        )
        root.addView(nexaView, FrameLayout.LayoutParams(-1, -1))

        sceneView = SceneModeView(
            this,
            onExit = { applyScene("Normal", true) },
            onNexa = { showNexa() },
            onPrimary = {
                when (store.scene) {
                    "Estudo" -> if (studyRunning) {
                        studyRunning = false
                        handler.removeCallbacks(studyTicker)
                        refreshSceneView()
                    } else startStudy()
                    "Música" -> openMusicPlayer()
                    "Noite" -> applyScene("Normal", true)
                }
            },
            onLaunch = { launchPackage(it) },
            label = { repo.label(it) },
            icon = { repo.icon(it) },
            packages = { sceneDefaults(it) }
        )
        root.addView(sceneView, FrameLayout.LayoutParams(-1, -1))

        flowView = MiddesFlowView(
            this,
            onClose = { showHome() },
            onLaunch = { launchPackage(it) }
        )
        root.addView(flowView, FrameLayout.LayoutParams(-1, -1))

        gameView = GameModeView(
            this, repo,
            onExit = { applyScene("Normal", true) },
            onApps = { showDrawer(false) },
            onNexa = { toggleNexa() },
            launchApp = { launchPackage(it) }
        )
        root.addView(gameView, FrameLayout.LayoutParams(-1, -1))

        listOf<View>(drawerView, settingsView, nexaView, sceneView, flowView, gameView).forEach {
            it.visibility = View.GONE
        }

        setContentView(root)
        applyWallpaper()
        hudView.setAccent(MiddesColors.purpleBright)
        refreshHome()
    }

    private fun toggleNexa() {
        nexa.toggle()
        refreshHome()
        if (::nexaView.isInitialized) nexaView.refresh()
        if (screen == Screen.SETTINGS) settingsView.rebuild()
    }

    private fun showNexa() {
        if (::gameView.isInitialized) gameView.stop()
        if (::flowView.isInitialized) flowView.stop()
        screen = Screen.NEXA
        setScreenVisibility(screen)
        nexaView.refresh()
    }

    private fun handleNexaCommand(raw: String) {
        val normalized = normalizeVoice(raw)
        val wake = listOf("nexa", "nessa").firstOrNull { normalized.contains(it) } ?: return
        var command = normalized.substringAfter(wake).trim().trim(',', '.', ':', ';')
        command = command
            .removePrefix("por favor ")
            .removePrefix("pode ")
            .removePrefix("poderia ")
            .removePrefix("quero que voce ")
            .removePrefix("quero que vc ")
            .removePrefix("gostaria que voce ")
            .removePrefix("gostaria que vc ")
            .trim()

        if (command.isBlank()) {
            nexa.speak("Estou ouvindo.")
            return
        }

        val actions = splitNexaChain(command)
        if (actions.size > 1) {
            executeNexaChain(actions)
        } else {
            handleNexaSingleCommand(command)
        }
    }

    private fun splitNexaChain(command: String): List<String> {
        val normalized = command
            .replace(Regex("\\s+e depois\\s+"), " e ")
            .replace(Regex("\\s+depois\\s+"), " e ")\n            .replace(Regex("\\s+em seguida\\s+"), " e ")\n            .replace(Regex("\\s+dai\\s+"), " e ")\n            .replace(Regex("\\s+daí\\s+"), " e ")
            .replace(Regex("\\s*,\\s*"), " e ")
            .replace(Regex("\\s+tambem\\s+"), " e ")
            .replace(Regex("\\s+também\\s+"), " e ")
            .replace(Regex("\\s+entao\\s+"), " e ")
            .replace(Regex("\\s+então\\s+"), " e ")
        return normalized
            .split(Regex("\\s+e\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(3)
    }

    private fun normalizeNexaAction(command: String): String =
        command
            .trim()
            .replace(Regex("^\\s*(por favor|tambem|também)\\s+"), "")
            .replace(Regex("\\b(o|a|os|as|um|uma|uns|umas)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun executeNexaChain(actions: List<String>) {
        val results = mutableListOf<String>()
        actions.forEach { action ->
            results += executeNexaAction(action)
        }

        val successCount = results.count { it == "ok" }
        when {
            successCount == actions.size -> nexa.speak("Comando concluído.")
            successCount > 0 -> nexa.speak("Executei parte do comando, mas não consegui concluir tudo.")
            else -> nexa.speak("Não consegui executar o comando.")
        }
    }

    private fun executeNexaAction(command: String): String {
        return try {
            handleNexaSingleCommand(command, silent = true)
            "ok"
        } catch (error: Exception) {
            Log.e("NEXA", "Falha na ação: $command", error)
            "error"
        }
    }

    private fun handleNexaSingleCommand(command: String, silent: Boolean = false) {
        val command = normalizeNexaAction(command)
        when {
            isSceneChangeRequest(command) -> {
                val target = sceneFromCommand(command)
                if (target == null) {
                    if (!silent) nexa.speak("Qual modo você quer ativar?")
                } else if (applyScene(target, true)) {
                    if (!silent) {
                        nexa.speak(
                            when (target) {
                                "Estudo" -> "Modo estudo ativado."
                                "Música" -> "Modo música ativado."
                                "Noite" -> "Modo noite ativado."
                                "Gaming" -> "Modo gaming ativado."
                                else -> "Modo normal ativado."
                            }
                        )
                    }
                } else if (!silent) {
                    nexa.speak("Não consegui ativar esse modo.")
                }
            }
            matchesAny(command, "ativar modo estudo", "ativa modo estudo", "modo estudo", "modo de estudo", "ativar estudo", "estudo", "estudar", "estudio") -> {
                if (applyScene("Estudo", true)) {
                    if (!silent) nexa.speak("Modo estudo ativado.")
                } else if (!silent) nexa.speak("Não consegui ativar o modo estudo.")
            }
            matchesAny(command, "iniciar foco", "inicia foco", "ligar foco", "liga foco", "ativar foco", "ativa foco", "comecar foco", "comeca foco", "continuar foco") -> {
                if (store.scene != "Estudo" && !applyScene("Estudo", true)) {
                    if (!silent) nexa.speak("Não consegui abrir o modo estudo.")
                    return
                }
                startStudy()
                if (!silent) nexa.speak("Foco iniciado.")
            }
            matchesAny(command, "pausar foco", "pausa foco") -> {
                studyRunning = false
                handler.removeCallbacks(studyTicker)
                refreshHome()
                if (!silent) nexa.speak("Foco pausado.")
            }
            matchesAny(command, "resetar foco", "resetar estudo", "reiniciar foco") -> {
                studyRunning = false
                handler.removeCallbacks(studyTicker)
                studyRemainingSeconds = 25 * 60
                refreshHome()
                if (!silent) nexa.speak("Foco reiniciado.")
            }
            matchesAny(command, "ativar modo gaming", "ativa modo gaming", "ligar modo gaming", "liga modo gaming", "entrar no gaming", "entrar gaming", "modo gaming", "modo de gaming", "modo game", "modo gamer", "ativar gaming", "ativa gaming", "ligar gaming", "liga gaming", "ativar game", "ativa game", "ligar game", "liga game", "ativar gamer", "ativa gamer", "ligar gamer", "liga gamer", "gaming", "game", "gamer") -> {
                if (applyScene("Gaming", true)) {
                    if (!silent) nexa.speak("Modo gaming ativado.")
                } else if (!silent) nexa.speak("Não consegui ativar o modo gaming.")
            }
            matchesAny(command, "ativar modo musica", "ativa modo musica", "ligar modo musica", "liga modo musica", "entrar no modo musica", "entrar musica", "modo musica", "modo de musica", "ativar musica", "ativa musica", "ligar musica", "liga musica", "musica", "musical", "audio") -> {
                if (applyScene("Música", true)) {
                    if (!silent) nexa.speak("Modo música ativado.")
                } else if (!silent) nexa.speak("Não consegui ativar o modo música.")
            }
            matchesAny(command, "abrir musica", "abrir player", "abrir spotify", "abrir youtube music") -> {
                openMusicPlayer(silent)
            }
            matchesAny(command, "ativar modo noite", "ativar modo noturno", "ativa modo noite", "ativa modo noturno", "ligar modo noite", "liga modo noite", "entrar no modo noite", "entrar noite", "modo noite", "modo noturno", "ativar noite", "ativa noite", "ligar noite", "liga noite", "noite", "noturno") -> {
                if (applyScene("Noite", true)) {
                    if (!silent) nexa.speak("Modo noite ativado.")
                } else if (!silent) nexa.speak("Não consegui ativar o modo noite.")
            }
            matchesAny(command, "ativar modo normal", "ativa modo normal", "ligar modo normal", "liga modo normal", "voltar ao normal", "voltar pro normal", "entrar no modo normal", "modo normal", "modo padrao", "normal", "padrao", "principal") -> {
                if (applyScene("Normal", true)) {
                    if (!silent) nexa.speak("Modo normal ativado.")
                } else if (!silent) nexa.speak("Não consegui voltar ao modo normal.")
            }
            matchesAny(command, "ativar flow", "ativa flow", "abrir flow", "abrir middes flow", "modo flow") -> {
                showFlow()
                if (!silent) nexa.speak("Flow ativado.")
            }
            matchesAny(command, "abrir nexa", "mostrar nexa", "abrir assistente", "abrir nucleo", "mostrar nucleo") -> {
                showNexa()
                if (!silent) nexa.speak("Núcleo NEXA aberto.")
            }
            matchesAny(command, "abrir cenas", "abrir cena", "mostrar cenas", "escolher cena") -> {
                applyScene("Normal", true)
                if (!silent) nexa.speak("Use comandos como modo estudo, modo música, modo noite ou modo gaming.")
            }
            matchesAny(command, "abrir whatsapp", "abrir whats", "abrir zap", "abrir wpp", "abre whatsapp", "abre zap", "abra whatsapp", "abra zap") ->
                openVoiceApp("com.whatsapp", "WhatsApp", silent)
            matchesAny(command, "abrir youtube", "abre youtube", "abra youtube") ->
                openVoiceApp("com.google.android.youtube", "YouTube", silent)
            matchesAny(command, "abrir chrome", "abrir navegador", "abre chrome", "abre navegador", "abra chrome", "abra navegador") ->
                openVoiceApp("com.android.chrome", "Chrome", silent)
            matchesAny(command, "abrir telefone", "abrir discador", "abrir chamadas", "abrir ligacoes", "abrir ligação", "telefone", "discador") ->
                openSystemApp(Intent(Intent.ACTION_DIAL), "o telefone", silent)
            matchesAny(command, "abrir camera", "abrir câmera", "camera", "câmera") ->
                openSystemApp(Intent(MediaStoreIntent.ACTION_IMAGE_CAPTURE), "a câmera", silent)
            matchesAny(command, "abrir galeria", "abrir fotos", "abrir imagens", "fotos", "galeria") ->
                openSystemApp(Intent(Intent.ACTION_VIEW).apply { type = "image/*" }, "a galeria", silent)
            matchesAny(command, "abrir arquivos", "abrir gerenciador", "abrir documentos", "meus arquivos", "arquivos") ->
                openSystemApp(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) }, "os arquivos", silent)
            matchesAny(command, "abrir notificacoes", "abrir notificações", "notificacoes", "notificações") -> {
                openAndroidSettingsPage("android.settings.NOTIFICATION_SETTINGS", "as configurações de notificações", silent)
            }
            matchesAny(command, "abrir bluetooth", "bluetooth") -> {
                openAndroidSettingsPage(Settings.ACTION_BLUETOOTH_SETTINGS, "as configurações de Bluetooth", silent)
            }
            matchesAny(command, "abrir wifi", "wi fi", "wifi") -> {
                openAndroidSettingsPage(Settings.ACTION_WIFI_SETTINGS, "as configurações de Wi-Fi", silent)
            }
            matchesAny(command, "abrir configuracoes", "abrir configuracao", "abre configuracoes", "abra configuracoes") -> {
                openAndroidSettings(silent)
                if (!silent) nexa.speak("Abrindo as configurações.")
            }
            matchesAny(command, "abrir aplicativos", "abrir apps", "gaveta", "lista de aplicativos", "abrir lista") -> {
                showDrawer(false)
                if (!silent) nexa.speak("Abrindo os aplicativos.")
            }
            matchesAny(command, "voltar", "tela inicial", "inicio", "home") -> {
                if (store.scene == "Gaming") applyScene("Normal", true) else showHome()
                if (!silent) nexa.speak("Voltando para a tela inicial.")
            }
            command.startsWith("abrir ") || command.startsWith("abre ") || command.startsWith("abra ") ||
                command.startsWith("iniciar ") || command.startsWith("inicia ") || command.startsWith("inicie ") -> {
                val appName = command
                    .removePrefix("abrir ").removePrefix("abre ").removePrefix("abra ")
                    .removePrefix("iniciar ").removePrefix("inicia ").removePrefix("inicie ")
                    .removePrefix("aplicativo ").removePrefix("app ").trim()
                openInstalledAppByName(appName, silent)
            }
            else -> {
                if (!silent) nexa.speak("Não reconheci esse comando.")
                throw IllegalArgumentException("Comando não reconhecido: $command")
            }
        }
    }

    private fun performHomeSceneAction() {
        when (store.scene) {
            "Estudo" -> {
                if (studyRunning) {
                    studyRunning = false
                    handler.removeCallbacks(studyTicker)
                } else {
                    startStudy()
                }
                refreshHome()
            }
            "Música" -> openMusicPlayer()
        }
    }

    private fun startStudy() {
        if (studyRemainingSeconds <= 0) studyRemainingSeconds = 25 * 60
        studyRunning = true
        handler.removeCallbacks(studyTicker)
        handler.post(studyTicker)
    }

    private fun formatStudyTime(): String {
        val minutes = studyRemainingSeconds / 60
        val seconds = studyRemainingSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun openMusicPlayer(silent: Boolean = false) {
        val packageName = repo.firstInstalled(sceneDefaults("Música") + listOf(
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.google.android.youtube"
        )).firstOrNull()
        if (packageName != null && repo.launch(packageName)) {
            store.recordLaunch(packageName)
            if (!silent) nexa.speak("Abrindo o player.")
        } else {
            showDrawer(true)
            if (!silent) nexa.speak("Não encontrei um aplicativo de música.")
        }
    }

    private fun openVoiceApp(packageName: String, label: String, silent: Boolean = false) {
        if (repo.icon(packageName) != null) {
            if (!silent) nexa.speak("Abrindo $label.")
            handler.postDelayed({
                if (repo.launch(packageName)) store.recordLaunch(packageName)
            }, 280L)
        } else {
            if (!silent) nexa.speak("O $label não está instalado.")
        }
    }

    private fun openInstalledAppByName(name: String, silent: Boolean = false) {
        val app = repo.findBySpokenName(name)
        if (app == null) {
            if (!silent) nexa.speak("Não encontrei esse aplicativo.")
            return
        }
        if (repo.icon(app.packageName) != null) {
            if (!silent) nexa.speak("Abrindo " + app.label + ".")
            handler.postDelayed({
                if (repo.launch(app.packageName)) store.recordLaunch(app.packageName)
            }, 280L)
        } else {
            if (!silent) nexa.speak("Não consegui abrir " + app.label + ".")
        }
    }

    private fun openSystemApp(intent: Intent, label: String, silent: Boolean = false) {
        try {
            if (intent.resolveActivity(packageManager) == null) {
                if (!silent) nexa.speak("Não encontrei $label neste aparelho.")
                return
            }
            if (!silent) nexa.speak("Abrindo $label.")
            handler.postDelayed({
                try {
                    startActivity(intent)
                } catch (_: Exception) {
                    if (!silent) nexa.speak("Não consegui abrir $label.")
                }
            }, 220L)
        } catch (_: Exception) {
            if (!silent) nexa.speak("Não consegui abrir $label.")
        }
    }

    private fun openAndroidSettingsPage(action: String, label: String, silent: Boolean = false) {
        try {
            startActivity(Intent(action))
            if (!silent) nexa.speak("Abrindo $label.")
        } catch (_: Exception) {
            if (!silent) nexa.speak("Não consegui abrir $label.")
        }
    }

    private fun matchesAny(command: String, vararg values: String): Boolean =
        values.any { command == it || command.contains(it) }

    private fun containsWord(command: String, word: String): Boolean =
        Regex("\\b" + Regex.escape(word) + "\\b").containsMatchIn(command)

    private fun sceneFromCommand(command: String): String? = when {
        containsWord(command, "estudo") || containsWord(command, "estudar") || containsWord(command, "estudio") ||
            containsWord(command, "foco") -> "Estudo"
        containsWord(command, "gaming") || containsWord(command, "game") || containsWord(command, "gamer") ||
            containsWord(command, "jogo") || containsWord(command, "jogos") || containsWord(command, "jogar") -> "Gaming"
        containsWord(command, "musica") || containsWord(command, "musical") || containsWord(command, "audio") ||
            containsWord(command, "som") -> "Música"
        containsWord(command, "noite") || containsWord(command, "noturno") || containsWord(command, "noturna") -> "Noite"
        containsWord(command, "normal") || containsWord(command, "padrao") || containsWord(command, "principal") -> "Normal"
        else -> null
    }

    private fun isSceneChangeRequest(command: String): Boolean {
        val actionWords = listOf(
            "trocar", "troca", "troque",
            "mudar", "muda", "mude",
            "alterar", "altera", "altere",
            "ativar", "ativa", "ative",
            "colocar", "coloca", "coloque",
            "selecionar", "seleciona", "selecione",
            "entrar", "entra"
        )
        return sceneFromCommand(command) != null && actionWords.any { containsWord(command, it) }
    }

    private fun normalizeVoice(value: String): String =
        Normalizer.normalize(value.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun showHome() {
        store.scene = "Normal"
        studyRunning = false
        handler.removeCallbacks(studyTicker)
        screen = Screen.HOME
        if (::gameView.isInitialized) gameView.stop()
        if (::flowView.isInitialized) flowView.stop()
        setScreenVisibility(screen)
        refreshHome()
    }

    private fun showDrawer(focusSearch: Boolean) {
        if (::gameView.isInitialized) gameView.stop()
        if (::flowView.isInitialized) flowView.stop()
        drawerView.refresh()
        screen = Screen.DRAWER
        setScreenVisibility(screen)
        if (focusSearch) drawerView.focusSearch()
    }

    private fun showSettings() {
        if (::gameView.isInitialized) gameView.stop()
        if (::flowView.isInitialized) flowView.stop()
        settingsView.rebuild()
        screen = Screen.SETTINGS
        setScreenVisibility(screen)
    }

    private fun showFlow() {
        if (::gameView.isInitialized) gameView.stop()
        if (store.scene == "Gaming") {
            store.scene = "Normal"
            SceneManager.applySystem(this, window, "Normal")
        }
        flowView.setApps(buildFlowApps().map { it to repo.icon(it) })
        screen = Screen.FLOW
        setScreenVisibility(screen)
        flowView.start()
    }

    private fun setScreenVisibility(active: Screen) {
        homeView.visibility = if (active == Screen.HOME) View.VISIBLE else View.GONE
        drawerView.visibility = if (active == Screen.DRAWER) View.VISIBLE else View.GONE
        settingsView.visibility = if (active == Screen.SETTINGS) View.VISIBLE else View.GONE
        nexaView.visibility = if (active == Screen.NEXA) View.VISIBLE else View.GONE
        sceneView.visibility = if (active == Screen.SCENE) View.VISIBLE else View.GONE
        flowView.visibility = if (active == Screen.FLOW) View.VISIBLE else View.GONE
        gameView.visibility = if (active == Screen.GAME) View.VISIBLE else View.GONE
        if (active != Screen.FLOW) flowView.stop()
        if (active != Screen.GAME) gameView.stop()
    }

    private fun applyScene(scene: String, save: Boolean): Boolean {
        if (scene !in SceneManager.scenes) return false

        return try {
            if (save) store.scene = scene
            if (scene != "Estudo") {
                studyRunning = false
                handler.removeCallbacks(studyTicker)
            }

            SceneManager.applySystem(this, window, scene)

            when (scene) {
                "Gaming" -> {
                    screen = Screen.GAME
                    setScreenVisibility(screen)
                    gameView.start(buildGameApps(), true)
                }
                "Normal" -> {
                    screen = Screen.HOME
                    setScreenVisibility(screen)
                    refreshHome()
                }
                else -> {
                    screen = Screen.SCENE
                    setScreenVisibility(screen)
                    sceneView.setScene(scene, formatStudyTime(), studyRunning)
                }
            }
            true
        } catch (error: Exception) {
            Log.e("NEXA", "Falha ao ativar cena $scene", error)
            false
        }
    }

    private fun refreshSceneView() {
        if (::sceneView.isInitialized && screen == Screen.SCENE) {
            sceneView.setScene(store.scene, formatStudyTime(), studyRunning)
        }
    }

    private fun showScenePicker() {
        val labels = SceneManager.scenes.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Cena")
            .setSingleChoiceItems(labels, labels.indexOf(store.scene).coerceAtLeast(0)) { dialog, which ->
                applyScene(labels[which], true)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSceneAppsChooser() {
        val labels = SceneManager.scenes.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Aplicativos por cena")
            .setItems(labels) { _, which -> showAppsForScene(labels[which]) }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun showAppsForScene(scene: String) {
        val apps = repo.getLaunchableApps()
        val selected = store.sceneApps(scene, sceneDefaults(scene)).toMutableSet()
        val checked = BooleanArray(apps.size) { selected.contains(apps[it].packageName) }

        AlertDialog.Builder(this)
            .setTitle("Apps em $scene")
            .setMultiChoiceItems(apps.map { it.label }.toTypedArray(), checked) { _, which, isChecked ->
                if (isChecked) selected.add(apps[which].packageName) else selected.remove(apps[which].packageName)
            }
            .setPositiveButton("Salvar") { _, _ ->
                store.setSceneApps(scene, selected)
                drawerView.refresh()
                refreshHome()
                if (screen == Screen.GAME && scene == "Gaming") gameView.start(buildGameApps(), false)
            }
            .setNeutralButton("Restaurar padrão") { _, _ ->
                store.restoreSceneApps(scene)
                drawerView.refresh()
                refreshHome()
                if (screen == Screen.GAME && scene == "Gaming") gameView.start(buildGameApps(), false)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAppMenu(app: InstalledApp) {
        val favorite = store.favorites().contains(app.packageName)
        val items = arrayOf(
            if (favorite) "Remover dos favoritos" else "Adicionar aos favoritos",
            "Informações do app",
            "Desinstalar"
        )
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        store.toggleFavorite(app.packageName)
                        drawerView.refresh()
                        refreshHome()
                    }
                    1 -> openAppInfo(app.packageName)
                    2 -> uninstallApp(app.packageName)
                }
            }
            .show()
    }

    private fun launchPackage(packageName: String) {
        if (repo.launch(packageName)) {
            store.recordLaunch(packageName)
            if (screen == Screen.HOME) refreshHome()
        } else {
            Toast.makeText(this, "Não foi possível abrir o aplicativo.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppInfo(packageName: String) {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (_: Exception) {
            Toast.makeText(this, "Não consegui abrir as informações do app.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uninstallApp(packageName: String) {
        try {
            startActivity(Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (_: Exception) {
            Toast.makeText(this, "O Android não permite desinstalar este aplicativo por aqui.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun chooseWallpaper() {
        wallpaperPicker.launch(arrayOf("image/*"))
    }

    private fun applyWallpaper() {
        val uri = store.wallpaperUri
        if (uri == null) {
            wallpaperView.setImageDrawable(null)
            wallpaperView.setBackgroundColor(MiddesColors.background)
        } else {
            try {
                wallpaperView.setImageURI(Uri.parse(uri))
            } catch (_: Exception) {
                store.wallpaperUri = null
                wallpaperView.setImageDrawable(null)
                wallpaperView.setBackgroundColor(MiddesColors.background)
            }
        }
        val dim = if (store.dimWallpaper) 105 else 38
        dimView.setBackgroundColor(android.graphics.Color.argb(dim, 0, 0, 0))
    }

    private fun refreshHome() {
        if (!::homeView.isInitialized) return
        val battery = (getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        homeView.refresh(battery)
    }

    private fun homeFavorites(): List<String> {
        val saved = store.favorites().filter { repo.icon(it) != null }
        if (saved.isNotEmpty()) return saved

        val defaults = repo.firstInstalled(
            listOf("com.whatsapp", "com.google.android.youtube", "com.android.chrome", "com.google.android.googlequicksearchbox")
        )
        if (defaults.isNotEmpty()) store.setFavorites(defaults)
        return defaults
    }

    private fun sceneDefaults(scene: String): List<String> {
        val defaults = when (scene) {
            "Estudo" -> listOf(
                "com.google.android.apps.docs",
                "com.google.android.gm",
                "com.google.android.youtube",
                "com.android.chrome"
            )
            "Música" -> listOf(
                "com.spotify.music",
                "com.google.android.apps.youtube.music",
                "com.google.android.youtube"
            )
            "Gaming" -> listOf(
                "com.dts.freefireth",
                "com.supercell.clashofclans",
                "com.mojang.minecraftpe",
                "com.roblox.client"
            )
            else -> listOf(
                "com.whatsapp",
                "com.google.android.youtube",
                "com.android.chrome",
                "com.google.android.googlequicksearchbox"
            )
        }
        return store.sceneApps(scene, defaults)
    }

    private fun buildFlowApps(): List<String> =
        (store.recentApps() + homeFavorites() + sceneDefaults(store.scene) + sceneDefaults("Normal"))
            .filter { repo.icon(it) != null }
            .distinct()
            .take(8)

    private fun buildGameApps(): List<String> = sceneDefaults("Gaming").take(8)

    private fun openAndroidSettings(silent: Boolean = false) {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(this, "Não foi possível abrir as configurações.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMicrophoneSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (_: Exception) {
            openAndroidSettings()
        }
    }

    private object MediaStoreIntent {
        const val ACTION_IMAGE_CAPTURE = "android.media.action.IMAGE_CAPTURE"
    }

    private enum class Screen { HOME, DRAWER, SETTINGS, NEXA, SCENE, FLOW, GAME }
}
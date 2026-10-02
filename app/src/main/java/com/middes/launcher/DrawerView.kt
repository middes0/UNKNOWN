package com.middes.launcher

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView

class DrawerView(
    context: android.content.Context,
    private val repo: AppRepository,
    private val store: LauncherStore,
    private val onClose: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onLaunch: (String) -> Unit,
    private val onLongPress: (InstalledApp) -> Unit
) : LinearLayout(context) {

    private val search = EditText(context)
    private val recentRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val favoriteRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val appsRoot = LinearLayout(context).apply { orientation = VERTICAL }
    private var allApps = emptyList<InstalledApp>()

    init {
        orientation = VERTICAL
        setPadding(MiddesUi.dp(context, 18f), MiddesUi.dp(context, 20f), MiddesUi.dp(context, 18f), MiddesUi.dp(context, 14f))
        setBackgroundColor(Color.rgb(7, 6, 10))
        build()
        refresh()
    }

    private fun build() {
        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(MiddesUi.text(context, "‹", 38f, MiddesColors.white).apply {
            gravity = Gravity.CENTER
            contentDescription = "Voltar"
            setOnClickListener { onClose() }
        }, LayoutParams(MiddesUi.dp(context, 46f), MiddesUi.dp(context, 50f)))

        val title = LinearLayout(context).apply { orientation = VERTICAL }
        title.addView(MiddesUi.text(context, "Aplicativos", 24f, MiddesColors.white, true),
            LayoutParams(-1, MiddesUi.dp(context, 30f)))
        title.addView(MiddesUi.text(context, "Pesquisa e acesso rápido", 10.5f, MiddesColors.muted),
            LayoutParams(-1, MiddesUi.dp(context, 20f)))
        top.addView(title, LayoutParams(0, MiddesUi.dp(context, 50f), 1f))
        top.addView(MiddesUi.iconButton(context, "⚙", "Configurações") { onOpenSettings() },
            LayoutParams(MiddesUi.dp(context, 42f), MiddesUi.dp(context, 34f)))
        addView(top)

        search.hint = "Pesquisar aplicativo"
        search.setHintTextColor(Color.rgb(111, 107, 124))
        search.setTextColor(MiddesColors.white)
        search.textSize = 14f
        search.setSingleLine(true)
        search.setPadding(MiddesUi.dp(context, 18f), 0, MiddesUi.dp(context, 18f), 0)
        search.background = MiddesUi.rounded(context, MiddesColors.surfaceRaised, 20f)
        addView(search, LayoutParams(-1, MiddesUi.dp(context, 52f)).apply {
            setMargins(0, MiddesUi.dp(context, 12f), 0, MiddesUi.dp(context, 10f))
        })

        addView(MiddesUi.text(context, "FAVORITOS", 8.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.13f
        }, LayoutParams(-1, MiddesUi.dp(context, 18f)))

        addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(favoriteRow, HorizontalScrollView.LayoutParams(-2, MiddesUi.dp(context, 74f)))
        }, LayoutParams(-1, MiddesUi.dp(context, 74f)).apply {
            setMargins(0, MiddesUi.dp(context, 4f), 0, MiddesUi.dp(context, 8f))
        })

        addView(MiddesUi.text(context, "RECENTES", 8.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.13f
        }, LayoutParams(-1, MiddesUi.dp(context, 18f)))

        addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(recentRow, HorizontalScrollView.LayoutParams(-2, MiddesUi.dp(context, 74f)))
        }, LayoutParams(-1, MiddesUi.dp(context, 74f)).apply {
            setMargins(0, MiddesUi.dp(context, 4f), 0, MiddesUi.dp(context, 10f))
        })

        val scroll = ScrollView(context).apply {
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            addView(appsRoot, ScrollView.LayoutParams(-1, -2))
        }
        addView(scroll, LayoutParams(-1, 0, 1f))

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    fun refresh() {
        allApps = repo.getLaunchableApps()
        renderFavorites()
        renderRecents()
        renderApps(search.text?.toString().orEmpty())
    }

    fun focusSearch() {
        search.requestFocus()
        search.selectAll()
        search.post {
            val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(search, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun renderFavorites() {
        favoriteRow.removeAllViews()
        val favorites = store.favorites()
            .mapNotNull { repo.findByPackage(it) }
            .distinctBy { it.packageName }
            .take(10)

        if (favorites.isEmpty()) {
            favoriteRow.addView(MiddesUi.text(context, "Segure um app para adicioná-lo.", 10f, MiddesColors.muted),
                LayoutParams(MiddesUi.dp(context, 240f), MiddesUi.dp(context, 74f)))
            return
        }

        favorites.forEach { app ->
            favoriteRow.addView(miniApp(app), LayoutParams(MiddesUi.dp(context, 66f), MiddesUi.dp(context, 74f)).apply {
                setMargins(0, 0, MiddesUi.dp(context, 8f), 0)
            })
        }
    }

    private fun renderRecents() {
        recentRow.removeAllViews()
        val recent = store.recentApps()
            .mapNotNull { repo.findByPackage(it) }
            .distinctBy { it.packageName }
            .take(6)

        if (recent.isEmpty()) {
            recentRow.addView(MiddesUi.text(context, "Os apps usados aparecem aqui.", 10f, MiddesColors.muted),
                LayoutParams(MiddesUi.dp(context, 250f), MiddesUi.dp(context, 74f)))
            return
        }

        recent.forEach { app ->
            recentRow.addView(miniApp(app), LayoutParams(MiddesUi.dp(context, 66f), MiddesUi.dp(context, 74f)).apply {
                setMargins(0, 0, MiddesUi.dp(context, 8f), 0)
            })
        }
    }

    private fun miniApp(app: InstalledApp): LinearLayout =
        LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setOnClickListener { onLaunch(app.packageName) }
            setOnLongClickListener {
                onLongPress(app)
                true
            }
            val icon = try { app.resolveInfo.loadIcon(context.packageManager) } catch (_: Exception) { null }
            if (icon != null) addView(MiddesUi.appIcon(context, icon, 42), LayoutParams(-1, MiddesUi.dp(context, 46f)))
            addView(MiddesUi.text(context, app.label, 8.5f, MiddesColors.muted).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }, LayoutParams(-1, MiddesUi.dp(context, 24f)))
        }

    private fun renderApps(query: String) {
        appsRoot.removeAllViews()
        val normalized = query.trim().lowercase()
        val filtered = if (normalized.isBlank()) allApps else allApps.filter {
            it.label.lowercase().contains(normalized)
        }

        if (filtered.isEmpty()) {
            appsRoot.addView(MiddesUi.text(context, "Nenhum aplicativo encontrado.", 12f, MiddesColors.muted).apply {
                gravity = Gravity.CENTER
                setPadding(0, MiddesUi.dp(context, 42f), 0, MiddesUi.dp(context, 42f))
            })
            return
        }

        val columns = ((resources.displayMetrics.widthPixels / resources.displayMetrics.density) / 84f)
            .toInt().coerceIn(3, 5)

        var row: LinearLayout? = null
        filtered.forEachIndexed { index, app ->
            if (index % columns == 0) {
                row = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    gravity = Gravity.TOP
                }
                appsRoot.addView(row, LayoutParams(-1, MiddesUi.dp(context, 108f)))
            }
            row?.addView(appItem(app), LinearLayout.LayoutParams(0, MiddesUi.dp(context, 104f), 1f))
        }
    }

    private fun appItem(app: InstalledApp): View {
        val item = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(MiddesUi.dp(context, 3f), MiddesUi.dp(context, 4f), MiddesUi.dp(context, 3f), MiddesUi.dp(context, 4f))
            setOnClickListener { onLaunch(app.packageName) }
            setOnLongClickListener {
                onLongPress(app)
                true
            }
            contentDescription = app.label
        }

        val icon = try { app.resolveInfo.loadIcon(context.packageManager) } catch (_: Exception) { null }
        val iconWrap = android.widget.FrameLayout(context)
        if (icon != null) iconWrap.addView(MiddesUi.appIcon(context, icon, 56),
            android.widget.FrameLayout.LayoutParams(-1, MiddesUi.dp(context, 62f), Gravity.CENTER))
        item.addView(iconWrap, LayoutParams(-1, MiddesUi.dp(context, 68f)))
        item.addView(MiddesUi.text(context, app.label, 10f, MiddesColors.text).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LayoutParams(-1, MiddesUi.dp(context, 28f)))
        return item
    }
}
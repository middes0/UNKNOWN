package com.middes.launcher

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeView(
    context: android.content.Context,
    private val onOpenDrawer: () -> Unit,
    private val onOpenDrawerSearch: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onOpenFlow: () -> Unit,
    private val onOpenNexa: () -> Unit,
    private val onScenePicker: () -> Unit,
    private val onSceneAction: () -> Unit,
    private val onLaunchApp: (String) -> Unit,
    private val appLabel: (String) -> String?,
    private val appIcon: (String) -> Drawable?,
    private val favoritePackages: () -> List<String>,
    private val scenePackages: (String) -> List<String>,
    private val nexaState: () -> NexaState,
    private val studyStatus: () -> String,
    private val studyRunning: () -> Boolean
) : FrameLayout(context) {

    private val clock = MiddesUi.text(context, "", 68f, MiddesColors.white)
    private val date = MiddesUi.text(context, "", 12f, MiddesColors.muted)
    private val greeting = MiddesUi.text(context, "", 11.5f, MiddesColors.muted)
    private val sceneChip = MiddesUi.text(context, "NORMAL", 9f, MiddesColors.purpleBright, true)
    private val sceneTitle = MiddesUi.text(context, "", 20f, MiddesColors.white, true)
    private val sceneSubtitle = MiddesUi.text(context, "", 11f, MiddesColors.muted)
    private val sceneAction = MiddesUi.text(context, "FLOW", 9f, MiddesColors.purpleBright, true)
    private val quickApps = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val nexaBadge = MiddesUi.text(context, "NEXA • DESLIGADA", 8.5f, MiddesColors.muted, true)
    private val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            MiddesUi.dp(context, 20f), MiddesUi.dp(context, 20f),
            MiddesUi.dp(context, 20f), MiddesUi.dp(context, 16f)
        )
    }

    private var downX = 0f
    private var downY = 0f
    private var swipeTriggered = false
    private val threshold = MiddesUi.dp(context, 78f)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        addView(content, FrameLayout.LayoutParams(-1, -1))
        build()
    }

    private fun build() {
        val top = LinearLayout(context).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(MiddesUi.text(context, "MIDDES", 14f, MiddesColors.white, true).apply {
            letterSpacing = 0.22f
        }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 24f), 1f))
        brand.addView(nexaBadge, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 18f), 1f))
        top.addView(brand, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 44f), 1f))

        top.addView(MiddesUi.text(context, "—", 10f, MiddesColors.muted, true).apply {
            tag = "battery"
            gravity = android.view.Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(55, 255, 255, 255), 14f)
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 54f), MiddesUi.dp(context, 30f)).apply {
            setMargins(0, 0, MiddesUi.dp(context, 8f), 0)
        })
        top.addView(MiddesUi.iconButton(context, "⚙", "Configurações") { onOpenSettings() },
            LinearLayout.LayoutParams(MiddesUi.dp(context, 42f), MiddesUi.dp(context, 34f)))
        content.addView(top)

        clock.gravity = android.view.Gravity.CENTER
        clock.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        clock.isLongClickable = true
        clock.setOnLongClickListener { onScenePicker(); true }
        clock.setOnClickListener { onScenePicker() }
        content.addView(clock, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 78f)).apply {
            setMargins(0, MiddesUi.dp(context, 28f), 0, 0)
        })

        date.gravity = android.view.Gravity.CENTER
        content.addView(date, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)))
        greeting.gravity = android.view.Gravity.CENTER
        content.addView(greeting, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 28f)))

        val contextCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 18f), MiddesUi.dp(context, 16f), MiddesUi.dp(context, 18f), MiddesUi.dp(context, 14f))
            background = MiddesUi.rounded(context, Color.argb(214, 13, 11, 19), 28f)
        }
        val line = LinearLayout(context).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        line.addView(MiddesUi.text(context, "MOMENTO", 8.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.12f
        }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 18f), 1f))
        sceneChip.gravity = android.view.Gravity.CENTER
        sceneChip.setPadding(MiddesUi.dp(context, 10f), 0, MiddesUi.dp(context, 10f), 0)
        sceneChip.setOnClickListener { onScenePicker() }
        line.addView(sceneChip, LinearLayout.LayoutParams(MiddesUi.dp(context, 84f), MiddesUi.dp(context, 24f)))
        contextCard.addView(line)
        contextCard.addView(sceneTitle, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 30f)).apply {
            setMargins(0, MiddesUi.dp(context, 4f), 0, 0)
        })
        contextCard.addView(sceneSubtitle, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 35f)))
        sceneAction.apply {
            gravity = android.view.Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(55, 190, 132, 250), 14f)
            setOnClickListener { onSceneAction() }
        }
        contextCard.addView(sceneAction, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 34f)).apply {
            setMargins(0, MiddesUi.dp(context, 6f), 0, 0)
        })

        content.addView(contextCard, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 154f)).apply {
            setMargins(0, MiddesUi.dp(context, 12f), 0, MiddesUi.dp(context, 12f))
        })

        content.addView(MiddesUi.text(context, "SEUS APPS", 8.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.14f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 18f)).apply {
            setMargins(MiddesUi.dp(context, 2f), 0, 0, MiddesUi.dp(context, 8f))
        })

        content.addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(quickApps, HorizontalScrollView.LayoutParams(-2, MiddesUi.dp(context, 88f)))
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 88f)))

        content.addView(Space(context), LinearLayout.LayoutParams(1, 0, 1f))

        val dock = LinearLayout(context).apply {
            gravity = android.view.Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(225, 10, 9, 14), 26f)
            setPadding(MiddesUi.dp(context, 6f), MiddesUi.dp(context, 5f), MiddesUi.dp(context, 6f), MiddesUi.dp(context, 5f))
        }
        dock.addView(dockItem("⌂", "Início") {}, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 56f), 1f))
        dock.addView(dockItem("✦", "Flow") { onOpenFlow() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 56f), 1f))
        dock.addView(dockItem("⌕", "Apps") { onOpenDrawer() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 56f), 1f))
        dock.addView(dockItem("◉", "Cena") { onScenePicker() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 56f), 1f))
        dock.addView(dockItem("◌", "NEXA") { onOpenNexa() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 56f), 1f))
        content.addView(dock, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 64f)))
    }

    private fun dockItem(glyph: String, label: String, action: () -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setOnClickListener { action() }
            addView(MiddesUi.text(context, glyph, 21f, MiddesColors.muted, true).apply {
                gravity = android.view.Gravity.CENTER
            }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 30f)))
            addView(MiddesUi.text(context, label, 8.5f, MiddesColors.muted, true).apply {
                gravity = android.view.Gravity.CENTER
            }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 20f)))
        }

    fun refresh(scene: String, batteryPercent: Int) {
        val info = SceneManager.description(scene, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        sceneTitle.text = info.first
        val subtitleBase = info.second
        sceneSubtitle.text = if (scene == "Estudo") {
            subtitleBase + "  " + studyStatus()
        } else subtitleBase

        sceneChip.text = scene.uppercase(Locale("pt", "BR"))
        val accent = SceneManager.accent(scene)
        sceneChip.setTextColor(accent)
        sceneChip.background = MiddesUi.rounded(context, Color.argb(62, Color.red(accent), Color.green(accent), Color.blue(accent)), 12f)

        sceneAction.visibility = if (scene == "Estudo" || scene == "Música") View.VISIBLE else View.GONE
        sceneAction.text = when (scene) {
            "Estudo" -> if (studyRunning()) "PAUSAR FOCO • " + studyStatus() else "INICIAR FOCO • " + studyStatus()
            "Música" -> "ABRIR PLAYER"
            else -> ""
        }
        sceneAction.setTextColor(accent)
        sceneAction.background = MiddesUi.rounded(
            context,
            Color.argb(55, Color.red(accent), Color.green(accent), Color.blue(accent)),
            14f
        )

        clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        date.text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date())
            .replaceFirstChar { it.uppercaseChar() }

        greeting.text = when (nexaState()) {
            NexaState.LISTENING -> "NEXA está ouvindo."
            NexaState.SPEAKING -> "NEXA está respondendo."
            NexaState.READY -> "NEXA pronta."
            NexaState.OFF -> "Seu launcher, do seu jeito."
        }

        findViewWithTag<TextView>("battery")?.text = if (batteryPercent >= 0) "$batteryPercent%" else "—"
        nexaBadge.text = when (nexaState()) {
            NexaState.OFF -> "NEXA • DESLIGADA"
            NexaState.LISTENING -> "NEXA • OUVINDO"
            NexaState.SPEAKING -> "NEXA • FALANDO"
            NexaState.READY -> "NEXA • PRONTA"
        }

        quickApps.removeAllViews()
        val candidates = (favoritePackages() + scenePackages(scene) + scenePackages("Normal") +
            listOf("com.whatsapp", "com.google.android.youtube", "com.android.chrome", "com.google.android.googlequicksearchbox"))
            .distinct()
            .filter { appIcon(it) != null }
            .take(6)

        candidates.forEach { pkg ->
            val icon = appIcon(pkg) ?: return@forEach
            quickApps.addView(
                quickItem(pkg, icon),
                LinearLayout.LayoutParams(MiddesUi.dp(context, 70f), MiddesUi.dp(context, 88f)).apply {
                    setMargins(0, 0, MiddesUi.dp(context, 12f), 0)
                }
            )
        }

        quickApps.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setOnClickListener { onOpenDrawer() }
                addView(MiddesUi.text(context, "+", 25f, accent).apply { gravity = android.view.Gravity.CENTER },
                    LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 54f)))
                addView(MiddesUi.text(context, "Todos", 9f, MiddesColors.muted, true).apply { gravity = android.view.Gravity.CENTER },
                    LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 28f)))
            },
            LinearLayout.LayoutParams(MiddesUi.dp(context, 64f), MiddesUi.dp(context, 88f))
        )
    }

    private fun quickItem(packageName: String, icon: Drawable): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setOnClickListener { onLaunchApp(packageName) }
            addView(MiddesUi.appIcon(context, icon, 52), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 54f)))
            addView(MiddesUi.text(context, appLabel(packageName).orEmpty(), 9f, MiddesColors.muted).apply {
                gravity = android.view.Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 28f)))
        }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                swipeTriggered = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (swipeTriggered) return true
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (kotlin.math.abs(dy) > threshold &&
                    kotlin.math.abs(dy) > kotlin.math.abs(dx) * 1.15f &&
                    kotlin.math.abs(dy) > touchSlop
                ) {
                    swipeTriggered = true
                    if (dy < 0) onOpenDrawer() else onOpenDrawerSearch()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> swipeTriggered = false
        }
        return super.onInterceptTouchEvent(event)
    }
}
package com.middes.launcher

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Space
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeView(
    context: android.content.Context,
    private val onOpenDrawer: () -> Unit,
    private val onOpenDrawerSearch: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onOpenControl: () -> Unit,
    private val onOpenFlow: () -> Unit,
    private val onOpenNexa: () -> Unit,
    private val onLaunchApp: (String) -> Unit,
    private val appLabel: (String) -> String?,
    private val appIcon: (String) -> Drawable?,
    private val favoritePackages: () -> List<String>,
    private val nexaState: () -> NexaState,
    private val onQuickAction: (String) -> Unit,
    private val sceneSwipeEnabled: () -> Boolean,
    private val onSceneSwipe: (Int) -> Unit
) : FrameLayout(context) {

    private val clock = MiddesUi.text(context, "", 64f, MiddesColors.white)
    private val date = MiddesUi.text(context, "", 10.5f, MiddesColors.muted)
    private val systemLine = MiddesUi.text(context, "", 8f, MiddesColors.muted, true)
    private val nexaStateText = MiddesUi.text(context, "", 8.5f, MiddesColors.purpleBright, true)
    private val battery = MiddesUi.text(context, "—", 8.5f, MiddesColors.text, true)
    private val quickApps = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER
    }
    private var downX = 0f
    private var downY = 0f
    private var swipeTriggered = false
    private val threshold = MiddesUi.dp(context, 78f)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        setBackgroundColor(Color.TRANSPARENT)
        addView(buildContent(), FrameLayout.LayoutParams(-1, -1))
    }

    private fun buildContent(): View {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                MiddesUi.dp(context, 18f), MiddesUi.dp(context, 18f),
                MiddesUi.dp(context, 18f), MiddesUi.dp(context, 12f)
            )
        }

        val top = LinearLayout(context).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        brand.addView(MiddesUi.text(context, "MIDDES", 13f, MiddesColors.white, true).apply {
            letterSpacing = 0.22f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 20f)))
        brand.addView(systemLine, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 17f)))
        top.addView(brand, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 40f), 1f))

        val status = MiddesUi.text(context, "● NEXA", 8f, MiddesColors.purpleBright, true).apply {
            gravity = android.view.Gravity.CENTER
            letterSpacing = 0.07f
            background = MiddesUi.rounded(context, Color.argb(34, 185, 132, 255), 12f)
        }
        top.addView(status, LinearLayout.LayoutParams(MiddesUi.dp(context, 68f), MiddesUi.dp(context, 28f)).apply {
            setMargins(0, 0, MiddesUi.dp(context, 6f), 0)
        })

        battery.gravity = android.view.Gravity.CENTER
        battery.background = MiddesUi.rounded(context, Color.argb(34, 255, 255, 255), 12f)
        top.addView(battery, LinearLayout.LayoutParams(MiddesUi.dp(context, 58f), MiddesUi.dp(context, 28f)))
        content.addView(top)

        date.gravity = android.view.Gravity.CENTER
        content.addView(date, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)).apply {
            setMargins(0, MiddesUi.dp(context, 15f), 0, 0)
        })
        clock.gravity = android.view.Gravity.CENTER
        clock.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        content.addView(clock, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 76f)))

        val coreStage = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }
        val core = MiddesCoreView(context, { nexaState() }, { nexaState() != NexaState.OFF })
        core.setOnClickListener { onOpenNexa() }
        core.contentDescription = "Abrir núcleo NEXA"
        coreStage.addView(core, FrameLayout.LayoutParams(MiddesUi.dp(context, 178f), MiddesUi.dp(context, 178f), android.view.Gravity.CENTER))
        content.addView(coreStage, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 190f)).apply {
            setMargins(0, MiddesUi.dp(context, 2f), 0, 0)
        })

        nexaStateText.gravity = android.view.Gravity.CENTER
        content.addView(nexaStateText, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)))

        content.addView(MiddesUi.text(context, "DESLIZE ↑ PARA APLICATIVOS  •  ↓ PARA BUSCA", 7.5f, MiddesColors.muted, true).apply {
            gravity = android.view.Gravity.CENTER
            letterSpacing = 0.07f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)).apply {
            setMargins(0, 0, 0, MiddesUi.dp(context, 8f))
        })

        content.addView(buildQuickBar(), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 68f)))

        val favoritesTitle = LinearLayout(context).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        favoritesTitle.addView(MiddesUi.text(context, "FAVORITOS", 8f, MiddesColors.muted, true).apply {
            letterSpacing = 0.16f
        }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 22f), 1f))
        favoritesTitle.addView(MiddesUi.text(context, "VER TODOS  ›", 7.5f, MiddesColors.purpleBright, true).apply {
            gravity = android.view.Gravity.CENTER
            setOnClickListener { onOpenDrawer() }
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 74f), MiddesUi.dp(context, 22f)))
        content.addView(favoritesTitle, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)).apply {
            setMargins(0, MiddesUi.dp(context, 8f), 0, 0)
        })

        val appsCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 10f), MiddesUi.dp(context, 4f), MiddesUi.dp(context, 10f), 0)
            background = MiddesUi.rounded(context, Color.argb(72, 10, 11, 17), 19f, Color.argb(24, 255, 255, 255))
        }
        appsCard.addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(quickApps, FrameLayout.LayoutParams(-2, MiddesUi.dp(context, 76f)))
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 76f)))
        content.addView(appsCard, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 84f)))

        content.addView(Space(context), LinearLayout.LayoutParams(1, 0, 1f))

        val dock = LinearLayout(context).apply {
            gravity = android.view.Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(195, 7, 8, 13), 21f, Color.argb(28, 255, 255, 255))
            setPadding(MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f))
        }
        dock.addView(dockItem("⌕", "APPS") { onOpenDrawer() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 54f), 1f))
        dock.addView(dockItem("✦", "FLOW") { onOpenFlow() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 54f), 1f))
        dock.addView(dockItem("◌", "NEXA") { onOpenNexa() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 54f), 1f))
        dock.addView(dockItem("◈", "CONTROL") { onOpenControl() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 54f), 1f))
        content.addView(dock, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 62f)))
        return content
    }

    private fun buildQuickBar(): View {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(82, 12, 13, 20), 18f, Color.argb(22, 255, 255, 255))
            setPadding(MiddesUi.dp(context, 5f), MiddesUi.dp(context, 5f), MiddesUi.dp(context, 5f), MiddesUi.dp(context, 5f))
        }
        listOf(
            "wifi" to "WI-FI",
            "bluetooth" to "BT",
            "sound" to "SOM",
            "display" to "TELA"
        ).forEach { (id, label) ->
            val item = MiddesUi.text(context, label, 7.5f, MiddesColors.muted, true).apply {
                gravity = android.view.Gravity.CENTER
                letterSpacing = 0.06f
                background = MiddesUi.rounded(context, Color.argb(20, 255, 255, 255), 14f)
                setOnClickListener { onQuickAction(id) }
            }
            bar.addView(item, LinearLayout.LayoutParams(0, -1, 1f).apply {
                setMargins(MiddesUi.dp(context, 2f), 0, MiddesUi.dp(context, 2f), 0)
            })
        }
        return bar
    }

    private fun dockItem(glyph: String, label: String, action: () -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setOnClickListener { action() }
            addView(MiddesUi.text(context, glyph, 19f, MiddesColors.text, true).apply {
                gravity = android.view.Gravity.CENTER
            }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 29f)))
            addView(MiddesUi.text(context, label, 7.2f, MiddesColors.muted, true).apply {
                gravity = android.view.Gravity.CENTER
                letterSpacing = 0.05f
            }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 19f)))
        }

    private fun quickItem(packageName: String, icon: Drawable): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setOnClickListener { onLaunchApp(packageName) }
            addView(MiddesUi.appIcon(context, icon, 46), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 50f)))
            addView(MiddesUi.text(context, appLabel(packageName).orEmpty(), 7.8f, MiddesColors.muted).apply {
                gravity = android.view.Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)))
        }

    fun refresh(batteryPercent: Int) {
        clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        date.text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date())
            .replaceFirstChar { it.uppercaseChar() }
        battery.text = if (batteryPercent >= 0) "$batteryPercent%" else "—"

        val state = nexaState()
        systemLine.text = when (state) {
            NexaState.OFF -> "SYSTEM READY  //  VOICE CORE OFFLINE"
            NexaState.READY -> "SYSTEM ONLINE  //  VOICE CORE READY"
            NexaState.LISTENING -> "VOICE LINK ACTIVE  //  LISTENING"
            NexaState.PROCESSING -> "VOICE LINK ACTIVE  //  PROCESSING"
            NexaState.EXECUTING -> "VOICE LINK ACTIVE  //  EXECUTING"
            NexaState.SPEAKING -> "VOICE LINK ACTIVE  //  SPEAKING"
        }
        nexaStateText.text = when (state) {
            NexaState.OFF -> "NEXA  //  OFFLINE"
            NexaState.READY -> "NEXA  //  STANDBY"
            NexaState.LISTENING -> "NEXA  //  OUVINDO"
            NexaState.PROCESSING -> "NEXA  //  PROCESSANDO"
            NexaState.EXECUTING -> "NEXA  //  EXECUTANDO"
            NexaState.SPEAKING -> "NEXA  //  RESPONDENDO"
        }
        nexaStateText.setTextColor(if (state == NexaState.OFF) MiddesColors.muted else MiddesColors.purpleBright)

        quickApps.removeAllViews()
        val candidates = (favoritePackages() + listOf(
            "com.whatsapp",
            "com.google.android.youtube",
            "com.android.chrome",
            "com.google.android.googlequicksearchbox"
        )).distinct().filter { appIcon(it) != null }.take(5)

        candidates.forEach { pkg ->
            val d = appIcon(pkg) ?: return@forEach
            quickApps.addView(quickItem(pkg, d), LinearLayout.LayoutParams(MiddesUi.dp(context, 72f), MiddesUi.dp(context, 76f)).apply {
                setMargins(0, 0, MiddesUi.dp(context, 5f), 0)
            })
        }
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
                if (sceneSwipeEnabled() &&
                    kotlin.math.abs(dx) > threshold &&
                    kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.15f &&
                    kotlin.math.abs(dx) > touchSlop
                ) {
                    swipeTriggered = true
                    performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onSceneSwipe(if (dx < 0f) 1 else -1)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> swipeTriggered = false
        }
        return super.onInterceptTouchEvent(event)
    }
}

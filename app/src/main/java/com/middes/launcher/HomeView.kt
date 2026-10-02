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
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeView(
    context: android.content.Context,
    private val onOpenDrawer: () -> Unit,
    private val onOpenDrawerSearch: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onOpenFlow: () -> Unit,
    private val onOpenNexa: () -> Unit,
    private val onLaunchApp: (String) -> Unit,
    private val appLabel: (String) -> String?,
    private val appIcon: (String) -> Drawable?,
    private val favoritePackages: () -> List<String>,
    private val nexaState: () -> NexaState
) : FrameLayout(context) {

    private val clock = MiddesUi.text(context, "", 72f, MiddesColors.white)
    private val date = MiddesUi.text(context, "", 10.5f, MiddesColors.muted)
    private val systemLine = MiddesUi.text(context, "", 8.5f, MiddesColors.muted, true)
    private val nexaStateText = MiddesUi.text(context, "", 9f, MiddesColors.purpleBright, true)
    private val battery = MiddesUi.text(context, "—", 8.5f, MiddesColors.muted, true)
    private val quickApps = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
    }
    private var downX = 0f
    private var downY = 0f
    private var swipeTriggered = false
    private val threshold = MiddesUi.dp(context, 78f)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        addView(buildContent(), FrameLayout.LayoutParams(-1, -1))
    }

    private fun buildContent(): View {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                MiddesUi.dp(context, 18f), MiddesUi.dp(context, 20f),
                MiddesUi.dp(context, 18f), MiddesUi.dp(context, 14f)
            )
        }

        val top = LinearLayout(context).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(MiddesUi.text(context, "MIDDES", 14f, MiddesColors.white, true).apply {
            letterSpacing = 0.23f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)))
        brand.addView(systemLine, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 18f)))
        top.addView(brand, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 42f), 1f))

        battery.gravity = android.view.Gravity.CENTER
        battery.background = MiddesUi.rounded(context, Color.argb(40, 255, 255, 255), 14f)
        battery.setPadding(MiddesUi.dp(context, 11f), 0, MiddesUi.dp(context, 11f), 0)
        top.addView(battery, LinearLayout.LayoutParams(MiddesUi.dp(context, 62f), MiddesUi.dp(context, 30f)).apply {
            setMargins(0, 0, MiddesUi.dp(context, 7f), 0)
        })
        top.addView(MiddesUi.iconButton(context, "⚙", "Configurações") { onOpenSettings() },
            LinearLayout.LayoutParams(MiddesUi.dp(context, 40f), MiddesUi.dp(context, 34f)))
        content.addView(top)

        date.gravity = android.view.Gravity.CENTER
        content.addView(date, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)).apply {
            setMargins(0, MiddesUi.dp(context, 22f), 0, 0)
        })

        clock.gravity = android.view.Gravity.CENTER
        clock.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        content.addView(clock, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 88f)))

        val coreStage = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }
        addCoreRing(coreStage, 222, 22)
        addCoreRing(coreStage, 188, 36)
        val core = MiddesUi.text(context, "NEXA", 24f, MiddesColors.white, true).apply {
            gravity = android.view.Gravity.CENTER
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.10f
            background = MiddesUi.rounded(
                context,
                Color.argb(30, 190, 132, 250),
                160f,
                Color.argb(125, 190, 132, 250)
            )
            setOnClickListener { onOpenNexa() }
            contentDescription = "Abrir núcleo NEXA"
        }
        coreStage.addView(core, FrameLayout.LayoutParams(MiddesUi.dp(context, 154f), MiddesUi.dp(context, 154f), android.view.Gravity.CENTER))
        content.addView(coreStage, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 205f)).apply {
            setMargins(0, MiddesUi.dp(context, 6f), 0, 0)
        })

        nexaStateText.gravity = android.view.Gravity.CENTER
        content.addView(nexaStateText, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)))

        val protocol = MiddesUi.text(context, "VOICE INTERFACE  //  SAY  “NEXA” + COMMAND", 8f, MiddesColors.muted, true).apply {
            gravity = android.view.Gravity.CENTER
            letterSpacing = 0.10f
        }
        content.addView(protocol, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 24f)).apply {
            setMargins(0, 0, 0, MiddesUi.dp(context, 10f))
        })

        val appsCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 13f), MiddesUi.dp(context, 10f), MiddesUi.dp(context, 13f), MiddesUi.dp(context, 7f))
            background = MiddesUi.rounded(context, Color.argb(92, 10, 10, 17), 22f, Color.argb(38, 255, 255, 255))
        }
        appsCard.addView(MiddesUi.text(context, "QUICK ACCESS", 7.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.16f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 18f)))
        appsCard.addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(quickApps, FrameLayout.LayoutParams(-2, MiddesUi.dp(context, 76f)))
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 76f)))
        content.addView(appsCard, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 104f)))

        content.addView(Space(context), LinearLayout.LayoutParams(1, 0, 1f))

        val dock = LinearLayout(context).apply {
            gravity = android.view.Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(205, 7, 7, 12), 24f, Color.argb(34, 255, 255, 255))
            setPadding(MiddesUi.dp(context, 5f), MiddesUi.dp(context, 4f), MiddesUi.dp(context, 5f), MiddesUi.dp(context, 4f))
        }
        dock.addView(dockItem("⌕", "APPS") { onOpenDrawer() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 56f), 1f))
        dock.addView(dockItem("✦", "FLOW") { onOpenFlow() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 56f), 1f))
        dock.addView(dockItem("◉", "NEXA") { onOpenNexa() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 56f), 1f))
        dock.addView(dockItem("⚙", "SYSTEM") { onOpenSettings() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 56f), 1f))
        content.addView(dock, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 64f)))

        return content
    }

    private fun addCoreRing(parent: FrameLayout, size: Int, alpha: Int) {
        val ring = TextView(context)
        ring.background = MiddesUi.rounded(context, Color.TRANSPARENT, size / 2f, Color.argb(alpha, 190, 132, 250))
        parent.addView(ring, FrameLayout.LayoutParams(MiddesUi.dp(context, size.toFloat()), MiddesUi.dp(context, size.toFloat()), android.view.Gravity.CENTER))
    }

    private fun dockItem(glyph: String, label: String, action: () -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setOnClickListener { action() }
            addView(MiddesUi.text(context, glyph, 20f, MiddesColors.text, true).apply {
                gravity = android.view.Gravity.CENTER
            }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 30f)))
            addView(MiddesUi.text(context, label, 7.5f, MiddesColors.muted, true).apply {
                gravity = android.view.Gravity.CENTER
                letterSpacing = 0.06f
            }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 20f)))
        }

    private fun quickItem(packageName: String, icon: Drawable): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setOnClickListener { onLaunchApp(packageName) }
            addView(MiddesUi.appIcon(context, icon, 46), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 50f)))
            addView(MiddesUi.text(context, appLabel(packageName).orEmpty(), 8f, MiddesColors.muted).apply {
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
        systemLine.text = when (nexaState()) {
            NexaState.OFF -> "SYSTEM READY  //  VOICE CORE OFFLINE"
            NexaState.READY -> "SYSTEM ONLINE  //  VOICE CORE READY"
            NexaState.LISTENING -> "VOICE LINK ACTIVE  //  LISTENING"
            NexaState.SPEAKING -> "VOICE LINK ACTIVE  //  SPEAKING"
        }
        nexaStateText.text = when (nexaState()) {
            NexaState.OFF -> "NEXA  //  OFFLINE"
            NexaState.READY -> "NEXA  //  STANDBY"
            NexaState.LISTENING -> "NEXA  //  LISTENING"
            NexaState.SPEAKING -> "NEXA  //  SPEAKING"
        }
        nexaStateText.setTextColor(if (nexaState() == NexaState.OFF) MiddesColors.muted else MiddesColors.purpleBright)

        quickApps.removeAllViews()
        val candidates = (favoritePackages() + listOf(
            "com.whatsapp",
            "com.google.android.youtube",
            "com.android.chrome",
            "com.google.android.googlequicksearchbox"
        )).distinct().filter { appIcon(it) != null }.take(5)

        candidates.forEach { pkg ->
            val d = appIcon(pkg) ?: return@forEach
            quickApps.addView(quickItem(pkg, d), LinearLayout.LayoutParams(MiddesUi.dp(context, 72f), MiddesUi.dp(context, 72f)).apply {
                setMargins(0, 0, MiddesUi.dp(context, 8f), 0)
            })
        }
        quickApps.addView(MiddesUi.text(context, "+", 22f, MiddesColors.purpleBright, true).apply {
            gravity = android.view.Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(28, 190, 132, 250), 18f)
            setOnClickListener { onOpenDrawer() }
            contentDescription = "Todos os aplicativos"
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 60f), MiddesUi.dp(context, 60f)))
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

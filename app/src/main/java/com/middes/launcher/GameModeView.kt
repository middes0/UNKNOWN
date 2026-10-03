package com.middes.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.BatteryManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class GameModeView(
    context: Context,
    private val repo: AppRepository,
    private val onExit: () -> Unit,
    private val onApps: () -> Unit,
    private val onNexa: () -> Unit,
    private val launchApp: (String) -> Unit,
    private val onSceneSwipe: (Int) -> Unit = {}
) : FrameLayout(context) {

    private val orbit = FrameLayout(context)
    private var running = false
    private var angle = 0.0
    private var exiting = false
    private var downX = 0f
    private var downY = 0f
    private var swipeTriggered = false
    private val swipeThreshold = MiddesUi.dp(context, 76f)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            angle = (angle + 0.009) % (Math.PI * 2)
            positionOrbit()
            updateRealMetrics()
            invalidate()
            postDelayed(this, 100L)
        }
    }

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.rgb(3, 3, 5))

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 18f), MiddesUi.dp(context, 24f), MiddesUi.dp(context, 18f), MiddesUi.dp(context, 12f)
            )
        }
        addView(content, LayoutParams(-1, -1))

        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(MiddesUi.text(context, "MIDDES", 11f, MiddesColors.muted, true).apply {
            letterSpacing = 0.18f
        }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 24f), 1f))
        top.addView(MiddesUi.text(context, "GAMING", 9f, MiddesColors.gaming, true).apply {
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(55, Color.red(MiddesColors.gaming), Color.green(MiddesColors.gaming), Color.blue(MiddesColors.gaming)), 14f)
            setPadding(MiddesUi.dp(context, 10f), 0, MiddesUi.dp(context, 10f), 0)
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 82f), MiddesUi.dp(context, 28f)))
        content.addView(top)

        content.addView(MiddesUi.text(context, "00:00", 56f, MiddesColors.white).apply {
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
            tag = "gameClock"
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 68f)).apply {
            setMargins(0, MiddesUi.dp(context, 24f), 0, 0)
        })
        content.addView(MiddesUi.text(context, "", 10f, MiddesColors.muted).apply {
            gravity = Gravity.CENTER
            tag = "gameDate"
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)))

        content.addView(MiddesUi.text(context, "AMBIENTE GAMING", 10f, MiddesColors.muted, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 28f)).apply {
            setMargins(0, MiddesUi.dp(context, 10f), 0, 0)
        })

        val metrics = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        metric(metrics, "BATERIA")
        metric(metrics, "TEMP.")
        metric(metrics, "RAM LIVRE")
        content.addView(metrics, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 78f)).apply {
            setMargins(0, MiddesUi.dp(context, 8f), 0, MiddesUi.dp(context, 8f))
        })

        orbit.clipChildren = false
        orbit.clipToPadding = false
        content.addView(orbit, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(220, 10, 9, 15), 25f)
        }
        nav.addView(navItem("INÍCIO") { exitAnimated() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 54f), 1f))
        nav.addView(navItem("APPS") { onApps() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 54f), 1f))
        nav.addView(navItem("NEXA") { onNexa() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 54f), 1f))
        content.addView(nav, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 60f)))

        addView(MiddesUi.text(context, "×", 26f, MiddesColors.text).apply {
            gravity = Gravity.CENTER
            contentDescription = "Sair do Gaming"
            setOnClickListener { exitAnimated() }
        }, LayoutParams(MiddesUi.dp(context, 46f), MiddesUi.dp(context, 46f), Gravity.TOP or Gravity.END).apply {
            topMargin = MiddesUi.dp(context, 14f)
        })
    }

    fun start(appPackages: List<String>, animate: Boolean) {
        exiting = false
        running = true
        updateClock()
        setApps(appPackages)
        positionOrbit()
        if (animate) {
            alpha = 0f
            animate().alpha(1f).setDuration(480L).start()
        } else {
            alpha = 1f
        }
        removeCallbacks(ticker)
        post(ticker)
    }

    fun stop() {
        running = false
        removeCallbacks(ticker)
        animate().cancel()
        alpha = 0f
        exiting = false
    }

    fun performExitAnimation() {
        exitAnimated()
    }

    private fun exitAnimated() {
        if (exiting) return
        exiting = true
        running = false
        removeCallbacks(ticker)
        animate().alpha(0f).scaleX(0.94f).scaleY(0.94f).translationY(MiddesUi.dp(context, 18f).toFloat())
            .setDuration(220L).withEndAction {
                onExit()
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
                translationY = 0f
                exiting = false
            }.start()
    }

    private fun metric(parent: LinearLayout, title: String) {
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(120, 14, 12, 20), 18f)
        }
        box.addView(MiddesUi.text(context, title, 8f, MiddesColors.muted, true).apply {
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 18f)))
        box.addView(MiddesUi.text(context, metricValue(title), 13f, MiddesColors.white, true).apply {
            gravity = Gravity.CENTER
            tag = "metric_" + title
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 26f)))
        parent.addView(box, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 72f), 1f).apply {
            setMargins(MiddesUi.dp(context, 3f), 0, MiddesUi.dp(context, 3f), 0)
        })
    }

    private fun metricValue(title: String): String = when (title) {
        "BATERIA" -> batteryText()
        "TEMP." -> temperatureText()
        else -> freeRamText()
    }

    private fun setApps(appPackages: List<String>) {
        orbit.removeAllViews()
        appPackages.distinct().take(8).forEach { pkg ->
            val icon = repo.icon(pkg) ?: return@forEach
            val node = FrameLayout(context).apply {
                background = MiddesUi.rounded(context, Color.argb(175, 13, 11, 19), 22f)
                contentDescription = repo.label(pkg).orEmpty()
                setOnClickListener { launchApp(pkg) }
            }
            node.addView(ImageView(context).apply {
                setImageDrawable(icon)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f))
            }, FrameLayout.LayoutParams(MiddesUi.dp(context, 56f), MiddesUi.dp(context, 56f), Gravity.CENTER))
            orbit.addView(node, LayoutParams(MiddesUi.dp(context, 64f), MiddesUi.dp(context, 64f)))
        }
        orbit.post { positionOrbit() }
    }

    private fun positionOrbit() {
        if (orbit.width <= 0 || orbit.height <= 0 || orbit.childCount == 0) return
        val cx = orbit.width / 2f
        val cy = orbit.height / 2f
        val radius = kotlin.math.min(MiddesUi.dp(context, 88f).toFloat(), kotlin.math.min(orbit.width, orbit.height) * 0.34f)
        for (i in 0 until orbit.childCount) {
            val child = orbit.getChildAt(i)
            val a = angle + Math.PI * 2.0 * i / orbit.childCount
            child.translationX = cx + kotlin.math.cos(a).toFloat() * radius - child.width / 2f
            child.translationY = cy + kotlin.math.sin(a).toFloat() * radius - child.height / 2f
        }
    }

    private fun updateClock() {
        findViewWithTag<TextView>("gameClock")?.text =
            java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
        findViewWithTag<TextView>("gameDate")?.text =
            java.text.SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(java.util.Date())
                .replaceFirstChar { it.uppercaseChar() }
        updateRealMetrics()
    }

    private fun updateRealMetrics() {
        findViewWithTag<TextView>("metric_BATERIA")?.text = batteryText()
        findViewWithTag<TextView>("metric_TEMP.")?.text = temperatureText()
        findViewWithTag<TextView>("metric_RAM LIVRE")?.text = freeRamText()
    }

    private fun batteryText(): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val value = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (value >= 0) "$value%" else "—"
    }

    private fun temperatureText(): String =
        try {
            val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val raw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
            if (raw > 0) String.format(Locale.getDefault(), "%.1f°C", raw / 10f) else "—"
        } catch (_: Exception) { "—" }

    private fun freeRamText(): String =
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            String.format(Locale.getDefault(), "%.1f GB", info.availMem / 1073741824.0)
        } catch (_: Exception) { "—" }

    private fun navItem(label: String, action: () -> Unit): TextView =
        MiddesUi.text(context, label, 8.5f, MiddesColors.muted, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
            setOnClickListener { action() }
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height * 0.53f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = MiddesUi.dp(context, 1f).toFloat()
            color = Color.argb(115, Color.red(MiddesColors.gaming), Color.green(MiddesColors.gaming), Color.blue(MiddesColors.gaming))
        }
        canvas.drawCircle(cx, cy, MiddesUi.dp(context, 86f).toFloat(), paint)
        paint.color = Color.argb(38, 255, 255, 255)
        canvas.drawCircle(cx, cy, MiddesUi.dp(context, 112f).toFloat(), paint)
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
                if (kotlin.math.abs(dx) > swipeThreshold &&
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
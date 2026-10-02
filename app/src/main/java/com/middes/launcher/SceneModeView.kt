package com.middes.launcher

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

class SceneModeView(
    context: android.content.Context,
    private val onExit: () -> Unit,
    private val onNexa: () -> Unit,
    private val onPrimary: () -> Unit,
    private val onSecondary: () -> Unit,
    private val onTertiary: () -> Unit,
    private val onLaunch: (String) -> Unit,
    private val label: (String) -> String?,
    private val icon: (String) -> Drawable?,
    private val packages: (String) -> List<String>
) : FrameLayout(context) {

    private val protocolArt = ProtocolArtView(context)
    private val title = MiddesUi.text(context, "", 30f, MiddesColors.white, true)
    private val code = MiddesUi.text(context, "", 8f, MiddesColors.muted, true)
    private val subtitle = MiddesUi.text(context, "", 9.5f, MiddesColors.muted)
    private val metric = MiddesUi.text(context, "", 46f, MiddesColors.white)
    private val primary = MiddesUi.text(context, "", 8.5f, MiddesColors.white, true)
    private val secondary = MiddesUi.text(context, "", 8f, MiddesColors.muted, true)
    private val tertiary = MiddesUi.text(context, "", 8f, MiddesColors.muted, true)
    private val appRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }
    private val warmOverlay = android.view.View(context)
    private var currentScene = "Normal"
    private var nightWarm = false
    private var nightClock = false
    private var studyStatus = "25:00"

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.TRANSPARENT)
        build()
    }

    private fun build() {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 18f), MiddesUi.dp(context, 18f), MiddesUi.dp(context, 18f), MiddesUi.dp(context, 10f))
        }
        addView(content, LayoutParams(-1, -1))

        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(MiddesUi.text(context, "MIDDES // PRIVATE PROTOCOL", 8.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.12f
        }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 24f), 1f))
        top.addView(MiddesUi.text(context, "NEXA", 7.5f, MiddesColors.purpleBright, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { onNexa() }
            background = MiddesUi.rounded(context, Color.argb(28, 185, 132, 255), 11f)
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 50f), MiddesUi.dp(context, 25f)))
        top.addView(MiddesUi.text(context, "×", 29f, MiddesColors.white).apply {
            gravity = Gravity.CENTER
            contentDescription = "Fechar protocolo"
            setOnClickListener { exitAnimated() }
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 40f), MiddesUi.dp(context, 40f)))
        content.addView(top)

        content.addView(code, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 19f)).apply {
            setMargins(0, MiddesUi.dp(context, 10f), 0, 0)
        })
        title.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        content.addView(title, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 48f)))
        subtitle.gravity = Gravity.CENTER_VERTICAL
        content.addView(subtitle, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 31f)))

        val artFrame = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }
        artFrame.addView(protocolArt, LayoutParams(-1, -1))
        warmOverlay.background = android.graphics.drawable.ColorDrawable(Color.argb(0, 255, 171, 90))
        artFrame.addView(warmOverlay, LayoutParams(-1, -1))
        content.addView(artFrame, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 188f)).apply {
            setMargins(0, MiddesUi.dp(context, 5f), 0, 0)
        })

        metric.gravity = Gravity.CENTER
        content.addView(metric, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 60f)).apply {
            setMargins(0, MiddesUi.dp(context, 0f), 0, 0)
        })

        val actionRow = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(54, 10, 12, 18), 19f, Color.argb(24, 255, 255, 255))
            setPadding(MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f))
        }
        styleAction(primary, true)
        styleAction(secondary, false)
        styleAction(tertiary, false)
        actionRow.addView(primary, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 50f), 1f))
        actionRow.addView(secondary, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 50f), 1f).apply {
            setMargins(MiddesUi.dp(context, 3f), 0, 0, 0)
        })
        actionRow.addView(tertiary, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 50f), 1f).apply {
            setMargins(MiddesUi.dp(context, 3f), 0, 0, 0)
        })
        content.addView(actionRow, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 58f)))

        content.addView(MiddesUi.text(context, "MÓDULOS DO PROTOCOLO", 7.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.14f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 18f)).apply {
            setMargins(0, MiddesUi.dp(context, 10f), 0, 0)
        })
        content.addView(android.widget.HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = android.view.View.OVER_SCROLL_NEVER
            addView(appRow, FrameLayout.LayoutParams(-2, MiddesUi.dp(context, 73f)))
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 75f)))

        content.addView(android.view.View(context), LinearLayout.LayoutParams(1, 0, 1f))

        val bottom = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(190, 7, 8, 13), 20f, Color.argb(25, 255, 255, 255))
        }
        bottom.addView(navItem("NORMAL") { exitAnimated() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 50f), 1f))
        bottom.addView(navItem("NEXA") { onNexa() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 50f), 1f))
        content.addView(bottom, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 54f)))
    }

    private fun styleAction(view: TextView, active: Boolean) {
        view.gravity = Gravity.CENTER
        view.letterSpacing = 0.05f
        view.background = MiddesUi.rounded(
            context,
            if (active) Color.argb(55, Color.red(MiddesColors.purpleBright), Color.green(MiddesColors.purpleBright), Color.blue(MiddesColors.purpleBright))
            else Color.argb(24, 255, 255, 255),
            15f
        )
    }

    private fun navItem(name: String, action: () -> Unit): TextView =
        MiddesUi.text(context, name, 8f, MiddesColors.muted, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.07f
            setOnClickListener { action() }
        }

    fun setScene(value: String, status: String, running: Boolean, protocolStatus: String = "") {
        currentScene = value
        protocolArt.scene = value
        protocolArt.studyRunning = running
        protocolArt.studyStatus = status
        protocolArt.nightWarm = nightWarm
        protocolArt.nightClock = nightClock
        protocolArt.invalidate()

        val accent = SceneManager.accent(value)
        code.setTextColor(accent)
        primary.setTextColor(accent)

        val protocolSuffix = if (protocolStatus.isBlank()) "" else "  •  $protocolStatus"
        when (value) {
            "Estudo" -> {
                code.text = "SCENE 01  //  FOCUS ENGINE"
                title.text = "FOCUS ENGINE"
                subtitle.text = "Sessão de concentração com ciclo independente.$protocolSuffix"
                metric.text = status
                metric.setTextColor(accent)
                primary.text = if (running) "PAUSAR FOCO" else "INICIAR FOCO"
                secondary.text = "RESETAR"
                tertiary.text = if (protocolArt.taskDone) "TAREFA ✓" else "TAREFA"
                subtitle.setTextColor(MiddesColors.muted)
                secondary.setTextColor(MiddesColors.study)
                tertiary.setTextColor(MiddesColors.study)
            }
            "Música" -> {
                code.text = "SCENE 02  //  AUDIO ENGINE"
                title.text = "AUDIO MATRIX"
                subtitle.text = "Central de reprodução com transporte e resposta visual."
                metric.text = if (protocolArt.musicPlaying) "PLAYING" else "READY"
                metric.setTextColor(MiddesColors.music)
                primary.text = "ABRIR PLAYER"
                secondary.text = if (protocolArt.musicPlaying) "PAUSAR" else "PLAY"
                tertiary.text = "PRÓXIMA"
                secondary.setTextColor(MiddesColors.music)
                tertiary.setTextColor(MiddesColors.music)
            }
            "Noite" -> {
                code.text = "SCENE 03  //  NIGHT ENGINE"
                title.text = "NIGHT LAYER"
                subtitle.text = "Interface baixa, relógio noturno e filtro de luz quente.$protocolSuffix"
                metric.text = if (nightClock) SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                               else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                metric.setTextColor(MiddesColors.night)
                primary.text = "ENCERRAR"
                secondary.text = "ESCURECER"
                tertiary.text = if (nightWarm) "LUZ NORMAL" else "LUZ QUENTE"
                secondary.setTextColor(MiddesColors.night)
                tertiary.setTextColor(MiddesColors.night)
            }
            else -> {
                code.text = "SCENE  //  NORMAL"
                title.text = "MIDDES SYSTEM"
                subtitle.text = "Sistema principal."
                metric.text = "—"
                metric.setTextColor(accent)
                primary.text = "VOLTAR"
                secondary.text = "NEXA"
                tertiary.text = "FLOW"
                secondary.setTextColor(accent)
                tertiary.setTextColor(accent)
            }
        }

        primary.setOnClickListener { onPrimary() }
        secondary.setOnClickListener {
            when (value) {
                "Estudo" -> onSecondary()
                "Música" -> {
                    protocolArt.musicPlaying = !protocolArt.musicPlaying
                    protocolArt.invalidate()
                    onSecondary()
                    setScene(value, status, running)
                }
                "Noite" -> onSecondary()
                else -> onSecondary()
            }
        }
        tertiary.setOnClickListener {
            when (value) {
                "Estudo" -> {
                    protocolArt.taskDone = !protocolArt.taskDone
                    tertiary.text = if (protocolArt.taskDone) "TAREFA ✓" else "TAREFA"
                    protocolArt.invalidate()
                    animateAction(tertiary)
                }
                "Música" -> onTertiary()
                "Noite" -> {
                    nightWarm = !nightWarm
                    warmOverlay.background = android.graphics.drawable.ColorDrawable(
                        if (nightWarm) Color.argb(38, 255, 169, 92) else Color.argb(0, 255, 169, 92)
                    )
                    protocolArt.nightWarm = nightWarm
                    protocolArt.invalidate()
                    tertiary.text = if (nightWarm) "LUZ NORMAL" else "LUZ QUENTE"
                    animateAction(tertiary)
                }
                else -> onTertiary()
            }
        }

        rebuildApps(value)
    }

    fun performEnterAnimation(value: String) {
        animateScene(value)
    }

    fun performExitAnimation() {
        exitAnimated()
    }

    private fun rebuildApps(value: String) {
        appRow.removeAllViews()
        val accent = SceneManager.accent(value)
        packages(value).distinct().take(6).forEach { pkg ->
            val d = icon(pkg) ?: return@forEach
            val item = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener { onLaunch(pkg) }
            }
            item.addView(ImageView(context).apply {
                setImageDrawable(d)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(MiddesUi.dp(context, 5f), MiddesUi.dp(context, 5f), MiddesUi.dp(context, 5f), MiddesUi.dp(context, 5f))
                background = MiddesUi.rounded(context, Color.argb(16, Color.red(accent), Color.green(accent), Color.blue(accent)), 15f)
            }, LinearLayout.LayoutParams(MiddesUi.dp(context, 48f), MiddesUi.dp(context, 48f)))
            item.addView(MiddesUi.text(context, label(pkg).orEmpty(), 7.3f, MiddesColors.muted).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(MiddesUi.dp(context, 75f), MiddesUi.dp(context, 20f)))
            appRow.addView(item, LinearLayout.LayoutParams(MiddesUi.dp(context, 76f), MiddesUi.dp(context, 70f)).apply {
                setMargins(0, 0, MiddesUi.dp(context, 6f), 0)
            })
        }
    }

    private fun animateScene(value: String) {
        alpha = 0f
        translationY = when (value) {
            "Estudo" -> MiddesUi.dp(context, 22f).toFloat()
            "Música" -> 0f
            "Noite" -> -MiddesUi.dp(context, 16f).toFloat()
            else -> MiddesUi.dp(context, 10f).toFloat()
        }
        scaleX = 0.985f
        scaleY = 0.985f
        animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(
            when (value) {
                "Estudo" -> 420L
                "Música" -> 520L
                "Noite" -> 650L
                else -> 360L
            }
        ).start()
        protocolArt.start()
    }

    private fun animateAction(view: View) {
        view.animate().scaleX(1.06f).scaleY(1.06f).setDuration(90L).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
        }.start()
    }

    private fun exitAnimated() {
        protocolArt.stop()
        animate().alpha(0f).translationY(MiddesUi.dp(context, 20f).toFloat()).scaleX(0.98f).scaleY(0.98f)
            .setDuration(230L).withEndAction {
                onExit()
                alpha = 1f
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
            }.start()
    }

    fun showNightClock() {
        nightClock = !nightClock
        protocolArt.nightClock = nightClock
        protocolArt.invalidate()
    }

    override fun onDetachedFromWindow() {
        protocolArt.stop()
        super.onDetachedFromWindow()
    }

    private class ProtocolArtView(context: android.content.Context) : android.view.View(context) {
        var scene = "Normal"
        var studyRunning = false
        var taskDone = false
        var musicPlaying = false
        var nightWarm = false
        var nightClock = false
        var studyStatus = "25:00"
        private var phase = 0f
        private var active = false
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val tick = object : Runnable {
            override fun run() {
                if (!active) return
                phase += 0.05f
                invalidate()
                postDelayed(this, 32L)
            }
        }

        fun start() {
            active = true
            removeCallbacks(tick)
            post(tick)
        }

        fun stop() {
            active = false
            removeCallbacks(tick)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return
            val cx = w / 2f
            val cy = h / 2f

            when (scene) {
                "Estudo" -> drawStudy(canvas, cx, cy, w, h)
                "Música" -> drawMusic(canvas, cx, cy, w, h)
                "Noite" -> drawNight(canvas, cx, cy, w, h)
                else -> drawNormal(canvas, cx, cy)
            }
        }

        private fun drawStudy(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float) {
            val accent = MiddesColors.study
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(2f)
            paint.color = Color.argb(65, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawCircle(cx, cy, minOf(w, h) * 0.36f, paint)
            val total = 25f * 60f
            val remaining = parseTime()
            val progress = ((total - remaining) / total).coerceIn(0f, 1f)
            paint.color = Color.argb(210, Color.red(accent), Color.green(accent), Color.blue(accent))
            paint.strokeWidth = dp(4f)
            canvas.drawArc(RectF(cx - minOf(w, h) * 0.36f, cy - minOf(w, h) * 0.36f, cx + minOf(w, h) * 0.36f, cy + minOf(w, h) * 0.36f),
                -90f, 360f * progress, false, paint)
            paint.strokeWidth = dp(1f)
            paint.color = Color.argb(55, 255, 255, 255)
            for (i in 0 until 8) {
                val a = i * Math.PI / 4 + phase * 0.16
                canvas.drawLine(
                    cx + cos(a).toFloat() * dp(68f),
                    cy + sin(a).toFloat() * dp(68f),
                    cx + cos(a).toFloat() * dp(82f),
                    cy + sin(a).toFloat() * dp(82f),
                    paint
                )
            }
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(if (studyRunning) 155 else 70, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawCircle(cx, cy, dp(8f) + sin(phase * 2.0).toFloat() * dp(2f), paint)
            paint.color = Color.argb(95, 255, 255, 255)
            canvas.drawCircle(cx, cy, dp(26f), paint)
            paint.color = Color.argb(190, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawCircle(cx, cy, dp(5f), paint)
            if (taskDone) {
                paint.color = Color.argb(220, 106, 242, 178)
                canvas.drawCircle(cx + dp(70f), cy - dp(58f), dp(5f), paint)
            }
        }

        private fun drawMusic(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float) {
            val accent = MiddesColors.music
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(2f)
            val radius = minOf(w, h) * 0.30f
            paint.color = Color.argb(85, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawCircle(cx, cy, radius, paint)
            canvas.drawCircle(cx, cy, radius * 0.62f, paint)
            val rotation = phase * 28f
            paint.color = Color.argb(205, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawArc(RectF(cx - radius, cy - radius, cx + radius, cy + radius), rotation, 78f, false, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(32, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawCircle(cx, cy, radius * 0.57f, paint)
            paint.color = Color.argb(180, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawCircle(cx, cy, dp(12f), paint)

            val bars = 18
            val gap = w / (bars + 2)
            for (i in 0 until bars) {
                val x = gap * (i + 1)
                val wave = (sin(phase * 2.0 + i * 0.72) + 1.0) * 0.5
                val barH = dp(10f) + wave.toFloat() * dp(if (musicPlaying) 48f else 18f)
                paint.color = Color.argb(if (musicPlaying) 170 else 65, Color.red(accent), Color.green(accent), Color.blue(accent))
                canvas.drawRoundRect(RectF(x - dp(1.8f), h - barH, x + dp(1.8f), h), dp(2f), dp(2f), paint)
            }
        }

        private fun drawNight(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float) {
            val accent = MiddesColors.night
            paint.style = Paint.Style.FILL
            val moonR = minOf(w, h) * 0.22f
            paint.color = Color.argb(185, 232, 234, 255)
            canvas.drawCircle(cx, cy - dp(6f), moonR, paint)
            paint.color = Color.argb(235, 4, 5, 10)
            canvas.drawCircle(cx + moonR * 0.42f, cy - moonR * 0.20f, moonR * 0.88f, paint)

            for (i in 0 until 28) {
                val x = ((i * 47) % w.toInt()).toFloat()
                val y = ((i * 29 + 13) % maxOf(1, h.toInt())).toFloat()
                val twinkle = ((sin(phase * 1.7 + i) + 1.0) * 0.5).toFloat()
                paint.color = Color.argb((50 + 125 * twinkle).toInt(), 218, 221, 242)
                canvas.drawCircle(x, y, dp(if (i % 5 == 0) 1.5f else 0.8f), paint)
            }

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1.5f)
            paint.color = Color.argb(110, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawRoundRect(RectF(cx - w * 0.33f, h - dp(28f), cx + w * 0.33f, h - dp(14f)), dp(7f), dp(7f), paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(110, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawRoundRect(RectF(cx - w * 0.33f, h - dp(28f), cx - w * 0.02f, h - dp(14f)), dp(7f), dp(7f), paint)

            if (nightWarm) {
                paint.color = Color.argb(70, 255, 172, 94)
                canvas.drawCircle(cx + moonR * 1.65f, cy + moonR * 0.95f, dp(6f), paint)
            }
        }

        private fun drawNormal(canvas: Canvas, cx: Float, cy: Float) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1f)
            paint.color = Color.argb(55, 255, 255, 255)
            canvas.drawCircle(cx, cy, dp(60f), paint)
            canvas.drawCircle(cx, cy, dp(34f), paint)
        }

        private fun parseTime(): Float {
            return try {
                val pieces = studyStatus.split(":")
                val minutes = pieces.getOrNull(0)?.toIntOrNull() ?: 25
                val seconds = pieces.getOrNull(1)?.toIntOrNull() ?: 0
                (minutes * 60 + seconds).toFloat()
            } catch (_: Exception) {
                25f * 60f
            }
        }

        private fun dp(value: Float): Float =
            value * resources.displayMetrics.density
    }
}

package com.middes.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

class NexaView(
    context: Context,
    private val onClose: () -> Unit,
    private val onToggle: () -> Unit,
    private val onApps: () -> Unit,
    private val onFlow: () -> Unit,
    private val enabled: () -> Boolean,
    private val state: () -> NexaState
) : FrameLayout(context) {

    private val stateText = MiddesUi.text(context, "OFFLINE", 9.5f, MiddesColors.muted, true)
    private val transcript = MiddesUi.text(context, "Nenhum comando recente.", 11.5f, MiddesColors.muted)
    private val hint = MiddesUi.text(context, "", 9f, MiddesColors.muted)
    private val history = mutableListOf<String>()
    private var lastState: NexaState? = null
    private lateinit var core: MiddesCoreView
    private lateinit var signal: SignalView

    init {
        setBackgroundColor(Color.TRANSPARENT)
        build()
        refresh()
        alpha = 0f
        post {
            animate().alpha(1f).translationY(0f).setDuration(360L).start()
            if (::core.isInitialized) core.startIntro()
            if (::signal.isInitialized) signal.start()
        }
    }

    private fun build() {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 20f), MiddesUi.dp(context, 18f), MiddesUi.dp(context, 20f), MiddesUi.dp(context, 12f))
        }
        addView(content, LayoutParams(-1, -1))

        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(MiddesUi.text(context, "MIDDES", 11f, MiddesColors.white, true).apply {
            letterSpacing = 0.16f
        }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 24f), 1f))
        top.addView(MiddesUi.text(context, "VOICE CORE", 7.5f, MiddesColors.purpleBright, true).apply {
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(32, 185, 132, 255), 11f)
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 78f), MiddesUi.dp(context, 25f)))
        top.addView(MiddesUi.text(context, "×", 28f, MiddesColors.white).apply {
            gravity = Gravity.CENTER
            contentDescription = "Fechar NEXA"
            setOnClickListener { exitAnimated() }
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 40f), MiddesUi.dp(context, 40f)))
        content.addView(top)

        val titleRow = LinearLayout(context).apply { gravity = Gravity.BOTTOM }
        titleRow.addView(MiddesUi.text(context, "NEXA", 35f, MiddesColors.white, true).apply {
            letterSpacing = 0.15f
        }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 48f), 1f))
        titleRow.addView(MiddesUi.text(context, "AI / LOCAL CORE", 7.5f, MiddesColors.muted, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.10f
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 94f), MiddesUi.dp(context, 28f)))
        content.addView(titleRow, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 52f)).apply {
            setMargins(0, MiddesUi.dp(context, 10f), 0, 0)
        })

        val stage = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }
        core = MiddesCoreView(context, { state() }, enabled)
        core.setOnClickListener { onToggle() }
        core.contentDescription = "Ativar ou desativar NEXA"
        stage.addView(core, FrameLayout.LayoutParams(MiddesUi.dp(context, 205f), MiddesUi.dp(context, 205f), Gravity.CENTER))
        content.addView(stage, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 220f)).apply {
            setMargins(0, MiddesUi.dp(context, 6f), 0, 0)
        })

        stateText.gravity = Gravity.CENTER
        content.addView(stateText, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 24f)))

        hint.gravity = Gravity.CENTER
        content.addView(hint, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 28f)))

        signal = SignalView(context)
        content.addView(signal, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 44f)).apply {
            setMargins(0, MiddesUi.dp(context, 3f), 0, 0)
        })

        val logCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 14f), MiddesUi.dp(context, 11f), MiddesUi.dp(context, 14f), MiddesUi.dp(context, 8f))
            background = MiddesUi.rounded(context, Color.argb(70, 11, 13, 20), 18f, Color.argb(22, 255, 255, 255))
        }
        logCard.addView(MiddesUi.text(context, "NEXA LOG // RECENTE", 7.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.13f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 18f)))
        transcript.gravity = Gravity.CENTER_VERTICAL
        transcript.maxLines = 3
        logCard.addView(transcript, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 47f)))
        content.addView(logCard, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 77f)).apply {
            setMargins(0, MiddesUi.dp(context, 8f), 0, 0)
        })

        content.addView(MiddesUi.text(context, "COMANDOS RÁPIDOS", 7.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.13f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 19f)).apply {
            setMargins(0, MiddesUi.dp(context, 8f), 0, 0)
        })

        val commandRow = LinearLayout(context).apply { gravity = Gravity.CENTER }
        listOf("“abrir WhatsApp”", "“modo estudo”", "“modo gaming”").forEachIndexed { index, value ->
            commandRow.addView(commandHint(value), LinearLayout.LayoutParams(0, MiddesUi.dp(context, 39f), 1f).apply {
                if (index > 0) setMargins(MiddesUi.dp(context, 4f), 0, 0, 0)
            })
        }
        content.addView(commandRow, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 39f)))

        content.addView(View(context), LinearLayout.LayoutParams(1, 0, 1f))

        val nav = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(195, 7, 8, 13), 21f, Color.argb(25, 255, 255, 255))
        }
        nav.addView(navItem("APPS") { onApps() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 50f), 1f))
        nav.addView(navItem("FLOW") { onFlow() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 50f), 1f))
        nav.addView(navItem(if (enabled()) "DESATIVAR" else "ATIVAR") { onToggle() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 50f), 1f))
        content.addView(nav, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 56f)))
    }

    private fun commandHint(value: String): TextView =
        MiddesUi.text(context, value, 8f, MiddesColors.muted, true).apply {
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(22, 255, 255, 255), 13f)
        }

    private fun navItem(label: String, action: () -> Unit): TextView =
        MiddesUi.text(context, label, 8f, MiddesColors.muted, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.07f
            setOnClickListener { action() }
        }

    fun refresh() {
        val current = state()
        stateText.text = when (current) {
            NexaState.OFF -> "OFFLINE  //  VOICE LINK CLOSED"
            NexaState.READY -> "STANDBY  //  ESCUTANDO WAKE WORD"
            NexaState.LISTENING -> "LISTENING  //  OUVINDO"
            NexaState.PROCESSING -> "PROCESSING  //  ANALISANDO"
            NexaState.EXECUTING -> "EXECUTING  //  AÇÃO EM CURSO"
            NexaState.SPEAKING -> "SPEAKING  //  NEXA RESPONDENDO"
        }
        stateText.setTextColor(if (enabled()) MiddesColors.purpleBright else MiddesColors.muted)
        hint.text = when (current) {
            NexaState.LISTENING -> "Pode falar agora."
            NexaState.PROCESSING -> "Comando recebido. Validando."
            NexaState.EXECUTING -> "Executando a ação solicitada."
            NexaState.SPEAKING -> "Resposta em andamento."
            NexaState.OFF -> "Toque no núcleo para ativar."
            NexaState.READY -> "Diga “NEXA” e depois o comando."
        }
        if (history.isNotEmpty()) {
            transcript.text = history.takeLast(3).reversed().joinToString("\n") { "› " + it }
        }
        if (lastState != current) {
            lastState = current
            if (::core.isInitialized) core.refreshAnimation()
            if (::signal.isInitialized) signal.setState(current)
        } else if (::signal.isInitialized) {
            signal.setState(current)
        }
    }

    fun addCommand(value: String) {
        val stamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        history += stamp + "  " + value.trim()
        if (history.size > 8) history.removeAt(0)
        refresh()
    }

    private fun exitAnimated() {
        signal.stop()
        core.stopIntro()
        animate().alpha(0f).translationY(MiddesUi.dp(context, 16f).toFloat()).setDuration(220L).withEndAction {
            onClose()
            alpha = 1f
            translationY = 0f
        }.start()
    }

    override fun onDetachedFromWindow() {
        if (::signal.isInitialized) signal.stop()
        if (::core.isInitialized) core.stopIntro()
        super.onDetachedFromWindow()
    }

    private class SignalView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f }
        private var phase = 0f
        private var active = true
        private var currentState = NexaState.OFF
        private val tick = object : Runnable {
            override fun run() {
                if (!active) return
                phase += 0.13f
                invalidate()
                postDelayed(this, 32L)
            }
        }

        init { post(tick) }
        fun start() { active = true; removeCallbacks(tick); post(tick) }
        fun stop() { active = false; removeCallbacks(tick) }
        fun setState(value: NexaState) { currentState = value; invalidate() }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return
            val amp = when (currentState) {
                NexaState.LISTENING -> h * 0.38f
                NexaState.SPEAKING -> h * 0.32f
                NexaState.PROCESSING, NexaState.EXECUTING -> h * 0.22f
                else -> h * 0.09f
            }
            paint.color = Color.argb(85, Color.red(MiddesColors.purpleBright), Color.green(MiddesColors.purpleBright), Color.blue(MiddesColors.purpleBright))
            val path = android.graphics.Path()
            path.moveTo(0f, h / 2f)
            for (x in 0..w.toInt() step 5) {
                val y = h / 2f + sin(x * 0.055f + phase) * amp * sin((x / w) * Math.PI).toFloat()
                if (x == 0) path.moveTo(x.toFloat(), y) else path.lineTo(x.toFloat(), y)
            }
            canvas.drawPath(path, paint)
        }
    }
}

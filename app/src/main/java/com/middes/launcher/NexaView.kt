package com.middes.launcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.view.animation.LinearInterpolator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
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

    private val stateText = MiddesUi.text(context, "OFFLINE", 10f, MiddesColors.muted, true)
    private val core = MiddesUi.text(context, "NEXA", 24f, MiddesColors.white, true)
    private val transcript = MiddesUi.text(context, "Nenhum comando recente.", 12f, MiddesColors.muted)
    private val hint = MiddesUi.text(context, "", 9.5f, MiddesColors.muted)
    private val history = mutableListOf<String>()
    private var animatedState: NexaState? = null
    private var coreAnimator: ValueAnimator? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
        build()
        refresh()
    }

    private fun build() {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 22f), MiddesUi.dp(context, 24f), MiddesUi.dp(context, 22f), MiddesUi.dp(context, 16f))
        }
        addView(content, LayoutParams(-1, -1))

        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(MiddesUi.text(context, "MIDDES // VOICE CORE", 10f, MiddesColors.muted, true).apply {
            letterSpacing = 0.14f
        }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 28f), 1f))
        top.addView(MiddesUi.text(context, "×", 30f, MiddesColors.white).apply {
            gravity = Gravity.CENTER
            contentDescription = "Voltar"
            setOnClickListener { onClose() }
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 42f), MiddesUi.dp(context, 42f)))
        content.addView(top)

        content.addView(MiddesUi.text(context, "NEXA", 34f, MiddesColors.white, true).apply {
            letterSpacing = 0.19f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 46f)).apply {
            setMargins(0, MiddesUi.dp(context, 18f), 0, 0)
        })
        content.addView(MiddesUi.text(context, "NÚCLEO DE COMANDO", 9f, MiddesColors.muted, true).apply {
            letterSpacing = 0.20f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 24f)))

        val coreWrap = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }
        addRing(coreWrap, 238, 20)
        addRing(coreWrap, 208, 44)
        core.gravity = Gravity.CENTER
        core.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        core.setOnClickListener { onToggle() }
        core.background = MiddesUi.rounded(context, Color.argb(32, 185, 132, 255), 180f, Color.argb(135, 185, 132, 255))
        coreWrap.addView(core, FrameLayout.LayoutParams(MiddesUi.dp(context, 180f), MiddesUi.dp(context, 180f), Gravity.CENTER))
        content.addView(coreWrap, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 280f)))

        stateText.gravity = Gravity.CENTER
        content.addView(stateText, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 24f)))
        hint.gravity = Gravity.CENTER
        content.addView(hint, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 34f)))

        val logCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 16f), MiddesUi.dp(context, 14f), MiddesUi.dp(context, 16f), MiddesUi.dp(context, 14f))
            background = MiddesUi.rounded(context, Color.argb(105, 12, 12, 19), 22f, Color.argb(30, 255, 255, 255))
        }
        logCard.addView(MiddesUi.text(context, "ÚLTIMA ATIVIDADE", 8f, MiddesColors.muted, true), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 18f)))
        transcript.gravity = Gravity.CENTER_VERTICAL
        logCard.addView(transcript, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 48f)))
        content.addView(logCard, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 84f)).apply {
            setMargins(0, MiddesUi.dp(context, 12f), 0, 0)
        })

        content.addView(MiddesUi.text(context, "COMANDOS", 8f, MiddesColors.muted, true).apply {
            letterSpacing = 0.16f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 20f)).apply {
            setMargins(0, MiddesUi.dp(context, 14f), 0, MiddesUi.dp(context, 4f))
        })

        val commandRow = LinearLayout(context).apply { gravity = Gravity.CENTER }
        commandRow.addView(commandHint("“abrir WhatsApp”"), LinearLayout.LayoutParams(0, MiddesUi.dp(context, 42f), 1f).apply {
            setMargins(0, 0, MiddesUi.dp(context, 4f), 0)
        })
        commandRow.addView(commandHint("“modo estudo”"), LinearLayout.LayoutParams(0, MiddesUi.dp(context, 42f), 1f).apply {
            setMargins(MiddesUi.dp(context, 4f), 0, MiddesUi.dp(context, 4f), 0)
        })
        commandRow.addView(commandHint("“modo gaming”"), LinearLayout.LayoutParams(0, MiddesUi.dp(context, 42f), 1f).apply {
            setMargins(MiddesUi.dp(context, 4f), 0, 0, 0)
        })
        content.addView(commandRow)

        val nav = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(190, 8, 8, 14), 24f, Color.argb(28, 255, 255, 255))
        }
        nav.addView(navItem("APPS") { onApps() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 50f), 1f))
        nav.addView(navItem("FLOW") { onFlow() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 50f), 1f))
        nav.addView(navItem("NEXA") { onToggle() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 50f), 1f))
        content.addView(nav, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 56f)).apply {
            setMargins(0, MiddesUi.dp(context, 14f), 0, 0)
        })
    }

    private fun addRing(parent: FrameLayout, size: Int, alpha: Int) {
        val ring = TextView(context)
        ring.background = MiddesUi.rounded(context, Color.TRANSPARENT, size / 2f, Color.argb(alpha, 185, 132, 255))
        parent.addView(ring, FrameLayout.LayoutParams(MiddesUi.dp(context, size.toFloat()), MiddesUi.dp(context, size.toFloat()), Gravity.CENTER))
    }

    private fun commandHint(value: String): TextView =
        MiddesUi.text(context, value, 8.5f, MiddesColors.muted, true).apply {
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(34, 255, 255, 255), 14f)
        }

    private fun navItem(label: String, action: () -> Unit): TextView =
        MiddesUi.text(context, label, 8.5f, MiddesColors.muted, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
            setOnClickListener { action() }
        }

    fun refresh() {
        val currentState = state()
        stateText.text = when (currentState) {
            NexaState.OFF -> "OFFLINE"
            NexaState.READY -> "READY // AGUARDANDO WAKE WORD"
            NexaState.LISTENING -> "LISTENING // OUVINDO"
            NexaState.PROCESSING -> "PROCESSING // ANALISANDO COMANDO"
            NexaState.EXECUTING -> "EXECUTING // EXECUTANDO AÇÃO"
            NexaState.SPEAKING -> "SPEAKING // RESPOSTA DA NEXA"
        }
        stateText.setTextColor(if (enabled()) MiddesColors.purpleBright else MiddesColors.muted)
        core.text = if (enabled()) "NEXA" else "NEXA\nOFF"
        hint.text = when (currentState) {
            NexaState.LISTENING -> "Pode falar agora."
            NexaState.PROCESSING -> "Entendi. Processando seu comando."
            NexaState.EXECUTING -> "Executando no sistema."
            NexaState.SPEAKING -> "NEXA está respondendo."
            else -> if (enabled()) "Diga “NEXA” seguido do comando." else "Toque no núcleo para ativar a interface de voz."
        }
        if (history.isNotEmpty()) transcript.text = history.takeLast(3).reversed().joinToString("\n") { "› " + it }
        if (animatedState != currentState) {
            animatedState = currentState
            animateCore(currentState)
        }
    }

    private fun animateCore(currentState: NexaState) {
        coreAnimator?.cancel()
        core.clearAnimation()
        core.rotation = 0f
        core.scaleX = 1f
        core.scaleY = 1f
        core.alpha = 1f

        if (!enabled() || currentState == NexaState.OFF) return

        val duration = when (currentState) {
            NexaState.READY -> 1800L
            NexaState.LISTENING -> 650L
            NexaState.PROCESSING -> 850L
            NexaState.EXECUTING -> 600L
            NexaState.SPEAKING -> 500L
            NexaState.OFF -> 0L
        }
        if (duration == 0L) return

        coreAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val phase = animator.animatedValue as Float
                val wave = sin(phase * 2f * PI).toFloat()
                when (currentState) {
                    NexaState.READY -> {
                        val scale = 0.985f + (wave + 1f) * 0.0075f
                        core.scaleX = scale
                        core.scaleY = scale
                        core.alpha = 0.9f + (wave + 1f) * 0.05f
                    }
                    NexaState.LISTENING -> {
                        val scale = 0.97f + (wave + 1f) * 0.02f
                        core.scaleX = scale
                        core.scaleY = scale
                        core.alpha = 0.82f + (wave + 1f) * 0.09f
                    }
                    NexaState.PROCESSING -> {
                        core.rotation = phase * 360f
                        val scale = 0.975f + (wave + 1f) * 0.0175f
                        core.scaleX = scale
                        core.scaleY = scale
                    }
                    NexaState.EXECUTING -> {
                        core.rotation = -phase * 360f
                        val scale = 0.98f + (wave + 1f) * 0.025f
                        core.scaleX = scale
                        core.scaleY = scale
                    }
                    NexaState.SPEAKING -> {
                        val scale = 0.97f + (wave + 1f) * 0.025f
                        core.scaleX = scale
                        core.scaleY = scale
                        core.alpha = 0.9f + (wave + 1f) * 0.05f
                    }
                    NexaState.OFF -> Unit
                }
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        coreAnimator?.cancel()
        coreAnimator = null
        super.onDetachedFromWindow()
    }

    fun addCommand(value: String) {
        val stamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        history += stamp + "  " + value.trim()
        if (history.size > 8) history.removeAt(0)
        refresh()
    }
}
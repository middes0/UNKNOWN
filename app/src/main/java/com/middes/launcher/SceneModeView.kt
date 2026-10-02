package com.middes.launcher

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class SceneModeView(
    context: android.content.Context,
    private val onExit: () -> Unit,
    private val onNexa: () -> Unit,
    private val onPrimary: () -> Unit,
    private val onLaunch: (String) -> Unit,
    private val label: (String) -> String?,
    private val icon: (String) -> Drawable?,
    private val packages: (String) -> List<String>
) : FrameLayout(context) {

    private val title = MiddesUi.text(context, "", 27f, MiddesColors.white, true)
    private val code = MiddesUi.text(context, "", 8.5f, MiddesColors.muted, true)
    private val subtitle = MiddesUi.text(context, "", 10f, MiddesColors.muted)
    private val primary = MiddesUi.text(context, "", 9f, MiddesColors.purpleBright, true)
    private val metric = MiddesUi.text(context, "", 58f, MiddesColors.white)
    private val appRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        build()
    }

    private fun build() {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(MiddesUi.dp(context, 22f), MiddesUi.dp(context, 24f), MiddesUi.dp(context, 22f), MiddesUi.dp(context, 14f))
        }
        addView(content, LayoutParams(-1, -1))

        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(MiddesUi.text(context, "MIDDES // PRIVATE PROTOCOL", 9f, MiddesColors.muted, true).apply {
            letterSpacing = 0.13f
        }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 24f), 1f))
        top.addView(MiddesUi.text(context, "×", 30f, MiddesColors.white).apply {
            gravity = Gravity.CENTER
            contentDescription = "Fechar protocolo"
            setOnClickListener { onExit() }
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 42f), MiddesUi.dp(context, 42f)))
        content.addView(top)

        code.gravity = Gravity.CENTER_VERTICAL
        content.addView(code, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)).apply {
            setMargins(0, MiddesUi.dp(context, 26f), 0, 0)
        })
        title.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        content.addView(title, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 54f)))
        subtitle.gravity = Gravity.CENTER_VERTICAL
        content.addView(subtitle, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 40f)))

        metric.gravity = Gravity.CENTER
        content.addView(metric, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 92f)).apply {
            setMargins(0, MiddesUi.dp(context, 20f), 0, 0)
        })

        primary.gravity = Gravity.CENTER
        primary.setOnClickListener { onPrimary() }
        content.addView(primary, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 48f)))

        content.addView(MiddesUi.text(context, "ATALHOS DO PROTOCOLO", 8f, MiddesColors.muted, true).apply {
            letterSpacing = 0.15f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 20f)).apply {
            setMargins(0, MiddesUi.dp(context, 22f), 0, MiddesUi.dp(context, 4f))
        })

        content.addView(android.widget.HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(appRow, FrameLayout.LayoutParams(-2, MiddesUi.dp(context, 92f)))
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 92f)))
        content.addView(android.view.View(context), LinearLayout.LayoutParams(1, 0, 1f))

        val nav = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(185, 7, 7, 12), 24f, Color.argb(26, 255, 255, 255))
        }
        nav.addView(navItem("NORMAL") { onExit() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 54f), 1f))
        nav.addView(navItem("NEXA") { onNexa() }, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 54f), 1f))
        content.addView(nav, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 60f)))
    }

    private fun navItem(name: String, action: () -> Unit): TextView =
        MiddesUi.text(context, name, 8.5f, MiddesColors.muted, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
            setOnClickListener { action() }
        }

    fun setScene(value: String, status: String, running: Boolean) {
        appRow.removeAllViews()
        val accent = SceneManager.accent(value)
        code.setTextColor(accent)
        title.text = when (value) {
            "Estudo" -> "FOCUS PROTOCOL"
            "Música" -> "AUDIO MATRIX"
            "Noite" -> "NIGHT SYSTEM"
            else -> "MIDDES SYSTEM"
        }
        code.text = when (value) {
            "Estudo" -> "SCENE 01 // CONCENTRATION"
            "Música" -> "SCENE 02 // AUDIO"
            "Noite" -> "SCENE 03 // LOW LIGHT"
            else -> "SCENE // NORMAL"
        }
        subtitle.text = when (value) {
            "Estudo" -> "Ambiente isolado para foco e estudo."
            "Música" -> "Interface dedicada aos seus players."
            "Noite" -> "Brilho reduzido e presença mínima."
            else -> "Sistema principal."
        }
        metric.text = when (value) {
            "Estudo" -> status
            "Música" -> "AUDIO"
            "Noite" -> java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            else -> "—"
        }
        metric.setTextColor(accent)
        primary.text = when (value) {
            "Estudo" -> if (running) "PAUSAR FOCO" else "INICIAR FOCO"
            "Música" -> "ABRIR PLAYER"
            "Noite" -> "ENCERRAR MODO"
            else -> "VOLTAR"
        }
        primary.setTextColor(accent)
        primary.background = MiddesUi.rounded(
            context,
            Color.argb(36, Color.red(accent), Color.green(accent), Color.blue(accent)),
            16f,
            Color.argb(82, Color.red(accent), Color.green(accent), Color.blue(accent))
        )

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
            }, LinearLayout.LayoutParams(MiddesUi.dp(context, 55f), MiddesUi.dp(context, 58f)))
            item.addView(MiddesUi.text(context, label(pkg).orEmpty(), 8.5f, MiddesColors.muted).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(MiddesUi.dp(context, 78f), MiddesUi.dp(context, 28f)))
            appRow.addView(item, LinearLayout.LayoutParams(MiddesUi.dp(context, 80f), MiddesUi.dp(context, 88f)).apply {
                setMargins(0, 0, MiddesUi.dp(context, 8f), 0)
            })
        }
    }
}

package com.middes.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView

class MiddesFlowView(
    context: Context,
    private val onClose: () -> Unit,
    private val onLaunch: (String) -> Unit
) : FrameLayout(context) {

    private val orbit = FrameLayout(context)
    private var angle = 0.0
    private var running = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            angle = (angle + 0.0085) % (Math.PI * 2)
            positionOrbit()
            invalidate()
            postDelayed(this, 16L)
        }
    }

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.argb(238, 5, 4, 8))

        addView(MiddesUi.text(context, "MIDDES FLOW", 12f, MiddesColors.muted, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.17f
        }, LayoutParams(-1, MiddesUi.dp(context, 28f), Gravity.TOP).apply {
            topMargin = MiddesUi.dp(context, 22f)
        })

        orbit.clipChildren = false
        orbit.clipToPadding = false
        addView(orbit, LayoutParams(-1, 0, Gravity.TOP).apply {
            topMargin = MiddesUi.dp(context, 60f)
            bottomMargin = MiddesUi.dp(context, 80f)
        })

        addView(MiddesUi.text(context, "FLOW", 13f, MiddesColors.white, true).apply {
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(230, 19, 15, 28), 50f,
                Color.argb(170, MiddesColors.purpleBright))
            contentDescription = "Fechar Flow"
            setOnClickListener { onClose() }
        }, LayoutParams(MiddesUi.dp(context, 94f), MiddesUi.dp(context, 94f), Gravity.CENTER))

        addView(MiddesUi.text(context, "×", 28f, MiddesColors.text).apply {
            gravity = Gravity.CENTER
            contentDescription = "Fechar"
            setOnClickListener { onClose() }
        }, LayoutParams(MiddesUi.dp(context, 48f), MiddesUi.dp(context, 48f), Gravity.TOP or Gravity.END).apply {
            topMargin = MiddesUi.dp(context, 14f)
        })
    }

    fun setApps(apps: List<Pair<String, android.graphics.drawable.Drawable?>>) {
        orbit.removeAllViews()
        apps.take(8).forEach { pair ->
            val icon = pair.second ?: return@forEach
            val node = FrameLayout(context).apply {
                alpha = 0f
                setOnClickListener { onLaunch(pair.first) }
            }
            node.addView(ImageView(context).apply {
                setImageDrawable(icon)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f), MiddesUi.dp(context, 4f))
            }, FrameLayout.LayoutParams(MiddesUi.dp(context, 54f), MiddesUi.dp(context, 54f), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
            node.addView(MiddesUi.text(context, packageLabel(pair.first), 8.5f, MiddesColors.muted).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }, FrameLayout.LayoutParams(-1, MiddesUi.dp(context, 26f), Gravity.BOTTOM))
            orbit.addView(node, LayoutParams(MiddesUi.dp(context, 82f), MiddesUi.dp(context, 82f)))
            node.animate().alpha(1f).setDuration(260L).start()
        }
        orbit.post { positionOrbit() }
    }

    fun start() {
        running = true
        positionOrbit()
        removeCallbacks(ticker)
        post(ticker)
    }

    fun stop() {
        running = false
        removeCallbacks(ticker)
    }

    private fun positionOrbit() {
        if (orbit.width <= 0 || orbit.height <= 0 || orbit.childCount == 0) return
        val cx = orbit.width / 2f
        val cy = orbit.height / 2f
        val radius = kotlin.math.min(MiddesUi.dp(context, 116f).toFloat(), kotlin.math.min(orbit.width, orbit.height) * 0.36f)
        for (i in 0 until orbit.childCount) {
            val child = orbit.getChildAt(i)
            val a = angle + Math.PI * 2.0 * i / orbit.childCount
            child.translationX = cx + kotlin.math.cos(a).toFloat() * radius - child.width / 2f
            child.translationY = cy + kotlin.math.sin(a).toFloat() * radius - child.height / 2f
        }
    }

    private fun packageLabel(packageName: String): String =
        try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.')
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = MiddesUi.dp(context, 1f).toFloat()
            color = Color.argb(90, Color.red(MiddesColors.purpleBright), Color.green(MiddesColors.purpleBright), Color.blue(MiddesColors.purpleBright))
        }
        canvas.drawCircle(cx, cy, MiddesUi.dp(context, 116f).toFloat(), paint)
        paint.color = Color.argb(30, 255, 255, 255)
        canvas.drawCircle(cx, cy, MiddesUi.dp(context, 145f).toFloat(), paint)
    }
}
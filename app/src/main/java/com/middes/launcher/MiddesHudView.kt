package com.middes.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.roundToInt
import kotlin.math.sin

class MiddesHudView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private var accent = MiddesColors.purpleBright
    private var mode = "Normal"
    private var active = true

    init {
        setWillNotDraw(false)
        isClickable = false
    }

    fun setAccent(color: Int) {
        accent = color
        invalidate()
    }

    fun setMode(scene: String) {
        mode = scene
        accent = SceneManager.accent(scene)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        active = true
        post(frame)
    }

    override fun onDetachedFromWindow() {
        active = false
        removeCallbacks(frame)
        super.onDetachedFromWindow()
    }

    private val frame = object : Runnable {
        override fun run() {
            if (!active) return
            phase = (phase + 0.025f) % 1f
            invalidate()
            postDelayed(this, 32L)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val alphaBoost = when (mode) {
            "Noite" -> 0.55f
            "Música" -> 1.15f
            "Gaming" -> 1.25f
            "Estudo" -> 0.95f
            else -> 0.72f
        }
        val a = { value: Int -> (value * alphaBoost).roundToInt().coerceIn(0, 255) }
        val x = dp(14f).toFloat()
        val y = dp(28f).toFloat()
        val len = dp(22f).toFloat()

        paint.strokeWidth = dp(1f).toFloat()
        paint.color = Color.argb(a(80), Color.red(accent), Color.green(accent), Color.blue(accent))

        // Four HUD corner brackets: keeps the futuristic look without covering the content.
        canvas.drawLine(x, y, x + len, y, paint)
        canvas.drawLine(x, y, x, y + len, paint)
        canvas.drawLine(w - x, y, w - x - len, y, paint)
        canvas.drawLine(w - x, y, w - x, y + len, paint)
        canvas.drawLine(x, h - dp(70f), x + len, h - dp(70f), paint)
        canvas.drawLine(x, h - dp(70f), x, h - dp(70f) - len, paint)
        canvas.drawLine(w - x, h - dp(70f), w - x - len, h - dp(70f), paint)
        canvas.drawLine(w - x, h - dp(70f), w - x, h - dp(70f) - len, paint)

        paint.color = Color.argb(a(24), 255, 255, 255)
        canvas.drawLine(w * 0.18f, dp(30f), w * 0.34f, dp(30f), paint)
        canvas.drawLine(w * 0.66f, dp(30f), w * 0.82f, dp(30f), paint)

        val scanY = dp(48f).toFloat() + ((h - dp(120f).toFloat()) * phase)
        paint.color = Color.argb(a(12), Color.red(accent), Color.green(accent), Color.blue(accent))
        canvas.drawLine(w * 0.08f, scanY, w * 0.92f, scanY, paint)

        // Sparse moving data points around the perimeter.
        fill.color = Color.argb(a(80), Color.red(accent), Color.green(accent), Color.blue(accent))
        for (i in 0 until 10) {
            val px = w * (0.08f + (i / 9f) * 0.84f)
            val py = if (i % 2 == 0) dp(56f).toFloat() else h - dp(82f).toFloat()
            val pulse = ((sin(phase * Math.PI * 2.0 + i) + 1.0) * 0.5).toFloat()
            canvas.drawCircle(px, py, dp(0.8f).toFloat() + pulse * dp(0.9f).toFloat(), fill)
        }

        // Mode-specific micro-markers.
        when (mode) {
            "Estudo" -> {
                paint.color = Color.argb(a(55), Color.red(MiddesColors.study), Color.green(MiddesColors.study), Color.blue(MiddesColors.study))
                paint.style = Paint.Style.STROKE
                canvas.drawRect(RectF(w * 0.07f, h * 0.27f, w * 0.11f, h * 0.30f), paint)
            }
            "Música" -> {
                paint.color = Color.argb(a(65), Color.red(MiddesColors.music), Color.green(MiddesColors.music), Color.blue(MiddesColors.music))
                canvas.drawArc(RectF(w * 0.83f, h * 0.25f, w * 0.93f, h * 0.35f), phase * 360f, 70f, false, paint)
            }
            "Noite" -> {
                paint.color = Color.argb(a(45), 210, 214, 240)
                canvas.drawCircle(w * 0.87f, h * 0.20f, dp(2f).toFloat(), paint)
            }
            "Gaming" -> {
                paint.color = Color.argb(a(75), Color.red(MiddesColors.gaming), Color.green(MiddesColors.gaming), Color.blue(MiddesColors.gaming))
                canvas.drawLine(w * 0.84f, h * 0.78f, w * 0.93f, h * 0.78f, paint)
                canvas.drawLine(w * 0.885f, h * 0.73f, w * 0.885f, h * 0.83f, paint)
            }
        }
        paint.style = Paint.Style.STROKE
    }

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density).roundToInt()
}

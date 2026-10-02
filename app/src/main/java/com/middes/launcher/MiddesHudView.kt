package com.middes.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class MiddesHudView(context: Context) : View(context) {
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f).toFloat()
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private var accent = MiddesColors.purpleBright
    private var active = true

    init {
        setWillNotDraw(false)
        isClickable = false
    }

    fun setAccent(color: Int) {
        accent = color
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
            phase += 0.018f
            invalidate()
            postDelayed(this, 32L)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cx = w * 0.5f
        val cy = h * 0.42f
        val maxR = minOf(w, h) * 0.38f

        ringPaint.color = Color.argb(55, Color.red(accent), Color.green(accent), Color.blue(accent))
        ringPaint.strokeWidth = dp(1f).toFloat()
        canvas.drawCircle(cx, cy, maxR, ringPaint)
        ringPaint.color = Color.argb(24, 255, 255, 255)
        canvas.drawCircle(cx, cy, maxR * 0.72f, ringPaint)
        canvas.drawCircle(cx, cy, maxR * 0.46f, ringPaint)

        ringPaint.color = Color.argb(34, Color.red(accent), Color.green(accent), Color.blue(accent))
        ringPaint.strokeWidth = dp(2f).toFloat()
        canvas.drawArc(cx - maxR * 1.02f, cy - maxR * 1.02f, cx + maxR * 1.02f, cy + maxR * 1.02f,
            phase * 57.3f, 62f, false, ringPaint)
        canvas.drawArc(cx - maxR * 0.80f, cy - maxR * 0.80f, cx + maxR * 0.80f, cy + maxR * 0.80f,
            -phase * 42f, 38f, false, ringPaint)

        ringPaint.color = Color.argb(28, 255, 255, 255)
        ringPaint.strokeWidth = dp(0.7f).toFloat()
        canvas.drawLine(w * 0.08f, cy, w * 0.92f, cy, ringPaint)
        canvas.drawLine(cx, h * 0.09f, cx, h * 0.78f, ringPaint)

        dotPaint.color = Color.argb(85, Color.red(accent), Color.green(accent), Color.blue(accent))
        for (i in 0 until 28) {
            val a = i * (Math.PI * 2.0 / 28.0) + phase * 0.22
            val r = maxR * if (i % 2 == 0) 1.055f else 0.995f
            val x = cx + cos(a).toFloat() * r
            val y = cy + sin(a).toFloat() * r
            canvas.drawCircle(x, y, if (i % 2 == 0) dp(1.3f).toFloat() else dp(0.7f).toFloat(), dotPaint)
        }

        val tick = dp(18f).toFloat()
        ringPaint.color = Color.argb(45, 255, 255, 255)
        ringPaint.strokeWidth = dp(1f).toFloat()
        canvas.drawLine(dp(18f).toFloat(), dp(36f).toFloat(), dp(18f).toFloat() + tick, dp(36f).toFloat(), ringPaint)
        canvas.drawLine(w - dp(18f).toFloat(), dp(36f).toFloat(), w - dp(18f).toFloat() - tick, dp(36f).toFloat(), ringPaint)
        canvas.drawLine(dp(18f).toFloat(), h - dp(92f).toFloat(), dp(18f).toFloat() + tick, h - dp(92f).toFloat(), ringPaint)
        canvas.drawLine(w - dp(18f).toFloat(), h - dp(92f).toFloat(), w - dp(18f).toFloat() - tick, h - dp(92f).toFloat(), ringPaint)
    }

    private fun dp(value: Float): Int =
        kotlin.math.round(value * resources.displayMetrics.density).toInt()
}

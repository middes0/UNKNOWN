package com.middes.launcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

class MiddesCoreView(
    context: Context,
    private val stateProvider: () -> NexaState,
    private val enabledProvider: () -> Boolean,
    private val accentProvider: () -> Int = { MiddesColors.purpleBright }
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private var phase = 0f
    private var attached = false
    private var introAnimator: ValueAnimator? = null
    private val pulse = object : Runnable {
        override fun run() {
            if (!attached) return
            phase = (phase + 0.035f) % 1f
            invalidate()
            postDelayed(this, 28L)
        }
    }

    init {
        setWillNotDraw(false)
        isClickable = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        post(pulse)
    }

    override fun onDetachedFromWindow() {
        attached = false
        removeCallbacks(pulse)
        introAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    fun startIntro() {
        introAnimator?.cancel()
        alpha = 0f
        scaleX = 0.82f
        scaleY = 0.82f
        introAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 380L
            interpolator = LinearInterpolator()
            addUpdateListener { a ->
                val v = a.animatedValue as Float
                alpha = v
                scaleX = 0.82f + 0.18f * v
                scaleY = scaleX
            }
            start()
        }
    }

    fun stopIntro() {
        introAnimator?.cancel()
        introAnimator = null
        alpha = 1f
        scaleX = 1f
        scaleY = 1f
    }

    fun refreshAnimation() {
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val base = minOf(width, height) * 0.47f
        val accent = accentProvider()
        val enabled = enabledProvider()
        val state = stateProvider()
        val intensity = when (state) {
            NexaState.OFF -> 0.25f
            NexaState.READY -> 0.58f
            NexaState.LISTENING -> 0.95f
            NexaState.PROCESSING -> 1.0f
            NexaState.EXECUTING -> 0.9f
            NexaState.SPEAKING -> 0.82f
        } * if (enabled) 1f else 0.55f

        val wave = ((sin(phase * Math.PI * 2.0) + 1.0) * 0.5).toFloat()
        val outer = base * (0.78f + wave * 0.025f)
        val middle = base * 0.61f
        val inner = base * 0.40f

        paint.color = withAlpha(accent, (32 + 50 * intensity).toInt())
        paint.strokeWidth = dp(1f)
        canvas.drawCircle(cx, cy, outer, paint)
        paint.color = Color.argb(42, 255, 255, 255)
        canvas.drawCircle(cx, cy, middle, paint)
        paint.color = withAlpha(accent, (56 + 70 * intensity).toInt())
        canvas.drawCircle(cx, cy, inner, paint)

        val sweep = when (state) {
            NexaState.PROCESSING -> 260f
            NexaState.EXECUTING -> -220f
            NexaState.LISTENING -> 115f
            NexaState.SPEAKING -> 155f
            else -> 78f
        }
        val rotation = phase * 360f * if (state == NexaState.EXECUTING) -1f else 1f
        paint.color = withAlpha(accent, (80 + 110 * intensity).toInt())
        paint.strokeWidth = dp(2f)
        canvas.drawArc(
            RectF(cx - outer, cy - outer, cx + outer, cy + outer),
            rotation,
            sweep,
            false,
            paint
        )
        paint.color = withAlpha(accent, (55 + 85 * intensity).toInt())
        paint.strokeWidth = dp(1.3f)
        canvas.drawArc(
            RectF(cx - middle, cy - middle, cx + middle, cy + middle),
            -rotation * 0.7f,
            sweep * 0.62f,
            false,
            paint
        )

        val nodes = 24
        fill.color = withAlpha(accent, (70 + 105 * intensity).toInt())
        for (i in 0 until nodes) {
            val a = i * (Math.PI * 2.0 / nodes) + phase * 0.6
            val radius = outer * if (i % 3 == 0) 1.018f else 0.99f
            val x = cx + cos(a).toFloat() * radius
            val y = cy + sin(a).toFloat() * radius
            canvas.drawCircle(x, y, dp(if (i % 3 == 0) 1.6f else 0.7f), fill)
        }

        // Geometric "core" instead of a big text button.
        val coreRadius = base * 0.27f
        fill.color = Color.argb((18 + 30 * intensity).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent))
        canvas.drawCircle(cx, cy, coreRadius * (1.0f + wave * 0.035f), fill)
        paint.color = withAlpha(accent, (120 + 85 * intensity).toInt())
        paint.strokeWidth = dp(1.2f)
        canvas.drawCircle(cx, cy, coreRadius, paint)

        paint.color = Color.argb(130, 255, 255, 255)
        paint.strokeWidth = dp(0.8f)
        canvas.drawLine(cx - coreRadius * 0.55f, cy, cx + coreRadius * 0.55f, cy, paint)
        canvas.drawLine(cx, cy - coreRadius * 0.55f, cx, cy + coreRadius * 0.55f, paint)

        for (i in 0 until 4) {
            val a = rotation * Math.PI / 180.0 + i * Math.PI / 2.0
            val x1 = cx + cos(a).toFloat() * coreRadius * 1.26f
            val y1 = cy + sin(a).toFloat() * coreRadius * 1.26f
            val x2 = cx + cos(a).toFloat() * coreRadius * 1.52f
            val y2 = cy + sin(a).toFloat() * coreRadius * 1.52f
            paint.color = withAlpha(accent, 125)
            paint.strokeWidth = dp(1.5f)
            canvas.drawLine(x1, y1, x2, y2, paint)
        }

        fill.color = Color.argb(230, 245, 248, 255)
        canvas.drawCircle(cx, cy, dp(4.5f), fill)
        fill.color = withAlpha(accent, 210)
        canvas.drawCircle(cx, cy, dp(2.5f), fill)
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density
}

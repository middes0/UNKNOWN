package com.middes.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.ImageView
import android.widget.TextView

object MiddesColors {
    val background = Color.rgb(5, 5, 8)
    val surface = Color.rgb(14, 12, 20)
    val surfaceRaised = Color.rgb(20, 17, 28)
    val text = Color.rgb(226, 223, 233)
    val muted = Color.rgb(143, 138, 156)
    val purpleBright = Color.rgb(190, 132, 250)
    val study = Color.rgb(117, 141, 225)
    val music = Color.rgb(214, 103, 181)
    val night = Color.rgb(117, 89, 175)
    val gaming = Color.rgb(176, 91, 235)
}

object MiddesUi {
    fun dp(context: Context, value: Float): Int =
        kotlin.math.round(value * context.resources.displayMetrics.density).toInt()

    fun rounded(context: Context, color: Int, radius: Float = 22f, strokeColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(context, radius).toFloat()
            if (strokeColor != null) setStroke(dp(context, 1f), strokeColor)
        }

    fun text(context: Context, value: String, size: Float, color: Int = MiddesColors.text, bold: Boolean = false): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

    fun iconButton(context: Context, glyph: String, description: String, action: () -> Unit): TextView =
        text(context, glyph, 22f, MiddesColors.text, true).apply {
            gravity = Gravity.CENTER
            contentDescription = description
            background = rounded(context, Color.argb(60, 255, 255, 255), 18f)
            setOnClickListener { action() }
        }

    fun appIcon(context: Context, drawable: android.graphics.drawable.Drawable, size: Int = 52): ImageView =
        ImageView(context).apply {
            setImageDrawable(drawable)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(context, 3f), dp(context, 3f), dp(context, 3f), dp(context, 3f))
            layoutParams = android.view.ViewGroup.LayoutParams(dp(context, size.toFloat()), dp(context, size.toFloat()))
        }
}
package com.middes.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.ImageView
import android.widget.TextView

object MiddesColors {
    val white = Color.rgb(239, 243, 255)
    val background = Color.rgb(3, 4, 8)
    val surface = Color.rgb(9, 10, 16)
    val surfaceRaised = Color.rgb(15, 16, 25)
    val text = Color.rgb(221, 228, 242)
    val muted = Color.rgb(122, 133, 153)
    val purpleBright = Color.rgb(185, 132, 255)
    val study = Color.rgb(105, 176, 255)
    val music = Color.rgb(224, 105, 205)
    val night = Color.rgb(132, 104, 202)
    val gaming = Color.rgb(194, 88, 255)
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
            includeFontPadding = true
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

    fun iconButton(context: Context, glyph: String, description: String, action: () -> Unit): TextView =
        text(context, glyph, 21f, MiddesColors.text, true).apply {
            gravity = Gravity.CENTER
            contentDescription = description
            background = rounded(context, Color.argb(40, 255, 255, 255), 17f, Color.argb(30, 255, 255, 255))
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

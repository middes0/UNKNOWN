package com.middes.launcher

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var clock: TextView
    private lateinit var appsContainer: LinearLayout
    private lateinit var search: EditText
    private lateinit var drawer: LinearLayout

    private val bg = Color.rgb(10, 10, 12)
    private val panel = Color.rgb(24, 24, 28)
    private val white = Color.WHITE
    private val gray = Color.rgb(160, 160, 168)
    private val accent = Color.rgb(255, 196, 55)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        buildHome()
    }

    override fun onResume() {
        super.onResume()
        if (::clock.isInitialized) updateClock()
        if (::drawer.isInitialized && drawer.visibility == View.VISIBLE) loadApps(search.text.toString())
    }

    private fun buildHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20, 22, 20, 16)
            setBackgroundColor(bg)
        }

        clock = TextView(this).apply {
            textSize = 52f
            setTextColor(white)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        root.addView(clock, LinearLayout.LayoutParams(-1, 72))

        val date = TextView(this).apply {
            textSize = 14f
            setTextColor(gray)
            gravity = Gravity.CENTER
            text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
                .format(Date()).replaceFirstChar { it.uppercase() }
        }
        root.addView(date, LinearLayout.LayoutParams(-1, 30))

        val spacer = Space(this)
        root.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))

        val message = TextView(this).apply {
            text = "Middes"
            textSize = 26f
            setTextColor(white)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(message, LinearLayout.LayoutParams(-1, 45))

        val subtitle = TextView(this).apply {
            text = "Seu espaço. Seus aplicativos."
            textSize = 14f
            setTextColor(gray)
            gravity = Gravity.CENTER
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, 30))

        val drawerButton = TextView(this).apply {
            text = "☷\nAplicativos"
            textSize = 15f
            setTextColor(white)
            gravity = Gravity.CENTER
            setBackgroundColor(panel)
            setOnClickListener { openDrawer() }
        }
        root.addView(drawerButton, LinearLayout.LayoutParams(170, 72).apply {
            setMargins(0, 24, 0, 16)
        })

        val dock = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(panel)
        }

        val home = TextView(this).apply {
            text = "⌂"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(accent)
        }
        dock.addView(home, LinearLayout.LayoutParams(64, 54))

        val apps = TextView(this).apply {
            text = "☷"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(white)
            setOnClickListener { openDrawer() }
        }
        dock.addView(apps, LinearLayout.LayoutParams(64, 54))

        root.addView(dock, LinearLayout.LayoutParams(-1, 58))

        drawer = buildDrawer()
        drawer.visibility = View.GONE
        root.addView(drawer, LinearLayout.LayoutParams(-1, 0, 0f))

        setContentView(root)
        updateClock()
    }

    private fun buildDrawer(): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val back = TextView(this).apply {
            text = "‹"
            textSize = 36f
            setTextColor(white)
            gravity = Gravity.CENTER
            setOnClickListener { closeDrawer() }
        }
        top.addView(back, LinearLayout.LayoutParams(48, 52))

        val title = TextView(this).apply {
            text = "Aplicativos"
            textSize = 21f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(title, LinearLayout.LayoutParams(0, 52, 1f))
        container.addView(top)

        search = EditText(this).apply {
            hint = "Pesquisar aplicativos"
            setHintTextColor(Color.rgb(120, 120, 128))
            setTextColor(white)
            textSize = 15f
            setSingleLine(true)
            setPadding(18, 0, 18, 0)
            setBackgroundColor(panel)
        }
        container.addView(search, LinearLayout.LayoutParams(-1, 52).apply {
            setMargins(0, 8, 0, 8)
        })

        val scroll = ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        appsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4, 0, 12)
        }
        scroll.addView(appsContainer, android.widget.FrameLayout.LayoutParams(-1, -2))
        container.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                loadApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(e: android.text.Editable?) = Unit
        })

        return container
    }

    private fun openDrawer() {
        val root = findViewById<LinearLayout>(android.R.id.content)
        val content = root.getChildAt(0) as? LinearLayout ?: return

        for (i in 0 until content.childCount) {
            val child = content.getChildAt(i)
            if (child === drawer) {
                content.removeViewAt(i)
                content.addView(drawer, i, LinearLayout.LayoutParams(-1, 0, 1f))
                break
            }
        }

        drawer.visibility = View.VISIBLE
        search.setText("")
        loadApps("")
    }

    private fun closeDrawer() {
        drawer.visibility = View.GONE
    }

    private fun updateClock() {
        clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun loadApps(query: String) {
        appsContainer.removeAllViews()
        val pm = packageManager

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .filter { it.loadLabel(pm).toString().contains(query, ignoreCase = true) }
            .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.getDefault()) }

        var row: LinearLayout? = null

        apps.forEachIndexed { index, info ->
            if (index % 4 == 0) {
                row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                }
                appsContainer.addView(row, LinearLayout.LayoutParams(-1, 104))
            }

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                setPadding(2, 4, 2, 4)
                setOnClickListener {
                    val launch = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setClassName(info.activityInfo.packageName, info.activityInfo.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(launch)
                }
            }

            val icon = ImageView(this).apply {
                setImageDrawable(info.loadIcon(pm))
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
            item.addView(icon, LinearLayout.LayoutParams(58, 58))

            val label = TextView(this).apply {
                text = info.loadLabel(pm)
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(white)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            item.addView(label, LinearLayout.LayoutParams(-1, 30))
            row?.addView(item, LinearLayout.LayoutParams(0, 100, 1f))
        }
    }
}

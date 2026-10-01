package com.middes.launcher

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private lateinit var appCount: TextView
    private lateinit var search: EditText

    private val bg = Color.rgb(10, 10, 12)
    private val panel = Color.rgb(24, 24, 28)
    private val white = Color.WHITE
    private val gray = Color.rgb(160, 160, 168)
    private val accent = Color.rgb(255, 196, 55)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::clock.isInitialized) {
            updateClock()
            loadApps(search.text.toString())
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 12)
            setBackgroundColor(bg)
        }

        clock = TextView(this).apply {
            textSize = 48f
            setTextColor(white)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        root.addView(clock, LinearLayout.LayoutParams(-1, 66))

        val date = TextView(this).apply {
            textSize = 14f
            setTextColor(gray)
            gravity = Gravity.CENTER
        }
        root.addView(date, LinearLayout.LayoutParams(-1, 30))

        search = EditText(this).apply {
            hint = "Pesquisar aplicativos"
            setHintTextColor(Color.rgb(120, 120, 128))
            setTextColor(white)
            textSize = 15f
            setSingleLine(true)
            setPadding(18, 0, 18, 0)
            setBackgroundColor(panel)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, 52).apply {
            setMargins(0, 14, 0, 10)
        })

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "Aplicativos"
            textSize = 19f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        }
        header.addView(title, LinearLayout.LayoutParams(0, 38, 1f))

        appCount = TextView(this).apply {
            textSize = 12f
            setTextColor(gray)
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        header.addView(appCount, LinearLayout.LayoutParams(0, 38, 1f))
        root.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        appsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4, 0, 8)
        }
        scroll.addView(appsContainer, ScrollView.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                loadApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(e: Editable?) = Unit
        })

        val dock = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(panel)
        }

        val home = TextView(this).apply {
            text = "⌂"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(accent)
            setOnClickListener {
                search.setText("")
                search.clearFocus()
            }
        }
        dock.addView(home, LinearLayout.LayoutParams(58, 48))
        root.addView(dock, LinearLayout.LayoutParams(-1, 54).apply {
            setMargins(0, 6, 0, 0)
        })

        setContentView(root)

        updateClock()
        date.text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
            .format(Date()).replaceFirstChar { it.uppercase() }

        loadApps("")
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

        appCount.text = if (query.isBlank()) {
            "${apps.size} apps"
        } else {
            "${apps.size} encontrados"
        }

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
                setPadding(1, 2, 1, 0)
            }
            item.addView(label, LinearLayout.LayoutParams(-1, 30))

            row?.addView(item, LinearLayout.LayoutParams(0, 100, 1f))
        }
    }
}

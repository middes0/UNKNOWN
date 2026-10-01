package com.middes.launcher

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var clock: TextView
    private lateinit var appsGrid: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::clock.isInitialized) updateClock()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 48, 28, 24)
            setBackgroundColor(android.graphics.Color.rgb(11, 11, 13))
        }

        clock = TextView(this).apply {
            textSize = 52f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(clock, LinearLayout.LayoutParams(-1, 90))

        val date = TextView(this).apply {
            textSize = 16f
            setTextColor(android.graphics.Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        root.addView(date, LinearLayout.LayoutParams(-1, 42))

        val search = EditText(this).apply {
            hint = "Pesquisar aplicativos"
            setHintTextColor(android.graphics.Color.GRAY)
            setTextColor(android.graphics.Color.WHITE)
            setSingleLine(true)
            setPadding(28, 0, 28, 0)
            setBackgroundColor(android.graphics.Color.rgb(28, 28, 32))
        }
        root.addView(search, LinearLayout.LayoutParams(-1, 58).apply { setMargins(0, 20, 0, 22) })

        appsGrid = GridLayout(this).apply {
            columnCount = 4
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = true
        }
        val scroll = ScrollView(this).apply { addView(appsGrid) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = loadApps(s?.toString().orEmpty())
            override fun afterTextChanged(e: android.text.Editable?) = Unit
        })

        val dock = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(10, 10, 10, 10)
            setBackgroundColor(android.graphics.Color.rgb(28, 28, 32))
        }
        val home = TextView(this).apply {
            text = "⌂"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { search.setText(""); search.clearFocus() }
        }
        dock.addView(home, LinearLayout.LayoutParams(70, 58))
        root.addView(dock, LinearLayout.LayoutParams(-1, 78))

        setContentView(root)
        updateClock()
        date.text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date()).replaceFirstChar { it.uppercase() }
        loadApps("")
    }

    private fun updateClock() {
        clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun loadApps(query: String) {
        appsGrid.removeAllViews()
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .filter { it.loadLabel(pm).toString().contains(query, ignoreCase = true) }
            .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.getDefault()) }

        for (info in apps) {
            val button = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(6, 10, 6, 10)
                setOnClickListener {
                    val launch = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setClassName(info.activityInfo.packageName, info.activityInfo.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(launch)
                }
            }
            val icon = ImageView(this).apply { setImageDrawable(info.loadIcon(pm)) }
            button.addView(icon, LinearLayout.LayoutParams(58, 58))
            val label = TextView(this).apply {
                text = info.loadLabel(pm)
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(android.graphics.Color.WHITE)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            button.addView(label, LinearLayout.LayoutParams(82, 34))
            appsGrid.addView(button, GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            })
        }
    }
}

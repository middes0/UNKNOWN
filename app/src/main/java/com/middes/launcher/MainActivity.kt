package com.middes.launcher

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private lateinit var appCount: TextView
    private lateinit var search: EditText

    private val bg = Color.rgb(10, 10, 12)
    private val panel = Color.rgb(24, 24, 28)
    private val panel2 = Color.rgb(32, 32, 37)
    private val white = Color.WHITE
    private val gray = Color.rgb(165, 165, 172)
    private val accent = Color.rgb(255, 196, 55)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
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
            setPadding(24, 38, 24, 18)
            setBackgroundColor(bg)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        clock = TextView(this).apply {
            textSize = 58f
            setTextColor(white)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        top.addView(clock, LinearLayout.LayoutParams(-1, 78))

        val date = TextView(this).apply {
            textSize = 15f
            setTextColor(gray)
            gravity = Gravity.CENTER
        }
        top.addView(date, LinearLayout.LayoutParams(-1, 34))

        val title = TextView(this).apply {
            text = "Aplicativos"
            textSize = 20f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        }
        top.addView(title, LinearLayout.LayoutParams(-1, 42).apply {
            setMargins(4, 18, 4, 0)
        })

        appCount = TextView(this).apply {
            textSize = 12f
            setTextColor(gray)
        }
        top.addView(appCount, LinearLayout.LayoutParams(-1, 24).apply {
            setMargins(4, 0, 4, 4)
        })

        search = EditText(this).apply {
            hint = "  Pesquisar aplicativos"
            setHintTextColor(Color.rgb(125, 125, 132))
            setTextColor(white)
            textSize = 15f
            setSingleLine(true)
            setPadding(18, 0, 18, 0)
            setBackgroundColor(panel)
        }
        top.addView(search, LinearLayout.LayoutParams(-1, 54).apply {
            setMargins(0, 6, 0, 14)
        })

        root.addView(top)

        appsGrid = GridLayout(this).apply {
            columnCount = 4
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setPadding(0, 0, 0, 8)
            addView(appsGrid, ViewGroup.LayoutParams(-1, -2))
        }
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
            setPadding(12, 8, 12, 8)
            setBackgroundColor(panel)
        }

        val home = TextView(this).apply {
            text = "⌂"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(accent)
            setOnClickListener {
                search.setText("")
                search.clearFocus()
            }
        }
        dock.addView(home, LinearLayout.LayoutParams(64, 52))
        root.addView(dock, LinearLayout.LayoutParams(-1, 68).apply {
            setMargins(0, 8, 0, 0)
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
        appsGrid.removeAllViews()
        val pm = packageManager

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = pm.queryIntentActivities(launcherIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .filter {
                it.loadLabel(pm).toString().contains(query, ignoreCase = true)
            }
            .sortedBy {
                it.loadLabel(pm).toString().lowercase(Locale.getDefault())
            }

        appCount.text = if (query.isBlank()) {
            "${apps.size} aplicativos disponíveis"
        } else {
            "${apps.size} resultado(s)"
        }

        for (info in apps) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(4, 8, 4, 12)
                setOnClickListener {
                    val launch = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setClassName(info.activityInfo.packageName, info.activityInfo.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(launch)
                }
            }

            val iconFrame = FrameLayout(this).apply {
                setBackgroundColor(panel2)
                setPadding(9, 9, 9, 9)
            }
            val icon = ImageView(this).apply {
                setImageDrawable(info.loadIcon(pm))
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
            iconFrame.addView(icon, FrameLayout.LayoutParams(54, 54, Gravity.CENTER))
            item.addView(iconFrame, LinearLayout.LayoutParams(72, 72))

            val label = TextView(this).apply {
                text = info.loadLabel(pm)
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(white)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(2, 5, 2, 0)
            }
            item.addView(label, LinearLayout.LayoutParams(82, 30))

            appsGrid.addView(item, GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            })
        }
    }
}

package com.middes.launcher

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var root: FrameLayout
    private lateinit var homeView: LinearLayout
    private lateinit var drawerView: LinearLayout
    private lateinit var appsContainer: LinearLayout
    private lateinit var search: EditText
    private lateinit var clock: TextView

    private val bg = Color.rgb(8, 9, 11)
    private val card = Color.rgb(19, 20, 24)
    private val card2 = Color.rgb(27, 28, 33)
    private val white = Color.WHITE
    private val gray = Color.rgb(150, 153, 162)
    private val accent = Color.rgb(255, 196, 55)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::clock.isInitialized) updateClock()
        if (::drawerView.isInitialized && drawerView.visibility == View.VISIBLE) {
            loadApps(search.text.toString())
        }
    }

    override fun onBackPressed() {
        if (::drawerView.isInitialized && drawerView.visibility == View.VISIBLE) closeDrawer()
        else super.onBackPressed()
    }

    private fun rounded(color: Int, radius: Float = 28f): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }

    private fun buildUi() {
        root = FrameLayout(this).apply { setBackgroundColor(bg) }
        homeView = buildHome()
        drawerView = buildDrawer()

        root.addView(homeView, FrameLayout.LayoutParams(-1, -1))
        drawerView.visibility = View.GONE
        root.addView(drawerView, FrameLayout.LayoutParams(-1, -1))

        setContentView(root)
        updateClock()
    }

    private fun buildHome(): LinearLayout {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 26, 22, 14)
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = TextView(this).apply {
            text = "MIDDES"
            textSize = 14f
            letterSpacing = 0.22f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        }
        header.addView(brand, LinearLayout.LayoutParams(0, 44, 1f))

        val status = TextView(this).apply {
            text = "●  ONLINE"
            textSize = 10f
            letterSpacing = 0.08f
            setTextColor(Color.rgb(135, 190, 145))
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(status, LinearLayout.LayoutParams(-2, 44))
        view.addView(header)

        val timeBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 42, 0, 0)
        }

        clock = TextView(this).apply {
            textSize = 66f
            setTextColor(white)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        timeBlock.addView(clock, LinearLayout.LayoutParams(-1, 82))

        val date = TextView(this).apply {
            textSize = 13f
            setTextColor(gray)
            gravity = Gravity.CENTER
            letterSpacing = 0.04f
            text = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
                .format(Date()).replaceFirstChar { it.uppercase() }
        }
        timeBlock.addView(date, LinearLayout.LayoutParams(-1, 32))
        view.addView(timeBlock)

        view.addView(Space(this), LinearLayout.LayoutParams(1, 0, 1f))

        val section = TextView(this).apply {
            text = "SEUS APLICATIVOS"
            textSize = 11f
            letterSpacing = 0.12f
            setTextColor(gray)
        }
        view.addView(section, LinearLayout.LayoutParams(-1, 30))

        val open = TextView(this).apply {
            text = "Todos os aplicativos   ›"
            textSize = 15f
            setTextColor(white)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 0, 20, 0)
            background = rounded(card, 24f)
            setOnClickListener { openDrawer() }
        }
        view.addView(open, LinearLayout.LayoutParams(-1, 58).apply {
            setMargins(0, 0, 0, 14)
        })

        val dock = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = rounded(card, 24f)
            setPadding(8, 4, 8, 4)
        }

        val home = TextView(this).apply {
            text = "HOME"
            textSize = 10f
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setTextColor(accent)
            typeface = Typeface.DEFAULT_BOLD
        }
        dock.addView(home, LinearLayout.LayoutParams(0, 52, 1f))

        val apps = TextView(this).apply {
            text = "APPS"
            textSize = 10f
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setTextColor(gray)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { openDrawer() }
        }
        dock.addView(apps, LinearLayout.LayoutParams(0, 52, 1f))

        view.addView(dock, LinearLayout.LayoutParams(-1, 60))
        return view
    }

    private fun buildDrawer(): LinearLayout {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 14)
            setBackgroundColor(bg)
        }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }

        val back = TextView(this).apply {
            text = "‹"
            textSize = 38f
            setTextColor(white)
            gravity = Gravity.CENTER
            setOnClickListener { closeDrawer() }
        }
        top.addView(back, LinearLayout.LayoutParams(48, 52))

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "Aplicativos"
            textSize = 23f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        }
        titleBox.addView(title, LinearLayout.LayoutParams(-1, 32))
        val sub = TextView(this).apply {
            text = "Tudo instalado neste dispositivo"
            textSize = 11f
            setTextColor(gray)
        }
        titleBox.addView(sub, LinearLayout.LayoutParams(-1, 22))
        top.addView(titleBox, LinearLayout.LayoutParams(0, 52, 1f))
        view.addView(top)

        search = EditText(this).apply {
            hint = "Buscar aplicativo"
            hintTextColor = Color.rgb(115, 118, 125)
            setTextColor(white)
            textSize = 14f
            setSingleLine(true)
            setPadding(20, 0, 20, 0)
            background = rounded(card2, 22f)
        }
        view.addView(search, LinearLayout.LayoutParams(-1, 52).apply {
            setMargins(0, 14, 0, 12)
        })

        val scroll = ScrollView(this)
        appsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4, 0, 12)
        }
        scroll.addView(appsContainer, FrameLayout.LayoutParams(-1, -2))
        view.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                loadApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(e: android.text.Editable?) = Unit
        })

        return view
    }

    private fun openDrawer() {
        homeView.visibility = View.GONE
        drawerView.visibility = View.VISIBLE
        search.setText("")
        loadApps("")
    }

    private fun closeDrawer() {
        drawerView.visibility = View.GONE
        homeView.visibility = View.VISIBLE
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

        apps.forEachIndexed { index, info ->
            val row = if (index % 4 == 0) {
                LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                }.also {
                    appsContainer.addView(it, LinearLayout.LayoutParams(-1, 108))
                }
            } else {
                appsContainer.getChildAt(appsContainer.childCount - 1) as LinearLayout
            }

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                setPadding(4, 6, 4, 6)
                setOnClickListener {
                    val launch = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setClassName(info.activityInfo.packageName, info.activityInfo.name)
                    }
                    try { startActivity(launch) } catch (_: Exception) {}
                }
            }

            val iconBox = FrameLayout(this).apply {
                background = rounded(card, 18f)
                addView(ImageView(this@MainActivity).apply {
                    setImageDrawable(info.loadIcon(pm))
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setPadding(10, 10, 10, 10)
                }, FrameLayout.LayoutParams(58, 58, Gravity.CENTER))
            }
            item.addView(iconBox, LinearLayout.LayoutParams(70, 66))

            val label = TextView(this).apply {
                text = info.loadLabel(pm)
                textSize = 10.5f
                gravity = Gravity.CENTER
                setTextColor(white)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            item.addView(label, LinearLayout.LayoutParams(-1, 28))
            row.addView(item, LinearLayout.LayoutParams(0, 104, 1f))
        }
    }
}

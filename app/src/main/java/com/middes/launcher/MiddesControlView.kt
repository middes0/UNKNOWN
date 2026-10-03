package com.middes.launcher

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MiddesControlView(
    context: android.content.Context,
    private val onBack: () -> Unit,
    private val onMode: (String) -> Unit,
    private val onNexa: () -> Unit,
    private val onApps: () -> Unit,
    private val onFlow: () -> Unit,
    private val onProfiles: () -> Unit,
    private val onSettings: () -> Unit,
    private val onWallpaper: () -> Unit,
    private val onQuickAction: (String) -> Unit,
    private val snapshotProvider: () -> NexaSystemContext.Snapshot
) : ScrollView(context) {

    private val list = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            MiddesUi.dp(context, 18f),
            MiddesUi.dp(context, 18f),
            MiddesUi.dp(context, 18f),
            MiddesUi.dp(context, 30f)
        )
    }

    private val modeValue = MiddesUi.text(context, "NORMAL", 21f, MiddesColors.purpleBright, true)
    private val modeDetail = MiddesUi.text(context, "", 9.5f, MiddesColors.muted)
    private val nexaValue = MiddesUi.text(context, "", 12f, MiddesColors.purpleBright, true)
    private val batteryValue = MiddesUi.text(context, "—", 12f, MiddesColors.text, true)
    private val focusValue = MiddesUi.text(context, "Sem foco ativo", 12f, MiddesColors.text, true)
    private val volumeValue = MiddesUi.text(context, "—", 12f, MiddesColors.text, true)
    private val dndValue = MiddesUi.text(context, "—", 11f, MiddesColors.text, true)
    private val wallpaperValue = MiddesUi.text(context, "—", 11f, MiddesColors.text, true)

    init {
        setBackgroundColor(MiddesColors.background)
        addView(list, LayoutParams(-1, -2))
        build()
        refresh()
    }

    private fun build() {
        val header = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(MiddesUi.text(context, "‹", 38f, MiddesColors.white).apply {
            gravity = Gravity.CENTER
            contentDescription = "Voltar"
            setOnClickListener { onBack() }
        }, LinearLayout.LayoutParams(MiddesUi.dp(context, 46f), MiddesUi.dp(context, 52f)))

        val title = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        title.addView(
            MiddesUi.text(context, "MIDDES CONTROL", 23f, MiddesColors.white, true),
            LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 30f))
        )
        title.addView(
            MiddesUi.text(context, "Centro de controle do launcher", 10.5f, MiddesColors.muted),
            LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 20f))
        )
        header.addView(title, LinearLayout.LayoutParams(0, MiddesUi.dp(context, 52f), 1f))
        list.addView(header)

        val hero = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                MiddesUi.dp(context, 20f),
                MiddesUi.dp(context, 18f),
                MiddesUi.dp(context, 20f),
                MiddesUi.dp(context, 18f)
            )
            background = MiddesUi.rounded(
                context,
                Color.argb(50, 185, 132, 255),
                24f,
                Color.argb(65, 185, 132, 255)
            )
        }
        hero.addView(MiddesUi.text(context, "MODO ATUAL", 8f, MiddesColors.muted, true).apply {
            letterSpacing = 0.14f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 18f)))
        hero.addView(modeValue, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 31f)))
        hero.addView(modeDetail, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 26f)))

        val modeScroller = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val modes = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listOf("Normal", "Estudo", "Música", "Noite", "Gaming").forEach { scene ->
            modes.addView(
                modeButton(scene),
                LinearLayout.LayoutParams(
                    MiddesUi.dp(context, 106f),
                    MiddesUi.dp(context, 42f)
                ).apply { setMargins(0, 0, MiddesUi.dp(context, 6f), 0) }
            )
        }
        modeScroller.addView(modes, android.view.ViewGroup.LayoutParams(-2, MiddesUi.dp(context, 48f)))
        hero.addView(modeScroller, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 48f)).apply {
            setMargins(0, MiddesUi.dp(context, 5f), 0, 0)
        })
        list.addView(hero, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 175f)).apply {
            setMargins(0, MiddesUi.dp(context, 14f), 0, 0)
        })

        list.addView(MiddesUi.text(context, "STATUS", 8.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.15f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)).apply {
            setMargins(MiddesUi.dp(context, 2f), MiddesUi.dp(context, 17f), 0, MiddesUi.dp(context, 6f))
        })

        val grid = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        grid.addView(metricRow(
            metric("NEXA", nexaValue),
            metric("BATERIA", batteryValue)
        ), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 76f)))
        grid.addView(metricRow(
            metric("FOCO", focusValue),
            metric("VOLUME", volumeValue)
        ), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 76f)).apply {
            topMargin = MiddesUi.dp(context, 6f)
        })
        grid.addView(metricRow(
            metric("NÃO PERTURBE", dndValue),
            metric("WALLPAPER", wallpaperValue)
        ), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 76f)).apply {
            topMargin = MiddesUi.dp(context, 6f)
        })
        list.addView(grid)

        list.addView(MiddesUi.text(context, "ACESSO RÁPIDO", 8.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.15f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)).apply {
            setMargins(MiddesUi.dp(context, 2f), MiddesUi.dp(context, 17f), 0, MiddesUi.dp(context, 6f))
        })

        val quickGrid = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        quickGrid.addView(actionRow(
            actionButton("◌", "NEXA") { onNexa() },
            actionButton("⌕", "APLICATIVOS") { onApps() }
        ), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 70f)))
        quickGrid.addView(actionRow(
            actionButton("✦", "FLOW") { onFlow() },
            actionButton("☷", "PERFIS") { onProfiles() }
        ), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 70f)).apply {
            topMargin = MiddesUi.dp(context, 6f)
        })
        quickGrid.addView(actionRow(
            actionButton("⚙", "SISTEMA") { onSettings() },
            actionButton("▣", "WALLPAPER") { onWallpaper() }
        ), LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 70f)).apply {
            topMargin = MiddesUi.dp(context, 6f)
        })
        list.addView(quickGrid)

        list.addView(MiddesUi.text(context, "CONTROLES DO APARELHO", 8.5f, MiddesColors.muted, true).apply {
            letterSpacing = 0.15f
        }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 22f)).apply {
            setMargins(MiddesUi.dp(context, 2f), MiddesUi.dp(context, 17f), 0, MiddesUi.dp(context, 6f))
        })

        val systemBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = MiddesUi.rounded(context, Color.argb(62, 12, 13, 20), 20f, Color.argb(24, 255, 255, 255))
            setPadding(MiddesUi.dp(context, 5f), MiddesUi.dp(context, 5f), MiddesUi.dp(context, 5f), MiddesUi.dp(context, 5f))
        }
        listOf(
            "wifi" to "WI-FI",
            "bluetooth" to "BT",
            "sound" to "SOM",
            "display" to "TELA"
        ).forEach { (id, label) ->
            systemBar.addView(
                MiddesUi.text(context, label, 8f, MiddesColors.text, true).apply {
                    gravity = Gravity.CENTER
                    background = MiddesUi.rounded(context, Color.argb(24, 255, 255, 255), 14f)
                    setOnClickListener { onQuickAction(id) }
                },
                LinearLayout.LayoutParams(0, MiddesUi.dp(context, 52f), 1f).apply {
                    setMargins(MiddesUi.dp(context, 2f), 0, MiddesUi.dp(context, 2f), 0)
                }
            )
        }
        list.addView(systemBar, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 62f)))
    }

    private fun modeButton(scene: String): TextView =
        MiddesUi.text(context, scene.uppercase(), 8.5f, SceneManager.accent(scene), true).apply {
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, Color.argb(26, 255, 255, 255), 15f, Color.argb(28, 255, 255, 255))
            setOnClickListener { onMode(scene) }
        }

    private fun metric(title: String, valueView: TextView): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(MiddesUi.dp(context, 13f), 0, MiddesUi.dp(context, 13f), 0)
            background = MiddesUi.rounded(context, MiddesColors.surfaceRaised, 18f)
            addView(MiddesUi.text(context, title, 7.5f, MiddesColors.muted, true),
                LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 18f)))
            addView(valueView, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 26f)))
        }

    private fun metricRow(left: View, right: View): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(left, LinearLayout.LayoutParams(0, -1, 1f))
            addView(right, LinearLayout.LayoutParams(0, -1, 1f).apply {
                marginStart = MiddesUi.dp(context, 6f)
            })
        }

    private fun actionButton(glyph: String, label: String, action: () -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = MiddesUi.rounded(context, MiddesColors.surfaceRaised, 19f, Color.argb(24, 255, 255, 255))
            setOnClickListener { action() }
            contentDescription = label
            addView(MiddesUi.text(context, glyph, 20f, MiddesColors.purpleBright, true).apply {
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 34f)))
            addView(MiddesUi.text(context, label, 7.2f, MiddesColors.muted, true).apply {
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(-1, MiddesUi.dp(context, 21f)))
        }

    private fun actionRow(left: View, right: View): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(left, LinearLayout.LayoutParams(0, -1, 1f))
            addView(right, LinearLayout.LayoutParams(0, -1, 1f).apply {
                marginStart = MiddesUi.dp(context, 6f)
            })
        }

    fun refresh() {
        val snapshot = try {
            snapshotProvider()
        } catch (_: Exception) {
            return
        }

        val accent = SceneManager.accent(snapshot.scene)
        modeValue.text = snapshot.scene.uppercase()
        modeValue.setTextColor(accent)

        modeDetail.text = when (snapshot.scene) {
            "Estudo" -> if (snapshot.studyRunning) {
                "Foco em andamento • " + formatSeconds(snapshot.studyRemainingSeconds)
            } else {
                "Foco pausado • " + formatSeconds(snapshot.studyRemainingSeconds)
            }
            "Música" -> "Controles de reprodução disponíveis no ambiente música."
            "Noite" -> "Ambiente noturno com brilho reduzido."
            "Gaming" -> "Ambiente dedicado para seus jogos e atalhos."
            else -> "Ambiente principal do MIDDES."
        }

        nexaValue.text = when (snapshot.nexaState) {
            NexaState.OFF -> "OFFLINE"
            NexaState.READY -> "STANDBY"
            NexaState.LISTENING -> "OUVINDO"
            NexaState.PROCESSING -> "PROCESSANDO"
            NexaState.EXECUTING -> "EXECUTANDO"
            NexaState.SPEAKING -> "RESPONDENDO"
        }
        nexaValue.setTextColor(if (snapshot.nexaState == NexaState.OFF) MiddesColors.muted else MiddesColors.purpleBright)

        batteryValue.text = if (snapshot.batteryPercent >= 0) {
            snapshot.batteryPercent.toString() + "%"
        } else {
            "INDISPONÍVEL"
        }

        focusValue.text = if (snapshot.scene == "Estudo") {
            formatSeconds(snapshot.studyRemainingSeconds) + if (snapshot.studyRunning) " • ATIVO" else " • PAUSADO"
        } else {
            "NÃO ATIVO"
        }

        volumeValue.text = if (snapshot.volumePercent >= 0) {
            snapshot.volumePercent.toString() + "%"
        } else {
            "INDISPONÍVEL"
        }

        dndValue.text = snapshot.dndStatus
        wallpaperValue.text = if (snapshot.wallpaperPersonalized) "PERSONALIZADO" else "PADRÃO"

        val hero = list.getChildAt(1) as? LinearLayout
        hero?.background = MiddesUi.rounded(
            context,
            Color.argb(48, Color.red(accent), Color.green(accent), Color.blue(accent)),
            24f,
            Color.argb(62, Color.red(accent), Color.green(accent), Color.blue(accent))
        )
    }

    private fun formatSeconds(total: Int): String {
        val safe = total.coerceAtLeast(0)
        return "%02d:%02d".format(Locale.US, safe / 60, safe % 60)
    }
}

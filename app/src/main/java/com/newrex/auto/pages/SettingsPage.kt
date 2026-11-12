// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.pages

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import com.newrex.auto.PlayerHub
import com.newrex.auto.R
import com.newrex.auto.data.ObdManager
import com.newrex.auto.data.Prefs
import kotlinx.coroutines.launch
import com.newrex.auto.ui.CarUi
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Page
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.dpf
import com.newrex.auto.ui.flp
import com.newrex.auto.ui.icon
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.onTap
import com.newrex.auto.ui.panel
import com.newrex.auto.ui.pill
import com.newrex.auto.ui.pressable
import com.newrex.auto.ui.rounded

/** Tema, ekran görünümü, oynatma kalitesi ve sürüş ayarları. */
class SettingsPage(ui: CarUi) : Page(ui) {
    override val title = "Ayarlar"

    override fun build(): View = with(ctx) {
        val current = Palette.accent
        val colors = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        Palette.accents.forEachIndexed { i, c ->
            colors.addView(swatch(c, i == Prefs.accentIndex) { Prefs.accentIndex = i }, lp(dp(48), dp(48)).margins(r = dp(12)))
        }
        Palette.extraAccents.forEach { c ->
            colors.addView(swatch(c, Prefs.accentIndex < 0 && c == current) { Prefs.customAccent = c },
                lp(dp(48), dp(48)).margins(r = dp(12)))
        }
        val customAccent = Prefs.accentIndex < 0 && current !in Palette.extraAccents
        colors.addView(customSwatch(if (customAccent) current else null) {
            ui.push(ColorPickerPage(ui, "Özel tema rengi", current, 1f) { Prefs.customAccent = it })
        }, lp(dp(48), dp(48)))

        val bgs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        Palette.backgrounds.forEach { c ->
            val shown = if (c == 0) Palette.BG_TOP else c
            bgs.addView(swatch(shown, Prefs.bgColor == c, text = if (c == 0) "Oto" else null) { Prefs.bgColor = c },
                lp(dp(48), dp(48)).margins(r = dp(12)))
        }
        val customBg = Prefs.bgColor != 0 && Prefs.bgColor !in Palette.backgrounds
        bgs.addView(customSwatch(if (customBg) Palette.safeBackground(Prefs.bgColor) else null) {
            val start = Palette.safeBackground(Prefs.bgColor).takeIf { it != 0 } ?: Palette.BG_TOP
            ui.push(ColorPickerPage(ui, "Özel arka plan", start, Palette.MAX_BG_VALUE) { Prefs.bgColor = it })
        }, lp(dp(48), dp(48)))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(24))

            addView(section("Tema rengi", HorizontalScrollView(this@with).apply {
                isHorizontalScrollBarEnabled = false
                addView(colors)
            }))
            addView(section("Arka plan", HorizontalScrollView(this@with).apply {
                isHorizontalScrollBarEnabled = false
                addView(bgs)
            }))

            addView(section("TV Görüntü Kalitesi", LinearLayout(this@with).apply {
                orientation = LinearLayout.VERTICAL
                val row = LinearLayout(this@with).apply { orientation = LinearLayout.HORIZONTAL }
                PlayerHub.QUALITIES.forEachIndexed { i, (n, _) ->
                    row.addView(pill(n, selected = i == Prefs.tvQuality) { Prefs.tvQuality = i; ui.pop(); ui.push(SettingsPage(ui)) },
                        lp(WRAP, dp(44)).margins(r = dp(10)))
                }
                addView(row)
                addView(label("Otomatik: yayının en iyi kalitesi. Düşük seçersen telefon daha az ısınır, daha az internet harcar. " +
                    "Değişiklik açık kanala hemen uygulanır.", 13f, Palette.TEXT_FAINT, lines = 3), lp(MATCH, WRAP).margins(t = dp(10)))
            }))

            // Sesli Karşılama
            addView(section("Sesli Karşılama", LinearLayout(this@with).apply {
                orientation = LinearLayout.VERTICAL
                val toggleLabel = label("", 15f, bold = true)
                fun updateVoice() {
                    val on = Prefs.voiceWelcome
                    toggleLabel.text = if (on) "Sesli karşılama açık: \"İyi yolculuklar ${Prefs.userName}\"" else "Sesli karşılama kapalı"
                    toggleLabel.setTextColor(if (on) Palette.GREEN else Palette.TEXT_DIM)
                }
                updateVoice()
                addView(toggleLabel, lp(MATCH, WRAP))

                val btnRow = LinearLayout(this@with).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(pill(if (Prefs.voiceWelcome) "Kapat" else "Aç") {
                        Prefs.voiceWelcome = !Prefs.voiceWelcome
                        updateVoice()
                    }, lp(WRAP, dp(40)).margins(r = dp(8), t = dp(8)))

                    addView(pill("Test Et (Dinle)") {
                        com.newrex.auto.VoiceWelcome.trigger(ctx)
                    }, lp(WRAP, dp(40)).margins(t = dp(8)))
                }
                addView(btnRow, lp(MATCH, WRAP))

                addView(label("Araca bağlandığınızda günün saatine göre Türkçe sesli karşılama yapar.",
                    12f, Palette.TEXT_FAINT, lines = 2), lp(MATCH, WRAP).margins(t = dp(6)))
            }))

            addView(section("Araç Profili & Plaka", LinearLayout(this@with).apply {
                orientation = LinearLayout.VERTICAL
                val carRow = LinearLayout(this@with).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(icon(R.drawable.ic_car, 24, Palette.accent), lp(dp(24), dp(24)).margins(r = dp(10)))
                    val textCol = LinearLayout(this@with).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(label(Prefs.carModel, 16f, bold = true))
                        addView(label("Aktif 3D / Kokpit Modeli", 12f, Palette.TEXT_DIM))
                    }
                    addView(textCol, lp(0, WRAP, 1f))

                    // Türk Plakası Rozeti
                    val plateBox = LinearLayout(this@with).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = rounded(android.graphics.Color.WHITE, dpf(4), dp(1), 0xFF888888.toInt())
                        setPadding(0, 0, dp(6), 0)

                        val trBadge = label("TR", 9f, android.graphics.Color.WHITE, bold = true).apply {
                            background = rounded(0xFF003399.toInt(), dpf(3))
                            setPadding(dp(4), dp(2), dp(4), dp(2))
                        }
                        addView(trBadge, lp(WRAP, MATCH))

                        val plateText = label(Prefs.carPlate, 11f, android.graphics.Color.BLACK, bold = true).apply {
                            typeface = android.graphics.Typeface.MONOSPACE
                            setPadding(dp(6), dp(2), 0, dp(2))
                        }
                        addView(plateText, lp(WRAP, WRAP))
                    }
                    addView(plateBox, lp(WRAP, WRAP))
                }
                addView(carRow, lp(MATCH, WRAP))

                val quickModels = listOf("Toyota C-HR (2025)", "Toyota Corolla", "Fiat Egea", "Renault Megane")
                val modelPills = LinearLayout(this@with).apply {
                    orientation = LinearLayout.HORIZONTAL
                    quickModels.forEach { m ->
                        addView(pill(m, selected = Prefs.carModel == m) {
                            Prefs.carModel = m
                            ui.pop(); ui.push(SettingsPage(ui))
                        }, lp(WRAP, dp(38)).margins(r = dp(8), t = dp(10)))
                    }
                }
                addView(HorizontalScrollView(this@with).apply {
                    isHorizontalScrollBarEnabled = false
                    addView(modelPills)
                }, lp(MATCH, WRAP))
            }))

            addView(section("OBD-II CAN-Bus Teşhis & Telemetri", LinearLayout(this@with).apply {
                orientation = LinearLayout.VERTICAL
                val obdState = ObdManager.state.value
                val isConnected = obdState == ObdManager.ConnectionState.CONNECTED
                val statusText = when (obdState) {
                    ObdManager.ConnectionState.CONNECTED -> "Bağlı ● (Gerçek CAN-Bus Aktif)"
                    ObdManager.ConnectionState.CONNECTING -> "Bağlanıyor..."
                    ObdManager.ConnectionState.INITIALIZING -> "Protokol Başlatılıyor..."
                    ObdManager.ConnectionState.ERROR -> "Bağlantı Hatası ⚠"
                    ObdManager.ConnectionState.DISCONNECTED -> "Bağlı Değil ○"
                }
                val statusColor = when (obdState) {
                    ObdManager.ConnectionState.CONNECTED -> Palette.GREEN
                    ObdManager.ConnectionState.CONNECTING, ObdManager.ConnectionState.INITIALIZING -> Palette.ORANGE
                    ObdManager.ConnectionState.ERROR -> Palette.RED
                    ObdManager.ConnectionState.DISCONNECTED -> Palette.TEXT_DIM
                }
                addView(label(statusText, 14f, statusColor, bold = true))

                val devices = ObdManager.refreshPairedDevices(this@with)
                if (devices.isEmpty()) {
                    addView(label("Eşleşmiş Bluetooth cihazı bulunamadı. Telefon Bluetooth ayarlarından ELM327 veya OBD2 adaptörünüzü eşleştirin.",
                        12f, Palette.TEXT_FAINT, lines = 3), lp(MATCH, WRAP).margins(t = dp(6)))
                } else {
                    addView(label("Cihaz seçin ve bağlanın:", 12f, Palette.TEXT_DIM), lp(MATCH, WRAP).margins(t = dp(8)))
                    val devRow = LinearLayout(this@with).apply { orientation = LinearLayout.HORIZONTAL }
                    devices.forEach { dev ->
                        @android.annotation.SuppressLint("MissingPermission")
                        val name = dev.name ?: dev.address
                        val isSel = ObdManager.selectedDevice.value?.address == dev.address && isConnected
                        devRow.addView(pill(name, selected = isSel) {
                            if (isSel) ObdManager.disconnect() else ObdManager.connect(ctx, dev)
                            ui.pop(); ui.push(SettingsPage(ui))
                        }, lp(WRAP, dp(38)).margins(r = dp(8), t = dp(6)))
                    }
                    addView(HorizontalScrollView(this@with).apply {
                        isHorizontalScrollBarEnabled = false
                        addView(devRow)
                    }, lp(MATCH, WRAP))
                }

                if (isConnected) {
                    val dtcBtnRow = LinearLayout(this@with).apply {
                        orientation = LinearLayout.HORIZONTAL
                        addView(pill("🔍 Arıza Kodlarını Tara (DTC)") {
                            scope.launch {
                                val codes = ObdManager.readDtcCodes()
                                if (codes.isEmpty()) ui.toast("Arıza kodu bulunamadı. Sistem temiz! 🎉")
                                else ui.toast("Bulunan Kodlar: ${codes.joinToString(", ")}", long = true)
                            }
                        }, lp(WRAP, dp(40)).margins(r = dp(8), t = dp(10)))

                        addView(pill("🧹 Hata Kodlarını Sil", res = R.drawable.ic_build) {
                            scope.launch {
                                val ok = ObdManager.clearDtcCodes()
                                if (ok) ui.toast("Motor arıza lambası ve kodlar sıfırlandı.")
                                else ui.toast("Kodlar sıfırlanamadı.")
                            }
                        }, lp(WRAP, dp(40)).margins(t = dp(10)))
                    }
                    addView(dtcBtnRow, lp(MATCH, WRAP))
                }
            }))

            addView(section("Hıza Duyarlı Akıllı Ses", LinearLayout(this@with).apply {
                orientation = LinearLayout.VERTICAL
                val on = Prefs.speedVolumeCompensate
                val volLabel = label(
                    if (on) "Hıza göre otomatik ses yükseltme: AÇIK" else "Hıza göre ses yükseltme: KAPALI",
                    15f,
                    if (on) Palette.GREEN else Palette.TEXT_DIM,
                    bold = true
                )
                addView(volLabel, lp(MATCH, WRAP))
                addView(pill(
                    if (on) "Devre dışı bırak" else "Etkinleştir",
                    res = R.drawable.ic_speed
                ) {
                    Prefs.speedVolumeCompensate = !Prefs.speedVolumeCompensate
                    ui.pop(); ui.push(SettingsPage(ui))
                }, lp(WRAP, dp(44)).margins(t = dp(10)))
                addView(label(
                    "Otomobil hızlandıkça (yol ve rüzgar sesi arttıkça) radyo/TV sesini hafifçe yükseltir; " +
                        "kırmızı ışıkta veya yavaşlayınca yumuşakça orijinal seviyesine indirir.",
                    13f, Palette.TEXT_FAINT, lines = 3
                ), lp(MATCH, WRAP).margins(t = dp(8)))
            }))

            addView(section("Park modu", LinearLayout(this@with).apply {
                orientation = LinearLayout.VERTICAL
                val toggleLabel = label("", 15f, bold = true)
                fun updateToggle() {
                    val on = Prefs.parkModeVideo
                    toggleLabel.text = if (on) "Sürüşte video gizlenir" else "Sürüşte video açık kalır"
                    toggleLabel.setTextColor(if (on) Palette.GREEN else Palette.ORANGE)
                }
                updateToggle()
                addView(toggleLabel, lp(MATCH, WRAP))
                addView(pill(
                    if (Prefs.parkModeVideo) "Kapatmak için dokun" else "Açmak için dokun",
                    res = if (Prefs.parkModeVideo) R.drawable.ic_parking else R.drawable.ic_car,
                ) {
                    Prefs.parkModeVideo = !Prefs.parkModeVideo
                    ui.pop()
                    ui.push(SettingsPage(ui))
                }, lp(WRAP, dp(44)).margins(t = dp(10)))
                addView(label(
                    "Açıkken araç hareket edince video ve web görüntüsü gizlenir, ses devam eder. " +
                        "Kapatırsan sürüşte de görüntü açık kalır. Dikkatli kullan!",
                    13f, Palette.TEXT_DIM, lines = 4), lp(MATCH, WRAP).margins(t = dp(10)))
                addView(label(
                    "Hız, araçtan alınamazsa telefonun GPS'inden ölçülür (telefonda konum izni gerekir).",
                    13f, Palette.TEXT_FAINT, lines = 2), lp(MATCH, WRAP).margins(t = dp(6)))
            }))
            addView(pill("NewREX hakkında", R.drawable.ic_info) { ui.push(AboutPage(ui)) }, lp(WRAP, dp(44)))
        }
        ScrollView(this).apply { addView(content) }
    }

    private fun swatch(color: Int, selected: Boolean, text: String? = null, onPick: () -> Unit): View = with(ctx) {
        FrameLayout(this).apply {
            val r = dpf(24)
            background = pressable(rounded(color, r, if (selected) dp(4) else dp(1), if (selected) Palette.TEXT else 0x40FFFFFF), r)
            if (text != null) addView(label(text, 12f, bold = true).apply { gravity = Gravity.CENTER }, flp(MATCH, MATCH))
            onTap(onPick)
        }
    }

    private fun customSwatch(selected: Int?, onPick: () -> Unit): View = with(ctx) {
        FrameLayout(this).apply {
            val r = dpf(24)
            background = pressable(com.newrex.auto.ui.ColorPickerView.rainbowDot(), r)
            if (selected != null) {
                addView(View(this@with).apply { background = rounded(selected, dpf(14), dp(3), Palette.TEXT) },
                    flp(dp(28), dp(28), Gravity.CENTER))
            } else {
                addView(label("+", 22f, bold = true).apply { gravity = Gravity.CENTER; setShadowLayer(dpf(4), 0f, 0f, 0xAA000000.toInt()) },
                    flp(MATCH, MATCH))
            }
            onTap(onPick)
        }
    }

    private fun section(title: String, body: View): View = with(ctx) {
        panel().apply {
            setPadding(dp(18), dp(14), dp(18), dp(16))
            addView(label(title, 18f, bold = true), lp(MATCH, WRAP).margins(b = dp(12)))
            addView(body, lp(MATCH, WRAP))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).margins(b = dp(12))
        }
    }
}


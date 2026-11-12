// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.phone

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.newrex.auto.data.CarLog
import com.newrex.auto.data.ObdManager
import com.newrex.auto.data.PerformanceTimer
import com.newrex.auto.data.Prefs
import com.newrex.auto.data.TelemetryHub
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.dpf
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.rounded

/**
 * Telefon ekranı: Kokpit Araç Modeli, Türk Plaka Özelleştirme, OBD-II ve 0-100 Performans Kartı
 */
object PhoneCockpitHelper {

    fun buildCockpitViews(
        context: Context,
        cockpitBox: LinearLayout,
        onRefreshNeeded: () -> Unit
    ) = with(context) {
        cockpitBox.removeAllViews()

        // 0. Sesli Karşılama Ayarı
        cockpitBox.addView(label("🎙️ Türkçe Sesli Karşılama", 15f, bold = true))
        val voiceRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(0x14FFFFFF, dpf(12))
            setPadding(dp(12), dp(8), dp(12), dp(8))

            val voiceStatus = label(if (Prefs.voiceWelcome) "Açık (\"İyi yolculuklar ${Prefs.userName}\")" else "Kapalı",
                14f, if (Prefs.voiceWelcome) Palette.GREEN else Palette.TEXT_DIM, bold = true)
            addView(voiceStatus, lp(0, WRAP, 1f))

            addView(Button(context).apply {
                text = if (Prefs.voiceWelcome) "Kapat" else "Aç"
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = rounded(0x22FFFFFF, dpf(10))
                setOnClickListener {
                    Prefs.voiceWelcome = !Prefs.voiceWelcome
                    onRefreshNeeded()
                }
            })
        }
        cockpitBox.addView(voiceRow, lp(MATCH, WRAP).margins(b = dp(12)))

        // 1. Plaka ve Model Düzenleme
        cockpitBox.addView(label("🚘 Araç ve Plaka Özelleştirme", 15f, bold = true))
        cockpitBox.addView(label("Araç kokpitinde ve 3D Toyota C-HR modelinde görüntülenecek plaka ve model adı:",
            12f, Palette.TEXT_DIM, lines = 2), lp(MATCH, WRAP).margins(t = dp(4), b = dp(8)))

        val plateRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(0x14FFFFFF, dpf(12))
            setPadding(dp(12), dp(8), dp(12), dp(8))

            val plateVal = label(Prefs.carPlate, 16f, Palette.TEXT, bold = true)
            addView(plateVal, lp(0, WRAP, 1f))

            addView(Button(context).apply {
                text = "Plakayı Değiştir"
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = rounded(Palette.accent, dpf(10))
                setOnClickListener {
                    showEditPlateDialog(context) { onRefreshNeeded() }
                }
            })
        }
        cockpitBox.addView(plateRow, lp(MATCH, WRAP).margins(b = dp(12)))

        // 2. OBD-II CAN-Bus Bağlantı & Canlı Veri Kartı
        cockpitBox.addView(label("🔌 OBD-II (ELM327) CAN-Bus Telemetrisi", 15f, bold = true))
        val obdState = ObdManager.state.value
        val obdColor = when (obdState) {
            ObdManager.ConnectionState.CONNECTED -> Palette.GREEN
            ObdManager.ConnectionState.CONNECTING,
            ObdManager.ConnectionState.INITIALIZING -> Palette.ORANGE
            ObdManager.ConnectionState.ERROR -> Palette.RED
            ObdManager.ConnectionState.DISCONNECTED -> Palette.TEXT_DIM
        }
        val obdText = when (obdState) {
            ObdManager.ConnectionState.CONNECTED -> "Bağlı (${ObdManager.selectedDevice.value?.name ?: "ELM327"})"
            ObdManager.ConnectionState.CONNECTING,
            ObdManager.ConnectionState.INITIALIZING -> "Bağlanıyor..."
            ObdManager.ConnectionState.ERROR -> "Bağlantı Hatası"
            ObdManager.ConnectionState.DISCONNECTED -> "Bağlı Değil"
        }
        cockpitBox.addView(label("Durum: $obdText", 13f, obdColor, bold = true), lp(MATCH, WRAP).margins(t = dp(4)))

        val snap = TelemetryHub.telemetry.value
        val statsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))

            fun statCell(title: String, v: String) = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = rounded(0x14FFFFFF, dpf(10))
                setPadding(dp(8), dp(6), dp(8), dp(6))
                addView(label(title, 10f, Palette.TEXT_FAINT, bold = true))
                addView(label(v, 14f, Palette.TEXT, bold = true), lp(WRAP, WRAP).margins(t = dp(2)))
            }

            val rpmStr = snap.rpm?.let { "$it" } ?: "--"
            val voltStr = snap.batteryVoltage?.let { "%.1f V".format(it) } ?: "--"
            val tempStr = snap.coolantTempC?.let { "$it°C" } ?: "--"
            val fuelStr = snap.fuelLevelPercent?.let { "%.0f%%".format(it) } ?: "--"

            addView(statCell("DEVİR", rpmStr), lp(0, WRAP, 1f).margins(r = dp(4)))
            addView(statCell("VOLTAJ", voltStr), lp(0, WRAP, 1f).margins(r = dp(4)))
            addView(statCell("HARARET", tempStr), lp(0, WRAP, 1f).margins(r = dp(4)))
            addView(statCell("YAKIT", fuelStr), lp(0, WRAP, 1f))
        }
        cockpitBox.addView(statsRow, lp(MATCH, WRAP).margins(b = dp(12)))

        // 3. 0-100 km/s Performans & Rekorlar
        cockpitBox.addView(label("⚡ 0-100 km/s Performans Sayacı", 15f, bold = true))
        val best0100 = CarLog.bestZeroHundred
        val bestText = best0100?.let { "🏆 En İyi 0-100 Derecesi: %.2f sn".format(it) } ?: "Henüz bir 0-100 ölçümü yapılmadı."
        cockpitBox.addView(label(bestText, 13f, if (best0100 != null) Palette.GREEN else Palette.TEXT_DIM),
            lp(MATCH, WRAP).margins(t = dp(4), b = dp(6)))

        val perfBtnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(Button(context).apply {
                text = "Sayacı Sıfırla / Hazırla"
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = rounded(0x22FFFFFF, dpf(10))
                setOnClickListener {
                    PerformanceTimer.reset()
                    onRefreshNeeded()
                }
            }, lp(WRAP, dp(38)).margins(r = dp(8)))
        }
        cockpitBox.addView(perfBtnRow, lp(MATCH, WRAP))
    }

    private fun showEditPlateDialog(context: Context, onSaved: () -> Unit) {
        val input = EditText(context).apply {
            setText(Prefs.carPlate)
            setSingleLine()
            setHint("Örn: 34 REX 1337")
        }
        AlertDialog.Builder(context)
            .setTitle("Araç Plakası")
            .setMessage("Türk plaka standardında [TR | PLAKA] olarak kokpitte gösterilir:")
            .setView(input)
            .setPositiveButton("Kaydet") { _, _ ->
                val newPlate = input.text.toString().trim()
                if (newPlate.isNotBlank()) {
                    Prefs.carPlate = newPlate
                    onSaved()
                }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }
}


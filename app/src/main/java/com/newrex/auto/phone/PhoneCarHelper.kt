// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.phone

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.newrex.auto.data.CarLog
import com.newrex.auto.data.FuelEntry
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.dpf
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.rounded
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Telefon ekranı: Araç kilometre, tüketim istatistikleri ve yakıt defteri kontrolcüsü
 */
object PhoneCarHelper {
    private val tr = Locale("tr", "TR")

    fun buildCarViews(
        context: Context,
        carBox: LinearLayout,
        onRefreshNeeded: () -> Unit
    ) {
        carBox.removeAllViews()
        val odo = CarLog.estimatedOdometer()
        val stats = CarLog.fuelStats(odo)

        carBox.addView(label("Araç kilometre ve yakıt bilgisini paylaşmıyorsa bu değerlerle tahmin yapılır: " +
            "girdiğin kilometreye GPS ile gidilen yol eklenir.", 13f, Palette.TEXT_DIM, lines = 4))

        carBox.addView(numberRow(context, "Kilometre sayacı", odo?.let { "%.0f".format(it) } ?: "", "km") {
            CarLog.setOdometer(it)
            onRefreshNeeded()
        })
        carBox.addView(numberRow(context, "Depo hacmi", "%.0f".format(CarLog.tankLiters), "L") {
            CarLog.tankLiters = it
            onRefreshNeeded()
        })
        carBox.addView(numberRow(context, "Son yağ değişimi", if (CarLog.oilChangeKm > 0) "%.0f".format(CarLog.oilChangeKm) else "", "km") {
            CarLog.oilChangeKm = it
            onRefreshNeeded()
        })
        carBox.addView(numberRow(context, "Yağ değişim aralığı", "%.0f".format(CarLog.oilIntervalKm), "km") {
            CarLog.oilIntervalKm = it
            onRefreshNeeded()
        })

        CarLog.oilKmLeft(odo)?.let { left ->
            carBox.addView(label(if (left < 0) String.format(tr, "🛢️ Yağ değişimi %,.0f km gecikti!", -left)
                else String.format(tr, "🛢️ Yağ değişimine %,.0f km kaldı", left), 14f,
                if (left < 1000) Palette.ORANGE else Palette.GREEN, bold = true), lp(MATCH, WRAP).margins(t = context.dp(6)))
        }

        carBox.addView(label("⛽ Yakıt defteri", 15f, bold = true), lp(MATCH, WRAP).margins(t = context.dp(14)))
        val fuel = CarLog.fuel()
        if (fuel.isNotEmpty()) {
            carBox.addView(label(listOf(
                "Ortalama: " + (stats.consumption?.let { "%.1f L/100 km".format(it) } ?: "en az 2 dolum gerekli"),
                "Km başı: " + (stats.costPerKm?.let { "%.2f ₺".format(it) } ?: "—"),
                "Bu ay: " + String.format(tr, "%,.0f ₺", stats.thisMonth),
                "Tahmini menzil: " + (stats.rangeKm?.let { "~%.0f km".format(it) } ?: "—"),
            ).joinToString("\n"), 14f, Palette.TEXT_DIM, lines = 4), lp(MATCH, WRAP).margins(t = context.dp(4)))
        }

        val fmt = SimpleDateFormat("d MMM yyyy", tr)
        fuel.reversed().take(8).forEach { e ->
            carBox.addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(context.dp(12), context.dp(8), context.dp(6), context.dp(8))
                background = rounded(0x14FFFFFF, context.dpf(12))
                addView(label(fmt.format(Date(e.time)) + "\n" +
                    String.format(tr, "%.1f L • %,.0f ₺ • %,.0f km", e.liters, e.price, e.km), 13f, lines = 2), lp(0, WRAP, 1f))
                addView(Button(context).apply {
                    text = "Sil"
                    setOnClickListener {
                        CarLog.removeFuel(e)
                        onRefreshNeeded()
                    }
                })
            }, lp(MATCH, WRAP).margins(t = context.dp(6)))
        }

        carBox.addView(Button(context).apply {
            text = "Yakıt aldım (depoyu fulledim)"
            setOnClickListener { showAddFuelDialog(context, onRefreshNeeded) }
        }, lp(MATCH, WRAP).margins(t = context.dp(10)))
    }

    private fun numberRow(context: Context, title: String, value: String, unit: String, onSave: (Float) -> Unit): View {
        val field = EditText(context).apply {
            setText(value)
            setSingleLine()
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Color.WHITE)
            gravity = Gravity.END
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(label(title, 14f, lines = 2), lp(0, WRAP, 1f))
            addView(field, lp(context.dp(90), WRAP))
            addView(label(unit, 13f, Palette.TEXT_DIM), lp(context.dp(28), WRAP).margins(l = context.dp(4)))
            addView(Button(context).apply {
                text = "Kaydet"
                isAllCaps = false
                setOnClickListener {
                    field.text.toString().replace(',', '.').toFloatOrNull()?.takeIf { it > 0 }?.let(onSave)
                    field.clearFocus()
                }
            })
        }
    }

    fun showAddFuelDialog(context: Context, onSaved: () -> Unit) {
        fun field(hint: String, value: String = "") = EditText(context).apply {
            this.hint = hint
            setText(value)
            setSingleLine()
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val liters = field("Litre (ör. 38,5)")
        val price = field("Tutar ₺ (ör. 1850)")
        val km = field("Kilometre sayacı", CarLog.estimatedOdometer()?.let { "%.0f".format(it) } ?: "")
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(20), context.dp(8), context.dp(20), 0)
            addView(liters); addView(price); addView(km)
            addView(label("Depoyu fullediğin alımları gir; tüketim böyle doğru hesaplanır. Kilometreyi aracın göstergesinden yaz.",
                12f, Palette.TEXT_DIM, lines = 3), lp(MATCH, WRAP).margins(t = context.dp(6)))
        }
        AlertDialog.Builder(context)
            .setTitle("Yakıt aldım")
            .setView(box)
            .setPositiveButton("Kaydet") { _, _ ->
                fun num(e: EditText) = e.text.toString().replace(',', '.').toFloatOrNull() ?: 0f
                if (num(liters) > 0f) {
                    CarLog.addFuel(FuelEntry(System.currentTimeMillis(), num(liters), num(price), num(km)))
                    onSaved()
                }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }
}


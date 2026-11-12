// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.phone

import android.app.DatePickerDialog
import android.content.Context
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.newrex.auto.data.Maintenance
import com.newrex.auto.data.Prefs
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.dpf
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.rounded
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Telefon ekranı: Araç periyodik bakım (Muayene, Sigorta, Yağ vb.) hatırlatıcısı kontrolcüsü
 */
object PhoneMaintenanceHelper {
    private val tr = Locale("tr", "TR")

    fun buildMaintenanceViews(
        context: Context,
        box: LinearLayout,
        summary: TextView,
        onRefreshNeeded: () -> Unit
    ) {
        box.removeAllViews()
        summary.text = Prefs.maintenanceDue().firstOrNull()?.let { (m, d) ->
            if (d < 0) "${m.emoji} ${m.title} ${-d} gün gecikti" else "${m.emoji} ${m.title}: $d gün kaldı"
        } ?: "Muayene, sigorta, yağ değişimi…"

        val today = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", tr)

        Maintenance.values().forEach { m ->
            val day = Prefs.maintenanceDay(m)?.let { LocalDate.ofEpochDay(it) }
            val left = day?.let { it.toEpochDay() - today.toEpochDay() }
            val (status, color) = when {
                left == null -> "Tarih girilmedi" to Palette.TEXT_FAINT
                left < 0 -> "${fmt.format(day)} • ${-left} gün gecikti" to Palette.RED
                left <= 15 -> "${fmt.format(day)} • $left gün kaldı" to Palette.ORANGE
                else -> "${fmt.format(day)} • $left gün kaldı" to Palette.GREEN
            }

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(context.dp(12), context.dp(10), context.dp(6), context.dp(10))
                background = rounded(0x14FFFFFF, context.dpf(12))

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(context.label("${m.emoji}  ${m.title}", 15f, bold = true))
                    addView(context.label(status, 12f, color), lp(MATCH, WRAP).margins(t = context.dp(3)))
                }, lp(0, WRAP, 1f))

                if (day != null) addView(Button(context).apply {
                    text = "Sil"
                    setOnClickListener {
                        Prefs.setMaintenanceDay(m, null)
                        onRefreshNeeded()
                    }
                })

                setOnClickListener {
                    val start = day ?: today.plusMonths(m.months.toLong())
                    DatePickerDialog(context, { _, y, mo, d ->
                        Prefs.setMaintenanceDay(m, LocalDate.of(y, mo + 1, d).toEpochDay())
                        onRefreshNeeded()
                    }, start.year, start.monthValue - 1, start.dayOfMonth).show()
                }
            }
            box.addView(row, lp(MATCH, WRAP).margins(t = context.dp(8)))
        }
    }
}



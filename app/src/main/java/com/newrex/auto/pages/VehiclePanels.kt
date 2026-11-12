// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.pages

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.newrex.auto.DriveState
import com.newrex.auto.R
import com.newrex.auto.SpeedAlert
import com.newrex.auto.data.CarLog
import com.newrex.auto.data.PerformanceTimer
import com.newrex.auto.data.Prefs
import com.newrex.auto.data.TripStats
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.dpf
import com.newrex.auto.ui.icon
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.panel
import com.newrex.auto.ui.pill
import com.newrex.auto.ui.rounded
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Araç Bilgi Sayfası (VehiclePage) Alt Panelleri
 */
object VehiclePanels {
    private val tr = Locale("tr", "TR")
    private var tripPage = 0
    private const val TRIPS_PER_PAGE = 10

    fun buildPanels(context: Context, currentKm: Float?): List<View> = listOf(
        performancePanel(context),
        drivingPanel(context),
        speedAlertPanel(context),
        fuelPanel(context, currentKm),
        maintenancePanel(context, currentKm),
        tripsPanel(context),
        parkPanel(context)
    )

    fun performancePanel(context: Context): View = with(context) {
        val b = box(context, R.drawable.ic_speed, "0-100 km/s Performans & Hızlanma (Drag)")
        val pState = PerformanceTimer.state.value
        val elapsed = PerformanceTimer.elapsedSeconds.value
        val split = PerformanceTimer.splitZeroFifty.value
        val best = CarLog.bestZeroHundred
        val last = CarLog.lastZeroHundred

        val statusText = when (pState) {
            PerformanceTimer.State.READY -> "HAZIR (Araç durdu, gaza basınca başlar)"
            PerformanceTimer.State.MEASURING -> "ÖLÇÜLÜYOR... (Tam gaz devam!)"
            PerformanceTimer.State.COMPLETED -> "TAMAMLANDI! 🎉"
            PerformanceTimer.State.IDLE -> "Beklemede (Ölçüm için aracı durdurun)"
        }
        val statusColor = when (pState) {
            PerformanceTimer.State.READY -> Palette.GREEN
            PerformanceTimer.State.MEASURING -> Palette.ORANGE
            PerformanceTimer.State.COMPLETED -> 0xFF00E5FF.toInt()
            PerformanceTimer.State.IDLE -> Palette.TEXT_DIM
        }

        b.addView(label(statusText, 13f, statusColor, bold = true))

        val currentStr = if (pState == PerformanceTimer.State.MEASURING) "%.2f sn".format(elapsed)
            else last?.let { "%.2f sn".format(it) } ?: "—"
        val splitStr = split?.let { "%.2f sn".format(it) } ?: "—"
        val bestStr = best?.let { "%.2f sn".format(it) } ?: "—"

        b.addView(stats(context,
            currentStr to "0-100 km/s",
            splitStr to "0-50 km/s (Ara zaman)",
            bestStr to "🏆 En İyi Derece",
            firstColor = statusColor
        ), lp(MATCH, WRAP).margins(t = dp(8)))

        val resetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(pill("↻ Sayacı Hazırla / Sıfırla") {
                PerformanceTimer.reset()
            }, lp(WRAP, dp(38)).margins(t = dp(8)))
        }
        b.addView(resetRow, lp(MATCH, WRAP))
        b
    }

    fun drivingPanel(context: Context): View = with(context) {
        val b = box(context, R.drawable.ic_car, "Sürüş puanı (bu yolculuk)")
        val km = DriveState.tripMeters.value / 1000f
        val score = TripStats.score(km)
        val movingMin = TripStats.movingMs / 60_000f
        val avg = if (movingMin > 1f) km / movingMin * 60f else null
        val color = when { score >= 85 -> Palette.GREEN; score >= 60 -> Palette.ORANGE; else -> Palette.RED }

        b.addView(stats(context,
            "$score / 100" to "Ekonomik sürüş",
            "${TripStats.harshAccel.value}" to "Ani hızlanma",
            "${TripStats.harshBrake.value}" to "Sert fren",
            "%.0f".format(TripStats.maxKmh.value) to "En yüksek km/s",
            (avg?.let { "%.0f".format(it) } ?: "—") to "Ortalama km/s",
            firstColor = color,
        ))
        b.addView(label("Ani hızlanma ve sert frenler puanı düşürür; yumuşak sürüş yakıt tasarrufu sağlar.",
            12f, Palette.TEXT_FAINT, lines = 2), lp(MATCH, WRAP).margins(t = dp(8)))
        b
    }

    fun speedAlertPanel(context: Context): View = with(context) {
        val b = box(context, R.drawable.ic_speed, "Hız uyarısı")
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val current = CarLog.speedLimit
        listOf(0, 50, 70, 90, 110, 120, 130, 140).forEach { v ->
            row.addView(pill(if (v == 0) "Kapalı" else "$v", selected = v == current, heightDp = 40) {
                CarLog.speedLimit = v
            }, lp(WRAP, dp(40)).margins(r = dp(8)))
        }
        b.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(row) })

        val sounds = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        SpeedAlert.SOUNDS.forEachIndexed { i, name ->
            sounds.addView(pill("🔔 $name", selected = i == CarLog.speedSound, heightDp = 40) {
                CarLog.speedSound = i
                SpeedAlert.play(context, i)
            }, lp(WRAP, dp(40)).margins(r = dp(8)))
        }
        b.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(sounds) },
            lp(MATCH, WRAP).margins(t = dp(8)))
        b.addView(label(if (current == 0) "Seçtiğin hızı geçince arabanın hoparlöründen sesli uyarı ve ekranda büyük kırmızı uyarı çıkar."
            else "$current km/s geçilince uyarır. Hız GPS ile ölçüldüğünde birkaç km/s sapabilir.",
            12f, Palette.TEXT_FAINT, lines = 2), lp(MATCH, WRAP).margins(t = dp(8)))
        b
    }

    fun fuelPanel(context: Context, currentKm: Float?): View = with(context) {
        val b = box(context, R.drawable.ic_gas, "Yakıt defteri")
        val s = CarLog.fuelStats(currentKm)
        if (CarLog.fuel().isEmpty()) {
            b.addView(label("Depoyu fullediğinde telefondaki NewREX uygulamasında \"Yakıt ve kilometre\" bölümüne " +
                "litre, tutar ve kilometreyi gir. Ortalama tüketim, km başı maliyet ve tahmini menzil burada çıkar.",
                13f, Palette.TEXT_DIM, lines = 4))
        } else {
            b.addView(stats(context,
                (s.consumption?.let { "%.1f".format(it) } ?: "—") to "L / 100 km",
                (s.costPerKm?.let { "%.2f ₺".format(it) } ?: "—") to "km başı",
                String.format(tr, "%,.0f ₺", s.thisMonth) to "Bu ay",
                (s.rangeKm?.let { "~%.0f km".format(it) } ?: "—") to "Tahmini menzil",
            ))
            if (s.consumption == null) b.addView(label("Ortalama için en az iki dolum ve 50 km yol gerekiyor.",
                12f, Palette.TEXT_FAINT, lines = 2), lp(MATCH, WRAP).margins(t = dp(8)))
        }
        b
    }

    fun maintenancePanel(context: Context, currentKm: Float?): View = with(context) {
        val b = box(context, R.drawable.ic_build, "Bakım hatırlatıcı")
        CarLog.oilKmLeft(currentKm)?.let { left ->
            val color = when { left < 0 -> Palette.RED; left <= 1000 -> Palette.ORANGE; else -> Palette.GREEN }
            val text = if (left < 0) String.format(tr, "%,.0f km geçti", -left) else String.format(tr, "%,.0f km kaldı", left)
            b.addView(row(context, "🛢️  Yağ değişimi", text, color), lp(MATCH, WRAP).margins(b = dp(6)))
        }
        val due = Prefs.maintenanceDue()
        if (due.isEmpty() && CarLog.oilChangeKm <= 0f) {
            b.addView(label("Muayene, sigorta, bakım ve lastik tarihlerini, son yağ değişimi kilometresini telefondaki " +
                "NewREX uygulamasından gir. Zamanı yaklaşınca burada ve araç açılınca uyarı çıkar.", 13f, Palette.TEXT_DIM, lines = 3))
        }
        due.forEach { (m, days) ->
            val color = when { days < 0 -> Palette.RED; days <= 15 -> Palette.ORANGE; else -> Palette.GREEN }
            val text = when { days < 0 -> "${-days} gün gecikti"; days == 0L -> "Bugün!"; else -> "$days gün kaldı" }
            b.addView(row(context, "${m.emoji}  ${m.title}", text, color), lp(MATCH, WRAP).margins(b = dp(6)))
        }
        b
    }

    fun tripsPanel(context: Context): View = with(context) {
        val b = box(context, R.drawable.ic_history, "Yolculuk geçmişi")
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        b.addView(list, lp(MATCH, WRAP))
        fillTrips(context, list)
        b
    }

    fun parkPanel(context: Context): View = with(context) {
        val b = box(context, R.drawable.ic_place, "Park")
        val lines = ArrayList<String>()
        if (!DriveState.driving.value && DriveState.stoppedSince > 0) {
            lines += "Şu an duruyorsun: ${duration((System.currentTimeMillis() - DriveState.stoppedSince) / 60_000)}"
        }
        val spot = Prefs.parkedSpot
        lines += if (spot == null) "Araç kapanınca bulunduğun yer kaydedilir; telefondaki \"Arabam nerede?\" bölümünden haritada açarsın."
        else "Son park yeri: " + SimpleDateFormat("d MMMM HH:mm", tr).format(Date(spot.time)) +
            " — telefondan haritada açabilir, otopark süresi için hatırlatıcı kurabilirsin."
        b.addView(label(lines.joinToString("\n"), 13f, Palette.TEXT_DIM, lines = 4))
        b
    }

    private fun fillTrips(context: Context, list: LinearLayout): Unit = with(context) {
        list.removeAllViews()
        val all = CarLog.trips()
        if (all.isEmpty()) {
            list.addView(label("Araç kapanınca (Android Auto bağlantısı kesilince) yolculuk buraya kaydedilir.",
                13f, Palette.TEXT_DIM, lines = 2))
            return
        }
        val pages = (all.size + TRIPS_PER_PAGE - 1) / TRIPS_PER_PAGE
        tripPage = tripPage.coerceIn(0, pages - 1)
        val trips = all.drop(tripPage * TRIPS_PER_PAGE).take(TRIPS_PER_PAGE)
        val fmt = SimpleDateFormat("d MMM HH:mm", tr)
        trips.forEach { t ->
            list.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = rounded(0x14FFFFFF, dpf(12))
                addView(label(fmt.format(Date(t.start)), 14f, bold = true), lp(dp(120), WRAP))
                addView(label("%.1f km • %s".format(t.km, duration(((t.end - t.start) / 60_000).coerceAtLeast(0))), 14f), lp(0, WRAP, 1f))
                addView(label("ort ${t.avgKmh} • en çok ${t.maxKmh} km/s", 13f, Palette.TEXT_DIM), lp(WRAP, WRAP).margins(r = dp(12)))
                val c = when { t.score >= 85 -> Palette.GREEN; t.score >= 60 -> Palette.ORANGE; else -> Palette.RED }
                addView(label("${t.score}", 15f, c, bold = true))
            }, lp(MATCH, WRAP).margins(b = dp(6)))
        }
        if (pages > 1) {
            list.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                if (tripPage > 0) addView(pill("‹ Yeni yolculuklar", heightDp = 40) { tripPage--; fillTrips(context, list) },
                    lp(WRAP, dp(40)).margins(r = dp(12)))
                addView(label("Sayfa ${tripPage + 1} / $pages  •  ${all.size} yolculuk", 14f, Palette.TEXT_DIM), lp(WRAP, WRAP).margins(r = dp(12)))
                if (tripPage < pages - 1) addView(pill("Eski yolculuklar ›", heightDp = 40) { tripPage++; fillTrips(context, list) },
                    lp(WRAP, dp(40)))
            }, lp(MATCH, WRAP).margins(t = dp(4)))
        }
    }

    private fun box(context: Context, iconRes: Int, title: String): LinearLayout = with(context) {
        panel().apply {
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(LinearLayout(this@with).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(icon(iconRes, 18, Palette.accent), lp(dp(18), dp(18)).margins(r = dp(8)))
                addView(label(title, 16f, bold = true))
            }, lp(MATCH, WRAP).margins(b = dp(8)))
        }
    }

    private fun stats(context: Context, vararg items: Pair<String, String>, firstColor: Int = Palette.TEXT): View = with(context) {
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            items.forEachIndexed { i, (value, caption) ->
                addView(LinearLayout(this@with).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(8), dp(12), dp(8))
                    background = rounded(0x14FFFFFF, dpf(12))
                    addView(label(value, 20f, if (i == 0) firstColor else Palette.TEXT, bold = true))
                    addView(label(caption, 12f, Palette.TEXT_DIM, lines = 2), lp(WRAP, WRAP).margins(t = dp(2)))
                }, lp(0, WRAP, 1f).margins(r = dp(8)))
            }
        }
    }

    private fun row(context: Context, title: String, value: String, color: Int): View = with(context) {
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(Palette.withAlpha(color, 0x22), dpf(12))
            addView(label(title, 15f), lp(0, WRAP, 1f))
            addView(label(value, 14f, color, bold = true))
        }
    }

    private fun duration(mins: Long) = if (mins >= 60) "${mins / 60} sa ${mins % 60} dk" else "$mins dk"
}


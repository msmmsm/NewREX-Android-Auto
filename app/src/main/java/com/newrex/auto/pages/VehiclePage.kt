// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.pages

import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.newrex.auto.DriveState
import com.newrex.auto.R
import com.newrex.auto.VehicleData
import com.newrex.auto.data.CarLog
import com.newrex.auto.data.Prefs
import com.newrex.auto.data.TripStats
import com.newrex.auto.ui.CarUi
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Page
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.dpf
import com.newrex.auto.ui.icon
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.neonText
import com.newrex.auto.ui.panel
import com.newrex.auto.ui.pill
import com.newrex.auto.ui.rounded
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Araç paneli: hız, yakıt/menzil, kilometre, yolculuk, rakım ve yön; sürüş puanı, hız uyarısı,
 * yakıt defteri, bakım, yolculuk geçmişi ve park bilgisi. Araç paylaşmayan bilgiler GPS ve
 * telefonda girilen kayıtlardan tahmin edilir ("~" ile gösterilir).
 */
class VehiclePage(ui: CarUi) : Page(ui) {
    override val title = "Araç"

    private val tr = Locale("tr", "TR")
    private lateinit var lower: LinearLayout
    private var drivingBox: LinearLayout? = null

    override fun build(): View = with(ctx) {
        val gauges = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val speed = gauge(gauges, R.drawable.ic_speed, "Hız", big = true)
        val fuel = gauge(gauges, R.drawable.ic_gas, "Yakıt / menzil")
        val odo = gauge(gauges, R.drawable.ic_car, "Kilometre")
        val trip = gauge(gauges, R.drawable.ic_place, "Bu yolculuk")
        val time = gauge(gauges, R.drawable.ic_history, "Yoldasın")
        val alt = gauge(gauges, R.drawable.ic_up, "Rakım / yön")

        val noPermission = "📍 Telefonda konum izni ver"
        observe(DriveState.speedKmh) { v ->
            speed.first.text = v?.let { "%.0f".format(it) } ?: "—"
            speed.second.text = when {
                v != null -> if (DriveState.speedFromCar.value) "km/s • araçtan" else "km/s • GPS"
                !DriveState.hasLocationPermission(ctx) -> noPermission
                else -> "km/s • GPS bekleniyor"
            }
            val limit = CarLog.speedLimit
            speed.first.setTextColor(if (limit > 0 && v != null && v > limit) Palette.RED else Palette.TEXT)
        }
        // İzin yoksa hız/yolculuk kartına dokununca telefonda izin sorulur
        listOf(speed, trip).forEach { (v, _) ->
            (v.parent as View).setOnClickListener {
                if (!DriveState.hasLocationPermission(ctx)) {
                    com.newrex.auto.MainActivity.askLocation(ctx.applicationContext)
                    ui.toast("Telefonunda konum iznini ver.", long = true)
                }
            }
        }

        // Saniyede bir değişmesi gerekmeyen değerler birkaç saniyede bir yenilenir
        scope.launch {
            while (true) {
                DriveState.ensureGps()
                val tripKm = DriveState.tripMeters.value / 1000f
                val start = DriveState.tripStart
                val mins = if (start > 0) ((System.currentTimeMillis() - start) / 60_000).coerceAtLeast(0) else null
                trip.first.text = String.format(tr, "%.1f km", tripKm)
                trip.second.text = if (!DriveState.hasLocationPermission(ctx)) noPermission else "GPS ile ölçülür"
                time.first.text = mins?.let { duration(it) } ?: "—"
                time.second.text = if (start > 0) "${SimpleDateFormat("HH:mm", tr).format(Date(start))}'den beri" else ""

                val carOdo = VehicleData.odometerKm.value
                val currentKm = carOdo ?: CarLog.estimatedOdometer(tripKm)
                odo.first.text = currentKm?.let { (if (carOdo == null) "~" else "") + String.format(tr, "%,.0f", it) } ?: "—"
                odo.second.text = when {
                    carOdo != null -> "km • araçtan"
                    currentKm != null -> "km • tahmini"
                    else -> "Telefonda km gir"
                }

                val pct = VehicleData.fuelPercent.value ?: VehicleData.batteryPercent.value
                val carRange = VehicleData.rangeKm.value
                val estRange = CarLog.fuelStats(currentKm).rangeKm
                fuel.first.text = when {
                    pct != null -> "%%%.0f".format(pct)
                    carRange != null -> "%.0f km".format(carRange)
                    estRange != null -> "~%.0f km".format(estRange)
                    else -> "—"
                }
                fuel.second.text = when {
                    VehicleData.lowFuel.value -> "⚠ Yakıt az!"
                    pct != null && carRange != null -> "%.0f km gidebilirsin".format(carRange)
                    pct != null -> "depo • araçtan"
                    carRange != null -> "menzil • araçtan"
                    estRange != null -> "menzil • tahmini"
                    else -> "Yakıt defterini doldur"
                }
                fuel.first.setTextColor(if (VehicleData.lowFuel.value) Palette.ORANGE else Palette.TEXT)

                alt.first.text = DriveState.altitude.value?.let { "${it.toInt()} m" } ?: "—"
                alt.second.text = DriveState.bearing.value?.let { "Yön: ${compass(it)}" } ?: "Yön: hareket edince"
                delay(3000)
            }
        }

        val headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = rounded(Palette.SURFACE, dpf(14), dp(1), 0x22FFFFFF)

            val carTitle = label(Prefs.carModel, 15f, bold = true)
            addView(carTitle, lp(0, WRAP, 1f))

            // Gerçek Türk Plakası Rozeti
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

        observe(VehicleData.model) {
            val titleText = it ?: Prefs.carModel
            (headerCard.getChildAt(0) as? TextView)?.text = "🚗 $titleText"
        }

        lower = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(12))
            addView(headerCard, lp(MATCH, WRAP).margins(b = dp(8)))
            addView(HorizontalScrollView(this@with).apply {
                isHorizontalScrollBarEnabled = false
                isFillViewport = true
                addView(gauges, lp(WRAP, WRAP))
            }, lp(MATCH, WRAP))
            addView(lower, lp(MATCH, WRAP))
        }
        scope.launch { CarLog.changes.collect { buildLower() } }
        // Sürüş puanı canlı güncellensin
        scope.launch { while (true) { delay(10_000); refreshDriving() } }
        ScrollView(this).apply { addView(content) }
    }

    private fun <T> observe(flow: Flow<T>, block: (T) -> Unit) {
        scope.launch { flow.collect { block(it) } }
    }

    private fun duration(mins: Long) = if (mins >= 60) "${mins / 60} sa ${mins % 60} dk" else "$mins dk"

    private fun compass(deg: Float): String {
        val names = arrayOf("Kuzey", "Kuzeydoğu", "Doğu", "Güneydoğu", "Güney", "Güneybatı", "Batı", "Kuzeybatı")
        return names[(((deg % 360) + 360) % 360 / 45f + 0.5f).toInt() % 8]
    }

    private fun currentKm(): Float? =
        VehicleData.odometerKm.value ?: CarLog.estimatedOdometer(DriveState.tripMeters.value / 1000f)

    // ---- Alt bölümler ----

    private fun buildLower() = with(ctx) {
        lower.removeAllViews()
        VehiclePanels.buildPanels(ctx, currentKm()).forEach {
            lower.addView(it, lp(MATCH, WRAP).margins(t = dp(10)))
        }
    }

    private fun refreshDriving() = with(ctx) {
        // Canlı sürüş puanı yenileme
        drivingBox?.let { b ->
            val newDriving = VehiclePanels.drivingPanel(ctx)
            val idx = lower.indexOfChild(b)
            if (idx >= 0) {
                lower.removeViewAt(idx)
                lower.addView(newDriving, idx, lp(MATCH, WRAP).margins(t = dp(10)))
                drivingBox = newDriving as? LinearLayout
            }
        }
    }

    /** Büyük değer + alt yazı. Dönen çift: (değer, alt yazı). */
    private fun gauge(row: LinearLayout, iconRes: Int, title: String, big: Boolean = false): Pair<TextView, TextView> = with(ctx) {
        val value = label("—", if (big) 40f else 30f, bold = true).apply { if (big) neonText(12) }
        val sub = label("", 12f, Palette.TEXT_DIM)
        val card = panel().apply {
            setPadding(dp(14), dp(12), dp(14), dp(12))
            minimumWidth = dp(if (big) 150 else 128)
            addView(LinearLayout(this@with).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(icon(iconRes, 18, Palette.accent), lp(dp(18), dp(18)).margins(r = dp(6)))
                addView(label(title, 13f, Palette.TEXT_DIM, bold = true))
            })
            addView(value, lp(WRAP, WRAP).margins(t = dp(8)))
            addView(sub, lp(WRAP, WRAP).margins(t = dp(2)))
            if (big) background = rounded(Palette.withAlpha(Palette.accent, 0x33), dpf(18), dp(1), Palette.withAlpha(Palette.accent, 0x88))
        }
        // Genişlik 0 olunca kaydırılabilir satırda kartlar en küçük boyuta iniyor, "12.3 km" gibi yazılar kesiliyordu
        row.addView(card, lp(WRAP, WRAP, if (big) 1.3f else 1f).margins(r = dp(8)))
        value to sub
    }
}

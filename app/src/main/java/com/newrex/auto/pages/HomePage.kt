// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.pages

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.newrex.auto.DriveState
import com.newrex.auto.PlayerHub
import com.newrex.auto.R
import com.newrex.auto.WeatherHub
import com.newrex.auto.data.Kind
import com.newrex.auto.data.MediaEntry
import com.newrex.auto.data.Prefs
import com.newrex.auto.data.TelemetryHub
import com.newrex.auto.data.TelemetrySnapshot
import com.newrex.auto.data.TelemetrySource
import com.newrex.auto.data.WeatherApi
import com.newrex.auto.ui.CarModelView
import com.newrex.auto.ui.CarUi
import com.newrex.auto.ui.CardArt
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Page
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.dpf
import com.newrex.auto.ui.flp
import com.newrex.auto.ui.gradient
import com.newrex.auto.ui.icon
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.onTap
import com.newrex.auto.ui.pressable
import com.newrex.auto.ui.rounded
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * NewREX Yeni Nesil Otomobil Kokpit & Telemetri Ana Ekranı (MBUX / Tesla İlhamlı)
 */
class HomePage(ui: CarUi) : Page(ui) {
    override val title = "Kokpit"

    private class Shortcut(
        val title: String, val subtitle: String, val icon: Int, val color: Int,
        val warn: Boolean = false, val onClick: () -> Unit,
    )

    private lateinit var container: LinearLayout
    private var lastWidth = 0

    override fun build(): View = with(ctx) {
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(16))
        }
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(container, FrameLayout.LayoutParams(MATCH, WRAP))
            addOnLayoutChangeListener { v, l, _, rr, _, _, _, _, _ ->
                val w = rr - l
                if (w > 0 && w != lastWidth) {
                    lastWidth = w
                    v.post { populate(ui.compact.value) }
                }
            }
        }

        // Reaktif güncellemeler (Telemetri, Hava, Medya, Ayarlar)
        scope.launch {
            combine(ui.compact, Prefs.changes, WeatherHub.report) { c, _, _ -> c }
                .collect { populate(it) }
        }
        scope.launch {
            TelemetryHub.telemetry.collect {
                // Sadece telemetri değiştiğinde tam yeniden çizim yerine hafif yenileme
                if (lastWidth > 0) populate(ui.compact.value)
            }
        }
        scroll
    }

    private fun populate(compact: Boolean): Unit = with(ctx) {
        if (lastWidth == 0) return
        container.removeAllViews()
        val gap = dp(if (compact) 8 else 12)
        val snap = TelemetryHub.telemetry.value

        // 1. Üst Durum & Bağlantı Çubuğu (Header)
        container.addView(buildHeader(compact, snap), lp(MATCH, WRAP).margins(b = gap))

        // 2. Merkez Araç Modeli ve Durum Kartı (3D / 2.5D Görünüm)
        container.addView(buildVehicleCenterCard(compact, snap), lp(MATCH, WRAP).margins(b = gap))

        // 3. Canlı Telemetri Gösterge Çubuğu (Hız, Devir/Voltaj, Yakıt, Menzil)
        container.addView(buildTelemetryRow(compact, snap), lp(MATCH, WRAP).margins(b = gap))

        // 4. Medya Başlatıcıları ([ TV ] [ YouTube ] [ Radyo ] [ Web ])
        container.addView(
            label("MEDYA MERKEZİ", 11f, Palette.TEXT_FAINT, bold = true).apply { letterSpacing = 0.12f },
            lp(MATCH, WRAP).margins(b = dp(6))
        )
        val bigCards = listOf(
            bigCard("TV", "Kanallar, IPTV, TV+, Netflix", R.drawable.ic_live_tv, 0xFF06B6D4.toInt(), CardArt.Kind.TV, compact, live = true) {
                ui.push(TvHubPage(ui))
            },
            bigCard("YouTube", "Video izle", R.drawable.ic_smart_display, 0xFFEF4444.toInt(), CardArt.Kind.YOUTUBE, compact) {
                ui.push(WebPage(ui, WebPage.Mode.YOUTUBE))
            },
            bigCard("Radyo", "Türk radyoları", R.drawable.ic_radio, 0xFF8B5CF6.toInt(), CardArt.Kind.RADIO, compact) {
                ui.push(RadioPage(ui))
            },
            bigCard("Web", "İnternet tarayıcı", R.drawable.ic_language, 0xFF3B82F6.toInt(), CardArt.Kind.WEB, compact) {
                ui.push(WebPage(ui, WebPage.Mode.WEB))
            },
        )
        val bigPerRow = if (compact) 2 else 4
        addGrid(bigCards, bigPerRow, dp(if (compact) 110 else 180), gap)

        // 5. Son Kullanılanlar (Recently Played) Şeridi
        val recents = Prefs.recents().take(3)
        if (recents.isNotEmpty()) {
            container.addView(
                label("SON KULLANILANLAR", 11f, Palette.TEXT_FAINT, bold = true).apply { letterSpacing = 0.12f },
                lp(MATCH, WRAP).margins(t = dp(8), b = dp(6))
            )
            val recentViews = recents.map { recentCard(it, compact) }
            addGrid(recentViews, recentViews.size.coerceIn(1, 3), dp(54), gap)
        }

        // 6. Sistem Kısayolları (Favoriler, Podcast, Araç Paneli, Hava, Yansıt, Ayarlar)
        val shortcuts = ArrayList<Shortcut>()
        shortcuts += Shortcut("Favoriler", "Kanal ve radyolar", R.drawable.ic_star, Palette.ORANGE) { ui.push(FavoritesPage(ui)) }
        shortcuts += Shortcut("Podcast", "Kaldığın yerden", R.drawable.ic_podcast, 0xFFE040FB.toInt()) { ui.push(PodcastPage(ui)) }
        val due = Prefs.maintenanceDue().firstOrNull { it.second <= 15 }
        shortcuts += Shortcut(
            "Araç Bilgisi",
            due?.let { (m, d) -> if (d < 0) "${m.title} gecikti!" else "${m.title}: $d gün" } ?: "Bakım ve teşhis",
            R.drawable.ic_speed,
            0xFF16B98A.toInt(),
            warn = due != null
        ) { ui.push(VehiclePage(ui)) }

        val w = WeatherHub.report.value
        shortcuts += Shortcut(
            w?.let { "${WeatherApi.emoji(it.code, it.isDay)} ${it.temp}°" } ?: "Hava",
            w?.let { it.place ?: WeatherApi.describe(it.code) } ?: "Hava durumu",
            R.drawable.ic_cloud,
            0xFF7DB8FF.toInt()
        ) { ui.push(WeatherPage(ui)) }

        shortcuts += Shortcut("Yansıt", "Telefon ekranı", R.drawable.ic_cast, 0xFF16B98A.toInt()) { ui.push(MirrorPage(ui)) }
        shortcuts += Shortcut("Ayarlar", "Tema ve bağlantı", R.drawable.ic_settings, 0xFF8B93A7.toInt()) { ui.push(SettingsPage(ui)) }

        val usable = lastWidth - container.paddingLeft - container.paddingRight
        val cols = (usable / dp(if (compact) 120 else 170)).coerceIn(2, 6)
        container.addView(View(this), lp(MATCH, gap))
        addGrid(shortcuts.map { smallCard(it, compact) }, cols, dp(if (compact) 92 else 108), gap)

        // 7. Hakkında / Alt Bilgi
        container.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(label("NewREX Kokpit hakkında", 13f, Palette.TEXT_FAINT), lp(WRAP, WRAP))
            addView(icon(R.drawable.ic_down, 22, Palette.TEXT_FAINT), lp(dp(22), dp(22)))
        }, lp(MATCH, WRAP).margins(t = dp(18)))
    }

    /** 1. Üst Durum & Bağlantı Çubuğu */
    private fun buildHeader(compact: Boolean, snap: TelemetrySnapshot): View = with(ctx) {
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(Palette.SURFACE, dpf(18), dp(1), 0x22FFFFFF)

            // Logo & Başlık
            val badge = FrameLayout(this@with).apply {
                background = rounded(Palette.withAlpha(Palette.accent, 0x35), dpf(14))
                addView(icon(R.drawable.ic_car, 24, Palette.accent), flp(dp(24), dp(24), Gravity.CENTER))
            }
            addView(badge, lp(dp(44), dp(44)))

            val heading = LinearLayout(this@with).apply {
                orientation = LinearLayout.VERTICAL
                addView(label("NewREX", 18f, bold = true))
                val subText = when {
                    snap.isObdConnected -> "OBD-II CAN-Bus Aktif"
                    snap.speedSource == TelemetrySource.CAR_HARDWARE -> "Android Auto Donanım Bağlı"
                    else -> "Kokpit ve Medya Merkezi"
                }
                addView(label(subText, 11f, Palette.TEXT_DIM), lp(WRAP, WRAP).margins(t = dp(2)))
            }
            addView(heading, lp(0, WRAP, 1f).margins(l = dp(12)))

            // OBD-II Durum Rozeti
            val obdChip = label(if (snap.isObdConnected) "OBD2 ●" else "OBD2 ○", 10f,
                if (snap.isObdConnected) Palette.GREEN else Palette.TEXT_FAINT, bold = true).apply {
                setPadding(dp(8), dp(4), dp(8), dp(4))
                background = rounded(if (snap.isObdConnected) Palette.withAlpha(Palette.GREEN, 0x22) else 0x1AFFFFFF, dpf(8))
            }
            addView(obdChip, lp(WRAP, WRAP).margins(r = dp(8)))

            // Sürüş Durum Rozeti (PARK / SÜRÜŞTE)
            val stateColor = if (snap.isDriving) Palette.ORANGE else Palette.GREEN
            val stateText = if (snap.isDriving) "SÜRÜŞTE" else "PARK"
            addView(label(stateText, 11f, stateColor, bold = true).apply {
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = rounded(Palette.withAlpha(stateColor, 0x28), dpf(12))
            })
        }
    }

    /** 2. Merkez Araç Perspektif Kartı (3D / 2.5D Etkileşimli Model) */
    private fun buildVehicleCenterCard(compact: Boolean, snap: TelemetrySnapshot): View = with(ctx) {
        val h = dp(if (compact) 140 else 175)
        CarModelView(
            context = this,
            scope = scope,
            onEditPlateRequested = { ui.push(VehiclePage(ui)) },
            onHotspotSelected = { hotspot ->
                when (hotspot) {
                    CarModelView.HotspotType.HOOD_ENGINE,
                    CarModelView.HotspotType.FUEL_TANK,
                    CarModelView.HotspotType.WHEELS_SPEED -> ui.push(VehiclePage(ui))
                }
            }
        ).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH, h)
        }
    }

    /** 3. Canlı Telemetri Gösterge Çubuğu (Hız, Devir/Voltaj, Yakıt, Menzil) */
    private fun buildTelemetryRow(compact: Boolean, snap: TelemetrySnapshot): View = with(ctx) {
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val h = dp(if (compact) 60 else 72)

            val speedVal = snap.speedKmh?.let { "%.0f".format(it) } ?: "--"
            val voltVal = snap.batteryVoltage?.let { "%.1f V".format(it) } ?: snap.rpm?.let { "$it RPM" } ?: "--"
            val fuelVal = snap.fuelLevelPercent?.let { "%.0f%%".format(it) } ?: "--"
            val rangeVal = snap.rangeKm?.let { "%.0f km".format(it) } ?: snap.odometerKm?.let { "%.0f km".format(it) } ?: "--"

            val items = listOf(
                Triple("HIZ", "$speedVal km/s", snap.speedSource != TelemetrySource.NONE),
                Triple(if (snap.rpm != null) "DEVİR" else "VOLTAJ", voltVal, snap.batteryVoltage != null || snap.rpm != null),
                Triple("YAKIT", fuelVal, snap.fuelLevelPercent != null),
                Triple("MENZİL / KM", rangeVal, snap.rangeKm != null || snap.odometerKm != null)
            )

            items.forEachIndexed { i, (header, value, hasData) ->
                val cell = LinearLayout(this@with).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = rounded(Palette.SURFACE, dpf(14), dp(1), 0x1AFFFFFF)
                    addView(label(header, 10f, Palette.TEXT_FAINT, bold = true))
                    val valColor = if (hasData) Palette.TEXT else Palette.TEXT_FAINT
                    val valView = label(value, 15f, valColor, bold = true)
                    addView(valView, lp(WRAP, WRAP).margins(t = dp(2)))
                }
                val lp = lp(0, h, 1f).apply {
                    if (i > 0) margins(l = dp(8))
                }
                addView(cell, lp)
            }
        }
    }

    /** Son Kullanılan Medya Kartı */
    private fun recentCard(e: MediaEntry, compact: Boolean): View = with(ctx) {
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = pressable(rounded(Palette.SURFACE, dpf(14), dp(1), 0x1AFFFFFF), dpf(14))
            setPadding(dp(10), dp(6), dp(10), dp(6))

            val iconView = ImageView(this@with).apply {
                val imgUrl = e.image
                if (!imgUrl.isNullOrBlank()) {
                    load(imgUrl) {
                        crossfade(true)
                        transformations(RoundedCornersTransformation(dpf(6)))
                    }
                } else {
                    setImageDrawable(rounded(Palette.accent, dpf(6)))
                }
            }
            addView(iconView, lp(dp(36), dp(36)).margins(r = dp(8)))

            val textBox = LinearLayout(this@with).apply {
                orientation = LinearLayout.VERTICAL
                addView(label(e.title, 13f, bold = true))
                val sub = when (e.kind) {
                    Kind.TV -> "TV Yayını"
                    Kind.RADIO -> "Radyo"
                    Kind.PODCAST -> "Podcast"
                }
                addView(label(sub, 10f, Palette.TEXT_DIM))
            }
            addView(textBox, lp(0, WRAP, 1f))

            val playIcon = label("▶", 13f, Palette.accent, bold = true)
            addView(playIcon, lp(WRAP, WRAP).margins(r = dp(4)))

            onTap {
                val list = Prefs.recents().filter { it.kind == e.kind }
                PlayerHub.play(list, list.indexOfFirst { it.key == e.key }.coerceAtLeast(0))
                if (e.kind == Kind.TV) ui.push(PlayerPage(ui))
            }
        }
    }

    private fun smallCard(sc: Shortcut, compact: Boolean): View = with(ctx) {
        val h = dp(if (compact) 92 else 108)
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = pressable(rounded(Palette.SURFACE, dpf(16), dp(1), if (sc.warn) Palette.RED else 0x1AFFFFFF), dpf(16))
            val badge = FrameLayout(this@with).apply {
                background = rounded(Palette.withAlpha(sc.color, 0x30), dpf(12))
                addView(icon(sc.icon, if (compact) 22 else 26, sc.color), flp(dp(if (compact) 22 else 26), dp(if (compact) 22 else 26), Gravity.CENTER))
            }
            addView(badge, lp(dp(if (compact) 38 else 44), dp(if (compact) 38 else 44)))
            addView(label(sc.title, if (compact) 12f else 13f, if (sc.warn) Palette.RED else Palette.TEXT, bold = true).apply {
                maxLines = 1
                gravity = Gravity.CENTER
            }, lp(WRAP, WRAP).margins(t = dp(6)))
            addView(label(sc.subtitle, 10f, Palette.TEXT_DIM).apply {
                maxLines = 1
                gravity = Gravity.CENTER
            }, lp(WRAP, WRAP).margins(t = dp(2)))
            onTap { sc.onClick() }
        }
    }

    private fun bigCard(
        title: String, subtitle: String, iconRes: Int, color: Int, artKind: CardArt.Kind,
        compact: Boolean, live: Boolean = false, onClick: () -> Unit
    ): View = with(ctx) {
        val h = dp(if (compact) 110 else 180)
        FrameLayout(this).apply {
            background = pressable(gradient(Palette.SURFACE, Palette.withAlpha(color, 0x22), dpf(18)), dpf(18))
            val pad = dp(if (compact) 12 else 16)
            setPadding(pad, pad, pad, pad)

            val content = LinearLayout(this@with).apply {
                orientation = LinearLayout.VERTICAL
                val topRow = LinearLayout(this@with).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    val badge = FrameLayout(this@with).apply {
                        background = rounded(Palette.withAlpha(color, 0x38), dpf(12))
                        addView(icon(iconRes, if (compact) 22 else 28, color), flp(dp(if (compact) 22 else 28), dp(if (compact) 22 else 28), Gravity.CENTER))
                    }
                    addView(badge, lp(dp(if (compact) 38 else 48), dp(if (compact) 38 else 48)))
                    if (live) {
                        val liveChip = label("CANLI", 9f, Palette.RED, bold = true).apply {
                            setPadding(dp(6), dp(2), dp(6), dp(2))
                            background = rounded(Palette.withAlpha(Palette.RED, 0x30), dpf(6))
                        }
                        addView(liveChip, lp(WRAP, WRAP).margins(l = dp(8)))
                    }
                }
                addView(topRow, lp(MATCH, WRAP))

                val textBox = LinearLayout(this@with).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(label(title, if (compact) 15f else 18f, bold = true))
                    addView(label(subtitle, 11f, Palette.TEXT_DIM).apply { maxLines = 1 }, lp(WRAP, WRAP).margins(t = dp(2)))
                }
                addView(textBox, lp(MATCH, WRAP).margins(t = dp(if (compact) 8 else 14)))
            }
            addView(content, flp(MATCH, WRAP, Gravity.BOTTOM))
            onTap { onClick() }
        }
    }

    private fun addGrid(views: List<View>, perRow: Int, height: Int, gap: Int): Unit = with(ctx) {
        var row: LinearLayout? = null
        views.forEachIndexed { i, v ->
            if (i % perRow == 0) {
                row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                container.addView(row, lp(MATCH, height).margins(b = gap))
            }
            val lp = lp(0, MATCH, 1f).apply {
                if (i % perRow > 0) margins(l = gap)
            }
            row?.addView(v, lp)
        }
        val rem = views.size % perRow
        if (rem != 0 && row != null) {
            for (j in 0 until (perRow - rem)) {
                row?.addView(View(this), lp(0, MATCH, 1f).apply { margins(l = gap) })
            }
        }
    }
}

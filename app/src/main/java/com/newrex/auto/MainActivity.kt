// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto

import android.Manifest
import android.animation.LayoutTransition
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import coil.load
import com.newrex.auto.data.Board
import com.newrex.auto.data.CarLog
import com.newrex.auto.data.FuelEntry
import com.newrex.auto.data.Maintenance
import com.newrex.auto.data.ParkedSpot
import com.newrex.auto.data.Playlist
import com.newrex.auto.data.Prefs
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.ClockView
import com.newrex.auto.ui.ColorPickerView
import com.newrex.auto.ui.FmDisplayView
import com.newrex.auto.ui.WaveBackground
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.dpf
import com.newrex.auto.ui.icon
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.neonText
import com.newrex.auto.ui.panel
import com.newrex.auto.ui.rounded
import com.newrex.auto.phone.PhoneCarHelper
import com.newrex.auto.phone.PhoneMaintenanceHelper
import com.newrex.auto.phone.PhoneThemeHelper
import com.newrex.auto.phone.PhoneTroubleshootHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Telefonda açılan ekran: fotoğraflar, listeler, izinler ve yardım. Bölümler dokununca açılır. */
class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var wave: WaveBackground
    private lateinit var logo: TextView
    private lateinit var themeSummary: TextView
    private lateinit var looksSummary: TextView
    private lateinit var locationCard: View
    private lateinit var locationStatus: TextView
    private lateinit var photosBox: LinearLayout
    private lateinit var photosSummary: TextView
    private lateinit var mirrorStatus: TextView
    private lateinit var mirrorButton: Button
    private lateinit var mirrorSummary: TextView
    private lateinit var parkStatus: TextView
    private lateinit var parkButtons: LinearLayout
    private lateinit var parkSummary: TextView
    private lateinit var maintenanceBox: LinearLayout
    private lateinit var maintenanceSummary: TextView
    private lateinit var playlistBox: LinearLayout
    private lateinit var carBox: LinearLayout
    private lateinit var carSummary: TextView
    private lateinit var playlistSummary: TextView
    private val tr = java.util.Locale("tr", "TR")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        Board.init(this)
        CarLog.init(this)
        Palette.load()
        CarEntry.apply(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(28), dp(18), dp(32))
            layoutTransition = LayoutTransition()
        }

        logo = label("", 36f, bold = true)
        col.addView(logo)
        paintLogo()
        col.addView(label("Android Auto için TV, YouTube, radyo ve daha fazlası", 14f, Palette.TEXT_DIM, lines = 2),
            lp(MATCH, WRAP).margins(t = dp(4), b = dp(20)))

        // Konum izni yoksa en üstte tek bir uyarı; izin verilince kaybolur
        locationStatus = label("Araç hız bilgisi vermezse hareketi telefonun GPS'inden algılarız (park modu, park yeri, hava durumu).",
            14f, Palette.TEXT_DIM, lines = 4)
        locationCard = panel().apply {
            setPadding(dp(16), dp(14), dp(16), dp(16))
            background = rounded(Palette.withAlpha(Palette.ORANGE, 0x22), dpf(18), dp(1), Palette.withAlpha(Palette.ORANGE, 0x88))
            addView(label("📍 Konum izni gerekli", 16f, bold = true))
            addView(locationStatus, lp(MATCH, WRAP).margins(t = dp(6)))
            addView(button("Konum iznini ver") {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            }, lp(MATCH, WRAP).margins(t = dp(10)))
        }
        col.addView(locationCard, lp(MATCH, WRAP).margins(b = dp(12)))

        photosBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        photosSummary = summary()
        col.addView(section(R.drawable.ic_photo, 0xFFE040FB.toInt(), "Araç ekranı fotoğrafları", photosSummary,
            label("Araçta açılışta saatin yanında görünür. Araç ekranında ✎ ile taşıyabilir, büyütebilir veya kaldırabilirsin. " +
                "Fotoğraflar sadece bu telefonda saklanır.", 14f, Palette.TEXT_DIM, lines = 5),
            photosBox,
            button("Fotoğraf ekle") { PhotoPickActivity.start(this) }))

        themeSummary = summary()
        col.addView(section(R.drawable.ic_settings, 0xFF00BCD4.toInt(), "Tema", themeSummary, *themeBody()))

        val qualitySummary = summary()
        col.addView(section(R.drawable.ic_live_tv, 0xFF06B6D4.toInt(), "TV görüntü kalitesi", qualitySummary,
            choiceButtons(PlayerHub.QUALITIES.map { it.first }, { Prefs.tvQuality }, { Prefs.tvQuality = it }, qualitySummary,
                "Otomatik: yayının en iyi kalitesi. Düşük seçersen telefon daha az ısınır, daha az internet harcar.")))

        val saverSummary = summary()
        col.addView(section(R.drawable.ic_speed, 0xFF16B98A.toInt(), "Tasarruf modu", saverSummary, saverBody(saverSummary)))

        looksSummary = summary()
        col.addView(section(R.drawable.ic_radio, 0xFFFFB238.toInt(), "Saat ve radyo görünümü", looksSummary, *looksBody()))

        mirrorStatus = label("", 14f, Palette.TEXT_DIM, lines = 6)
        mirrorButton = button("Ekranı arabaya yansıt") {
            if (Mirror.active.value) { Mirror.stop(this); mirrorStatus.postDelayed({ refreshMirror() }, 600); return@button }
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
            Mirror.requestStart(this)
        }
        mirrorSummary = summary()
        col.addView(section(R.drawable.ic_cast, 0xFF16B98A.toInt(), "Ekran yansıtma", mirrorSummary, mirrorStatus, mirrorButton))

        parkStatus = label("", 14f, Palette.TEXT_DIM, lines = 4)
        parkButtons = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        parkSummary = summary()
        col.addView(section(R.drawable.ic_parking, 0xFF2F80ED.toInt(), "Arabam nerede?", parkSummary, parkStatus, parkButtons))

        maintenanceBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        maintenanceSummary = summary()
        col.addView(section(R.drawable.ic_build, Palette.ORANGE, "Bakım hatırlatıcı", maintenanceSummary,
            label("Tarihe dokunup seç. 15 gün kala araç ekranında uyarı çıkar.", 14f, Palette.TEXT_DIM, lines = 3),
            maintenanceBox))

        carBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        carSummary = summary()
        col.addView(section(R.drawable.ic_gas, 0xFF16B98A.toInt(), "Yakıt ve kilometre", carSummary, carBox))

        playlistBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        playlistSummary = summary()
        col.addView(section(R.drawable.ic_live_tv, 0xFF7C5CFF.toInt(), "IPTV listeleri", playlistSummary,
            label("M3U / M3U8 bağlantısı ya da telefondaki bir liste dosyası ekleyebilirsin.", 14f, Palette.TEXT_DIM, lines = 3),
            playlistBox,
            button("Bağlantı ile liste ekle") { addPlaylistDialog() },
            button("Telefondan dosya seç") {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }, PICK_FILE)
            },
        ))

        col.addView(section(R.drawable.ic_info, 0xFF8B93A7.toInt(), "Yardım", summary().apply { text = "Arabada görünmüyorsa buraya bak" },
            label("1. Telefonu araca bağla, Android Auto açılsın.\n" +
                "2. Araç ekranında uygulamalar menüsüne gir.\n" +
                "3. \"NewREX Android Auto\" simgesine dokun.\n\n" +
                "Görünmüyorsa: Android Auto ayarları → Geliştirici ayarları → \"Bilinmeyen kaynaklar\" açık olmalı.",
                14f, Palette.TEXT_DIM, lines = 10),
            label("Sorun giderme", 15f, bold = true),
            label(verdict(), 14f, Palette.TEXT, lines = 8),
            fixRow(),
            entryRow(),
            button("Pil kısıtlamasını kaldır") { askBatteryExemption() },
            label(diagnostics(), 13f, Palette.TEXT_DIM, lines = 20)))

        col.addView(label(
            "NewREX Android Auto ${BuildConfig.VERSION_NAME} • Orijinal geliştirici: ${Brand.ORIGINAL_DEVELOPER}\n" +
                "Reklam yok, takip yok, hiçbir veri toplanmaz.\n\n${Brand.COPYRIGHT}\n${Brand.LICENSE_NOTICE}",
            12f, Palette.TEXT_FAINT, lines = 10).apply { gravity = Gravity.CENTER }, lp(MATCH, WRAP).margins(t = dp(18)))

        wave = WaveBackground(this)
        paintWave(animate = false)
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Palette.BG_TOP)
            addView(wave, FrameLayout.LayoutParams(MATCH, MATCH))
            addView(ScrollView(this@MainActivity).apply {
                isVerticalScrollBarEnabled = false
                // Android 15 içeriği durum çubuğunun altına kadar uzatıyor; başlık saatin altına girmesin
                setOnApplyWindowInsetsListener { v, insets ->
                    // Eski Android'lerde (8–10) yeni insets çağrısı uygulamayı çökertiyordu
                    val bars = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(insets, v)
                        .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    v.setPadding(0, bars.top, 0, bars.bottom)
                    insets
                }
                addView(col)
            }, FrameLayout.LayoutParams(MATCH, MATCH))
        })

        scope.launch { Board.photos.collect { refreshPhotos() } }
        scope.launch { CarLog.changes.collect { refreshCar() } }
        val untrusted = !BuildConfig.PARK_ONLY && !CarEntry.trustedInstall(this)
        if (untrusted && Prefs.selfInstallTried == 0) showDrivingGuide()
        else if (untrusted && Prefs.installFailShown != BuildConfig.VERSION_CODE) showInstallFailed()
        else scope.launch { Updates.check(this@MainActivity)?.let { Updates.show(this@MainActivity, it) } }
        if (intent?.getBooleanExtra(EXTRA_ASK_LOCATION, false) == true) askLocationNow()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_ASK_LOCATION, false)) askLocationNow()
    }

    private fun askLocationNow() {
        if (!DriveState.hasLocationPermission(this))
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
    }

    override fun onResume() {
        super.onResume()
        wave.running = true
        refreshLocation()
        refreshPlaylists()
        refreshMirror()
        refreshPark()
        refreshMaintenance()
        // Yansıtma servisi onaydan hemen sonra başlıyor; durumu biraz sonra tekrar oku
        mirrorStatus.postDelayed({ refreshMirror() }, 900)
    }

    override fun onPause() {
        wave.running = false
        super.onPause()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Başlığa dokununca açılıp kapanan bölüm. */
    private fun section(icon: Int, color: Int, title: String, summary: TextView, vararg body: View): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(6), 0, 0)
            body.forEach { addView(it, lp(MATCH, WRAP).margins(t = dp(8))) }
        }
        val chevron = icon(R.drawable.ic_chevron_right, 22, Palette.TEXT_FAINT)
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(FrameLayout(this@MainActivity).apply {
                background = rounded(Palette.withAlpha(color, 0x40), dpf(12))
                addView(icon(icon, 22, color), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
            }, lp(dp(42), dp(42)))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(label(title, 16f, bold = true))
                addView(summary, lp(MATCH, WRAP).margins(t = dp(3)))
            }, lp(0, WRAP, 1f).margins(l = dp(14)))
            addView(chevron, lp(dp(22), dp(22)))
            setOnClickListener {
                val open = content.visibility != View.VISIBLE
                content.visibility = if (open) View.VISIBLE else View.GONE
                chevron.animate().rotation(if (open) 90f else 0f).setDuration(180).start()
            }
        }
        return panel().apply {
            setPadding(dp(14), dp(12), dp(14), dp(14))
            background = rounded(0x1FFFFFFF, dpf(18))
            layoutTransition = LayoutTransition()
            addView(head, lp(MATCH, WRAP))
            addView(content, lp(MATCH, WRAP))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).margins(b = dp(10))
        }
    }

    private fun summary() = label("", 13f, Palette.TEXT_DIM, lines = 1)

    // ---- Yakıt ve kilometre ----

    private fun refreshCar() {
        val odo = CarLog.estimatedOdometer()
        val stats = CarLog.fuelStats(odo)
        carSummary.text = listOfNotNull(
            odo?.let { String.format(tr, "~%,.0f km", it) },
            stats.consumption?.let { "ort %.1f L/100 km".format(it) },
        ).joinToString(" • ").ifEmpty { "Kilometre, yakıt alımları, yağ değişimi" }

        PhoneCarHelper.buildCarViews(this, carBox) { refreshCar() }
    }

    // ---- Tema ----

    private fun paintLogo() {
        logo.text = SpannableStringBuilder("NewREX").apply {
            setSpan(ForegroundColorSpan(Palette.accent), 3, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        logo.neonText(12)
    }

    private fun paintWave(animate: Boolean = true) {
        wave.setPalette(0, Palette.accent, Palette.companion(Palette.accent), animate, base = Palette.safeBackground(Prefs.bgColor))
        themeSummaryText()
    }

    private fun themeSummaryText() {
        if (::themeSummary.isInitialized)
            themeSummary.text = if (Prefs.bgColor == 0) "Tema rengi ve arka plan" else "Tema rengi ve özel arka plan"
    }

    /** Tema değişti: araç ekranı kendini zaten yeniler; burada logo ve arka planı güncelliyoruz. */
    private fun themeChanged() {
        Palette.load()
        paintLogo()
        paintWave()
    }

    private fun themeBody(): Array<View> = PhoneThemeHelper.buildThemeBody(this) { themeChanged() }

    // ---- Saat ve radyo görünümü ----

    private fun looksBody(): Array<View> = PhoneThemeHelper.buildLooksBody(this) { looksSummary.text = it }

    private fun refreshPhotos() {
        val photos = Board.photos.value
        photosSummary.text = if (photos.isEmpty()) "Henüz fotoğraf yok" else "${photos.size} fotoğraf"
        photosBox.removeAllViews()
        val gap = dp(8)
        val cellSize = (resources.displayMetrics.widthPixels - dp(18 * 2 + 14 * 2) - gap * 2) / 3
        photos.chunked(3).forEach { rowPhotos ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowPhotos.forEachIndexed { i, p ->
                val cell = FrameLayout(this)
                cell.addView(ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = rounded(0x22FFFFFF, dpf(12))
                    clipToOutline = true
                    load(Board.file(p.file)) { size(cellSize, cellSize) }
                }, FrameLayout.LayoutParams(MATCH, MATCH))
                cell.addView(TextView(this).apply {
                    text = "✕"
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    background = rounded(0xCC000000.toInt(), dpf(14))
                    setOnClickListener {
                        AlertDialog.Builder(this@MainActivity).setMessage("Bu fotoğraf silinsin mi?")
                            .setPositiveButton("Sil") { _, _ -> Board.deletePhoto(p.file) }
                            .setNegativeButton("Vazgeç", null).show()
                    }
                }, FrameLayout.LayoutParams(dp(28), dp(28), Gravity.TOP or Gravity.END).margins(t = dp(6), r = dp(6)))
                row.addView(cell, lp(cellSize, cellSize).margins(r = if (i < 2) gap else 0))
            }
            photosBox.addView(row, lp(MATCH, WRAP).margins(t = gap))
        }
    }

    /** Uygulama arabada görünmüyorsa ekran görüntüsü alınıp geliştiriciye gönderilecek bilgiler. */
    /** Görünmeme sorununun olası sebebi, sade bir dille. */
    private fun verdict(): String {
        val info = if (Build.VERSION.SDK_INT >= 30) runCatching { packageManager.getInstallSourceInfo(packageName) }.getOrNull() else null
        @Suppress("DEPRECATION")
        val installer = info?.installingPackageName ?: runCatching { packageManager.getInstallerPackageName(packageName) }.getOrNull()
        val initiating = info?.initiatingPackageName
        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        val sleeping = !pm.isIgnoringBatteryOptimizations(packageName)
        val brand = Build.MANUFACTURER.lowercase()
        val oem = listOf("xiaomi", "redmi", "poco", "oppo", "realme", "oneplus", "huawei", "honor", "vivo").any { brand.contains(it) }
        val head = when {
            installer != "com.android.vending" ->
                "❌ Android Auto bu kurulumu tanımıyor. Aşağıdaki \"Arabada görünür yap\" düğmesine bas."
            initiating != null && initiating !in SelfInstall.INSTALLERS ->
                "❌ Telefonun kendi yükleyicisi kullanılmış ($initiating). Aşağıdaki \"Arabada görünür yap\" düğmesine bas."
            CarEntry.effective(this) == CarEntry.PARKED && Prefs.lastParkedSession == 0L ->
                "⚠️ Park kapısı açık ama araçta henüz açılmadı. Android Auto → Geliştirici ayarları → \"Bilinmeyen kaynaklar\" " +
                    "açık olmalı; sonra telefonu arabaya bağla, park halindeyken uygulamalar listesinde NewREX'e dokun."
            CarEntry.effective(this) == CarEntry.PARKED -> "✅ Park kapısı çalışıyor."
            Prefs.lastCarBind == 0L ->
                "⚠️ Kurulum doğru ama Android Auto NewREX'i hiç açmayı denemedi. Android Auto → Geliştirici ayarları → " +
                    "\"Bilinmeyen kaynaklar\" açık olmalı; açıksa kapatıp tekrar aç ve telefonu yeniden başlat." +
                    if (oem) " Bu marka ($brand) Android Auto'da ek kısıtlama yapabiliyor." else ""
            Prefs.lastCarSession == 0L ->
                "⚠️ Android Auto NewREX'i gördü ama ekranda açmadı. Android Auto → \"Başlatıcıyı özelleştir\" listesinde NewREX'i işaretle."
            else -> "✅ Kurulum ve bağlantı düzgün görünüyor."
        }
        return if (sleeping) "$head\n🔋 Pil tasarrufu NewREX'i uyutabiliyor; aşağıdaki düğmeyle kısıtlamayı kaldır." else head
    }

    /** Basit seçim düğmeleri (ör. TV kalitesi). */
    private fun choiceButtons(names: List<String>, get: () -> Int, set: (Int) -> Unit, summary: TextView, note: String): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun paint() {
            for (i in 0 until row.childCount) {
                val sel = i == get()
                row.getChildAt(i).background = rounded(if (sel) Palette.withAlpha(Palette.accent, 0x33) else 0x22FFFFFF, dpf(18),
                    dp(if (sel) 2 else 1), if (sel) Palette.accent else 0x33FFFFFF)
            }
            summary.text = names.getOrNull(get()) ?: ""
        }
        names.forEachIndexed { i, n ->
            row.addView(TextView(this).apply {
                text = n
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 14f
                setOnClickListener { set(i); paint() }
            }, lp(0, dp(40), 1f).margins(r = dp(6)))
        }
        paint()
        box.addView(row, lp(MATCH, WRAP))
        box.addView(label(note, 13f, Palette.TEXT_DIM, lines = 3), lp(MATCH, WRAP).margins(t = dp(8)))
        return box
    }

    /** Tasarruf modu: Kapalı / Açık / Otomatik. */
    private fun saverBody(summary: TextView): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val info = label("", 13f, Palette.TEXT_DIM, lines = 5)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun paint() {
            for (i in 0 until row.childCount) {
                val sel = i == Prefs.saverMode
                row.getChildAt(i).background = rounded(if (sel) Palette.withAlpha(Palette.accent, 0x33) else 0x22FFFFFF, dpf(18),
                    dp(if (sel) 2 else 1), if (sel) Palette.accent else 0x33FFFFFF)
            }
            val why = Saver.reason()
            summary.text = if (why != null) "Açık ($why)" else Saver.NAMES[Prefs.saverMode]
            info.text = "Açıkken arka plan sabit durur, dokununca çıkan efektler kapanır. Görüntü ve ses kalitesi değişmez. " +
                "Otomatik: telefon pil tasarrufundayken ya da ısındığında kendiliğinden açılır."
        }
        Saver.NAMES.forEachIndexed { i, n ->
            row.addView(TextView(this).apply {
                text = n
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 14f
                setOnClickListener { Prefs.saverMode = i; paint() }
            }, lp(0, dp(40), 1f).margins(r = dp(6)))
        }
        paint()
        box.addView(row, lp(MATCH, WRAP))
        box.addView(info, lp(MATCH, WRAP).margins(t = dp(8)))
        return box
    }

    /**
     * Normal APK kurulumunda Android Auto NewREX'i sadece park halindeyken açar. Uygulama her açılışta, bu
     * düzelene kadar, sürüşte de çalışması için gereken adımları nazikçe anlatır ve tek dokunuşla başlatır.
     */
    private fun showDrivingGuide() {
        AlertDialog.Builder(this)
            .setTitle("Sürüşte de çalışması için son bir adım")
            .setMessage("NewREX kuruldu 🎉 Şu an arabada sadece park halindeyken açılıyor. Sürüşte de kullanabilmek için " +
                "bir kez şunları yapman yeterli:\n\n" +
                "1. Play Store → sağ üstte profil resmin → Play Protect → ⚙️ → \"Uygulamaları Play Protect ile tara\"yı kısa süreliğine kapat.\n" +
                "    Samsung'daysan: Ayarlar → Güvenlik ve gizlilik → Otomatik Engelleyici'yi de kapat.\n\n" +
                "2. Buraya dön ve \"Şimdi yap\"a bas. Açılan ekranda \"Güncelle\"ye bas.\n\n" +
                "3. Kapattığın ayarları geri aç. Bir şey sorarsa \"Gönderme\" de.\n\n" +
                "4. Telefonu arabadan çıkarıp tekrar bağla. Hepsi bu!")
            .setPositiveButton("Şimdi yap") { _, _ -> fixInstall() }
            .setNegativeButton("Sonra", null)
            .show()
    }

    /**
     * "Güncelle"ye basıldı ama Android Auto kurulumu hâlâ tanımıyor (çoğunlukla Xiaomi/Redmi/POCO'da telefonun kendi
     * yükleyicisi araya giriyor). Her açılışta aynı adımı istemek yerine bir kez ne olduğunu anlatır; ekran
     * görüntüsü geliştiriciye gönderilebilsin diye teknik bilgiyi de yazar.
     */
    private fun showInstallFailed() {
        PhoneTroubleshootHelper.showInstallFailed(this)
    }

    /** "Arabada görünür yap": King Installer'ın işini uygulamanın içinden yapar. */
    private fun fixRow(): View = PhoneTroubleshootHelper.buildFixRow(this)

    private fun fixInstall() {
        PhoneTroubleshootHelper.fixInstall(this)
    }

    /** Arabaya giriş kapısı seçimi: Otomatik / Normal / Park. */
    private fun entryRow(): View = PhoneTroubleshootHelper.buildEntryRow(this)

    private fun askBatteryExemption() {
        PhoneTroubleshootHelper.askBatteryExemption(this)
    }

    private fun diagnostics(): String = PhoneTroubleshootHelper.getDiagnostics(this)

    private fun refreshMirror() {
        val on = Mirror.active.value
        mirrorStatus.text = if (on) "✅ Ekran araca yansıtılıyor. Araçta NewREX → Yansıt'a gir."
        else "Telefonundaki her şeyi (oyun, sosyal medya, uygulamalar) araç ekranında göster. Her başlatmada telefonda bir kez \"Başlat\" onayı istenir. " +
            "Netflix gibi korumalı uygulamalar siyah görünür. Yansıtma sırasında telefon ekranı açık kalır; uygulamaları telefonun kendi ekranından kullanırsın."
        mirrorButton.text = if (on) "Yansıtmayı durdur" else "Ekranı arabaya yansıt"
        mirrorSummary.text = if (on) "✅ Yansıtılıyor" else "Telefon ekranını araçta göster"
    }

    private fun refreshPark() {
        parkButtons.removeAllViews()
        val spot = Prefs.parkedSpot
        if (spot == null) {
            parkStatus.text = "Araç kapanınca (Android Auto bağlantısı kesilince) bulunduğun yer otomatik kaydedilir. " +
                "Henüz kayıtlı park yeri yok."
            parkSummary.text = "Henüz kayıtlı park yeri yok"
        } else {
            val time = java.text.SimpleDateFormat("d MMMM yyyy, HH:mm", tr).format(java.util.Date(spot.time))
            parkStatus.text = "🅿️ Son park: $time\nKonum hassasiyeti: ~${spot.accuracy.toInt()} m"
            parkSummary.text = "Son park: $time"
            parkButtons.addView(button("Haritada göster") { openMap(spot, walk = false) }, lp(MATCH, WRAP).margins(t = dp(8)))
            parkButtons.addView(button("Yürüyerek yol tarifi") { openMap(spot, walk = true) }, lp(MATCH, WRAP).margins(t = dp(8)))
            parkButtons.addView(button("Park yerini sil") { Prefs.parkedSpot = null; refreshPark() }, lp(MATCH, WRAP).margins(t = dp(8)))
            val ago = (System.currentTimeMillis() - spot.time) / 60_000
            parkStatus.append("\n⏱️ " + (if (ago >= 60) "${ago / 60} sa ${ago % 60} dk" else "$ago dk") + "dır park halinde")
        }
        // Otopark süresi hatırlatıcısı
        val timerEnd = ParkTimer.endTime(this)
        parkButtons.addView(label(if (timerEnd > 0) "⏰ Otopark süren " +
            java.text.SimpleDateFormat("HH:mm", tr).format(java.util.Date(timerEnd)) + " itibarıyla doluyor (10 dk önce bildirim gelir)"
            else "⏰ Otopark süresi hatırlatıcısı kur:", 14f, Palette.TEXT_DIM, lines = 3), lp(MATCH, WRAP).margins(t = dp(14)))
        val timers = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(30 to "30 dk", 60 to "1 sa", 120 to "2 sa", 180 to "3 sa").forEach { (m, t) ->
            timers.addView(button(t) {
                if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 3)
                ParkTimer.start(this, m)
                refreshPark()
            }, lp(0, WRAP, 1f).margins(r = dp(6)))
        }
        if (timerEnd > 0) timers.addView(button("İptal") { ParkTimer.cancel(this); refreshPark() }, lp(0, WRAP, 1f))
        parkButtons.addView(timers, lp(MATCH, WRAP).margins(t = dp(8)))
        if (timerEnd > 0) parkSummary.text = parkSummary.text.toString() + " • hatırlatıcı açık"
        parkButtons.addView(button("Şu anki yerimi park yeri olarak kaydet") {
            val loc = DriveState.bestLocation(this)
            if (loc == null) {
                AlertDialog.Builder(this).setMessage("Konum alınamadı. Konum iznini ver ve konumu aç.").setPositiveButton("Tamam", null).show()
            } else {
                Prefs.parkedSpot = ParkedSpot(loc.latitude, loc.longitude, System.currentTimeMillis(), loc.accuracy)
                refreshPark()
            }
        }, lp(MATCH, WRAP).margins(t = dp(8)))
    }

    private fun openMap(spot: ParkedSpot, walk: Boolean) {
        val ll = "${spot.lat},${spot.lon}"
        val nav = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$ll&mode=w"))
        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$ll?q=$ll(Arabam)"))
        runCatching { startActivity(if (walk) nav else geo) }.onFailure { runCatching { startActivity(geo) } }
    }

    private fun refreshMaintenance() {
        PhoneMaintenanceHelper.buildMaintenanceViews(this, maintenanceBox, maintenanceSummary) {
            refreshMaintenance()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        refreshLocation()
        // Araç bağlıysa hız ve yolculuk ölçümü hemen başlasın
        DriveState.ensureGps()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        if (requestCode != PICK_FILE || resultCode != RESULT_OK) return
        // Araç tarafı dosyayı daha sonra da okuyabilsin
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "Liste dosyası"
        Prefs.playlists = Prefs.playlists + Playlist(name.substringBeforeLast('.'), uri.toString())
        refreshPlaylists()
    }

    private fun refreshLocation() {
        val granted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        locationCard.visibility = if (granted) View.GONE else View.VISIBLE
    }

    private fun refreshPlaylists() {
        playlistBox.removeAllViews()
        val lists = Prefs.playlists
        playlistSummary.text = if (lists.isEmpty()) "Liste yok" else lists.joinToString(", ") { it.name }
        if (lists.isEmpty()) {
            playlistBox.addView(label("Henüz liste yok.", 14f, Palette.TEXT_FAINT), lp(MATCH, WRAP).margins(t = dp(10)))
        }
        lists.forEachIndexed { i, pl ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(6), dp(10))
                background = rounded(0x14FFFFFF, dpf(12))
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(label(pl.name, 15f, bold = true))
                    addView(label(pl.url, 11f, Palette.TEXT_FAINT), lp(MATCH, WRAP).margins(t = dp(2)))
                }, lp(0, WRAP, 1f))
                addView(Button(this@MainActivity).apply {
                    text = "Sil"
                    setOnClickListener {
                        Prefs.playlists = Prefs.playlists.filterIndexed { j, _ -> j != i }
                        refreshPlaylists()
                    }
                })
            }
            playlistBox.addView(row, lp(MATCH, WRAP).margins(t = dp(10)))
        }
    }

    private fun addPlaylistDialog() {
        val name = EditText(this).apply { hint = "Liste adı (ör. Evdeki liste)"; setSingleLine() }
        val url = EditText(this).apply {
            hint = "https://…/liste.m3u"
            setSingleLine()
            inputType = InputType.TYPE_TEXT_VARIATION_URI
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(name)
            addView(url)
        }
        AlertDialog.Builder(this)
            .setTitle("IPTV listesi ekle")
            .setView(box)
            .setPositiveButton("Ekle") { _, _ ->
                val u = url.text.toString().trim()
                if (u.startsWith("http://") || u.startsWith("https://")) {
                    val n = name.text.toString().trim().ifEmpty { "Liste ${Prefs.playlists.size + 1}" }
                    Prefs.playlists = Prefs.playlists + Playlist(n, u)
                    refreshPlaylists()
                }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = rounded(Palette.accent, dpf(12))
        setOnClickListener { onClick() }
    }

    companion object {
        private const val PICK_FILE = 7
        private const val EXTRA_ASK_LOCATION = "askLocation"

        /** Araç ekranından: telefonda NewREX'i açıp konum iznini sorar. */
        fun askLocation(context: android.content.Context) {
            context.startActivity(Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_ASK_LOCATION, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
    }
}

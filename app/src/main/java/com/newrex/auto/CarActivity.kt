// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.newrex.auto.data.Prefs
import android.view.Gravity
import com.newrex.auto.ui.CarUi
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.dpf
import com.newrex.auto.ui.flp
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.panel
import com.newrex.auto.ui.pill
import com.newrex.auto.ui.rounded

/**
 * Android Auto'nun "park halindeyken" uygulama kapısı (Android 15+). Bu kapı Play Store dışından kurulan
 * uygulamalarda da Android Auto'daki "Bilinmeyen kaynaklar" ayarıyla açılır; King Installer'ın çalışmadığı
 * telefonlarda (Xiaomi, Tecno...) OmniDrive arabada buradan görünür. Ekran, araç ekranında doğrudan çalışan
 * normal bir uygulama ekranıdır; sürüş başlayınca Android Auto bu kapıyı kendisi kapatır.
 */
class CarActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var ui: CarUi? = null
    private var guideShown = false
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        com.newrex.auto.data.CarLog.init(this)
        Prefs.lastCarSession = System.currentTimeMillis()
        Prefs.lastParkedSession = System.currentTimeMillis()
        DriveState.start(this)
        com.newrex.auto.ui.SessionAlerts.reset()
        connectMediaService()
    }


    /**
     * MediaController bağlantısı MediaLibraryService'i oluşturur ve bağlı tutar.
     * Oynatıcı yalnızca servis içinde başlatılır; UI komutları MediaController üzerinden oturuma iletilir.
     */
    private fun connectMediaService() {
        val token = SessionToken(this, ComponentName(this, OmniDriveMediaService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            if (destroyed) return@addListener
            runCatching { future.get() }
                .onSuccess { controller ->
                    mediaController = controller
                    PlayerHub.attachController(controller)
                    buildUi()
                }
                .onFailure {
                    setContentView(android.widget.TextView(this).apply {
                        text = "Medya hizmeti başlatılamadı. OmniDrive'i yeniden açın."
                        gravity = Gravity.CENTER
                        setTextColor(Palette.TEXT)
                        setPadding(dp(24), dp(24), dp(24), dp(24))
                    })
                }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun buildUi(startOnApps: Boolean = false) {
        ui?.destroy()
        val newUi = CarUi(this, startOnApps) { openSettings ->
            handler.post {
                buildUi(openSettings || ui?.onApps == true)
                if (openSettings) ui?.push(com.newrex.auto.pages.SettingsPage(ui!!))
            }
        }
        ui = newUi
        val frame = android.widget.FrameLayout(this).apply { addView(newUi.root) }
        setContentView(frame)
        newUi.setInsets(0, 0, 0, 0)
        // Telefonda zaten denendiyse (ör. Xiaomi'de tutmadıysa) her bağlantıda aynı adımları tekrar isteme
        if (!guideShown && !BuildConfig.PARK_ONLY && !CarEntry.trustedInstall(this) && Prefs.selfInstallTried == 0) {
            guideShown = true
            frame.addView(drivingGuide { v -> frame.removeView(v) })
        }
    }

    /**
     * Park kapısından açıldık: Android Auto sürüşte uygulamayı kapatacak. Kullanıcıya bunu nasıl düzelteceğini
     * telefondaki birkaç adımla, nazikçe anlatır (her bağlantıda bir kez; dokununca kapanır).
     */
    private fun drivingGuide(close: (android.view.View) -> Unit): android.view.View {
        val box = android.widget.FrameLayout(this)
        box.setBackgroundColor(0xCC05060C.toInt())
        box.isClickable = true
        val card = panel().apply {
            setPadding(dp(24), dp(20), dp(24), dp(20))
            background = rounded(0xF2161A2B.toInt(), dpf(22), dp(1), Palette.withAlpha(Palette.accent, 0x99))
            addView(label("🚗  Sürüşte de kullanmak için küçük bir adım kaldı", 20f, bold = true, lines = 2))
            addView(label("OmniDrive şu an \"park modunda\" açıldı: araç hareket edince Android Auto uygulamayı kapatır. " +
                "Bunu telefondan bir kez düzeltmen yeterli:", 15f, Palette.TEXT_DIM, lines = 3), lp(MATCH, WRAP).margins(t = dp(10)))
            addView(label("1.  Telefonda Play Protect'i (Samsung'da Otomatik Engelleyici'yi de) kısa süreliğine kapat\n" +
                "2.  OmniDrive'i aç → çıkan pencerede \"Şimdi yap\" → \"Güncelle\"\n" +
                "3.  Kapattıklarını geri aç, telefonu arabadan çıkarıp tekrar bağla",
                15f, Palette.TEXT, lines = 8).apply { setLineSpacing(dpf(4), 1f) }, lp(MATCH, WRAP).margins(t = dp(12)))
            addView(pill("Anladım", selected = true) { close(box) }, lp(WRAP, dp(44)).margins(t = dp(16)))
        }
        // Küçük araç ekranlarında da sığsın diye kaydırılabilir
        val scroll = android.widget.ScrollView(this).apply { isFillViewport = false; addView(card) }
        box.addView(scroll, flp(dp(820).coerceAtMost(resources.displayMetrics.widthPixels - dp(32)), WRAP, Gravity.CENTER))
        return box
    }

    override fun onResume() {
        super.onResume()
        ui?.setSurfaceVisible(true)
    }

    override fun onPause() {
        ui?.setSurfaceVisible(false)
        super.onPause()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val u = ui
        if (u != null && u.pageCount > 1) u.back() else @Suppress("DEPRECATION") super.onBackPressed()
    }

    override fun onDestroy() {
        destroyed = true
        mediaController?.let(PlayerHub::detachController)
        mediaController = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        handler.removeCallbacksAndMessages(null)
        ui?.destroy()
        ui = null
        if (isFinishing) {
            TripSaver.save()
            ParkSaver.save(this)
            DriveState.stop()
        }
        super.onDestroy()
    }
}

// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.phone

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.newrex.auto.BuildConfig
import com.newrex.auto.CarEntry
import com.newrex.auto.SelfInstall
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Telefon ekranı: Kurulum sorunlarını giderme, Android Auto tanıları ve optimizasyon kontrolleri
 */
object PhoneTroubleshootHelper {
    private val tr = Locale("tr", "TR")

    fun buildFixRow(activity: Activity): View {
        if (BuildConfig.PARK_ONLY) return View(activity)
        val box = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        box.addView(Button(activity).apply {
            text = "Arabada görünür yap"
            setOnClickListener { fixInstall(activity) }
        }, lp(MATCH, WRAP).margins(t = activity.dp(8)))
        box.addView(label("Yükleyiciler:\n" + SelfInstall.report(activity), 12f, Palette.TEXT_FAINT, lines = 4), lp(MATCH, WRAP).margins(t = activity.dp(4)))
        return box
    }

    fun fixInstall(activity: Activity) {
        fun toast(t: String) = Toast.makeText(activity, t, Toast.LENGTH_LONG).show()
        if (!SelfInstall.canRequestInstalls(activity)) {
            toast("Önce NewREX'e \"bilinmeyen uygulamaları yükleme\" izni ver, sonra tekrar bas.")
            runCatching { activity.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))) }
            return
        }
        val installer = SelfInstall.available(activity)
        if (installer == null) {
            val disabled = SelfInstall.INSTALLERS.firstOrNull { SelfInstall.state(activity, it) == SelfInstall.State.DISABLED }
            AlertDialog.Builder(activity)
                .setTitle("Google yükleyicisi kapalı")
                .setMessage(if (disabled != null)
                    "Bu telefonda Google'ın paket yükleyicisi kapatılmış. Bilgisayara bağlayıp şu komutu bir kez çalıştır, sonra tekrar bas:\n\nadb shell pm enable $disabled"
                else "Bu telefonda Google'ın paket yükleyicisi bulunamadı. Bu Yardım bölümünün ekran görüntüsünü geliştiriciye gönder.")
                .setPositiveButton("Tamam", null)
                .show()
            return
        }
        toast("Açılan ekranda \"Güncelle\" ya da \"Yükle\"ye bas. NewREX yeniden açılınca bu bölüme tekrar bak.")
        if (!SelfInstall.start(activity, installer)) {
            toast("Yükleyici açılamadı. Bu bölümün ekran görüntüsünü geliştiriciye gönder.")
        } else {
            Prefs.selfInstallTried = BuildConfig.VERSION_CODE
            Prefs.installFailShown = 0
        }
    }

    fun showInstallFailed(activity: Activity) {
        Prefs.installFailShown = BuildConfig.VERSION_CODE
        val info = if (Build.VERSION.SDK_INT >= 30) runCatching { activity.packageManager.getInstallSourceInfo(activity.packageName) }.getOrNull() else null
        @Suppress("DEPRECATION")
        val installer = info?.installingPackageName ?: runCatching { activity.packageManager.getInstallerPackageName(activity.packageName) }.getOrNull()
        AlertDialog.Builder(activity)
            .setTitle("Kurulum bu telefonda tutmadı")
            .setMessage("\"Güncelle\"ye bastın ama Android Auto NewREX'i hâlâ tanımıyor. Bu, bazı telefonlarda (özellikle " +
                "Xiaomi, Redmi ve POCO) telefonun kendi yükleyicisi araya girdiğinde oluyor. Merak etme, NewREX arabada " +
                "park halindeyken çalışmaya devam eder.\n\n" +
                "Bir kez daha denemek istersen:\n" +
                "• Play Protect'in kapalı olduğundan emin ol.\n" +
                "• Açılan ekran telefonun kendi ekranıysa (güvenlik taraması vb.) \"Yine de yükle\" ya da \"Devam\" de.\n\n" +
                "Yine olmazsa bu pencerenin ekran görüntüsünü geliştiriciye gönder:\n" +
                "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}\n" +
                "Kuran: ${installer ?: "-"} / ${info?.initiatingPackageName ?: "-"}\n" +
                SelfInstall.report(activity))
            .setPositiveButton("Tekrar dene") { _, _ -> fixInstall(activity) }
            .setNegativeButton("Tamam", null)
            .show()
    }

    fun buildEntryRow(activity: Activity): View {
        if (BuildConfig.PARK_ONLY) return label("Bu NewREX'in Park sürümü: arabada park halindeyken açılır, King Installer gerekmez. " +
            "Android Auto → Geliştirici ayarları → \"Bilinmeyen kaynaklar\" açık olmalı.", 13f, Palette.TEXT_DIM, lines = 4)
        val box = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val info = label("", 13f, Palette.TEXT_DIM, lines = 4)
        val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        val names = listOf("Otomatik", "Normal", "Park")
        fun paint() {
            for (i in 0 until row.childCount) {
                val sel = i == Prefs.carEntry
                row.getChildAt(i).background = rounded(if (sel) Palette.withAlpha(Palette.accent, 0x33) else 0x22FFFFFF, activity.dpf(18),
                    activity.dp(if (sel) 2 else 1), if (sel) Palette.accent else 0x33FFFFFF)
            }
            info.text = (if (CarEntry.effective(activity) == CarEntry.PARKED)
                "Şu an: Park kapısı. Arabada her kurulumda görünür ama sadece park halindeyken açılır."
            else "Şu an: Normal kapı. Sürüşte de açılır (video kapanır); arabada görünmesi için King Installer ile kurulmuş olmalı.") +
                if (!CarEntry.parkedSupported()) "\nPark kapısı Android 15 ve üstünde var." else "\nDeğiştirdikten sonra telefonu arabadan çıkarıp tekrar bağla."
        }
        names.forEachIndexed { i, n ->
            row.addView(TextView(activity).apply {
                text = n
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 14f
                setOnClickListener { Prefs.carEntry = i; CarEntry.apply(activity); paint() }
            }, lp(0, activity.dp(40), 1f).margins(r = activity.dp(6)))
        }
        paint()
        box.addView(label("Arabaya giriş kapısı", 14f, bold = true))
        box.addView(row, lp(MATCH, WRAP).margins(t = activity.dp(6)))
        box.addView(info, lp(MATCH, WRAP).margins(t = activity.dp(6)))
        return box
    }

    @SuppressLint("BatteryLife")
    fun askBatteryExemption(activity: Activity) {
        val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(activity.packageName)) {
            Toast.makeText(activity, "Zaten kısıtlama yok.", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            activity.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${activity.packageName}")))
        }.onFailure { activity.startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
    }

    fun getDiagnostics(activity: Activity): String {
        val fmt = SimpleDateFormat("d MMM HH:mm", tr)
        fun time(t: Long) = if (t == 0L) "hiç olmadı" else fmt.format(Date(t))
        val install = runCatching {
            if (Build.VERSION.SDK_INT >= 30) {
                val i = activity.packageManager.getInstallSourceInfo(activity.packageName)
                val source = if (Build.VERSION.SDK_INT >= 33) " • kaynak=${i.packageSource}" else ""
                "yükleyen=${i.installingPackageName} • başlatan=${i.initiatingPackageName} • kaynak uygulama=${i.originatingPackageName}$source"
            } else {
                @Suppress("DEPRECATION")
                "yükleyen=${activity.packageManager.getInstallerPackageName(activity.packageName)}"
            }
        }.getOrElse { "okunamadı" }
        val aa = runCatching {
            activity.packageManager.getPackageInfo("com.google.android.projection.gearhead", 0).versionName
        }.getOrElse { "bulunamadı" }
        return "Telefon: ${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "NewREX ${BuildConfig.VERSION_NAME} • $install\n" +
            "Android Auto: $aa\n" +
            "Araç bağlanmayı denedi: ${time(Prefs.lastCarBind)}\n" +
            "Araç ekranı açıldı: ${time(Prefs.lastCarSession)}\n\n" +
            "Uygulama arabada görünmüyorsa bu bölümün ekran görüntüsünü geliştiriciye gönder."
    }
}


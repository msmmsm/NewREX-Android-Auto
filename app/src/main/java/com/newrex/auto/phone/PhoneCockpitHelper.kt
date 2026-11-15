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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.newrex.auto.data.CarLog
import com.newrex.auto.data.ObdManager
import com.newrex.auto.data.PerformanceTimer
import com.newrex.auto.data.Prefs
import com.newrex.auto.data.TelemetryHub
import com.newrex.auto.data.ToyotaChr2025PassionProfile
import com.newrex.auto.data.ToyotaEnhancedDiagnosticCatalog
import com.newrex.auto.data.EnhancedDiagnosticStatus
import com.newrex.auto.data.EquipmentStatus
import com.newrex.auto.data.DataAccessStatus
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.dpf
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.rounded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Telefon ekranı: Kokpit Araç Modeli, Türk Plaka Özelleştirme, OBD-II ve 0-100 Performans Kartı
 */
object PhoneCockpitHelper {
    private val diagnosticsScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)


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

        // 0.5 Ana Ekran Widget Yönetimi
        cockpitBox.addView(label("🧩 Ana Ekran Widget Yönetimi", 15f, bold = true))
        cockpitBox.addView(label("Android Auto ana ekranında görünmesini istediğiniz bileşenleri seçin:",
            12f, Palette.TEXT_DIM, lines = 2), lp(MATCH, WRAP).margins(t = dp(2), b = dp(6)))

        fun widgetRow(title: String, getter: () -> Boolean, setter: (Boolean) -> Unit) = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(0x14FFFFFF, dpf(10))
            setPadding(dp(12), dp(6), dp(8), dp(6))
            val statusText = label(if (getter()) "Açık" else "Kapalı", 13f, if (getter()) Palette.GREEN else Palette.TEXT_DIM, bold = true)
            addView(label(title, 13f, bold = true), lp(0, WRAP, 1f))
            addView(statusText, lp(WRAP, WRAP).margins(r = dp(8)))
            addView(Button(context).apply {
                text = if (getter()) "Gizle" else "Göster"
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = rounded(0x22FFFFFF, dpf(8))
                setOnClickListener {
                    setter(!getter())
                    onRefreshNeeded()
                }
            })
        }

        cockpitBox.addView(widgetRow("Üst Durum & Saat/Hava", { Prefs.showWidgetHeader }, { Prefs.showWidgetHeader = it }), lp(MATCH, WRAP).margins(b = dp(6)))
        cockpitBox.addView(widgetRow("Canlı Ekolayzır (Spektrum)", { Prefs.showWidgetEqualizer }, { Prefs.showWidgetEqualizer = it }), lp(MATCH, WRAP).margins(b = dp(6)))

        if (Prefs.showWidgetEqualizer) {
            val eqStyles = listOf("Klasik Stüdyo", "Cyber Neon", "Alev Kırmızı", "LED Izgara")
            val eqBox = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                eqStyles.forEachIndexed { idx, st ->
                    addView(Button(context).apply {
                        text = st
                        isAllCaps = false
                        val isSel = Prefs.equalizerStyle == idx
                        setTextColor(if (isSel) Color.WHITE else Palette.TEXT_DIM)
                        background = rounded(if (isSel) Palette.accent else 0x22FFFFFF, dpf(8))
                        setOnClickListener {
                            Prefs.equalizerStyle = idx
                            onRefreshNeeded()
                        }
                    }, lp(WRAP, dp(36)).margins(r = dp(6)))
                }
            }
            cockpitBox.addView(android.widget.HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                addView(eqBox)
            }, lp(MATCH, WRAP).margins(b = dp(6)))
        }

        cockpitBox.addView(widgetRow("3D Araç Modeli", { Prefs.showWidgetCar }, { Prefs.showWidgetCar = it }), lp(MATCH, WRAP).margins(b = dp(6)))
        cockpitBox.addView(widgetRow("Canlı Telemetri & Hız", { Prefs.showWidgetTelemetry }, { Prefs.showWidgetTelemetry = it }), lp(MATCH, WRAP).margins(b = dp(6)))
        cockpitBox.addView(widgetRow("Son Çalınanlar Şeridi", { Prefs.showWidgetRecents }, { Prefs.showWidgetRecents = it }), lp(MATCH, WRAP).margins(b = dp(12)))

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
        cockpitBox.addView(statsRow, lp(MATCH, WRAP).margins(b = dp(6)))

        // Standart hibrit PID 5B: yalnızca ECU destek bildirirse gerçek değer gösterilir.
        val hybridRemainingLife = TelemetryHub.obdHybridBatteryRemainingLifePercent.value
        val hybridBatteryText = hybridRemainingLife?.let { "%.1f%%".format(it) } ?: "Desteklenmiyor / veri yok"
        cockpitBox.addView(label("Hibrit batarya paketi kalan ömrü (SAE PID 5B)", 12f, Palette.TEXT_DIM, bold = true),
            lp(MATCH, WRAP).margins(t = dp(2)))
        cockpitBox.addView(label(hybridBatteryText, 14f, Palette.TEXT, bold = true),
            lp(MATCH, WRAP).margins(t = dp(2), b = dp(10)))
        cockpitBox.addView(label(
            "Bu standart değer SOC (şarj seviyesi) veya batarya sağlık testi (SOH) değildir. Hücre/blok gerilimi ve sıcaklık gibi Toyota'ya özel veriler bu ekranda henüz okunmaz.",
            10f, Palette.TEXT_FAINT, lines = 3
        ), lp(MATCH, WRAP).margins(b = dp(12)))

        // Kullanıcının aracı için donanım profili; donanım varlığı ile veri erişimi ayrı gösterilir.
        cockpitBox.addView(label(ToyotaChr2025PassionProfile.displayName, 14f, Palette.TEXT, bold = true),
            lp(MATCH, WRAP).margins(t = dp(8), b = dp(4)))
        cockpitBox.addView(label(
            "Kamera/radar envanteri — OEM sensörleri uygulamaya doğrudan görüntü veya ham hedef akışı vermez.",
            11f, Palette.TEXT_DIM, lines = 2
        ), lp(MATCH, WRAP).margins(b = dp(6)))
        ToyotaChr2025PassionProfile.sensors.forEach { sensor ->
            val equipment = when (sensor.equipmentStatus) {
                EquipmentStatus.CONFIRMED_PRESENT -> "Donanım: mevcut"
                EquipmentStatus.NOT_INCLUDED_IN_TRIM -> "Donanım: Passion paketinde yok"
                EquipmentStatus.VARIANT_DEPENDENT -> "Donanım: varyanta bağlı"
            }
            val access = when (sensor.dataAccess) {
                DataAccessStatus.OEM_SYSTEM_ONLY -> "Erişim: OEM sisteminde"
                DataAccessStatus.DIAGNOSTIC_STATUS_ONLY -> "Erişim: teşhis durumu araştırılabilir"
                DataAccessStatus.NOT_VERIFIED -> "Erişim: doğrulanmadı"
            }
            cockpitBox.addView(label("${sensor.name} · $equipment · $access", 11f, Palette.TEXT, bold = true),
                lp(MATCH, WRAP).margins(t = dp(4)))
            cockpitBox.addView(label(sensor.note, 10f, Palette.TEXT_FAINT, lines = 3),
                lp(MATCH, WRAP).margins(t = dp(1), b = dp(4)))
        }
        cockpitBox.addView(label(
            "Not: Envanter 2025 Türkiye Hybrid Passion donanım tablosuna dayanır. Araç üzerindeki donanım/ECU yazılımı fiziksel doğrulama gerektirir.",
            10f, Palette.TEXT_FAINT, lines = 2
        ), lp(MATCH, WRAP).margins(t = dp(4), b = dp(10)))

        cockpitBox.addView(label("Toyota gelişmiş ECU veri kataloğu", 14f, Palette.TEXT, bold = true),
            lp(MATCH, WRAP).margins(t = dp(4), b = dp(3)))
        cockpitBox.addView(label(
            "Aşağıdaki parametreler araştırma hedefidir; henüz bu araçtan okunmuş canlı değer değildir. Toyota OEM Data List protokolü doğrulanmadan sorgu gönderilmez.",
            10f, Palette.TEXT_FAINT, lines = 3
        ), lp(MATCH, WRAP).margins(b = dp(5)))
        ToyotaEnhancedDiagnosticCatalog.systems.forEach { system ->
            val status = when (system.status) {
                EnhancedDiagnosticStatus.OEM_DATA_LIST_DOCUMENTED -> "OEM Data List ailesi belgelenmiş · araç varyantı teyidi bekliyor"
                EnhancedDiagnosticStatus.VEHICLE_VARIANT_NOT_VERIFIED -> "Araç varyantında doğrulanmadı"
                EnhancedDiagnosticStatus.PROTOCOL_NOT_IMPLEMENTED -> "Protokol uygulanmadı"
            }
            cockpitBox.addView(label(system.name, 12f, Palette.TEXT, bold = true),
                lp(MATCH, WRAP).margins(t = dp(5)))
            cockpitBox.addView(label(status, 10f, Palette.TEXT_DIM),
                lp(MATCH, WRAP).margins(t = dp(1)))
            cockpitBox.addView(label(
                "İzlenecek alanlar: ${system.candidateMeasurements.joinToString(" · ")}",
                10f, Palette.TEXT_FAINT, lines = 3
            ), lp(MATCH, WRAP).margins(t = dp(2)))
            cockpitBox.addView(label(
                "Erişim: ${system.accessPath}. ${system.limitation}",
                10f, Palette.TEXT_FAINT, lines = 4
            ), lp(MATCH, WRAP).margins(t = dp(1), b = dp(4)))
        }

        // 2.1 OBD-II arıza kodları ve dondurulmuş kare tanı ekranı
        cockpitBox.addView(label("🩺 Araç arıza tanılama", 15f, bold = true))
        cockpitBox.addView(label(
            "ECU'dan okunan arıza kodlarını ve destekleniyorsa arıza anındaki kayıtlı sensör verilerini gösterir. " +
                "Açıklama kataloğunda bulunmayan kodlar tahmin edilmez.",
            12f, Palette.TEXT_DIM, lines = 3
        ), lp(MATCH, WRAP).margins(t = dp(3), b = dp(6)))

        val diagnosticStatus = label(
            if (obdState == ObdManager.ConnectionState.CONNECTED) "Tanılama için hazır." else "Tanılama için OBD-II adaptörünü bağla.",
            12f, Palette.TEXT_DIM, lines = 3
        )
        val dtcListBox = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        fun renderDtcs(codes: List<String>, scanCompleted: Boolean = true) {
            dtcListBox.removeAllViews()
            if (codes.isEmpty()) {
                dtcListBox.addView(label(
                    if (scanCompleted) "Bu taramada görüntülenebilir arıza kodu alınmadı." else "Henüz arıza taraması yapılmadı.",
                    12f, Palette.TEXT_FAINT
                ))
            } else {
                codes.distinct().forEach { code ->
                    val description = com.newrex.auto.data.ObdDiagnostics.description(code)
                    val row = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        background = rounded(0x14FFFFFF, dpf(9))
                        setPadding(dp(10), dp(7), dp(10), dp(7))
                        addView(label(code, 14f, Palette.TEXT, bold = true))
                        addView(label(description ?: "Bu kod için Türkçe açıklama kataloğunda kayıt bulunmuyor.",
                            12f, Palette.TEXT_DIM, lines = 3), lp(MATCH, WRAP).margins(t = dp(2)))
                    }
                    dtcListBox.addView(row, lp(MATCH, WRAP).margins(b = dp(5)))
                }
            }
        }
        renderDtcs(TelemetryHub.obdDtcList.value, scanCompleted = TelemetryHub.obdDtcList.value.isNotEmpty())

        val historyBox = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val historyDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
        fun renderHistory() {
            historyBox.removeAllViews()
            val history = Prefs.dtcHistory
            if (history.isEmpty()) {
                historyBox.addView(label("Henüz doğrulanmış DTC geçmişi yok.", 12f, Palette.TEXT_FAINT))
                return
            }
            historyBox.addView(label("Son 10 kayıt / toplam ${history.size}", 10f, Palette.TEXT_FAINT), lp(MATCH, WRAP).margins(b = dp(4)))
            history.take(10).forEach { entry ->
                val stateText = if (entry.presentInLastScan) "Son doğrulanmış taramada bulundu" else "Son taramada raporlanmadı (arıza giderildiği anlamına gelmez)"
                val stateColor = if (entry.presentInLastScan) Palette.ORANGE else Palette.TEXT_DIM
                val clearedText = entry.clearedAtMs?.let { " • ECU temizleme onayı: ${historyDateFormat.format(Date(it))}" } ?: ""
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    background = rounded(0x14FFFFFF, dpf(9))
                    setPadding(dp(10), dp(7), dp(10), dp(7))
                    addView(label(entry.code, 13f, Palette.TEXT, bold = true))
                    addView(label(stateText, 11f, stateColor, bold = true), lp(MATCH, WRAP).margins(t = dp(2)))
                    addView(label(
                        "İlk: ${historyDateFormat.format(Date(entry.firstSeenMs))} • Son: ${historyDateFormat.format(Date(entry.lastSeenMs))} • Görüldüğü tarama: ${entry.scanCount}$clearedText",
                        10f, Palette.TEXT_FAINT, lines = 3
                    ), lp(MATCH, WRAP).margins(t = dp(2)))
                }
                historyBox.addView(row, lp(MATCH, WRAP).margins(b = dp(5)))
            }
        }
        renderHistory()

        fun diagnosticButton(title: String, action: () -> Unit) = Button(context).apply {
            text = title
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(0x22FFFFFF, dpf(10))
            setOnClickListener { action() }
        }
        fun connected(): Boolean {
            if (ObdManager.state.value == ObdManager.ConnectionState.CONNECTED) return true
            diagnosticStatus.text = "OBD-II bağlı değil. Önce ELM327 adaptörünü bağla."
            diagnosticStatus.setTextColor(Palette.ORANGE)
            return false
        }
        val readDtcButton = diagnosticButton("Arıza kodlarını oku") {
            if (!connected()) return@diagnosticButton
            diagnosticStatus.text = "ECU arıza kodları okunuyor…"
            diagnosticStatus.setTextColor(Palette.TEXT_DIM)
            diagnosticsScope.launch {
                val scan = ObdManager.scanDtcCodes()
                if (scan.completed) {
                    Prefs.recordDtcScan(scan.codes)
                    renderDtcs(scan.codes, scanCompleted = true)
                    renderHistory()
                }
                diagnosticStatus.text = when {
                    ObdManager.state.value != ObdManager.ConnectionState.CONNECTED ->
                        "Okuma tamamlanamadı: OBD-II bağlantısı kesildi."
                    !scan.completed ->
                        "ECU yanıtı doğrulanamadı. Geçmiş kayıtlar değiştirilmedi; yeniden dene."
                    scan.codes.isEmpty() ->
                        "Doğrulanmış taramada görüntülenebilir arıza kodu alınmadı. Bu sonuç, desteklenen ECU'ların tümünü kapsadığı anlamına gelmeyebilir."
                    else -> "${scan.codes.size} arıza kodu okundu ve geçmişe kaydedildi."
                }
            }
        }
        cockpitBox.addView(readDtcButton, lp(MATCH, WRAP).margins(b = dp(6)))
        cockpitBox.addView(dtcListBox, lp(MATCH, WRAP).margins(b = dp(6)))
        cockpitBox.addView(label("Kayıtlı arıza geçmişi", 13f, bold = true), lp(MATCH, WRAP).margins(b = dp(4)))
        cockpitBox.addView(historyBox, lp(MATCH, WRAP).margins(b = dp(6)))

        val freezeFrameButton = diagnosticButton("Dondurulmuş kareyi oku") {
            if (!connected()) return@diagnosticButton
            diagnosticStatus.text = "ECU dondurulmuş kare verisi okunuyor…"
            diagnosticsScope.launch {
                val frame = ObdManager.readFreezeFrame()
                diagnosticStatus.text = when {
                    ObdManager.state.value != ObdManager.ConnectionState.CONNECTED ->
                        "Dondurulmuş kare okunamadı: OBD-II bağlantısı kesildi."
                    frame == null -> "ECU desteklenen bir dondurulmuş kare döndürmedi veya yanıt ayrıştırılamadı."
                    else -> buildString {
                        append("Dondurulmuş kare #${frame.frameNumber} • Geçmiş arıza anı verisi")
                        frame.rpm?.let { append("\nMotor devri: $it dev/dk") }
                        frame.speedKmh?.let { append("\nAraç hızı: $it km/s") }
                        frame.coolantTempC?.let { append("\nSoğutma suyu: $it °C") }
                        frame.intakeAirTempC?.let { append("\nEmme havası: $it °C") }
                        frame.engineLoadPercent?.let { append("\nMotor yükü: $it%") }
                        frame.throttlePercent?.let { append("\nGaz kelebeği: $it%") }
                    }
                }
                diagnosticStatus.setTextColor(Palette.TEXT)
            }
        }
        cockpitBox.addView(freezeFrameButton, lp(MATCH, WRAP).margins(b = dp(6)))

        val vehicleIdentityButton = diagnosticButton("ECU kimlik bilgilerini oku") {
            if (!connected()) return@diagnosticButton
            diagnosticStatus.text = "Standart OBD-II ECU kimlik bilgileri okunuyor…"
            diagnosticsScope.launch {
                val identity = ObdManager.readVehicleIdentity()
                diagnosticStatus.text = when {
                    ObdManager.state.value != ObdManager.ConnectionState.CONNECTED ->
                        "ECU kimlik okuması tamamlanamadı: OBD-II bağlantısı kesildi."
                    identity == null ->
                        "ECU, desteklenen standart Mode 09 kimlik verisi döndürmedi. Toyota hibrit, radar ve kamera ECU'larının listesi değildir."
                    else -> buildString {
                        append("Standart OBD-II emisyon ECU bilgileri")
                        identity.vin?.let { vin ->
                            append("\nŞasi no: ${vin.take(3)}•••••••••${vin.takeLast(5)}")
                        }
                        identity.ecuName?.let { append("\nECU adı: $it") }
                        if (identity.calibrationIds.isNotEmpty()) {
                            append("\nKalibrasyon kimliği: ${identity.calibrationIds.joinToString(", ")}")
                        }
                        if (identity.calibrationVerificationNumbers.isNotEmpty()) {
                            append("\nCVN: ${identity.calibrationVerificationNumbers.joinToString(", ")}")
                        }
                        append("\nNot: VIN tam hâliyle ekranda gösterilmez; bu bilgiler yalnızca bağlı OBD adaptöründen geçici olarak okunur.")
                    }
                }
                diagnosticStatus.setTextColor(Palette.TEXT)
            }
        }
        cockpitBox.addView(vehicleIdentityButton, lp(MATCH, WRAP).margins(b = dp(6)))

        val clearDtcButton = diagnosticButton("Arıza kodlarını sil…") {
            if (!connected()) return@diagnosticButton
            if (TelemetryHub.obdDtcList.value.isEmpty()) {
                diagnosticStatus.text = "Önce arıza kodlarını oku. Silinecek kayıtlı kod görünmüyor."
                return@diagnosticButton
            }
            AlertDialog.Builder(context)
                .setTitle("Arıza kodları silinsin mi?")
                .setMessage(
                    "Mode 04 komutu ECU'nun arıza kayıtlarını ve ilgili dondurulmuş kare/veri kayıtlarını silebilir. " +
                        "Bu işlem arızayı onarmaz; emisyon hazırlık monitörlerini sıfırlayabilir. Önce kodları ve araç belirtilerini kaydet. Devam edilsin mi?"
                )
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Kodları sil") { _, _ ->
                    diagnosticStatus.text = "ECU'dan silme onayı bekleniyor…"
                    diagnosticsScope.launch {
                        val cleared = ObdManager.clearDtcCodes()
                        if (cleared) {
                            Prefs.markDtcHistoryCleared()
                            renderDtcs(emptyList(), scanCompleted = true)
                            renderHistory()
                            diagnosticStatus.text = "ECU, Mode 04 temizleme isteğini olumlu yanıtladı. Geçmiş korundu; arıza giderilmiş sayılmaz. Yeniden tarama yap."
                        } else {
                            diagnosticStatus.text = "ECU temizleme işlemini onaylamadı. Kodlar silinmiş kabul edilmedi."
                        }
                    }
                }
                .show()
        }
        cockpitBox.addView(clearDtcButton, lp(MATCH, WRAP).margins(b = dp(6)))
        cockpitBox.addView(diagnosticStatus, lp(MATCH, WRAP).margins(t = dp(4), b = dp(12)))

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


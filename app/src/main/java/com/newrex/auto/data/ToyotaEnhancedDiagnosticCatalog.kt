package com.newrex.auto.data

/** Toyota OEM Data List için araştırma envanteri. Hiçbir satır çalıştırılabilir PID/komut içermez. */
enum class EnhancedDiagnosticStatus {
    OEM_DATA_LIST_DOCUMENTED,
    VEHICLE_VARIANT_NOT_VERIFIED,
    PROTOCOL_NOT_IMPLEMENTED
}

data class EnhancedDiagnosticItem(
    val id: String,
    val name: String,
    val candidateMeasurements: List<String>,
    val status: EnhancedDiagnosticStatus,
    val accessPath: String,
    val limitation: String
)

object ToyotaEnhancedDiagnosticCatalog {
    const val vehicle = ToyotaChr2025PassionProfile.displayName

    val systems = listOf(
        EnhancedDiagnosticItem(
            id = "hybrid_control",
            name = "Hibrit kontrol ECU'su",
            candidateMeasurements = listOf("Hibrit sistem çalışma durumları", "Motor/jeneratör çalışma bilgileri", "READY ve güç aktarım durumu"),
            status = EnhancedDiagnosticStatus.VEHICLE_VARIANT_NOT_VERIFIED,
            accessPath = "Toyota OEM teşhis cihazı → Hybrid Control / Data List",
            limitation = "2025 Türkiye Passion varyantındaki gerçek parametre adları ve erişim protokolü henüz doğrulanmadı."
        ),
        EnhancedDiagnosticItem(
            id = "hv_battery",
            name = "HV batarya ECU'su",
            candidateMeasurements = listOf("Batarya SOC", "Batarya sıcaklık sensörleri", "Batarya blok gerilimleri", "Batarya akımı"),
            status = EnhancedDiagnosticStatus.OEM_DATA_LIST_DOCUMENTED,
            accessPath = "Toyota OEM teşhis cihazı → HV Battery / Data List",
            limitation = "Bunlar Toyota hibrit teşhis Data List'lerinde görülen parametre sınıflarıdır; bu araç varyantında adları, sayıları ve ölçekleri teyit edilmeden okunmaz."
        ),
        EnhancedDiagnosticItem(
            id = "front_radar_pcs",
            name = "Ön radar / Pre-Collision ECU",
            candidateMeasurements = listOf("ECU iletişim ve arıza durumu", "Sistem kullanılabilirlik durumu"),
            status = EnhancedDiagnosticStatus.VEHICLE_VARIANT_NOT_VERIFIED,
            accessPath = "Toyota OEM teşhis cihazı → Pre-Collision / Data List",
            limitation = "Ham hedef listesi, mesafe ve bağıl hız akışı doğrulanmış değildir; yalnızca salt okunur teşhis durumu hedeflenir."
        ),
        EnhancedDiagnosticItem(
            id = "blind_spot",
            name = "Kör nokta (BSM) ECU'ları",
            candidateMeasurements = listOf("ECU iletişim ve arıza durumu", "Sistem kullanılabilirlik durumu"),
            status = EnhancedDiagnosticStatus.VEHICLE_VARIANT_NOT_VERIFIED,
            accessPath = "Toyota OEM teşhis cihazı → Blind Spot Monitor / Data List",
            limitation = "Radar hedeflerinin ham akışı veya mesafe/hız verisi doğrulanmamıştır."
        )
    )

    fun find(id: String): EnhancedDiagnosticItem? = systems.firstOrNull { it.id == id }
}

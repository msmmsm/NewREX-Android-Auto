package com.newrex.auto.data

/**
 * Teşhis sağlayıcılarının durumunu ve erişim sınırını ortaklaştırır.
 *
 * Bu katman yalnızca sağlayıcı envanteri sunar; ECU'ya komut gönderme veya ham CAN
 * çerçevesi üretme API'si içermez.
 */
enum class DiagnosticProviderKind {
    STANDARD_OBD_II,
    TOYOTA_OEM_ENHANCED
}

enum class DiagnosticProviderState {
    CONNECTED,
    NOT_CONNECTED,
    NOT_IMPLEMENTED,
    UNSUPPORTED
}

enum class DiagnosticConnectionState {
    DISCONNECTED,
    CONNECTING,
    INITIALIZING,
    CONNECTED,
    ERROR
}

enum class DiagnosticService {
    MODE_01_LIVE_DATA,
    MODE_02_FREEZE_FRAME,
    MODE_03_READ_DTC,
    MODE_04_CLEAR_DTC,
    MODE_09_VEHICLE_INFO
}

data class DiagnosticProviderSnapshot(
    val id: String,
    val displayName: String,
    val kind: DiagnosticProviderKind,
    val state: DiagnosticProviderState,
    val supportedServices: Set<DiagnosticService>,
    val limitation: String
)

interface DiagnosticProvider {
    fun snapshot(): DiagnosticProviderSnapshot
}

/** Mevcut ELM327 uygulamasının desteklediği standart OBD-II servislerini bildirir. */
class Elm327DiagnosticProvider(
    private val connectionState: DiagnosticConnectionState,
    private val adapterSelected: Boolean
) : DiagnosticProvider {
    override fun snapshot(): DiagnosticProviderSnapshot {
        val state = if (connectionState == DiagnosticConnectionState.CONNECTED && adapterSelected) {
            DiagnosticProviderState.CONNECTED
        } else {
            DiagnosticProviderState.NOT_CONNECTED
        }
        val services = if (state == DiagnosticProviderState.CONNECTED) {
            setOf(
                DiagnosticService.MODE_01_LIVE_DATA,
                DiagnosticService.MODE_02_FREEZE_FRAME,
                DiagnosticService.MODE_03_READ_DTC,
                DiagnosticService.MODE_04_CLEAR_DTC,
                DiagnosticService.MODE_09_VEHICLE_INFO
            )
        } else {
            emptySet()
        }
        val selection = if (adapterSelected) "Adaptör seçili." else "Adaptör seçilmedi."
        return DiagnosticProviderSnapshot(
            id = "elm327_standard_obd",
            displayName = "Standart OBD-II / ELM327",
            kind = DiagnosticProviderKind.STANDARD_OBD_II,
            state = state,
            supportedServices = services,
            limitation = "$selection Yalnızca uygulamadaki standart SAE OBD-II servisleri; Toyota özel ECU Data List erişimi değildir."
        )
    }
}

/**
 * Toyota OEM sağlayıcısı bilinçli olarak devre dışıdır.
 * Techstream/J2534 adı taşıyan genel bir adaptörün OEM Data List erişimi sağladığı varsayılmaz.
 */
class ToyotaOemDiagnosticProvider : DiagnosticProvider {
    override fun snapshot() = DiagnosticProviderSnapshot(
        id = "toyota_oem_enhanced",
        displayName = "Toyota OEM gelişmiş teşhis",
        kind = DiagnosticProviderKind.TOYOTA_OEM_ENHANCED,
        state = DiagnosticProviderState.NOT_IMPLEMENTED,
        supportedServices = emptySet(),
        limitation = "Toyota araç varyantı, onaylı teşhis arayüzü ve salt okunur protokol uygulaması doğrulanmadı. Henüz bağlantı veya ECU sorgusu yapılmaz."
    )
}

/** UI ve diğer katmanlar için tek, açık sağlayıcı envanteri. */
object DiagnosticProviderRegistry {
    fun snapshots(
        obdState: DiagnosticConnectionState,
        obdAdapterSelected: Boolean
    ): List<DiagnosticProviderSnapshot> = listOf(
        Elm327DiagnosticProvider(obdState, obdAdapterSelected).snapshot(),
        ToyotaOemDiagnosticProvider().snapshot()
    )
}

enum class DiagnosticValueQuality {
    VALID,
    STALE,
    INVALID,
    UNVERIFIED
}

/**
 * Güvenilir ölçümün kaynağı, ECU'su, birimi ve zamanı açık olmalıdır.
 * Bu model tek başına ölçüm üretmez veya veriyi kalıcılaştırmaz.
 */
data class DiagnosticReading(
    val parameterId: String,
    val value: Double,
    val unit: String,
    val source: String,
    val ecuId: String,
    val timestampEpochMs: Long,
    val quality: DiagnosticValueQuality
) {
    fun isTrustworthy(): Boolean =
        parameterId.isNotBlank() &&
            value.isFinite() &&
            unit.isNotBlank() &&
            source.isNotBlank() &&
            ecuId.isNotBlank() &&
            timestampEpochMs > 0L &&
            quality == DiagnosticValueQuality.VALID

    /** Ekranda yalnızca güncellik durumunu göstermek için saf değerlendirme. */
    fun displayedQuality(nowEpochMs: Long, maxAgeMs: Long = 15_000L): DiagnosticValueQuality {
        if (quality != DiagnosticValueQuality.VALID) return quality
        if (timestampEpochMs <= 0L || maxAgeMs <= 0L || timestampEpochMs > nowEpochMs) {
            return DiagnosticValueQuality.UNVERIFIED
        }
        return if (nowEpochMs - timestampEpochMs > maxAgeMs) {
            DiagnosticValueQuality.STALE
        } else {
            DiagnosticValueQuality.VALID
        }
    }
}

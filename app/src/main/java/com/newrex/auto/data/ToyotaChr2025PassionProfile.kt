package com.newrex.auto.data

/** User-confirmed reference vehicle: Turkish-market 2025 Toyota C-HR 1.8 Hybrid Passion. */
data class VehicleSensorCapability(val id: String, val name: String, val equipmentStatus: EquipmentStatus, val dataAccess: DataAccessStatus, val note: String)

enum class EquipmentStatus { CONFIRMED_PRESENT, NOT_INCLUDED_IN_TRIM, VARIANT_DEPENDENT }
enum class DataAccessStatus { OEM_SYSTEM_ONLY, DIAGNOSTIC_STATUS_ONLY, NOT_VERIFIED }

object ToyotaChr2025PassionProfile {
    const val displayName = "2025 Toyota C-HR 1.8 Hybrid Passion (Türkiye)"
    val sensors = listOf(
        VehicleSensorCapability("rear_camera", "Geri görüş kamerası", EquipmentStatus.CONFIRMED_PRESENT, DataAccessStatus.OEM_SYSTEM_ONLY, "OEM multimedya/park ekranı içindir; ELM327 üzerinden canlı video akışı beklenmez."),
        VehicleSensorCapability("front_recognition_camera", "Ön tanıma kamerası (TSS 3)", EquipmentStatus.CONFIRMED_PRESENT, DataAccessStatus.DIAGNOSTIC_STATUS_ONLY, "Görüntü akışı uygulamaya açık değildir; yalnızca belgelenmiş ECU durum/arıza verileri hedeflenebilir."),
        VehicleSensorCapability("front_millimeter_radar", "Ön milimetre dalga radarı", EquipmentStatus.CONFIRMED_PRESENT, DataAccessStatus.DIAGNOSTIC_STATUS_ONLY, "ACC/PCS sistemleri tarafından kullanılır; ham hedef listesi veya mesafe akışı henüz doğrulanmadı."),
        VehicleSensorCapability("blind_spot_radars", "Kör nokta yan radarları", EquipmentStatus.CONFIRMED_PRESENT, DataAccessStatus.DIAGNOSTIC_STATUS_ONLY, "BSM donanımı bulunur; gerçek zamanlı hedef verisi için OEM ECU protokolü gerekir."),
        VehicleSensorCapability("driver_monitor_camera", "Sürücü takip kamerası", EquipmentStatus.CONFIRMED_PRESENT, DataAccessStatus.NOT_VERIFIED, "Kamera görüntüsü alınmaz; varsa izin verilen durum ve arıza verileri ayrıca incelenir."),
        VehicleSensorCapability("panoramic_view_monitor", "360° çevre görüş (PVM)", EquipmentStatus.NOT_INCLUDED_IN_TRIM, DataAccessStatus.NOT_VERIFIED, "2025 Türkiye donanım tablosunda Passion için listelenmiyor; X-Sport ve üst paketlerde bulunuyor.")
    )
    fun capability(id: String): VehicleSensorCapability? = sensors.firstOrNull { it.id == id }
}
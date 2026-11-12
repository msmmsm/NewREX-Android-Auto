// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.data

import com.newrex.auto.DriveState
import com.newrex.auto.VehicleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Telemetri veri kaynağı önceliği:
 * 1. OBD2: En yüksek doğruluk ve doğrudan CAN-Bus motor verisi.
 * 2. CAR_HARDWARE: Android Auto CarHardwareManager (OEM izin veriyorsa).
 * 3. PHONE_GPS: Telefon GPS ve hareket sensörleri.
 * 4. NONE: Kaynak bağlı değil.
 */
enum class TelemetrySource(val label: String) {
    OBD2("OBD-II CAN-Bus"),
    CAR_HARDWARE("Android Auto OEM"),
    PHONE_GPS("Telefon GPS"),
    NONE("Bağlantı Yok")
}

/**
 * Otomobil Kokpit & Telemetri Veri Modeli
 */
data class TelemetrySnapshot(
    val speedKmh: Float? = null,
    val speedSource: TelemetrySource = TelemetrySource.NONE,
    val rpm: Int? = null,
    val coolantTempC: Int? = null,
    val batteryVoltage: Float? = null,
    val fuelLevelPercent: Float? = null,
    val fuelSource: TelemetrySource = TelemetrySource.NONE,
    val engineLoadPercent: Int? = null,
    val boostKpa: Float? = null,
    val odometerKm: Float? = null,
    val rangeKm: Float? = null,
    val isDriving: Boolean = false,
    val isObdConnected: Boolean = false,
    val dtcCount: Int = 0,
    val lastUpdatedMs: Long = 0L
)

/**
 * NewREX Çok Katmanlı Telemetri Merkezi (TelemetryHub)
 *
 * Veri Dürüstlüğü Prensibi:
 * Gerçek araç veya sensör verisi alınamıyorsa tahmini/uydurma değer gösterilmez.
 * Ekran bileşenleri null alanları "Veri Yok / Bağlantı Bekleniyor" olarak işler.
 */
object TelemetryHub {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // OBD-II katmanından gelen ham veriler (Faz 3'te ObdBluetoothManager tarafından beslenir)
    val obdConnected = MutableStateFlow(false)
    val obdSpeed = MutableStateFlow<Float?>(null)
    val obdRpm = MutableStateFlow<Int?>(null)
    val obdCoolantTemp = MutableStateFlow<Int?>(null)
    val obdBatteryVoltage = MutableStateFlow<Float?>(null)
    val obdFuelLevel = MutableStateFlow<Float?>(null)
    val obdEngineLoad = MutableStateFlow<Int?>(null)
    val obdBoost = MutableStateFlow<Float?>(null)
    val obdDtcList = MutableStateFlow<List<String>>(emptyList())

    /**
     * Tüm katmanları (OBD-II, Android Auto CarHardware, GPS) öncelik sırasına göre
     * tek bir birleşik StateFlow'da birleştiren reaktif telemetri akışı.
     */
    val telemetry: StateFlow<TelemetrySnapshot> = combine(
        obdConnected,
        obdSpeed,
        obdRpm,
        obdCoolantTemp,
        obdBatteryVoltage,
        obdFuelLevel,
        DriveState.speedKmh,
        DriveState.speedFromCar,
        DriveState.driving,
        VehicleData.fuelPercent,
        VehicleData.rangeKm,
        VehicleData.odometerKm,
        obdDtcList
    ) { values ->
        val isObd = values[0] as Boolean
        val oSpeed = values[1] as Float?
        val oRpm = values[2] as Int?
        val oCoolant = values[3] as Int?
        val oVolt = values[4] as Float?
        val oFuel = values[5] as Float?
        val driveSpeed = values[6] as Float?
        val driveFromCar = values[7] as Boolean
        val isDriving = values[8] as Boolean
        val carFuel = values[9] as Float?
        val carRange = values[10] as Float?
        val carOdo = values[11] as Float?
        @Suppress("UNCHECKED_CAST")
        val dtcs = values[12] as List<String>

        // 1. Hız Belirleme
        val (finalSpeed, speedSrc) = when {
            isObd && oSpeed != null -> oSpeed to TelemetrySource.OBD2
            driveFromCar && driveSpeed != null -> driveSpeed to TelemetrySource.CAR_HARDWARE
            driveSpeed != null -> driveSpeed to TelemetrySource.PHONE_GPS
            else -> null to TelemetrySource.NONE
        }

        // 2. Yakıt Seviyesi Belirleme
        val (finalFuel, fuelSrc) = when {
            isObd && oFuel != null -> oFuel to TelemetrySource.OBD2
            carFuel != null -> (carFuel * 100f) to TelemetrySource.CAR_HARDWARE
            else -> null to TelemetrySource.NONE
        }

        TelemetrySnapshot(
            speedKmh = finalSpeed,
            speedSource = speedSrc,
            rpm = if (isObd) oRpm else null,
            coolantTempC = if (isObd) oCoolant else null,
            batteryVoltage = if (isObd) oVolt else null,
            fuelLevelPercent = finalFuel,
            fuelSource = fuelSrc,
            engineLoadPercent = if (isObd) obdEngineLoad.value else null,
            boostKpa = if (isObd) obdBoost.value else null,
            odometerKm = carOdo,
            rangeKm = carRange,
            isDriving = isDriving,
            isObdConnected = isObd,
            dtcCount = dtcs.size,
            lastUpdatedMs = System.currentTimeMillis()
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = TelemetrySnapshot()
    )

    /**
     * OBD simülasyonu veya harici veri beslemesi için yardımcı fonksiyon
     */
    fun updateObdData(
        speed: Float? = null,
        rpm: Int? = null,
        coolant: Int? = null,
        voltage: Float? = null,
        fuel: Float? = null,
        load: Int? = null,
        boost: Float? = null,
        dtcs: List<String>? = null
    ) {
        speed?.let { obdSpeed.value = it }
        rpm?.let { obdRpm.value = it }
        coolant?.let { obdCoolantTemp.value = it }
        voltage?.let { obdBatteryVoltage.value = it }
        fuel?.let { obdFuelLevel.value = it }
        load?.let { obdEngineLoad.value = it }
        boost?.let { obdBoost.value = it }
        dtcs?.let { obdDtcList.value = it }
    }
}


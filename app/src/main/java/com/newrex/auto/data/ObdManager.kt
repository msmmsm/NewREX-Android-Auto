// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * OBD-II / ELM327 Bluetooth (CAN-Bus) İletişim Yöneticisi
 *
 * Standart ELM327 Bluetooth adaptörleri (vLinker, OBDLink, ucuz şeffaf mini ELM327)
 * ile haberleşerek doğrudan araç beyninden (ECU) telemetri ve hata kodlarını okur.
 */
object ObdManager {
    // Standart Bluetooth SPP (Serial Port Profile) UUID
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        INITIALIZING,
        CONNECTED,
        ERROR
    }

    val state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val selectedDevice = MutableStateFlow<BluetoothDevice?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // ELM327 komut/yanıt çiftlerinin polling ve DTC işlemleri arasında karışmasını önler.
    private val commandMutex = Mutex()
    private var workerJob: Job? = null
    private var socket: BluetoothSocket? = null
    private var inStream: InputStream? = null
    private var outStream: OutputStream? = null
    // null: adaptör destek bitmap'i vermedi; bu durumda temel PID'ler denenir.
    private var supportedPids: Set<Int>? = null

    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Eşleşmiş (paired) Bluetooth cihazları arasında OBD / ELM cihazlarını listeler
     */
    @SuppressLint("MissingPermission")
    fun refreshPairedDevices(context: Context): List<BluetoothDevice> {
        if (!hasBluetoothPermission(context)) return emptyList()
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        
        val list = adapter.bondedDevices?.toList() ?: emptyList()
        pairedDevices.value = list
        return list
    }

    /**
     * Belirtilen OBD2 adaptörüne bağlanır ve telemetri döngüsünü başlatır
     */
    @SuppressLint("MissingPermission")
    fun connect(context: Context, device: BluetoothDevice) {
        disconnect()
        selectedDevice.value = device

        workerJob = scope.launch {
            state.value = ConnectionState.CONNECTING
            TelemetryHub.obdConnected.value = false

            try {
                val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
                sock.connect()
                socket = sock
                inStream = sock.inputStream
                outStream = sock.outputStream

                state.value = ConnectionState.INITIALIZING

                // ELM327 Başlatma Sekansı
                sendCommand("ATZ")     // Reset
                delay(400)
                sendCommand("ATE0")    // Echo Off
                sendCommand("ATL0")    // Linefeeds Off
                sendCommand("ATS0")    // Spaces Off
                sendCommand("ATSP0")   // Otomatik Protokol (CAN/KWP vb.)
                delay(200)

                // ECU'nun desteklediği PID'leri keşfet; desteklenmeyen PID'ler her döngüde sorgulanmasın.
                supportedPids = discoverSupportedPids()

                state.value = ConnectionState.CONNECTED
                TelemetryHub.obdConnected.value = true

                // Telemetri Sorgulama Döngüsü
                startPollingLoop()

            } catch (e: Exception) {
                state.value = ConnectionState.ERROR
                TelemetryHub.obdConnected.value = false
                TelemetryHub.clearObdData()
                closeTransport()
            }
        }
    }

    fun disconnect() {
        workerJob?.cancel()
        workerJob = null
        closeTransport()
        selectedDevice.value = null
        state.value = ConnectionState.DISCONNECTED
        TelemetryHub.obdConnected.value = false
        TelemetryHub.clearObdData()
    }

    private fun closeTransport() {
        try { inStream?.close() } catch (_: Exception) {}
        try { outStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        inStream = null
        outStream = null
        supportedPids = null
    }

    private suspend fun discoverSupportedPids(): Set<Int>? {
        val supported = mutableSetOf<Int>()
        val first = parseSupportedPids(sendCommand("0100"), 0x00) ?: return null
        supported += first

        if (0x20 in supported) {
            val second = parseSupportedPids(sendCommand("0120"), 0x20)
            if (second != null) supported += second
        }
        if (0x40 in supported) {
            val third = parseSupportedPids(sendCommand("0140"), 0x40)
            if (third != null) supported += third
        }
        return supported
    }

    private fun isPidSupported(pid: Int): Boolean = supportedPids?.contains(pid) ?: true

    internal fun parseSupportedPids(raw: String, basePid: Int): Set<Int>? {
        val prefix = "41" + basePid.toString(16).uppercase().padStart(2, '0')
        val clean = raw.replace(" ", "").replace("\\r", "").replace("\\n", "").uppercase()
        val index = clean.indexOf(prefix)
        if (index < 0) return null
        val payload = clean.substring(index + prefix.length).take(8)
        if (payload.length != 8) return null
        val mask = payload.toLongOrNull(16) ?: return null
        return (0 until 32).mapNotNull { bit ->
            if ((mask and (1L shl (31 - bit))) != 0L) basePid + bit + 1 else null
        }.toSet()
    }

    internal fun parseEngineLoad(raw: String): Int? {
        val clean = cleanHex(raw, "4104") ?: return null
        val value = clean.take(2).toIntOrNull(16) ?: return null
        return ((value * 100f) / 255f).toInt()
    }

    /**
     * OBD-II PID Polling Döngüsü
     */
    private suspend fun CoroutineScope.startPollingLoop() {
        var loopCount = 0
        while (isActive) {
            try {
                // Hız (km/s) - PID 010D
                if (isPidSupported(0x0D)) {
                    val speedRaw = sendCommand("010D")
                    parseSpeed(speedRaw)?.let { TelemetryHub.obdSpeed.value = it }
                }

                // Motor Devri (RPM) - PID 010C
                if (isPidSupported(0x0C)) {
                    val rpmRaw = sendCommand("010C")
                    parseRpm(rpmRaw)?.let { TelemetryHub.obdRpm.value = it }
                }

                // Motor yükü - PID 0104
                if (loopCount % 5 == 0 && isPidSupported(0x04)) {
                    val loadRaw = sendCommand("0104")
                    parseEngineLoad(loadRaw)?.let { TelemetryHub.obdEngineLoad.value = it }
                }

                // Motor Harareti / Soğutma Suyu (°C) - PID 0105
                if (loopCount % 5 == 0 && isPidSupported(0x05)) {
                    val tempRaw = sendCommand("0105")
                    parseCoolant(tempRaw)?.let { TelemetryHub.obdCoolantTemp.value = it }
                }

                // Akü Voltajı (V) - "AT RV"
                if (loopCount % 10 == 0) {
                    val voltRaw = sendCommand("ATRV")
                    parseVoltage(voltRaw)?.let { TelemetryHub.obdBatteryVoltage.value = it }
                }

                // Yakıt Seviyesi (%) - PID 012F
                if (loopCount % 20 == 0 && isPidSupported(0x2F)) {
                    val fuelRaw = sendCommand("012F")
                    parseFuel(fuelRaw)?.let { TelemetryHub.obdFuelLevel.value = it }
                }

                loopCount++
                delay(80) // Sorgu sürelerine ek kısa bekleme; gerçek örnekleme hızı ECU/adaptöre bağlıdır.
            } catch (e: Exception) {
                if (isActive) {
                    delay(500)
                }
            }
        }
    }

    /**
     * Arıza Kodlarını (DTC) Oku (Mode 03)
     */
    suspend fun readDtcCodes(): List<String> = withContext(Dispatchers.IO) {
        if (state.value != ConnectionState.CONNECTED) return@withContext emptyList()
        try {
            val response = sendCommand("03")
            val dtcs = parseDtc(response)
            TelemetryHub.obdDtcList.value = dtcs
            dtcs
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Arıza Kodlarını Sıfırla (Mode 04 - Check Engine Söndür)
     */
    suspend fun clearDtcCodes(): Boolean = withContext(Dispatchers.IO) {
        if (state.value != ConnectionState.CONNECTED) return@withContext false
        try {
            val res = sendCommand("04")
            // Mode 04 için başarılı ECU yanıtı 44'tür; yalnızca onaydan sonra yerel liste temizlenir.
            val acknowledged = res.replace(" ", "").replace("\r", "").replace("\n", "").uppercase().contains("44")
            if (acknowledged) TelemetryHub.obdDtcList.value = emptyList()
            acknowledged
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun sendCommand(cmd: String): String = withContext(Dispatchers.IO) {
        commandMutex.withLock {
            val out = outStream ?: return@withLock ""
            val ins = inStream ?: return@withLock ""

            out.write((cmd + "\r").toByteArray())
            out.flush()

            val sb = StringBuilder()
            val buffer = ByteArray(128)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 1200) {
                if (ins.available() > 0) {
                    val read = ins.read(buffer)
                    if (read > 0) {
                        val chunk = String(buffer, 0, read)
                        sb.append(chunk)
                        if (chunk.contains(">")) break // ELM327 yanıt sonu belirteci
                    }
                } else {
                    delay(10)
                }
            }
            sb.toString().replace(">", "").trim()
        }
    }

    // ---- PID Ayrıştırıcı Fonksiyonlar ----

    internal fun parseSpeed(raw: String): Float? {
        val clean = cleanHex(raw, "410D") ?: return null
        return clean.toIntOrNull(16)?.toFloat()
    }

    internal fun parseRpm(raw: String): Int? {
        val clean = cleanHex(raw, "410C") ?: return null
        if (clean.length < 4) return null
        val a = clean.substring(0, 2).toIntOrNull(16) ?: return null
        val b = clean.substring(2, 4).toIntOrNull(16) ?: return null
        return ((a * 256) + b) / 4
    }

    internal fun parseCoolant(raw: String): Int? {
        val clean = cleanHex(raw, "4105") ?: return null
        val a = clean.toIntOrNull(16) ?: return null
        return a - 40
    }

    internal fun parseFuel(raw: String): Float? {
        val clean = cleanHex(raw, "412F") ?: return null
        val a = clean.toIntOrNull(16) ?: return null
        return (a * 100f) / 255f
    }

    internal fun parseVoltage(raw: String): Float? {
        val digits = raw.replace("V", "", ignoreCase = true).trim()
        return digits.toFloatOrNull()
    }

    internal fun parseDtc(raw: String): List<String> {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "")
        if (!clean.contains("43")) return emptyList()
        val hex = clean.substringAfter("43")
        val result = mutableListOf<String>()

        for (i in 0 until hex.length step 4) {
            if (i + 4 <= hex.length) {
                val codeHex = hex.substring(i, i + 4)
                if (codeHex == "0000") continue
                val first = codeHex[0].digitToIntOrNull(16) ?: continue
                val category = when (first ushr 2) {
                    0 -> "P"
                    1 -> "C"
                    2 -> "B"
                    else -> "U"
                }
                val subtype = first and 0x03
                result.add(category + subtype + codeHex.substring(1))
            }
        }
        return result
    }

    private fun cleanHex(raw: String, prefix: String): String? {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val idx = clean.indexOf(prefix)
        if (idx == -1) return null
        return clean.substring(idx + prefix.length)
    }
}


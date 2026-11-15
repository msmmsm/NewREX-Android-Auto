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
        if (basePid !in listOf(0x00, 0x20, 0x40, 0x60, 0x80, 0xA0, 0xC0, 0xE0)) return null
        val prefix = "41" + basePid.toString(16).uppercase().padStart(2, '0')
        // ELM yanıtındaki CAN başlıklarını, boşlukları ve satır sonlarını ayıkla.
        val clean = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        val index = clean.indexOf(prefix)
        if (index < 0) return null
        val payloadStart = index + prefix.length
        val payload = clean.substring(payloadStart, (payloadStart + 8).coerceAtMost(clean.length))
        if (payload.length != 8) return null
        val mask = payload.toLongOrNull(16) ?: return null
        return (0 until 32).mapNotNull { bit ->
            if ((mask and (1L shl (31 - bit))) != 0L) basePid + bit + 1 else null
        }.toSet()
    }

    internal fun parseEngineLoad(raw: String): Int? {
        val clean = cleanHex(raw, "4104") ?: return null
        if (clean.length < 2) return null
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
                // Başarısız/bozuk yanıt, önceki değeri ekranda sonsuza kadar tutmasın.
                // Hız (km/s) - PID 010D
                if (isPidSupported(0x0D)) {
                    val speedRaw = sendCommand("010D")
                    TelemetryHub.obdSpeed.value = parseSpeed(speedRaw)
                }

                // Motor Devri (RPM) - PID 010C
                if (isPidSupported(0x0C)) {
                    val rpmRaw = sendCommand("010C")
                    TelemetryHub.obdRpm.value = parseRpm(rpmRaw)
                }

                // Motor yükü - PID 0104
                if (loopCount % 5 == 0 && isPidSupported(0x04)) {
                    val loadRaw = sendCommand("0104")
                    TelemetryHub.obdEngineLoad.value = parseEngineLoad(loadRaw)
                }

                // Motor Harareti / Soğutma Suyu (°C) - PID 0105
                if (loopCount % 5 == 0 && isPidSupported(0x05)) {
                    val tempRaw = sendCommand("0105")
                    TelemetryHub.obdCoolantTemp.value = parseCoolant(tempRaw)
                }

                // Akü Voltajı (V) - "AT RV"
                if (loopCount % 10 == 0) {
                    val voltRaw = sendCommand("ATRV")
                    TelemetryHub.obdBatteryVoltage.value = parseVoltage(voltRaw)
                }

                // Yakıt Seviyesi (%) - PID 012F
                if (loopCount % 20 == 0 && isPidSupported(0x2F)) {
                    val fuelRaw = sendCommand("012F")
                    TelemetryHub.obdFuelLevel.value = parseFuel(fuelRaw)
                }

                loopCount++
                delay(80) // Sorgu sürelerine ek kısa bekleme; gerçek örnekleme hızı ECU/adaptöre bağlıdır.
            } catch (e: Exception) {
                if (isActive) {
                    // Bağlantı/okuma hatasında son geçerli ölçümleri canlı veri gibi göstermeyi bırak.
                    TelemetryHub.obdConnected.value = false
                    TelemetryHub.clearObdData()
                    state.value = ConnectionState.ERROR
                    closeTransport()
                }
                break
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
        if (clean.length < 2) return null
        return clean.take(2).toIntOrNull(16)?.toFloat()
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
        if (clean.length < 2) return null
        val a = clean.take(2).toIntOrNull(16) ?: return null
        return a - 40
    }

    internal fun parseFuel(raw: String): Float? {
        val clean = cleanHex(raw, "412F") ?: return null
        if (clean.length < 2) return null
        val a = clean.take(2).toIntOrNull(16) ?: return null
        return (a * 100f) / 255f
    }

    internal fun parseVoltage(raw: String): Float? {
        val digits = raw.replace("V", "", ignoreCase = true).trim()
        return digits.toFloatOrNull()
    }

    internal fun parseDtc(raw: String): List<String> {
        // Yalnızca hex karakterleri tut; "NO DATA", ELM komut yankısı ve satır sonları atılır.
        val clean = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        if (clean.isEmpty()) return emptyList()

        val result = linkedSetOf<String>()
        var cursor = 0
        // ELM327 başlıklar açıkken yanıtlar 7E8/7E9... ECU kimliğiyle art arda gelebilir.
        val ecuHeader = Regex("7E[89A-F][0-9A-F]")
        while (cursor < clean.length) {
            val response = clean.indexOf("43", cursor)
            if (response < 0) break
            val payloadStart = response + 2
            val nextHeader = ecuHeader.find(clean, payloadStart)?.range?.first ?: clean.length
            val payload = clean.substring(payloadStart, nextHeader)
            for (offset in 0 until payload.length - 3 step 4) {
                val codeHex = payload.substring(offset, offset + 4)
                if (codeHex == "0000") continue
                val first = codeHex[0].digitToIntOrNull(16) ?: continue
                val category = when (first ushr 2) {
                    0 -> "P"
                    1 -> "C"
                    2 -> "B"
                    else -> "U"
                }
                result += category + (first and 0x03) + codeHex.substring(1)
            }
            cursor = if (nextHeader < clean.length) nextHeader else clean.length
        }
        return result.toList()
    }

    private fun cleanHex(raw: String, prefix: String): String? {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val idx = clean.indexOf(prefix)
        if (idx == -1) return null
        return clean.substring(idx + prefix.length)
    }
}


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
    private var workerJob: Job? = null
    private var socket: BluetoothSocket? = null
    private var inStream: InputStream? = null
    private var outStream: OutputStream? = null

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

                state.value = ConnectionState.CONNECTED
                TelemetryHub.obdConnected.value = true

                // Telemetri Sorgulama Döngüsü
                startPollingLoop()

            } catch (e: Exception) {
                state.value = ConnectionState.ERROR
                TelemetryHub.obdConnected.value = false
                disconnect()
            }
        }
    }

    fun disconnect() {
        workerJob?.cancel()
        workerJob = null
        try { inStream?.close() } catch (_: Exception) {}
        try { outStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        inStream = null
        outStream = null
        state.value = ConnectionState.DISCONNECTED
        TelemetryHub.obdConnected.value = false
    }

    /**
     * OBD-II PID Polling Döngüsü
     */
    private suspend fun CoroutineScope.startPollingLoop() {
        var loopCount = 0
        while (isActive) {
            try {
                // Hız (km/s) - PID 010D
                val speedRaw = sendCommand("010D")
                parseSpeed(speedRaw)?.let { TelemetryHub.obdSpeed.value = it }

                // Motor Devri (RPM) - PID 010C
                val rpmRaw = sendCommand("010C")
                parseRpm(rpmRaw)?.let { TelemetryHub.obdRpm.value = it }

                // Motor Harareti / Soğutma Suyu (°C) - PID 0105
                if (loopCount % 5 == 0) {
                    val tempRaw = sendCommand("0105")
                    parseCoolant(tempRaw)?.let { TelemetryHub.obdCoolantTemp.value = it }
                }

                // Akü Voltajı (V) - "AT RV"
                if (loopCount % 10 == 0) {
                    val voltRaw = sendCommand("ATRV")
                    parseVoltage(voltRaw)?.let { TelemetryHub.obdBatteryVoltage.value = it }
                }

                // Yakıt Seviyesi (%) - PID 012F
                if (loopCount % 20 == 0) {
                    val fuelRaw = sendCommand("012F")
                    parseFuel(fuelRaw)?.let { TelemetryHub.obdFuelLevel.value = it }
                }

                loopCount++
                delay(80) // 12-15 Hz taze veri akışı
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
            TelemetryHub.obdDtcList.value = emptyList()
            res.contains("OK", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun sendCommand(cmd: String): String = withContext(Dispatchers.IO) {
        val out = outStream ?: return@withContext ""
        val ins = inStream ?: return@withContext ""

        out.write((cmd + "\r").toByteArray())
        out.flush()

        val sb = StringBuilder()
        val buffer = ByteArray(128)
        var read: Int
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < 1200) {
            if (ins.available() > 0) {
                read = ins.read(buffer)
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

    // ---- PID Ayrıştırıcı Fonksiyonlar ----

    private fun parseSpeed(raw: String): Float? {
        val clean = cleanHex(raw, "410D") ?: return null
        return clean.toIntOrNull(16)?.toFloat()
    }

    private fun parseRpm(raw: String): Int? {
        val clean = cleanHex(raw, "410C") ?: return null
        if (clean.length < 4) return null
        val a = clean.substring(0, 2).toIntOrNull(16) ?: return null
        val b = clean.substring(2, 4).toIntOrNull(16) ?: return null
        return ((a * 256) + b) / 4
    }

    private fun parseCoolant(raw: String): Int? {
        val clean = cleanHex(raw, "4105") ?: return null
        val a = clean.toIntOrNull(16) ?: return null
        return a - 40
    }

    private fun parseFuel(raw: String): Float? {
        val clean = cleanHex(raw, "412F") ?: return null
        val a = clean.toIntOrNull(16) ?: return null
        return (a * 100f) / 255f
    }

    private fun parseVoltage(raw: String): Float? {
        val digits = raw.replace("V", "", ignoreCase = true).trim()
        return digits.toFloatOrNull()
    }

    private fun parseDtc(raw: String): List<String> {
        val clean = raw.replace(" ", "").replace("\r", "").replace("\n", "")
        if (!clean.contains("43")) return emptyList()
        val hex = clean.substringAfter("43")
        val result = mutableListOf<String>()

        for (i in 0 until hex.length step 4) {
            if (i + 4 <= hex.length) {
                val codeHex = hex.substring(i, i + 4)
                if (codeHex == "0000") continue
                val prefix = when (codeHex[0]) {
                    '0' -> "P0"
                    '1' -> "P1"
                    '2' -> "P2"
                    '3' -> "P3"
                    '4' -> "C0"
                    '8' -> "B0"
                    'C' -> "U0"
                    else -> "P"
                }
                result.add(prefix + codeHex.substring(1))
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


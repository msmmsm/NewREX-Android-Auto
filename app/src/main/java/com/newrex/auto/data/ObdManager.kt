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
import kotlinx.coroutines.CancellationException
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
import java.net.SocketTimeoutException
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
    // Eski iptal edilmiş bağlantı işi yeni soketi kapatamasın.
    @Volatile private var connectionGeneration: Long = 0L
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
        val generation = connectionGeneration
        selectedDevice.value = device

        workerJob = scope.launch {
            var retriesUsed = 0
            while (isActive && generation == connectionGeneration && selectedDevice.value?.address == device.address) {
                state.value = ConnectionState.CONNECTING
                TelemetryHub.obdConnected.value = false
                TelemetryHub.clearObdData()
                closeTransport()

                try {
                    val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    // connect() hata verirse veya kullanıcı iptal ederse soket de kapatılabilsin.
                    socket = sock
                    sock.connect()
                    inStream = sock.inputStream
                    outStream = sock.outputStream

                    state.value = ConnectionState.INITIALIZING

                    // ELM327 başlatma; her yeni bağlantıda adaptör durumu yeniden kurulur.
                    sendCommand("ATZ")
                    delay(400)
                    sendCommand("ATE0")
                    sendCommand("ATL0")
                    sendCommand("ATS0")
                    sendCommand("ATSP0")
                    delay(200)

                    supportedPids = discoverSupportedPids()
                    state.value = ConnectionState.CONNECTED
                    TelemetryHub.obdConnected.value = true

                    // Bağlantı kaybolursa polling döngüsü döner; aşağıdaki politika yeniden dener.
                    startPollingLoop()
                    if (!isActive || selectedDevice.value?.address != device.address) break
                    if (state.value != ConnectionState.ERROR) markConnectionError()
                } catch (e: CancellationException) {
                    if (generation == connectionGeneration) closeTransport()
                    throw e
                } catch (e: Exception) {
                    if (isActive && generation == connectionGeneration) markConnectionError()
                    else throw CancellationException("Eski OBD bağlantı denemesi geçersiz kılındı")
                }

                if (!isActive || generation != connectionGeneration || selectedDevice.value?.address != device.address) break
                if (!ObdReconnectPolicy.shouldRetry(retriesUsed)) break
                delay(ObdReconnectPolicy.delayMs(retriesUsed))
                retriesUsed++
            }
            if (isActive && generation == connectionGeneration && selectedDevice.value?.address == device.address &&
                state.value == ConnectionState.ERROR
            ) {
                // Otomatik denemeler tükendi; kullanıcı aynı adaptörü manuel yeniden seçebilir.
                selectedDevice.value = device
            }
        }
    }

    fun disconnect() {
        connectionGeneration++
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

    /** SAE J1979 Mode 01 PID 5B: hibrit/EV batarya paketi kalan ömrü. */
    internal fun parseHybridBatteryRemainingLife(raw: String): Float? {
        val clean = cleanHex(raw, "415B") ?: return null
        if (clean.length < 2) return null
        val value = clean.take(2).toIntOrNull(16) ?: return null
        return (value * 100f) / 255f
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
                    val parsed = parseSpeed(speedRaw)
                    TelemetryHub.obdSpeed.value = parsed
                    if (parsed != null) TelemetryHub.markObdDataUpdated()
                }

                // Motor Devri (RPM) - PID 010C
                if (isPidSupported(0x0C)) {
                    val rpmRaw = sendCommand("010C")
                    val parsed = parseRpm(rpmRaw)
                    TelemetryHub.obdRpm.value = parsed
                    if (parsed != null) TelemetryHub.markObdDataUpdated()
                }

                // Motor yükü - PID 0104
                if (loopCount % 5 == 0 && isPidSupported(0x04)) {
                    val loadRaw = sendCommand("0104")
                    val parsed = parseEngineLoad(loadRaw)
                    TelemetryHub.obdEngineLoad.value = parsed
                    if (parsed != null) TelemetryHub.markObdDataUpdated()
                }

                // Motor Harareti / Soğutma Suyu (°C) - PID 0105
                if (loopCount % 5 == 0 && isPidSupported(0x05)) {
                    val tempRaw = sendCommand("0105")
                    val parsed = parseCoolant(tempRaw)
                    TelemetryHub.obdCoolantTemp.value = parsed
                    if (parsed != null) TelemetryHub.markObdDataUpdated()
                }

                // Akü Voltajı (V) - "AT RV"
                if (loopCount % 10 == 0) {
                    val voltRaw = sendCommand("ATRV")
                    val parsed = parseVoltage(voltRaw)
                    TelemetryHub.obdBatteryVoltage.value = parsed
                    if (parsed != null) TelemetryHub.markObdDataUpdated()
                }

                // Hibrit batarya kalan ömrü (%) - standart Mode 01 PID 5B.
                // Bu PID "remaining life" değeridir; SOC veya batarya sağlık (SOH) testi değildir.
                if (loopCount % 20 == 0 && supportedPids?.contains(0x5B) == true) {
                    val hybridRaw = sendCommand("015B")
                    val parsed = parseHybridBatteryRemainingLife(hybridRaw)
                    TelemetryHub.obdHybridBatteryRemainingLifePercent.value = parsed
                    if (parsed != null) TelemetryHub.markObdDataUpdated()
                }

                // Yakıt Seviyesi (%) - PID 012F
                if (loopCount % 20 == 0 && isPidSupported(0x2F)) {
                    val fuelRaw = sendCommand("012F")
                    val parsed = parseFuel(fuelRaw)
                    TelemetryHub.obdFuelLevel.value = parsed
                    if (parsed != null) TelemetryHub.markObdDataUpdated()
                }

                loopCount++
                delay(80) // Sorgu sürelerine ek kısa bekleme; gerçek örnekleme hızı ECU/adaptöre bağlıdır.
            } catch (e: Exception) {
                if (isActive) {
                    // Bağlantı/okuma hatasında son geçerli ölçümleri canlı veri gibi göstermeyi bırak.
                    markConnectionError()
                }
                break
            }
        }
    }

    /**
     * Mode 09: yalnızca standart kimlik PID bitmap'inde desteklendiği bildirilen alanları okur.
     * VIN ve ECU tanımlayıcıları uygulama belleğinde kalır; kalıcı depolama veya log kullanılmaz.
     */
    suspend fun readVehicleIdentity(): ObdVehicleIdentity? = withContext(Dispatchers.IO) {
        if (state.value != ConnectionState.CONNECTED) return@withContext null
        try {
            val supported = ObdVehicleInfo.parseSupportedPids(sendCommand("0900"))
                ?: return@withContext null
            val vin = if (0x02 in supported) ObdVehicleInfo.parseVin(sendCommand("0902")) else null
            val calibrationIds = if (0x04 in supported) {
                ObdVehicleInfo.parseAsciiValues(sendCommand("0904"), 0x04)
            } else emptyList()
            val cvns = if (0x06 in supported) {
                ObdVehicleInfo.parseCvns(sendCommand("0906"))
            } else emptyList()
            val ecuNames = if (0x0A in supported) {
                ObdVehicleInfo.parseAsciiValues(sendCommand("090A"), 0x0A)
            } else emptyList()
            if (vin == null && calibrationIds.isEmpty() && cvns.isEmpty() && ecuNames.isEmpty()) {
                null
            } else {
                TelemetryHub.markObdDataUpdated()
                ObdVehicleIdentity(
                    vin = vin,
                    ecuName = ecuNames.firstOrNull(),
                    calibrationIds = calibrationIds,
                    calibrationVerificationNumbers = cvns
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is java.io.IOException) markConnectionError()
            null
        }
    }

    /**
     * Arıza Kodlarını (DTC) Oku (Mode 03)
     */
    suspend fun readDtcCodes(): List<String> = scanDtcCodes().codes

    /** Mode 03 yanıtını doğrular; NO DATA geçerli boş sonuçtur, çözümlenemeyen yanıt tarama değildir. */
    suspend fun scanDtcCodes(): DtcScanResult = withContext(Dispatchers.IO) {
        if (state.value != ConnectionState.CONNECTED) return@withContext DtcScanResult(emptyList(), false)
        try {
            val response = sendCommand("03")
            if (!ObdDiagnostics.isDtcScanResponseValid(response)) {
                return@withContext DtcScanResult(emptyList(), false)
            }
            val dtcs = parseDtc(response)
            TelemetryHub.obdDtcList.value = dtcs
            TelemetryHub.markObdDataUpdated()
            DtcScanResult(dtcs, true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is java.io.IOException) markConnectionError()
            DtcScanResult(emptyList(), false)
        }
    }

    /**
     * Mode 02 Freeze Frame: ECU'nun arıza anında sakladığı desteklenen standart alanları okur.
     * Bu veri geçmiş bir andan gelir; canlı telemetri alanlarına yazılmaz.
     */
    suspend fun readFreezeFrame(): ObdFreezeFrame? = withContext(Dispatchers.IO) {
        if (state.value != ConnectionState.CONNECTED) return@withContext null
        try {
            // Önce Mode 02 destek bitmap'i okunur; desteklenmeyen PID'lere gereksiz istek atılmaz.
            val supported = ObdDiagnostics.parseSupportedFreezeFramePids(sendCommand("0200"))
                ?: return@withContext null
            val requested = listOf(0x0C, 0x0D, 0x05, 0x0F, 0x04, 0x11)
                .filter { it in supported }
            if (requested.isEmpty()) return@withContext null
            val parsed = requested.mapNotNull { pid ->
                val raw = sendCommand("02" + pid.toString(16).uppercase().padStart(2, '0'))
                val size = if (pid == 0x0C) 2 else 1
                ObdDiagnostics.parseFreezeFrameBytes(raw, pid, size)?.let { pid to it }
            }
            if (parsed.isEmpty()) return@withContext null
            val frame = parsed.first().second.first
            fun bytes(pid: Int): List<Int>? = parsed.firstOrNull {
                it.first == pid && it.second.first == frame
            }?.second?.second
            val rpmBytes = bytes(0x0C)
            val rpm = rpmBytes?.let { (it[0] * 256 + it[1]) / 4 }
            TelemetryHub.markObdDataUpdated()
            ObdFreezeFrame(
                frameNumber = frame,
                rpm = rpm,
                speedKmh = bytes(0x0D)?.firstOrNull(),
                coolantTempC = bytes(0x05)?.firstOrNull()?.minus(40),
                intakeAirTempC = bytes(0x0F)?.firstOrNull()?.minus(40),
                engineLoadPercent = bytes(0x04)?.firstOrNull()?.let { ObdDiagnostics.percent(it) },
                throttlePercent = bytes(0x11)?.firstOrNull()?.let { ObdDiagnostics.percent(it) }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is java.io.IOException) markConnectionError()
            null
        }
    }

    /**
     * Arıza Kodlarını Sıfırla (Mode 04 - Check Engine Söndür)
     */
    suspend fun clearDtcCodes(): Boolean = withContext(Dispatchers.IO) {
        if (state.value != ConnectionState.CONNECTED) return@withContext false
        try {
            val res = sendCommand("04")
            // Komut yankısı veya başka bir hex alanı değil, gerçek Mode 04 yanıtı aranır.
            val acknowledged = hasDtcClearAcknowledgement(res)
            if (acknowledged) {
                TelemetryHub.obdDtcList.value = emptyList()
                TelemetryHub.markObdDataUpdated()
            }
            acknowledged
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is java.io.IOException) markConnectionError()
            false
        }
    }

    private fun markConnectionError() {
        TelemetryHub.obdConnected.value = false
        TelemetryHub.clearObdData()
        state.value = ConnectionState.ERROR
        closeTransport()
    }

    private suspend fun sendCommand(cmd: String): String = withContext(Dispatchers.IO) {
        commandMutex.withLock {
            val out = outStream ?: throw java.io.IOException("OBD çıkış akışı kapalı")
            val ins = inStream ?: throw java.io.IOException("OBD giriş akışı kapalı")

            // Önceki komuttan kalmış baytlar yeni yanıtın başına eklenmesin.
            // ELM komutları mutex ile sıralandığından burada yalnızca bekleyen artıkları temizle.
            var drained = 0
            while (ins.available() > 0 && drained < MAX_RESPONSE_BYTES) {
                val skipped = ins.read()
                if (skipped < 0) throw java.io.IOException("OBD bağlantısı kapandı")
                drained++
            }
            if (ins.available() > 0) {
                throw java.io.IOException("OBD giriş tamponu beklenmeyen miktarda veri içeriyor")
            }

            out.write((cmd + "\r").toByteArray(Charsets.US_ASCII))
            out.flush()

            val response = StringBuilder()
            val buffer = ByteArray(128)
            val deadline = System.nanoTime() + COMMAND_TIMEOUT_MS * 1_000_000L
            var promptReceived = false

            while (System.nanoTime() < deadline) {
                if (!kotlin.coroutines.coroutineContext.isActive) {
                    throw CancellationException("OBD komutu iptal edildi")
                }
                val available = ins.available()
                if (available > 0) {
                    val read = ins.read(buffer, 0, minOf(buffer.size, available))
                    if (read < 0) throw java.io.IOException("OBD bağlantısı okuma sırasında kapandı")
                    if (read == 0) {
                        delay(5)
                        continue
                    }
                    for (index in 0 until read) {
                        val value = buffer[index].toInt().toChar()
                        if (value == '>') {
                            promptReceived = true
                            break
                        }
                        response.append(value)
                        if (response.length > MAX_RESPONSE_BYTES) {
                            throw java.io.IOException("OBD yanıtı izin verilen boyutu aştı")
                        }
                    }
                    if (promptReceived) break
                } else {
                    delay(10)
                }
            }

            if (!promptReceived) {
                throw SocketTimeoutException("ELM327 yanıt sonlandırıcısı zamanında alınamadı: $cmd")
            }
            response.toString().trim()
        }
    }

    private const val COMMAND_TIMEOUT_MS = 1_500L
    private const val MAX_RESPONSE_BYTES = 4_096

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
        val digits = raw.trim().removeSuffix("V").removeSuffix("v").trim()
        if (!digits.matches(Regex("^\\d{1,2}(?:\\.\\d{1,3})?$"))) return null
        return digits.toFloatOrNull()?.takeIf { it in 0.0f..30.0f }
    }

    internal fun hasDtcClearAcknowledgement(raw: String): Boolean {
        val clean = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        // 44, Mode 04 pozitif yanıt servis kodudur. Tek başına substring eşleşmesi
        // yerine ELM başlıksız yanıtı veya 7E8–7EF CAN yanıtını kabul et.
        if (clean == "44" || clean.startsWith("44")) return true
        val ecuHeader = Regex("7E[89A-F]")
        return ecuHeader.findAll(clean).any { match ->
            clean.substring(match.range.last + 1).matches(Regex("^(?:0[1-8])?44.*"))
        }
    }

    internal fun parseDtc(raw: String): List<String> {
        // Yalnızca hex karakterleri tut; "NO DATA", ELM komut yankısı ve satır sonları atılır.
        val clean = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        if (clean.isEmpty()) return emptyList()

        val result = linkedSetOf<String>()
        var cursor = 0
        // ELM327 başlıklar açıkken yanıtlar 7E8/7E9... ECU kimliğiyle art arda gelebilir.
        val ecuHeader = Regex("7E[89A-F]")
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


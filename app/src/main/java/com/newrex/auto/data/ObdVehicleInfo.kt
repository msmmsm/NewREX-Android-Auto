package com.newrex.auto.data

/** Mode 09 ile standart OBD emisyon ECU'sundan alınan kimlik bilgileri. VIN bellekte tutulur; diske/loga yazılmaz. */
data class ObdVehicleIdentity(
    val vin: String? = null,
    val ecuName: String? = null,
    val calibrationIds: List<String> = emptyList(),
    val calibrationVerificationNumbers: List<String> = emptyList()
)

/** SAE J1979 Service 09 ayrıştırıcıları. Yalnızca ECU'nun destek bitmap'inde ilan ettiği PID'ler sorgulanmalıdır. */
object ObdVehicleInfo {
    internal fun parseSupportedPids(raw: String): Set<Int>? =
        parseBitmap(raw, "4900")

    private fun parseBitmap(raw: String, prefix: String): Set<Int>? {
        val clean = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        val index = clean.indexOf(prefix)
        if (index < 0 || clean.length < index + prefix.length + 8) return null
        val mask = clean.substring(index + prefix.length, index + prefix.length + 8).toLongOrNull(16)
            ?: return null
        return (0 until 32).mapNotNull { bit ->
            if ((mask and (1L shl (31 - bit))) != 0L) bit + 1 else null
        }.toSet()
    }

    /** Mode 09 PID 02; ISO-TP ardışık çerçeve sıra baytlarını ve CAN başlıklarını atlar. */
    internal fun parseVin(raw: String): String? {
        val bytes = responseBytes(raw, 0x02) ?: return null
        val ascii = bytes.mapNotNull { value ->
            if (value in 0x20..0x7E) value.toChar() else null
        }.joinToString("").filter { it.isLetterOrDigit() }
        return ascii.takeIf { it.length == 17 && it.all { ch -> ch in 'A'..'Z' || ch in '0'..'9' } }
    }

    /** Mode 09 PID 04 (CALID) ve 0A (ECU adı) ASCII verileri. */
    internal fun parseAsciiValues(raw: String, pid: Int): List<String> {
        if (pid !in setOf(0x04, 0x0A)) return emptyList()
        val bytes = responseBytes(raw, pid) ?: return emptyList()
        val text = bytes.mapNotNull { value ->
            if (value in 0x20..0x7E) value.toChar() else null
        }.joinToString("").trim().trim('\u0000')
        return text.split(Regex("[\\u0000\\r\\n]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.any(Char::isLetterOrDigit) }
    }

    /** Mode 09 PID 06 CVN: her dört bayt bir doğrulama numarasıdır. */
    internal fun parseCvns(raw: String): List<String> {
        val bytes = responseBytes(raw, 0x06) ?: return emptyList()
        return bytes.take(bytes.size - bytes.size % 4).chunked(4)
            .map { chunk -> chunk.joinToString("") { it.toString(16).uppercase().padStart(2, '0') } }
    }

    private fun responseBytes(raw: String, pid: Int): List<Int>? {
        val clean = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        val marker = "49" + pid.toString(16).uppercase().padStart(2, '0')
        val index = clean.indexOf(marker)
        if (index < 0) return null
        val tail = clean.substring(index + marker.length)
        if (tail.length < 2) return null
        val parsed = tail.take(tail.length - tail.length % 2).chunked(2)
            .mapNotNull { it.toIntOrNull(16) }
        if (parsed.isEmpty()) return null
        // İlk bayt, Mode 09 CAN yanıtında veri sıra numarasıdır (VIN için 01).
        val start = if (parsed.first() in 0x01..0x05) 1 else 0
        return parsed.drop(start).takeWhile { it !in 0x21..0x2F }
            .ifEmpty { parsed.drop(start) }
    }
}

package com.newrex.auto.data

/** Mode 02 dondurulmuş kareden ayrıştırılmış, yalnızca ECU'nun döndürdüğü alanlar. */
data class ObdFreezeFrame(
    val frameNumber: Int,
    val rpm: Int? = null,
    val speedKmh: Int? = null,
    val coolantTempC: Int? = null,
    val intakeAirTempC: Int? = null,
    val engineLoadPercent: Int? = null,
    val throttlePercent: Int? = null
)

/** Sınırlı Türkçe DTC açıklama kataloğu; listede olmayan kodlar için tahmin yapılmaz. */
object ObdDiagnostics {
    private val descriptions = mapOf(
        "P0100" to "Kütle veya hacimsel hava akışı devresi arızası",
        "P0101" to "Kütle hava akışı sensörü aralık veya performans sorunu",
        "P0110" to "Emme havası sıcaklık sensörü devresi arızası",
        "P0115" to "Motor soğutma suyu sıcaklık sensörü devresi arızası",
        "P0120" to "Gaz kelebeği konum sensörü devresi arızası",
        "P0130" to "Oksijen sensörü devresi arızası (Bank 1, Sensör 1)",
        "P0171" to "Yakıt karışımı çok fakir (Bank 1)",
        "P0172" to "Yakıt karışımı çok zengin (Bank 1)",
        "P0300" to "Rastgele veya birden fazla silindirde tekleme algılandı",
        "P0301" to "1. silindirde tekleme algılandı",
        "P0302" to "2. silindirde tekleme algılandı",
        "P0303" to "3. silindirde tekleme algılandı",
        "P0304" to "4. silindirde tekleme algılandı",
        "P0420" to "Katalizör sistemi verimliliği eşik altında (Bank 1)",
        "P0440" to "Yakıt buharı emisyon kontrol sistemi arızası",
        "P0455" to "Yakıt buharı emisyon sisteminde büyük kaçak algılandı",
        "P0500" to "Araç hız sensörü devresi arızası",
        "P0562" to "Araç sistem voltajı düşük",
        "P0700" to "Şanzıman kontrol sistemi arıza bildirimi",
        "P0705" to "Şanzıman menzil sensörü devresi arızası"
    )

    fun description(code: String): String? = descriptions[code.trim().uppercase()]

    /** Mode 02 PID 00 destek bitmap'ini ayrıştırır. */
    internal fun parseSupportedFreezeFramePids(raw: String): Set<Int>? {
        val clean = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        val index = clean.indexOf("4200")
        if (index < 0) return null
        val payload = clean.substring(index + 4, (index + 12).coerceAtMost(clean.length))
        if (payload.length != 8) return null
        val mask = payload.toLongOrNull(16) ?: return null
        return (0 until 32).mapNotNull { bit ->
            if ((mask and (1L shl (31 - bit))) != 0L) bit + 1 else null
        }.toSet()
    }

    /**
     * Mode 02 yanıtından tek PID verisini çıkarır.
     * Beklenen yanıt: 42, PID, kare numarası, veri baytları.
     * CAN başlıkları ve boşluklar kabul edilir; eksik baytlar reddedilir.
     */
    internal fun parseFreezeFrameBytes(raw: String, pid: Int, byteCount: Int): Pair<Int, List<Int>>? {
        if (pid !in 0x00..0xFF || byteCount !in 1..4) return null
        val clean = raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        val prefix = "42" + pid.toString(16).uppercase().padStart(2, '0')
        val index = clean.indexOf(prefix)
        if (index < 0) return null
        val start = index + prefix.length
        val required = 2 + byteCount * 2
        if (clean.length - start < required) return null
        val frame = clean.substring(start, start + 2).toIntOrNull(16) ?: return null
        val bytes = (0 until byteCount).map { offset ->
            clean.substring(start + 2 + offset * 2, start + 4 + offset * 2).toIntOrNull(16)
                ?: return null
        }
        return frame to bytes
    }

    internal fun percent(a: Int): Int = ((a * 100f) / 255f).toInt()
}

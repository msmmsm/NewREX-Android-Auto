// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/** Biten bir yolculuk (Android Auto bağlantısı süresince). */
data class TripRecord(
    val start: Long,
    val end: Long,
    val km: Float,
    val movingMinutes: Int,
    val maxKmh: Int,
    val harshAccel: Int,
    val harshBrake: Int,
    val score: Int,
) {
    val avgKmh: Int get() = if (movingMinutes > 0) (km / movingMinutes * 60).roundToInt() else 0

    /** Bozuk, gerçek dışı veya geleceğe tarihli yolculukları geçmişe alma. */
    fun isValid(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val durationMs = end - start
        return start > 0L && end >= start &&
            durationMs <= 72L * 60 * 60 * 1000 &&
            end <= nowMillis + 5 * 60 * 1000L &&
            km.isFinite() && km in 0f..3_000f &&
            movingMinutes >= 0 && movingMinutes.toLong() <= durationMs / 60_000L + 1L &&
            maxKmh in 0..260 && harshAccel in 0..5_000 && harshBrake in 0..5_000 &&
            score in 0..100
    }

    fun toJson(): JSONObject = JSONObject().put("s", start).put("e", end).put("km", km.toDouble()).put("mm", movingMinutes)
        .put("max", maxKmh).put("ha", harshAccel).put("hb", harshBrake).put("sc", score)

    companion object {
        fun fromJson(o: JSONObject) = TripRecord(o.optLong("s"), o.optLong("e"), o.optDouble("km").toFloat(),
            o.optInt("mm"), o.optInt("max"), o.optInt("ha"), o.optInt("hb"), o.optInt("sc", 100))
    }
}

/** Yakıt alımı. Depo her seferinde fullenmiş kabul edilir (tüketim hesabı böyle doğru çıkar). */
data class FuelEntry(val time: Long, val liters: Float, val price: Float, val km: Float) {
    /** Girdi ve saklanan yakıt kaydını makul araç sınırları içinde tut. */
    fun isValid(nowMillis: Long = System.currentTimeMillis()): Boolean =
        time > 0L && time <= nowMillis + 5 * 60 * 1000L &&
            liters.isFinite() && liters in 0.1f..200f &&
            price.isFinite() && price in 0f..20_000_000f &&
            km.isFinite() && km in 0f..10_000_000f

    fun toJson(): JSONObject = JSONObject().put("t", time).put("l", liters.toDouble()).put("p", price.toDouble()).put("km", km.toDouble())

    companion object {
        fun fromJson(o: JSONObject) = FuelEntry(o.optLong("t"), o.optDouble("l").toFloat(), o.optDouble("p").toFloat(),
            o.optDouble("km").toFloat())
    }
}

/** Kullanıcının telefonda tuttuğu bakım/onarım kaydı; kilometre ve tutar kullanıcı girişidir. */
data class ServiceRecord(
    val time: Long,
    val category: String,
    val odometerKm: Float?,
    val cost: Float,
    val note: String,
) {
    fun isValid(nowMillis: Long = System.currentTimeMillis()): Boolean =
        time > 0L && time <= nowMillis + 5 * 60 * 1000L &&
            category.isNotBlank() && category.length <= 40 &&
            (odometerKm == null || (odometerKm.isFinite() && odometerKm in 0f..10_000_000f)) &&
            cost.isFinite() && cost in 0f..20_000_000f &&
            note.length <= 160

    fun toJson(): JSONObject = JSONObject()
        .put("t", time)
        .put("c", category)
        .put("km", odometerKm?.toDouble() ?: JSONObject.NULL)
        .put("cost", cost.toDouble())
        .put("note", note)

    companion object {
        fun fromJson(o: JSONObject): ServiceRecord = ServiceRecord(
            time = o.optLong("t"),
            category = o.optString("c"),
            odometerKm = if (o.isNull("km")) null else o.optDouble("km").toFloat(),
            cost = o.optDouble("cost").toFloat(),
            note = o.optString("note"),
        )
    }
}

/**
 * Cihazsız araç defteri: yolculuklar, yakıt alımları, tahmini kilometre, bakım ve servis geçmişi.
 * Her şey sadece telefonda saklanır. Kilometre ve menzil GPS'e ve girilen değerlere dayanan tahminlerdir.
 */
object CarLog {
    /** Saklanan en fazla yolculuk (eskiler silinir). Günde birkaç yolculukla yaklaşık bir yıl. */
    private const val MAX_TRIPS = 500
    private const val MAX_SERVICE_RECORDS = 200
    private lateinit var sp: SharedPreferences

    /** Kayıtlar değişince artar; ekranlar bunu dinleyip yenilenir. */
    val changes = MutableStateFlow(0)

    fun init(context: Context) {
        if (!::sp.isInitialized) sp = context.applicationContext.getSharedPreferences("newrex_car", Context.MODE_PRIVATE)
    }

    private fun bump() { changes.value++ }

    // ---- Ayarlar ----

    /** Hız uyarısı sınırı (km/s); 0 = kapalı. */
    var speedLimit: Int
        get() = sp.getInt("speedLimit", 0)
        set(v) { sp.edit().putInt("speedLimit", v).apply(); bump() }

    /** Hız uyarısının sesi (SpeedAlert.SOUNDS sırası). */
    var speedSound: Int
        get() = sp.getInt("speedSound", 0)
        set(v) { sp.edit().putInt("speedSound", v).apply(); bump() }

    /** En iyi 0-100 km/s hızlanma süresi (saniye). */
    var bestZeroHundred: Float?
        get() = sp.getFloat("best_0_100", 0f).takeIf { it > 0f }
        set(v) { if (v != null) sp.edit().putFloat("best_0_100", v).apply() else sp.edit().remove("best_0_100").apply(); bump() }

    /** Son yapılan 0-100 km/s hızlanma süresi (saniye). */
    var lastZeroHundred: Float?
        get() = sp.getFloat("last_0_100", 0f).takeIf { it > 0f }
        set(v) { if (v != null) sp.edit().putFloat("last_0_100", v).apply() else sp.edit().remove("last_0_100").apply(); bump() }


    /** Depo hacmi (litre); menzil tahmini için. Toyota C-HR hibrit ~43 L. */
    var tankLiters: Float
        get() = sp.getFloat("tank", 43f).takeIf { it.isFinite() && it in 5f..200f } ?: 43f
        set(v) { if (v.isFinite() && v in 5f..200f) { sp.edit().putFloat("tank", v).apply(); bump() } }

    var oilChangeKm: Float
        get() = sp.getFloat("oilKm", 0f).takeIf { it.isFinite() && it in 0f..10_000_000f } ?: 0f
        set(v) { if (v.isFinite() && v in 0f..10_000_000f) { sp.edit().putFloat("oilKm", v).apply(); bump() } }

    var oilIntervalKm: Float
        get() = sp.getFloat("oilInterval", 10_000f).takeIf { it.isFinite() && it in 1_000f..50_000f } ?: 10_000f
        set(v) { if (v.isFinite() && v in 1_000f..50_000f) { sp.edit().putFloat("oilInterval", v).apply(); bump() } }

    // ---- Kilometre tahmini ----

    /** Bütün yolculuklarda GPS ile ölçülen toplam mesafe. */
    private var gpsTotalKm: Float
        get() = sp.getFloat("gpsTotal", 0f).takeIf { it.isFinite() && it in 0f..10_000_000f } ?: 0f
        set(v) { if (v.isFinite() && v in 0f..10_000_000f) sp.edit().putFloat("gpsTotal", v).apply() }

    /** Kullanıcının girdiği kilometre ve o andaki GPS toplamı. */
    fun setOdometer(km: Float) {
        if (!km.isFinite() || km !in 0f..10_000_000f) return
        sp.edit().putFloat("odoBase", km).putFloat("odoGpsAtBase", gpsTotalKm).apply()
        bump()
    }

    /** Tahmini kilometre: girilen değer + o zamandan beri GPS ile gidilen yol. Girilmediyse null. */
    fun estimatedOdometer(liveTripKm: Float = 0f): Float? {
        val base = sp.getFloat("odoBase", 0f)
        if (!base.isFinite() || base <= 0f || base > 10_000_000f ||
            !liveTripKm.isFinite() || liveTripKm < 0f) return null
        val gpsAtBase = sp.getFloat("odoGpsAtBase", 0f).takeIf { it.isFinite() && it >= 0f } ?: return null
        return (base + (gpsTotalKm - gpsAtBase) + liveTripKm).takeIf { it.isFinite() && it in 0f..10_000_000f }
    }

    // ---- Yolculuklar ----

    fun trips(): List<TripRecord> = read("trips") { TripRecord.fromJson(it) }.filter { it.isValid() }

    /** Geçersiz kayıt kilometre toplamına eklenmez; başarılı eklemede true döner. */
    fun addTrip(t: TripRecord): Boolean {
        if (!t.isValid()) return false
        gpsTotalKm = (gpsTotalKm + t.km).coerceAtMost(10_000_000f)
        // Çok kısa bağlantılar (garajda açıp kapama) geçmişi doldurmasın.
        if (t.km >= 0.3f) write("trips", (listOf(t) + trips()).take(MAX_TRIPS).map { it.toJson() })
        bump()
        return true
    }

    // ---- Yakıt ----

    fun fuel(): List<FuelEntry> = read("fuel") { FuelEntry.fromJson(it) }
        .filter { it.isValid() }.sortedBy { it.km }

    /** Geçersiz yakıt kaydı tüketim ve maliyet hesaplarına alınmaz. */
    fun addFuel(e: FuelEntry): Boolean {
        if (!e.isValid()) return false
        write("fuel", (fuel() + e).map { it.toJson() })
        // Yakıt alırken girilen kilometre, tahmini kilometreyi de düzeltir.
        if (e.km > 0f) setOdometer(e.km) else bump()
        return true
    }

    fun removeFuel(e: FuelEntry) {
        write("fuel", fuel().filterNot { it == e }.map { it.toJson() })
        bump()
    }

    class FuelStats(
        /** L/100 km */
        val consumption: Float?,
        /** TL/km */
        val costPerKm: Float?,
        val thisMonth: Float,
        /** Depoda kalan tahmini yakıtla gidilebilecek km. */
        val rangeKm: Float?,
    )

    fun fuelStats(currentKm: Float?): FuelStats {
        val list = fuel()
        val now = java.time.LocalDate.now()
        val thisMonth = list.filter {
            val d = java.time.Instant.ofEpochMilli(it.time).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            d.year == now.year && d.month == now.month
        }.sumOf { it.price.toDouble() }.toFloat()
        // İlk dolumdan sonraki dolumlar, aradaki yolda harcanan yakıttır
        val withKm = list.filter { it.km > 0f }
        val safeCurrentKm = currentKm?.takeIf { it.isFinite() && it >= 0f }
        var consumption: Float? = null
        var costPerKm: Float? = null
        if (withKm.size >= 2) {
            val dist = withKm.last().km - withKm.first().km
            if (dist > 50f) {
                val used = withKm.drop(1)
                val litersUsed = used.sumOf { it.liters.toDouble() }.toFloat()
                val candidate = litersUsed / dist * 100f
                if (litersUsed.isFinite() && candidate.isFinite() && candidate in 0.5f..50f) {
                    consumption = candidate
                    val cost = used.sumOf { it.price.toDouble() }.toFloat() / dist
                    costPerKm = cost.takeIf { it.isFinite() && it >= 0f }
                }
            }
        }
        val last = withKm.lastOrNull()
        // Son yakıt alımından daha düşük bir kilometre verilmişse kalan menzil tahmini üretme.
        val range = if (consumption != null && last != null && safeCurrentKm != null && safeCurrentKm >= last.km) {
            val driven = safeCurrentKm - last.km
            val left = tankLiters - driven * consumption / 100f
            (left / consumption * 100f).takeIf { it.isFinite() }?.coerceAtLeast(0f)
        } else null
        return FuelStats(consumption, costPerKm, thisMonth, range)
    }

    // ---- Bakım / servis geçmişi ----

    fun serviceRecords(): List<ServiceRecord> =
        read("service_records") { ServiceRecord.fromJson(it) }
            .filter { it.isValid() }
            .sortedByDescending { it.time }

    /** Yeni bakım kaydını yerel deftere ekler; geçersiz giriş reddedilir. */
    fun addServiceRecord(record: ServiceRecord): Boolean {
        if (!record.isValid()) return false
        val updated = (listOf(record) + serviceRecords()).take(MAX_SERVICE_RECORDS)
        write("service_records", updated.map { it.toJson() })
        if (record.category.equals("Yağ değişimi", ignoreCase = true) && record.odometerKm != null) {
            oilChangeKm = record.odometerKm
        } else {
            bump()
        }
        return true
    }

    fun removeServiceRecord(record: ServiceRecord) {
        write("service_records", serviceRecords().filterNot { it == record }.map { it.toJson() })
        bump()
    }

    /** Yağ değişimine kalan km (kilometre biliniyorsa). */
    fun oilKmLeft(currentKm: Float?): Float? {
        val km = currentKm?.takeIf { it.isFinite() && it >= 0f } ?: return null
        if (oilChangeKm <= 0f) return null
        return (oilChangeKm + oilIntervalKm - km).takeIf { it.isFinite() }
    }

    private fun <T> read(key: String, parse: (JSONObject) -> T): List<T> {
        val arr = runCatching { JSONArray(sp.getString(key, "[]")) }.getOrElse { JSONArray() }
        return (0 until arr.length()).mapNotNull { runCatching { parse(arr.getJSONObject(it)) }.getOrNull() }
    }

    private fun write(key: String, list: List<JSONObject>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        sp.edit().putString(key, arr.toString()).apply()
    }
}

/** Seçilen dönemdeki doğrulanmış yolculukların toplu özeti. */
data class TripPeriodSummary(
    val tripCount: Int,
    val distanceKm: Float,
    val movingMinutes: Long,
    val averageScore: Int,
    val maxKmh: Int,
    val harshEvents: Long,
)

/** Yolculuk geçmişi için dönemsel özet; yalnızca doğrulanmış kayıtlar hesaba katılır. */
object TripAnalytics {
    fun summarize(trips: List<TripRecord>, fromMillis: Long, untilMillis: Long): TripPeriodSummary {
        if (fromMillis <= 0L || untilMillis <= fromMillis) return TripPeriodSummary(0, 0f, 0L, 0, 0, 0L)
        val selected = trips.filter { it.isValid() && it.start >= fromMillis && it.start < untilMillis }
        if (selected.isEmpty()) return TripPeriodSummary(0, 0f, 0L, 0, 0, 0L)
        val distance = selected.sumOf { it.km.toDouble() }.toFloat()
        return TripPeriodSummary(
            tripCount = selected.size,
            distanceKm = distance.takeIf { it.isFinite() } ?: 0f,
            movingMinutes = selected.sumOf { it.movingMinutes.toLong() },
            averageScore = (selected.sumOf { it.score } / selected.size.toFloat()).roundToInt().coerceIn(0, 100),
            maxKmh = selected.maxOf { it.maxKmh },
            harshEvents = selected.sumOf { it.harshAccel.toLong() + it.harshBrake.toLong() },
        )
    }
}

/**
 * Süren yolculuğun canlı istatistikleri. Hız örneklerinden en yüksek hız, hareket süresi ve
 * ani hızlanma / sert fren sayısı çıkarılır; bunlardan 100 üzerinden ekonomik sürüş puanı hesaplanır.
 */
object TripStats {
    private const val HARSH_ACCEL = 3.0f   // m/s² (~11 km/s her saniye)
    private const val HARSH_BRAKE = -3.5f  // m/s²

    val maxKmh = MutableStateFlow(0f)
    val harshAccel = MutableStateFlow(0)
    val harshBrake = MutableStateFlow(0)
    var movingMs = 0L
        private set

    private var lastSpeed = -1f
    private var lastTime = 0L
    private var lastHarsh = 0L

    fun reset() {
        maxKmh.value = 0f; harshAccel.value = 0; harshBrake.value = 0
        movingMs = 0; lastSpeed = -1f; lastTime = 0; lastHarsh = 0
    }

    /** Hız örneği (m/s); bozuk, fiziksel olarak olanaksız veya zaman sırası bozuk örnekler dışlanır. */
    fun onSpeed(mps: Float, time: Long) {
        val maxPlausibleMps = 260f / 3.6f
        if (!mps.isFinite() || mps !in 0f..maxPlausibleMps || time <= 0L) {
            // Geçersiz örnekten sonra eski hızla ivme hesabı yapılmasın.
            lastSpeed = -1f
            lastTime = 0L
            return
        }

        val kmh = mps * 3.6f
        if (kmh > maxKmh.value) maxKmh.value = kmh
        if (lastTime > 0L) {
            if (time <= lastTime) {
                // Saat geriye gittiyse bu örneği yalnızca yeni başlangıç noktası yap.
                lastSpeed = mps
                lastTime = time
                return
            }
            val dt = (time - lastTime) / 1000f
            if (dt in 0.2f..5f && lastSpeed > 1f) movingMs += (dt * 1000).toLong()
            // Örnekler arası 0,7–3 sn ise ivme anlamlı; aynı olay iki kez sayılmasın diye 3 sn bekle.
            if (dt in 0.7f..3f && lastSpeed >= 0f && time - lastHarsh > 3000L) {
                val acceleration = (mps - lastSpeed) / dt
                if (acceleration > HARSH_ACCEL) { harshAccel.value++; lastHarsh = time }
                if (acceleration < HARSH_BRAKE) { harshBrake.value++; lastHarsh = time }
            }
        }
        lastSpeed = mps
        lastTime = time
    }

    /** 100 üzerinden puan: her 10 km'deki ani hareket başına 8 puan düşer. */
    fun score(km: Float): Int {
        val events = harshAccel.value + harshBrake.value
        if (events == 0) return 100
        val safeKm = km.takeIf { it.isFinite() && it >= 0f } ?: 0f
        val per10 = events / (safeKm / 10f).coerceAtLeast(1f)
        return (100 - per10 * 8).roundToInt().coerceIn(0, 100)
    }
}

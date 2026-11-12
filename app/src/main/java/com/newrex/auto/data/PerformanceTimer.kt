// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 0-100 km/s ve 0-50 km/s Performans & Hızlanma Telemetrisi (Dragy / RaceBox stili)
 *
 * Araç durduğunda (0 km/s) otomatik olarak "HAZIR" durumuna geçer.
 * Gaza basıldığı milisaniyede süre başlar; 100 km/s'ye ulaşıldığında kilitlenir ve rekoru kaydeder.
 */
object PerformanceTimer {
    enum class State {
        IDLE,       // Beklemede (Araç hareket halinde, ölçüm yok)
        READY,      // Hazır (Araç durdu, gaza basılması bekleniyor)
        MEASURING,  // Ölçüm sürüyor (Kronometre çalışıyor)
        COMPLETED   // Ölçüm tamamlandı (0-100 süresi kilitlendi)
    }

    val state = MutableStateFlow(State.READY)
    val elapsedSeconds = MutableStateFlow(0.0f)
    val splitZeroFifty = MutableStateFlow<Float?>(null)
    val lastResult = MutableStateFlow<Float?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var startTimeMs = 0L
    private var fiftyTimeMs = 0L

    init {
        scope.launch {
            TelemetryHub.telemetry.collect { snap ->
                val speed = snap.speedKmh ?: 0f
                onSpeedUpdate(speed)
            }
        }
    }

    private fun onSpeedUpdate(speedKmh: Float) {
        val now = System.currentTimeMillis()

        when (state.value) {
            State.IDLE, State.COMPLETED -> {
                // Araç tamamen durursa yeni ölçüm için otomatik HAZIR ol
                if (speedKmh <= 1.5f) {
                    state.value = State.READY
                    elapsedSeconds.value = 0f
                    splitZeroFifty.value = null
                }
            }
            State.READY -> {
                // Araç dururken gaza basıldı ve hareket başladı
                if (speedKmh > 2.5f) {
                    startTimeMs = now
                    fiftyTimeMs = 0L
                    state.value = State.MEASURING
                }
            }
            State.MEASURING -> {
                if (startTimeMs > 0) {
                    val dt = (now - startTimeMs) / 1000f
                    elapsedSeconds.value = dt

                    // 0-50 km/s ara zamanı
                    if (speedKmh >= 50f && fiftyTimeMs == 0L) {
                        fiftyTimeMs = now
                        splitZeroFifty.value = (fiftyTimeMs - startTimeMs) / 1000f
                    }

                    // 0-100 km/s tamamlandı!
                    if (speedKmh >= 100f) {
                        val finalSec = (now - startTimeMs) / 1000f
                        elapsedSeconds.value = finalSec
                        lastResult.value = finalSec
                        CarLog.lastZeroHundred = finalSec

                        // En iyi derece kontrolü
                        val currentBest = CarLog.bestZeroHundred
                        if (currentBest == null || finalSec < currentBest) {
                            CarLog.bestZeroHundred = finalSec
                        }

                        state.value = State.COMPLETED
                    }

                    // Eğer sürücü 100'e ulaşmadan yavaşlarsa iptal et
                    if (speedKmh < 1.0f && dt > 3f) {
                        state.value = State.READY
                        elapsedSeconds.value = 0f
                    }
                }
            }
        }
    }

    /**
     * Sayacı elle sıfırlamak için
     */
    fun reset() {
        state.value = State.READY
        elapsedSeconds.value = 0f
        splitZeroFifty.value = null
    }
}


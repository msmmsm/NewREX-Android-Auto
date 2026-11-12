// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto

import com.newrex.auto.data.Prefs
import com.newrex.auto.data.TelemetryHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Hıza Duyarlı Akıllı Ses Kompansatörü (Speed-Compensated Volume Control)
 *
 * Otomobil hızlandıkça oluşan yol, tekerlek ve rüzgar gürültüsünü telafi etmek için
 * ExoPlayer ses seviyesini hafifçe (%10-%20) optimize eder. Araç yavaşlayınca veya durunca
 * orijinal ses seviyesine yumuşakça geri döner.
 */
object SpeedVolumeCompensator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var baseVolume = 1.0f

    init {
        scope.launch {
            TelemetryHub.telemetry.collect { snap ->
                if (!Prefs.speedVolumeCompensate) {
                    applyVolume(1.0f)
                    return@collect
                }

                val speed = snap.speedKmh ?: 0f
                val multiplier = when {
                    speed > 110f -> 1.20f // Otoyol yüksek hız (+%20)
                    speed > 70f  -> 1.12f // Şehirlerarası / Çevre yolu (+%12)
                    speed > 40f  -> 1.05f // Şehir içi akıcı (+%5)
                    else         -> 1.00f // Dururken veya düşük hızda orijinal seviye
                }
                applyVolume(multiplier)
            }
        }
    }

    private fun applyVolume(factor: Float) {
        val player = runCatching { PlayerHub.player }.getOrNull() ?: return
        val target = (baseVolume * factor).coerceIn(0f, 1.0f)
        if (player.volume != target) {
            player.volume = target
        }
    }

    fun setBaseVolume(vol: Float) {
        baseVolume = vol.coerceIn(0f, 1.0f)
    }
}


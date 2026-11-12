// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import com.newrex.auto.PlayerHub
import kotlin.math.sin
import kotlin.random.Random

/**
 * Müzik & Radyo Canlı Ekolayzır (Equalizer / Spectrum Analyzer) Widget'ı
 *
 * Yüklenen görseldeki gibi 4 farklı görsel stil:
 * 1. RAINBOW_VINTAGE: Klasik yeşil -> sarı -> kırmızı kademeli stüdyo VU barları
 * 2. CYBER_NEON: Mavi / Camgöbeği (Cyan) & Mor degrade zeminli futuristik neon barlar
 * 3. FLAME_FIRE: Kırmızı, turuncu, sarı alev spektrumu
 * 4. MINIMAL_DOTS: Segmentli ızgara LED noktaları
 */
class EqualizerView(context: Context, style: Int = 0) : View(context) {

    enum class Style(val label: String) {
        RAINBOW_VINTAGE("Klasik Stüdyo"),
        CYBER_NEON("Cyber Neon"),
        FLAME_FIRE("Alev Kırmızı"),
        MINIMAL_DOTS("LED Izgara")
    }

    var style: Style = Style.values()[style.coerceIn(0, Style.values().lastIndex)]
        set(v) { field = v; invalidate() }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val barRect = RectF()
    private val barCount = 24
    private val heights = FloatArray(barCount) { 0.2f }
    private val targetHeights = FloatArray(barCount) { 0.2f }
    private val peaks = FloatArray(barCount) { 0.2f }
    private var phase = 0f

    private var animator: ValueAnimator? = null

    init {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 80L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase += 0.15f
                val isPlaying = PlayerHub.playing.value
                for (i in 0 until barCount) {
                    if (isPlaying) {
                        // Müzik çalarken canlı dalga simülasyonu
                        val wave = (sin((phase + i * 0.45).toDouble()).toFloat() + 1f) * 0.35f
                        val noise = Random.nextFloat() * 0.3f
                        targetHeights[i] = (wave + noise).coerceIn(0.12f, 0.98f)
                    } else {
                        // Duraklatıldığında düşük bekleme çizgileri
                        targetHeights[i] = 0.08f
                    }
                    // Yumuşak geçiş
                    heights[i] += (targetHeights[i] - heights[i]) * 0.35f

                    // Tepe noktası (Peak indicator)
                    if (heights[i] > peaks[i]) {
                        peaks[i] = heights[i]
                    } else {
                        peaks[i] = (peaks[i] - 0.02f).coerceAtLeast(heights[i])
                    }
                }
                invalidate()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator?.start()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val pad = context.dpf(6)
        val availableW = w - pad * 2
        val availableH = h - pad * 2
        val barWidth = availableW / barCount
        val gap = barWidth * 0.22f
        val actualBarW = barWidth - gap

        val segments = 12
        val segH = availableH / segments
        val segGap = segH * 0.2f
        val actualSegH = segH - segGap

        for (i in 0 until barCount) {
            val bx = pad + i * barWidth
            val barLevel = heights[i]
            val activeSegments = (barLevel * segments).toInt().coerceIn(1, segments)

            for (s in 0 until activeSegments) {
                val sy = pad + availableH - (s + 1) * segH
                barRect.set(bx, sy, bx + actualBarW, sy + actualSegH)

                fillPaint.color = when (style) {
                    Style.RAINBOW_VINTAGE -> {
                        val ratio = s.toFloat() / segments
                        when {
                            ratio > 0.75f -> 0xFFFF3B30.toInt() // Kırmızı
                            ratio > 0.45f -> 0xFFFFCC00.toInt() // Sarı
                            else -> 0xFF34C759.toInt()          // Yeşil
                        }
                    }
                    Style.CYBER_NEON -> {
                        val ratio = s.toFloat() / segments
                        when {
                            ratio > 0.7f -> 0xFFE040FB.toInt()  // Magenta/Mor
                            ratio > 0.4f -> 0xFF3D5AFE.toInt()  // Elektrik Mavi
                            else -> 0xFF00E5FF.toInt()          // Camgöbeği Neon
                        }
                    }
                    Style.FLAME_FIRE -> {
                        val ratio = s.toFloat() / segments
                        when {
                            ratio > 0.7f -> 0xFFFF2A00.toInt()  // Kor Kırmızı
                            ratio > 0.35f -> 0xFFFF7A00.toInt() // Turuncu
                            else -> 0xFFFFD600.toInt()          // Ateş Sarısı
                        }
                    }
                    Style.MINIMAL_DOTS -> {
                        Palette.accent
                    }
                }

                canvas.drawRoundRect(barRect, context.dpf(2), context.dpf(2), fillPaint)
            }

            // Tepe çizgisi (Floating peak dot)
            val peakY = pad + availableH - (peaks[i] * segments).toInt().coerceIn(1, segments) * segH
            linePaint.color = when (style) {
                Style.RAINBOW_VINTAGE -> 0xFFFF3B30.toInt()
                Style.CYBER_NEON -> 0xFF00E5FF.toInt()
                Style.FLAME_FIRE -> 0xFFFF2A00.toInt()
                Style.MINIMAL_DOTS -> Palette.TEXT
            }
            linePaint.strokeWidth = context.dpf(2)
            canvas.drawLine(bx, peakY, bx + actualBarW, peakY, linePaint)
        }
    }
}

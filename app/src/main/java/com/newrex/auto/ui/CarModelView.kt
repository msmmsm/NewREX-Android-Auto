// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.newrex.auto.DriveState
import com.newrex.auto.Saver
import com.newrex.auto.data.Prefs
import com.newrex.auto.data.TelemetryHub
import com.newrex.auto.data.TelemetrySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/**
 * Toyota C-HR 2025 — 3D / 2.5D Etkileşimli Otomobil Kokpit Modeli
 *
 * Özellikler:
 * - Toyota C-HR 2025 "Hammerhead" keskin LED imza tasarımı & Bi-Tone tavan
 * - Özelleştirilebilir Türk Plaka standardı ([TR | 34 REX 1337])
 * - Park halindeyken parmakla 360° serbest döndürme
 * - Sürüşe geçildiğinde otomatik açı kilitleme ve GPU/ısınma koruması
 * - Etkileşimli Telemetri Noktaları (Kaput -> Motor/Hararet/Akü, Depo -> Yakıt/Menzil)
 * - Sketchfab / Harici .GLB model dosyasını yükleme desteği
 */
class CarModelView(
    context: Context,
    private val scope: CoroutineScope,
    private val onEditPlateRequested: () -> Unit = {},
    private val onHotspotSelected: (HotspotType) -> Unit = {}
) : FrameLayout(context) {

    enum class HotspotType {
        HOOD_ENGINE, // Kaput: Motor Sıcaklığı, Akü Voltajı, Devir
        FUEL_TANK,   // Depo: Yakıt Seviyesi, Kalan Menzil
        WHEELS_SPEED // Tekerlekler: Hız, Lastik/Yolculuk durumu
    }

    private var currentAngle = 35f // Başlangıç perspektif açısı (derece)
    private var lastTouchX = 0f
    private var isDragging = false
    private var observeJob: Job? = null

    // UI Bileşenleri
    private val chassisCanvas = ChrRendererView(context)
    private val hintText = TextView(context)
    private val driveLockBadge = TextView(context)
    private val modelBadge = TextView(context)

    // Hotspot Bilgi Baloncukları
    private val hoodCallout = createHotspotBadge("⚡ Motor & Akü") { onHotspotSelected(HotspotType.HOOD_ENGINE) }
    private val fuelCallout = createHotspotBadge("⛽ Yakıt") { onHotspotSelected(HotspotType.FUEL_TANK) }

    // Plaka Rozeti (Tıklanınca düzenleme açılır)
    private val plateBadge = createPlateView()

    init {
        // Zemin: Lüks otomotiv mat degrade zemin
        background = gradient(Palette.SURFACE, Palette.withAlpha(Palette.accent, 0x1A), context.dpf(22))
        val pad = context.dp(12)
        setPadding(pad, pad, pad, pad)

        // 1. Araç Çizim Katmanı (Toyota C-HR 2025)
        addView(chassisCanvas, flp(MATCH, MATCH))

        // 2. Etkileşimli Sıcak Noktalar (Hotspots)
        addView(hoodCallout, flp(WRAP, WRAP, Gravity.TOP or Gravity.START).margins(l = context.dp(16), t = context.dp(12)))
        addView(fuelCallout, flp(WRAP, WRAP, Gravity.BOTTOM or Gravity.END).margins(r = context.dp(16), b = context.dp(24)))

        // 3. Üst Model Başlığı & Plaka
        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            modelBadge.apply {
                text = Prefs.carModel // "Toyota C-HR (2025)"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Palette.TEXT)
            }
            addView(modelBadge, lp(0, WRAP, 1f))
            addView(plateBadge, lp(WRAP, WRAP))
        }
        addView(topBar, flp(MATCH, WRAP, Gravity.TOP))

        // 4. Durum & Kullanıcı Rehberi Alt Barı
        val bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            hintText.apply {
                text = "↻ 360° Döndürmek için kaydırın"
                textSize = 10f
                setTextColor(Palette.TEXT_FAINT)
            }
            addView(hintText, lp(0, WRAP, 1f))

            driveLockBadge.apply {
                text = "● SÜRÜŞ KİLİDİ"
                textSize = 9f
                setTextColor(Palette.ORANGE)
                typeface = Typeface.DEFAULT_BOLD
                visibility = GONE
                setPadding(context.dp(6), context.dp(2), context.dp(6), context.dp(2))
                background = rounded(Palette.withAlpha(Palette.ORANGE, 0x22), context.dpf(6))
            }
            addView(driveLockBadge, lp(WRAP, WRAP))
        }
        addView(bottomBar, flp(MATCH, WRAP, Gravity.BOTTOM))

        // Gerçek 1:1 Toyota C-HR 2024 3D Modelini Yükle
        scope.launch {
            Car3dModel.loadIfNeeded(context)
            chassisCanvas.postInvalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Sürüş halindeyken dikkat dağıtmamak ve GPU tasarrufu için döndürme kilitlenir
        if (DriveState.driving.value || Saver.on) {
            return super.onTouchEvent(event)
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - lastTouchX
                    lastTouchX = event.x
                    currentAngle = (currentAngle + (dx * 0.75f)) % 360f
                    if (currentAngle < 0) currentAngle += 360f
                    chassisCanvas.rotationAngle = currentAngle
                    updateHotspotPositions(currentAngle)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startObserving()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        observeJob?.cancel()
        observeJob = null
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = scope.launch(Dispatchers.Main) {
            launch {
                DriveState.driving.collect { isDriving ->
                    if (isDriving) {
                        animateToAngle(35f)
                        hintText.visibility = GONE
                        driveLockBadge.visibility = VISIBLE
                    } else {
                        hintText.visibility = VISIBLE
                        driveLockBadge.visibility = GONE
                    }
                }
            }
            launch {
                TelemetryHub.telemetry.collect { snap ->
                    updateTelemetryCallouts(snap)
                }
            }
            launch {
                Prefs.changes.collect {
                    modelBadge.text = Prefs.carModel
                    refreshPlateView()
                    chassisCanvas.invalidate()
                }
            }
        }
    }

    private fun animateToAngle(target: Float) {
        val anim = ValueAnimator.ofFloat(currentAngle, target).apply {
            duration = 350
            addUpdateListener {
                currentAngle = it.animatedValue as Float
                chassisCanvas.rotationAngle = currentAngle
                updateHotspotPositions(currentAngle)
            }
        }
        anim.start()
    }

    private fun updateHotspotPositions(angle: Float) {
        val rad = Math.toRadians(angle.toDouble())
        val cosVal = cos(rad).toFloat()
        hoodCallout.alpha = (0.5f + (cosVal * 0.5f)).coerceIn(0.2f, 1f)
        fuelCallout.alpha = (0.5f - (cosVal * 0.5f)).coerceIn(0.2f, 1f)
    }

    private fun updateTelemetryCallouts(snap: TelemetrySnapshot) {
        val volt = snap.batteryVoltage?.let { "%.1fV".format(it) }
        val temp = snap.coolantTempC?.let { "${it}°C" }
        hoodCallout.text = when {
            temp != null && volt != null -> "⚡ $temp | $volt"
            temp != null -> "🌡 $temp"
            volt != null -> "⚡ $volt"
            else -> "⚡ Motor & Akü"
        }

        val fuel = snap.fuelLevelPercent?.let { "%.0f%%".format(it) }
        val range = snap.rangeKm?.let { "%.0fkm".format(it) }
        fuelCallout.text = when {
            fuel != null && range != null -> "⛽ $fuel ($range)"
            fuel != null -> "⛽ $fuel"
            else -> "⛽ Depo"
        }
    }

    private fun createHotspotBadge(title: String, onClick: () -> Unit): TextView = TextView(context).apply {
        text = title
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Palette.TEXT)
        setPadding(context.dp(8), context.dp(4), context.dp(8), context.dp(4))
        background = pressable(rounded(Palette.withAlpha(Palette.accent, 0x40), context.dpf(10), context.dp(1), Palette.accent), context.dpf(10))
        onTap { onClick() }
    }

    /**
     * Gerçek Türk Plakası Görünümü [TR | 34 REX 1337]
     */
    private fun createPlateView(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = rounded(Color.WHITE, context.dpf(4), context.dp(1), 0xFF888888.toInt())
        setPadding(0, 0, context.dp(6), 0)

        // Sol Mavi TR Şeridi
        val trBadge = TextView(context).apply {
            text = "TR"
            textSize = 9f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = rounded(0xFF003399.toInt(), context.dpf(3))
            setPadding(context.dp(4), context.dp(2), context.dp(4), context.dp(2))
        }
        addView(trBadge, lp(WRAP, MATCH))

        // Plaka Metni
        val plateText = TextView(context).apply {
            tag = "plate_text"
            text = Prefs.carPlate
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.BLACK)
            setPadding(context.dp(6), context.dp(2), 0, context.dp(2))
        }
        addView(plateText, lp(WRAP, WRAP))

        onTap { onEditPlateRequested() }
    }

    private fun refreshPlateView() {
        plateBadge.findViewWithTag<TextView>("plate_text")?.text = Prefs.carPlate
    }

    /**
     * Toyota C-HR 2025 Özel Şasi ve Aydınlatma Çizici (ChassisRendererView)
     */
    private class ChrRendererView(context: Context) : View(context) {
        var rotationAngle = 35f
            set(v) { field = v; invalidate() }

        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = context.dpf(0.8f)
            color = 0x24FFFFFF
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            val cx = w / 2f
            val cy = h / 2f + context.dpf(6f)
            val scale = (w * 0.95f).coerceAtMost(h * 2.2f)

            Car3dModel.draw(
                canvas = canvas,
                centerX = cx,
                centerY = cy,
                scale = scale,
                yawAngleDeg = rotationAngle,
                pitchAngleDeg = 18f,
                bodyColor = Palette.accent,
                paint = bodyPaint,
                strokePaint = strokePaint
            )
        }
    }
}

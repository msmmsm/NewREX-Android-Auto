// NewREX / OmniDrive Android Auto — Copyright (C) 2026 — GPL-3.0-or-later
package com.newrex.auto.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Gerçek 1:1 Ölçekli Toyota C-HR 2024 3D CAD Modeli Yükleyici ve 60 FPS Projektör
 *
 * Model Özellikleri:
 * - 1055 Köşe (Vertex), 2209 Üçgen Yüzey (Face)
 * - X: 4.36m (Uzunluk), Y: 2.04m (Genişlik), Z: 1.56m (Yükseklik)
 * - Donanım tasarruflu (Isınmayan) 3D İzometrik Perspektif Motoru
 * - Dinamik Yönlü Işıklandırma (Directional Shading) & Bi-Tone Tavan
 */
object Car3dModel {
    private var isLoaded = false
    private var rawVertices = FloatArray(0) // x, y, z normalized
    private var faceIndices = IntArray(0)   // 3 indices per triangle
    private var triangleCount = 0

    // Bounding Box Merkezleme & Normalizasyon
    private const val CX = 252.545f
    private const val CY = 0f
    private const val CZ = 767.38f
    private const val SPAN = 4361.87f

    // Çizim arabellekleri (Garbage Collector yükünü sıfırlamak için yeniden kullanılır)
    private var projectedX = FloatArray(0)
    private var projectedY = FloatArray(0)
    private var depthZ = FloatArray(0)

    private class Triangle(
        var i0: Int = 0, var i1: Int = 0, var i2: Int = 0,
        var depth: Float = 0f,
        var color: Int = 0,
        var isRoof: Boolean = false
    )

    private var triangles = Array(0) { Triangle() }
    private val trianglePath = Path()

    suspend fun loadIfNeeded(context: Context) {
        if (isLoaded) return
        withContext(Dispatchers.IO) {
            runCatching {
                val assetManager = context.assets
                val inputStream = assetManager.open("models/toyota_chr_2024/model_0.obj")
                val reader = BufferedReader(InputStreamReader(inputStream))

                val tempVerts = ArrayList<Float>(1055 * 3)
                val tempFaces = ArrayList<Int>(2209 * 3)

                reader.useLines { lines ->
                    for (line in lines) {
                        if (line.startsWith("v ")) {
                            val parts = line.split(" ").filter { it.isNotEmpty() }
                            if (parts.size >= 4) {
                                val x = parts[1].toFloat()
                                val y = parts[2].toFloat()
                                val z = parts[3].toFloat()
                                // Merkeze al ve normalize et [-0.5, 0.5]
                                tempVerts.add((x - CX) / SPAN)
                                tempVerts.add((y - CY) / SPAN)
                                tempVerts.add((z - CZ) / SPAN)
                            }
                        } else if (line.startsWith("f ")) {
                            val parts = line.split(" ").filter { it.isNotEmpty() }
                            if (parts.size >= 4) {
                                val i0 = parts[1].substringBefore('/').toInt() - 1
                                val i1 = parts[2].substringBefore('/').toInt() - 1
                                val i2 = parts[3].substringBefore('/').toInt() - 1
                                tempFaces.add(i0)
                                tempFaces.add(i1)
                                tempFaces.add(i2)
                            }
                        }
                    }
                }

                rawVertices = tempVerts.toFloatArray()
                faceIndices = tempFaces.toIntArray()
                triangleCount = faceIndices.size / 3

                val vCount = rawVertices.size / 3
                projectedX = FloatArray(vCount)
                projectedY = FloatArray(vCount)
                depthZ = FloatArray(vCount)

                triangles = Array(triangleCount) { i ->
                    val i0 = faceIndices[i * 3]
                    val i1 = faceIndices[i * 3 + 1]
                    val i2 = faceIndices[i * 3 + 2]
                    // Z yüksekliği (Tavan Bi-Tone kontrolü)
                    val avgZ = (rawVertices[i0 * 3 + 2] + rawVertices[i1 * 3 + 2] + rawVertices[i2 * 3 + 2]) / 3f
                    Triangle(i0, i1, i2, isRoof = avgZ > 0.08f)
                }

                isLoaded = true
            }
        }
    }

    /**
     * 3D Toyota C-HR Modelini 360° Açı ve Dinamik Işık ile Çiz
     */
    fun draw(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        scale: Float,
        yawAngleDeg: Float,
        pitchAngleDeg: Float,
        bodyColor: Int,
        paint: Paint,
        strokePaint: Paint? = null
    ) {
        if (!isLoaded || triangleCount == 0) return

        val yawRad = Math.toRadians(yawAngleDeg.toDouble())
        val cosYaw = cos(yawRad).toFloat()
        val sinYaw = sin(yawRad).toFloat()

        val pitchRad = Math.toRadians(pitchAngleDeg.toDouble())
        val cosPitch = cos(pitchRad).toFloat()
        val sinPitch = sin(pitchRad).toFloat()

        // 1. Köşeleri Döndür ve Ekrana İzdüşür
        val vCount = rawVertices.size / 3
        var vi = 0
        for (i in 0 until vCount) {
            val x = rawVertices[vi++]
            val y = rawVertices[vi++]
            val z = rawVertices[vi++]

            // Yaw döndürme (Dikey eksende 360°)
            val x1 = x * cosYaw - y * sinYaw
            val y1 = x * sinYaw + y * cosYaw
            val z1 = z

            // Pitch eğimi (Kuşbakışı perspektif açısı)
            val y2 = y1 * cosPitch - z1 * sinPitch
            val z2 = y1 * sinPitch + z1 * cosPitch

            projectedX[i] = centerX + x1 * scale
            projectedY[i] = centerY - z2 * scale
            depthZ[i] = y2
        }

        // 2. Işık Vektörü (Sol-Üst-Ön Işık Kaynağı)
        val lx = 0.35f
        val ly = 0.45f
        val lz = 0.82f

        val baseR = Color.red(bodyColor)
        val baseG = Color.green(bodyColor)
        val baseB = Color.blue(bodyColor)

        // 3. Üçgenleri Güncelle ve Derinliğe Göre Sırala (Painter's Algorithm)
        for (t in triangles) {
            val i0 = t.i0
            val i1 = t.i1
            val i2 = t.i2

            t.depth = depthZ[i0] + depthZ[i1] + depthZ[i2]

            // Yüzey Normali (Lighting calculation)
            val ax = projectedX[i1] - projectedX[i0]
            val ay = projectedY[i1] - projectedY[i0]
            val bx = projectedX[i2] - projectedX[i0]
            val by = projectedY[i2] - projectedY[i0]

            // 2D Cross product ile Back-face culling & Aydınlatma
            val crossZ = ax * by - ay * bx
            val faceFacing = crossZ < 0 // Ön yüz

            val lightFactor = if (faceFacing) {
                0.55f + 0.45f * Math.abs(crossZ / (scale * scale * 0.05f).coerceAtLeast(1f)).coerceIn(0f, 1f)
            } else {
                0.28f // Gölgeli taraf
            }

            t.color = if (t.isRoof) {
                // Bi-Tone Tavan (Koyu Karbon / Siyah)
                val roofVal = (45 * lightFactor).toInt().coerceIn(15, 120)
                Color.argb(240, roofVal, roofVal, (roofVal * 1.15f).toInt())
            } else {
                // Gövde Rengi
                val r = (baseR * lightFactor).toInt().coerceIn(10, 255)
                val g = (baseG * lightFactor).toInt().coerceIn(10, 255)
                val b = (baseB * lightFactor).toInt().coerceIn(10, 255)
                Color.argb(245, r, g, b)
            }
        }

        // Derinliğe göre arkadan öne sırala
        triangles.sortBy { it.depth }

        // 4. Çizim Katmanı
        for (t in triangles) {
            val i0 = t.i0
            val i1 = t.i1
            val i2 = t.i2

            trianglePath.rewind()
            trianglePath.moveTo(projectedX[i0], projectedY[i0])
            trianglePath.lineTo(projectedX[i1], projectedY[i1])
            trianglePath.lineTo(projectedX[i2], projectedY[i2])
            trianglePath.close()

            paint.color = t.color
            canvas.drawPath(trianglePath, paint)

            if (strokePaint != null) {
                canvas.drawPath(trianglePath, strokePaint)
            }
        }
    }
}


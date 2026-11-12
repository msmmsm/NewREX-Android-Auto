// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.newrex.auto.PlayerHub
import com.newrex.auto.R
import com.newrex.auto.data.Kind
import com.newrex.auto.data.MediaEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Otomobil Kokpit Alt Çubuğu — Kalıcı Mini Oynatıcı (MiniPlayer)
 *
 * TV, Radyo veya Podcast dinlenirken tüm sayfalarda (Araç, Hava, Ayarlar vb.)
 * kesintisiz çalma durumunu, logosunu ve kontrollerini sürücünün parmak ucunda tutar.
 */
class MiniPlayerView(
    context: Context,
    private val scope: CoroutineScope,
    private val onOpenPlayer: (MediaEntry) -> Unit
) : FrameLayout(context) {

    private var observeJob: Job? = null

    private val logoView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private val titleView = TextView(context).apply {
        setTextColor(Palette.TEXT)
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    private val subtitleView = TextView(context).apply {
        setTextColor(Palette.TEXT_DIM)
        textSize = 12f
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    private val playPauseBtn = TextView(context).apply {
        textSize = 18f
        setTextColor(Palette.TEXT)
        gravity = Gravity.CENTER
    }

    init {
        visibility = GONE
        val h = context.dp(58)
        val pad = context.dp(8)
        setPadding(pad, 0, pad, 0)

        // Arka plan: Yarı saydam lüks koyu cam + ince tema kenarlığı
        background = rounded(Palette.SURFACE, context.dpf(16), context.dp(1), Palette.withAlpha(Palette.accent, 0x55))

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            // 1. Logo / İkon
            val logoSize = context.dp(40)
            addView(logoView, lp(logoSize, logoSize).margins(r = context.dp(10)))

            // 2. Metin Alanı (Başlık ve Kategori)
            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                addView(titleView, lp(MATCH, WRAP))
                addView(subtitleView, lp(MATCH, WRAP).margins(t = context.dp(2)))
            }
            addView(textLayout, lp(0, WRAP, 1f))

            // 3. Kontrol Düğmeleri
            val prevBtn = createControlBtn("⏮") { PlayerHub.prev() }
            val nextBtn = createControlBtn("⏭") { PlayerHub.next() }
            val closeBtn = createControlBtn("✕") { PlayerHub.stop() }

            playPauseBtn.apply {
                val btnSize = context.dp(40)
                background = pressable(rounded(Palette.withAlpha(Palette.accent, 0x40), context.dpf(20)), context.dpf(20))
                onTap(immediate = true) { PlayerHub.toggle() }
            }

            addView(prevBtn, lp(context.dp(36), context.dp(36)).margins(l = context.dp(4)))
            addView(playPauseBtn, lp(context.dp(40), context.dp(40)).margins(l = context.dp(6), r = context.dp(6)))
            addView(nextBtn, lp(context.dp(36), context.dp(36)).margins(r = context.dp(6)))
            addView(closeBtn, lp(context.dp(32), context.dp(32)))
        }

        addView(content, flp(MATCH, h, Gravity.CENTER))

        // Tıklanınca ilgili medya sayfasına geç
        onTap {
            PlayerHub.now.value?.let { onOpenPlayer(it) }
        }
    }

    private fun createControlBtn(iconText: String, action: () -> Unit): TextView = TextView(context).apply {
        text = iconText
        textSize = 15f
        setTextColor(Palette.TEXT_DIM)
        gravity = Gravity.CENTER
        background = pressable(rounded(0x1AFFFFFF, context.dpf(18)), context.dpf(18))
        onTap(immediate = true) { action() }
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
                PlayerHub.now.collect { entry ->
                    if (entry == null) {
                        visibility = GONE
                    } else {
                        visibility = VISIBLE
                        bind(entry)
                    }
                }
            }
            launch {
                PlayerHub.playing.collect { isPlaying ->
                    updatePlayState(isPlaying, PlayerHub.buffering.value)
                }
            }
            launch {
                PlayerHub.buffering.collect { isBuffering ->
                    updatePlayState(PlayerHub.playing.value, isBuffering)
                }
            }
        }
    }

    private fun bind(entry: MediaEntry) {
        titleView.text = entry.title
        subtitleView.text = when (entry.kind) {
            Kind.TV -> "Canlı TV"
            Kind.RADIO -> entry.group.ifBlank { "Radyo" }
            Kind.PODCAST -> entry.group.ifBlank { "Podcast" }
        }

        val imgUrl = entry.image
        if (!imgUrl.isNullOrBlank()) {
            logoView.load(imgUrl) {
                crossfade(true)
                transformations(RoundedCornersTransformation(context.dpf(8)))
                error(R.drawable.ic_car)
            }
        } else {
            logoView.setImageDrawable(rounded(Palette.accent, context.dpf(8)))
        }
    }

    private fun updatePlayState(isPlaying: Boolean, isBuffering: Boolean) {
        playPauseBtn.text = when {
            isBuffering -> "⏳"
            isPlaying -> "⏸"
            else -> "▶"
        }
    }
}


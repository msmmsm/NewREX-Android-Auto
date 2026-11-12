// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.phone

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.newrex.auto.data.Prefs
import com.newrex.auto.ui.ClockView
import com.newrex.auto.ui.ColorPickerView
import com.newrex.auto.ui.FmDisplayView
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.dpf
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.rounded

/**
 * Telefon ekranı: Tema rengi, özel arka plan, saat ve radyo gösterge seçicileri
 */
object PhoneThemeHelper {

    fun buildThemeBody(context: Context, onThemeChanged: () -> Unit): Array<View> {
        val accentPicker = buildColorPicker(context, Palette.accent, 1f) {
            Prefs.customAccent = it
            onThemeChanged()
        }
        val bgStart = Palette.safeBackground(Prefs.bgColor).takeIf { it != 0 } ?: Palette.BG_TOP
        val bgPicker = buildColorPicker(context, bgStart, Palette.MAX_BG_VALUE) {
            Prefs.bgColor = it
            onThemeChanged()
        }
        val accentRow = buildSwatchRow(context, (Palette.accents + Palette.extraAccents).toList(), accentPicker) { c ->
            val i = Palette.accents.indexOf(c)
            if (i >= 0) Prefs.accentIndex = i else Prefs.customAccent = c
            onThemeChanged()
        }
        val bgRow = buildSwatchRow(context, Palette.backgrounds.toList(), bgPicker) { c ->
            Prefs.bgColor = c
            onThemeChanged()
        }
        return arrayOf(
            label("Tema rengi", 15f, bold = true), accentRow, accentPicker,
            label("Arka plan", 15f, bold = true), bgRow, bgPicker,
            label("🌈 ile istediğin rengi seç. Arka plan yazılar okunsun diye fazla açık olamaz. Değişiklik araç ekranına hemen yansır.",
                12f, Palette.TEXT_FAINT, lines = 3),
        )
    }

    fun buildLooksBody(context: Context, onSummaryRefresh: (String) -> Unit): Array<View> {
        val clocks = ClockView.STYLES.indices.map { i -> ClockView(context, i) }
        val fms = FmDisplayView.STYLES.indices.map { i -> FmDisplayView(context, i).apply { freq = 1000 } }
        val clockRow = buildChoiceRow(context, clocks, ClockView.STYLES.map { it.name + if (it.analog) " ◷" else "" }, context.dp(150), context.dp(120),
            { Prefs.clockStyle }) {
            Prefs.clockStyle = it
            updateLooksSummary(onSummaryRefresh)
        }
        val fmRow = buildChoiceRow(context, fms, FmDisplayView.STYLES.map { it.name }, context.dp(210), context.dp(90),
            { Prefs.fmStyle }) {
            Prefs.fmStyle = it
            updateLooksSummary(onSummaryRefresh)
        }
        updateLooksSummary(onSummaryRefresh)
        return arrayOf(
            label("Saat", 15f, bold = true), clockRow,
            label("Radyo frekans göstergesi", 15f, bold = true), fmRow,
            label("◷ olanlar yuvarlak (akrepli) saat. Seçtiğin tasarım araç ekranına hemen yansır.", 12f, Palette.TEXT_FAINT, lines = 2),
        )
    }

    private fun updateLooksSummary(onSummaryRefresh: (String) -> Unit) {
        val s = "Saat: ${ClockView.STYLES[Prefs.clockStyle.coerceIn(0, ClockView.STYLES.lastIndex)].name} · " +
            "Radyo: ${FmDisplayView.STYLES[Prefs.fmStyle.coerceIn(0, FmDisplayView.STYLES.lastIndex)].name}"
        onSummaryRefresh(s)
    }

    private fun buildSwatchRow(context: Context, colors: List<Int>, picker: View, onPick: (Int) -> Unit): View {
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        colors.forEach { c ->
            row.addView(TextView(context).apply {
                if (c == 0) { text = "Oto"; gravity = Gravity.CENTER; setTextColor(Color.WHITE); textSize = 11f }
                background = rounded(if (c == 0) Palette.BG_TOP else c, context.dpf(19), context.dp(1), 0x55FFFFFF)
                setOnClickListener { picker.visibility = View.GONE; onPick(c) }
            }, lp(context.dp(38), context.dp(38)).margins(r = context.dp(8)))
        }
        row.addView(TextView(context).apply {
            text = "+"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 18f
            setShadowLayer(context.dpf(4), 0f, 0f, 0xAA000000.toInt())
            background = ColorPickerView.rainbowDot()
            setOnClickListener { picker.visibility = if (picker.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        }, lp(context.dp(38), context.dp(38)))
        return HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
    }

    private fun buildColorPicker(context: Context, initial: Int, maxValue: Float, onPick: (Int) -> Unit): View {
        val picker = ColorPickerView(context, initial, maxValue)
        val hexBox = EditText(context).apply {
            setSingleLine()
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            setText(ColorPickerView.hex(picker.color))
            background = rounded(0x22FFFFFF, context.dpf(10), context.dp(1), 0x33FFFFFF)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { v, _, _ ->
                ColorPickerView.parseHex(v.text.toString())?.let { c ->
                    picker.setColor(c)
                    setText(ColorPickerView.hex(picker.color))
                    onPick(picker.color)
                }
                false
            }
        }
        val preview = View(context)
        fun show(c: Int) { preview.background = rounded(c, context.dpf(10)) }
        show(picker.color)
        picker.onChange = { show(it); hexBox.setText(ColorPickerView.hex(it)) }
        picker.onCommit = { onPick(it) }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            addView(picker, lp(MATCH, context.dp(220)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(preview, lp(context.dp(44), context.dp(40)))
                addView(label("HEX", 13f, Palette.TEXT_DIM), lp(WRAP, WRAP).margins(l = context.dp(12), r = context.dp(8)))
                addView(hexBox, lp(0, context.dp(40), 1f))
            }, lp(MATCH, WRAP).margins(t = context.dp(6)))
        }
    }

    private fun buildChoiceRow(
        context: Context,
        previews: List<View>,
        names: List<String>,
        w: Int,
        h: Int,
        current: () -> Int,
        onPick: (Int) -> Unit
    ): View {
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val cards = previews.mapIndexed { i, v ->
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(context.dp(8), context.dp(8), context.dp(8), context.dp(6))
                addView(v, lp(MATCH, h))
                addView(label(names[i], 13f, Palette.TEXT_DIM), lp(WRAP, WRAP).margins(t = context.dp(4)))
            }
        }
        fun paint() {
            cards.forEachIndexed { i, c ->
                val sel = i == current()
                c.background = rounded(
                    if (sel) Palette.withAlpha(Palette.accent, 0x33) else 0x14FFFFFF,
                    context.dpf(14),
                    if (sel) context.dp(2) else context.dp(1),
                    if (sel) Palette.accent else 0x33FFFFFF
                )
            }
        }
        cards.forEachIndexed { i, c ->
            c.setOnClickListener { onPick(i); paint() }
            row.addView(c, lp(w, WRAP).margins(r = context.dp(10)))
        }
        paint()
        return HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
    }
}


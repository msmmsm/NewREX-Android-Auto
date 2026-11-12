// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.pages

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.newrex.auto.ui.CarUi
import com.newrex.auto.ui.ColorPickerView
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Page
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.flp
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.pill

/** Özel renk seçimi sayfası (Tema rengi veya arka plan rengi). */
class ColorPickerPage(
    ui: CarUi,
    override val title: String,
    private val initial: Int,
    private val maxValue: Float,
    private val onPick: (Int) -> Unit,
) : Page(ui) {
    override fun build(): View = with(ctx) {
        var picked = initial
        val preview = ColorPickerView(this, initial, maxValue).apply {
            onChange = { c -> picked = c }
        }
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
            addView(preview, lp(dp(220), dp(220)).margins(b = dp(16)))
            val actions = LinearLayout(this@with).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(pill("Kaydet") { onPick(picked); ui.pop() }, lp(dp(120), dp(44)).margins(r = dp(10)))
                addView(pill("Vazgeç") { ui.pop() }, lp(dp(100), dp(44)))
            }
            addView(actions, lp(WRAP, WRAP))
        }
    }
}


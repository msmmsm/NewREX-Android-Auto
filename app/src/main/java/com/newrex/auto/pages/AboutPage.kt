// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.pages

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import com.newrex.auto.Brand
import com.newrex.auto.BuildConfig
import com.newrex.auto.R
import com.newrex.auto.ui.CarUi
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Page
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.icon
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.neonText
import com.newrex.auto.ui.panel
import com.newrex.auto.ui.pill

/** Sürüm, lisans ve açık kaynak teşekkürleri. */
class AboutPage(ui: CarUi) : Page(ui) {
    override val title = "Hakkında"

    override fun build(): View = with(ctx) {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(24))

            addView(panel().apply {
                setPadding(dp(20), dp(18), dp(20), dp(18))
                addView(LinearLayout(this@with).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(icon(R.drawable.ic_car, 32, Palette.accent), lp(dp(32), dp(32)).margins(r = dp(12)))
                    addView(LinearLayout(this@with).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(label("NewREX", 22f, Palette.accent, bold = true).apply { neonText() })
                        addView(label("Android Auto • v${BuildConfig.VERSION_NAME}", 13f, Palette.TEXT_DIM))
                    })
                })
            }, lp(MATCH, WRAP).margins(b = dp(12)))

            addView(panel().apply {
                setPadding(dp(20), dp(16), dp(20), dp(16))
                addView(label(
                    "NewREX; arabanızın ekranında canlı TV, YouTube, radyo ve podcast dinlemenizi sağlayan " +
                        "ücretsiz ve reklamsız bir araç bilgi-eğlence merkezidir.\n\n" +
                        "Hiçbir kişisel bilginizi toplamaz, sunuculara göndermez. Tüm ayarlarınız ve listeleriniz " +
                        "yalnızca telefonunuzda saklanır.\n\n" +
                        "GPL-3.0 lisanslı açık kaynaklı bir projedir.",
                    14f, Palette.TEXT, lines = 10,
                ))
            }, lp(MATCH, WRAP).margins(b = dp(12)))

            addView(pill("GitHub'da İncele", R.drawable.ic_language) {
                ui.toast("github.com/msmmsm/NewREX-Android-Auto")
            }, lp(WRAP, dp(44)))
        }
        ScrollView(this).apply { addView(col) }
    }
}


// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.pages

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.newrex.auto.R
import com.newrex.auto.ui.CarUi
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Page
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.Tile
import com.newrex.auto.ui.TileAdapter
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.dpf
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.neonText
import com.newrex.auto.ui.panel
import com.newrex.auto.ui.pill
import com.newrex.auto.ui.tileGrid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Telefonda Yüklü Tüm Uygulamaları Gösteren ve Başlatan Uygulama Çekmecesi (App Drawer)
 */
class PhoneAppsPage(ui: CarUi) : Page(ui) {
    override val title = "Telefon Uygulamaları"

    private lateinit var adapter: TileAdapter
    private lateinit var status: TextView
    private lateinit var countBadge: TextView

    override fun build(): View = with(ctx) {
        adapter = TileAdapter(this)
        status = label("Uygulamalar taranıyor...", 15f, Palette.TEXT_DIM).apply { gravity = Gravity.CENTER }
        countBadge = label("", 12f, Palette.accent, bold = true)

        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(12))

            // Üst Açıklama ve Bilgi Kartı
            val infoCard = panel().apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(10), dp(14), dp(10))

                val textBox = LinearLayout(this@with).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(label("Telefon Uygulama Çekmecesi", 15f, bold = true))
                    addView(label("Telefonunuzda yüklü tüm uygulamaları doğrudan açabilirsiniz.", 11f, Palette.TEXT_DIM))
                }
                addView(textBox, lp(0, WRAP, 1f))
                addView(countBadge, lp(WRAP, WRAP))
            }
            addView(infoCard, lp(MATCH, WRAP).margins(b = dp(8)))

            addView(status, lp(MATCH, WRAP).margins(t = dp(20)))
            addView(tileGrid(adapter), lp(MATCH, 0, 1f))
        }.also {
            loadInstalledApps()
        }
    }

    private fun loadInstalledApps() {
        scope.launch {
            val pm = ctx.packageManager
            val apps = withContext(Dispatchers.IO) {
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                resolveInfos.mapNotNull { ri ->
                    val pkg = ri.activityInfo.packageName
                    if (pkg == ctx.packageName) return@mapNotNull null
                    val label = ri.loadLabel(pm).toString()
                    val icon = ri.loadIcon(pm)
                    AppItem(label, pkg, icon)
                }.sortedBy { it.label.lowercase() }
            }

            status.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
            if (apps.isEmpty()) status.text = "Açılabilir uygulama bulunamadı."
            countBadge.text = "${apps.size} Uygulama"

            adapter.items = apps.map { app ->
                Tile(
                    key = app.packageName,
                    title = app.label,
                    subtitle = app.packageName.substringAfterLast('.'),
                    image = null,
                    fallbackIcon = R.drawable.ic_apps,
                    art = { _ -> app.icon },
                    onClick = { launchApp(app.packageName) }
                )
            }
        }
    }

    private fun launchApp(packageName: String) {
        val launchIntent = ctx.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launchIntent != null) {
            ctx.startActivity(launchIntent)
        }
    }

    private data class AppItem(val label: String, val packageName: String, val icon: Drawable)
}


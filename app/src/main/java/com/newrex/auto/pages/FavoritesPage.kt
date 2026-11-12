// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.pages

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.newrex.auto.PlayerHub
import com.newrex.auto.data.Kind
import com.newrex.auto.data.MediaEntry
import com.newrex.auto.data.Prefs
import com.newrex.auto.ui.CarUi
import com.newrex.auto.ui.MATCH
import com.newrex.auto.ui.Page
import com.newrex.auto.ui.Palette
import com.newrex.auto.ui.Tile
import com.newrex.auto.ui.TileAdapter
import com.newrex.auto.ui.WRAP
import com.newrex.auto.ui.dp
import com.newrex.auto.ui.label
import com.newrex.auto.ui.lp
import com.newrex.auto.ui.margins
import com.newrex.auto.ui.pill
import com.newrex.auto.ui.tileGrid
import kotlinx.coroutines.launch

/** Favori kanallar, radyolar ve son çalınanlar tek yerde. */
class FavoritesPage(ui: CarUi) : Page(ui) {
    override val title = "Favoriler"

    private enum class Tab(val label: String) { ALL("Tüm favoriler"), TV("TV"), RADIO("Radyo"), RECENT("Son çalınanlar") }

    private var tab = Tab.ALL
    private lateinit var tabs: LinearLayout
    private lateinit var tiles: TileAdapter
    private lateinit var status: TextView

    override fun build(): View = with(ctx) {
        tiles = TileAdapter(this)
        tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        status = label("", 16f, Palette.TEXT_DIM, lines = 3).apply { gravity = Gravity.CENTER }
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(12))
            addView(tabs, lp(MATCH, dp(44)).margins(b = dp(8)))
            addView(status, lp(MATCH, WRAP).margins(t = dp(40)))
            addView(tileGrid(tiles), lp(MATCH, 0, 1f))
        }
    }

    override fun onShow() {
        refreshTabs()
        refreshList()
        scope.launch { Prefs.changes.collect { refreshTabs(); refreshList() } }
    }

    private fun refreshTabs() = with(ctx) {
        tabs.removeAllViews()
        Tab.values().forEach { t ->
            val count = countFor(t)
            tabs.addView(
                pill("${t.label} ($count)", selected = t == tab) { tab = t; refreshTabs(); refreshList() },
                lp(WRAP, dp(40)).margins(r = dp(8)),
            )
        }
    }

    private fun countFor(t: Tab) = when (t) {
        Tab.ALL -> Prefs.favorites().size
        Tab.TV -> Prefs.favorites(Kind.TV).size
        Tab.RADIO -> Prefs.favorites(Kind.RADIO).size
        Tab.RECENT -> Prefs.recents().size
    }

    private fun refreshList() {
        val list = when (tab) {
            Tab.ALL -> Prefs.favorites()
            Tab.TV -> Prefs.favorites(Kind.TV)
            Tab.RADIO -> Prefs.favorites(Kind.RADIO)
            Tab.RECENT -> Prefs.recents()
        }
        status.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        status.text = if (list.isEmpty()) when (tab) {
            Tab.RECENT -> "Henüz bir şey çalmadın.\nTV ya da radyo açtığında burada görünür."
            else -> "Yıldızladığın kanal ve radyolar burada görünür.\nBir yayın açıp ★ tuşuna basarak favorilere ekle."
        } else ""
        tiles.submit(list.map { e ->
            Tile(
                title = e.title,
                subtitle = when (e.kind) {
                    Kind.TV -> "TV"
                    Kind.RADIO -> e.group.ifBlank { "Radyo" }
                    Kind.PODCAST -> "Podcast"
                },
                logo = e.logo,
                action = { play(e, list) },
            )
        })
    }

    private fun play(entry: MediaEntry, list: List<MediaEntry>) {
        val playable = list.filter { it.kind == entry.kind }
        val start = playable.indexOfFirst { it.key == entry.key }.coerceAtLeast(0)
        PlayerHub.play(playable, start)
        if (entry.kind == Kind.TV) ui.push(PlayerPage(ui))
    }
}


// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.pages

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.Locale

/**
 * YouTube WebView için dar kapsamlı reklam isteği filtresi.
 *
 * Video CDN'lerini (özellikle googlevideo.com) engellemez; bu alan adları gerçek video akışını da taşır.
 * YouTube isteklerini değiştirdiğinde bu filtre kısmen etkisiz kalabilir.
 */
internal object YouTubeAdBlocker {
    private val adHosts = setOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
    )

    private val adPathMarkers = listOf(
        "/pagead/",
        "/api/stats/ads",
        "/get_midroll_info",
        "/ptracking",
        "/youtubei/v1/player/ad_break",
    )

    fun shouldBlock(url: String): Boolean {
        val uri = runCatching { android.net.Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        if (adHosts.any { host == it || host.endsWith(".$it") }) return true

        val pathAndQuery = (uri.encodedPath.orEmpty() + "?" + uri.encodedQuery.orEmpty())
            .lowercase(Locale.ROOT)
        val isYouTubeHost = host == "youtube.com" || host.endsWith(".youtube.com") ||
            host == "youtubei.googleapis.com"
        return isYouTubeHost && adPathMarkers.any { pathAndQuery.contains(it) }
    }

    fun emptyResponse(): WebResourceResponse =
        WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))

    /**
     * Sayfa içi reklam katmanlarını gizler ve görünür atlama düğmesine basar.
     * YouTube'un kendi oynatıcı akışını değiştirmez; bu nedenle her reklam biçimini engelleme garantisi yoktur.
     */
    val pageScript: String = """
        (function () {
          if (window.__newrexAdFilterInstalled) return;
          window.__newrexAdFilterInstalled = true;
          var style = document.createElement('style');
          style.id = 'newrex-ad-filter';
          style.textContent = [
            'ytd-ad-slot',
            'ytd-in-feed-ad-layout-renderer',
            'ytd-display-ad-renderer',
            'ytd-promoted-sparkles-web-renderer',
            'ytm-promoted-sparkles-web-renderer',
            '.ytp-ad-overlay-container',
            '.ytp-ad-text-overlay',
            '.ytp-ad-image-overlay'
          ].join(',') + '{display:none!important;visibility:hidden!important;pointer-events:none!important}';
          (document.head || document.documentElement).appendChild(style);

          function skipAd() {
            var button = document.querySelector(
              '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, button.ytp-ad-skip-button'
            );
            if (button && button.offsetParent !== null) {
              try { button.click(); } catch (_) {}
            }
            var player = document.querySelector('.html5-video-player.ad-showing');
            var video = player && player.querySelector('video');
            if (video && Number.isFinite(video.duration) && video.duration > 0 &&
                video.currentTime < video.duration - 0.25) {
              try { video.currentTime = Math.max(0, video.duration - 0.1); } catch (_) {}
            }
          }
          skipAd();
          window.setInterval(skipAd, 700);
          new MutationObserver(skipAd).observe(document.documentElement, {
            childList: true, subtree: true, attributes: true,
            attributeFilter: ['class', 'aria-label']
          });
        })();
    """.trimIndent()
}

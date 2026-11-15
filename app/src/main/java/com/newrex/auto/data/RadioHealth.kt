// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.data

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.TimeUnit

/** Kullanıcı isteğiyle tek bir radyo akışını düşük maliyetli ve güvenli biçimde sınar. */
object RadioHealth {
    enum class State { HEALTHY, SLOW, REDIRECT, AUTH_REQUIRED, NOT_FOUND, RATE_LIMITED, SERVER_ERROR, INVALID_CONTENT, NETWORK_ERROR, UNSAFE_ADDRESS }

    data class Result(
        val state: State,
        val message: String,
        val httpCode: Int? = null,
        val latencyMs: Long? = null,
        val contentType: String? = null,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun check(station: MediaEntry): Result = withContext(Dispatchers.IO) {
        if (station.kind != Kind.RADIO) {
            return@withContext Result(State.INVALID_CONTENT, "Yalnızca radyo istasyonları sınanabilir.")
        }
        val uri = runCatching { URI(station.url) }.getOrNull()
            ?: return@withContext Result(State.UNSAFE_ADDRESS, "Yayın adresi geçersiz.")
        val scheme = uri.scheme?.lowercase()
        val host = uri.host
        if (scheme !in setOf("http", "https") || host.isNullOrBlank() || uri.rawUserInfo != null ||
            host.equals("localhost", true) || host.endsWith(".local", true)) {
            return@withContext Result(State.UNSAFE_ADDRESS, "Yerel, kimlik bilgili veya desteklenmeyen adres sınanmadı.")
        }
        try {
            // Katalogdan gelen bir adresin yerel ağa yönelmesini engelle.
            val addresses = InetAddress.getAllByName(host)
            if (addresses.isEmpty() || addresses.any {
                    it.isAnyLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress ||
                        it.isSiteLocalAddress || it.isMulticastAddress
                }) {
                return@withContext Result(State.UNSAFE_ADDRESS, "Yerel veya özel ağ adresi güvenlik nedeniyle sınanmadı.")
            }
            val request = Request.Builder()
                .url(station.url)
                .header("User-Agent", Net.USER_AGENT)
                .header("Range", "bytes=0-0")
                .get()
                .build()
            val started = SystemClock.elapsedRealtime()
            client.newCall(request).execute().use { response ->
                val elapsed = SystemClock.elapsedRealtime() - started
                val type = response.header("Content-Type")?.substringBefore(';')?.trim()?.lowercase()
                val classification = classifyStatus(response.code, type)
                if (classification.first == State.HEALTHY || classification.first == State.SLOW) {
                    // Range desteklemeyen sunucularda da yalnızca ilk bayt okunur; gövde indirilmez.
                    val firstByte = response.body?.byteStream()?.read() ?: -1
                    if (firstByte < 0) return@withContext Result(
                        State.INVALID_CONTENT, "Sunucu başarılı yanıt verdi ancak yayın verisi göndermedi.",
                        response.code, elapsed, type
                    )
                }
                val state = if (classification.first == State.HEALTHY && elapsed >= 2000) State.SLOW else classification.first
                val message = if (state == State.SLOW) "Yayın yanıt verdi ancak bağlantı yavaş." else classification.second
                Result(state, message, response.code, elapsed, type)
            }
        } catch (e: Exception) {
            Result(State.NETWORK_ERROR, when (e) {
                is java.net.UnknownHostException -> "İstasyon sunucusunun adresi bulunamadı."
                is javax.net.ssl.SSLException -> "Güvenli bağlantı kurulamadı; sertifika veya TLS sorunu olabilir."
                is java.net.SocketTimeoutException -> "İlk yayın verisi 4 saniye içinde gelmedi."
                else -> "Yayın bağlantısı kurulamadı: \${e.message ?: "ağ hatası"}"
            })
        }
    }

    internal fun classifyStatus(code: Int, contentType: String?): Pair<State, String> = when {
        code in 300..399 -> State.REDIRECT to "Yayın yönlendirme yanıtı verdi; yönlendirme otomatik izlenmedi."
        code == 401 || code == 403 -> State.AUTH_REQUIRED to "Yayın erişim izni veya kimlik doğrulaması istiyor."
        code == 404 || code == 410 -> State.NOT_FOUND to "Yayın adresi bulunamadı veya kaldırılmış."
        code == 429 -> State.RATE_LIMITED to "Sunucu istek sınırına ulaştı; daha sonra tekrar deneyin."
        code >= 500 -> State.SERVER_ERROR to "Radyo sunucusu geçici bir hata döndürdü."
        code !in 200..299 -> State.NETWORK_ERROR to "Sunucu HTTP $code yanıtı verdi."
        contentType?.contains("text/html") == true -> State.INVALID_CONTENT to "Adres ses akışı yerine HTML sayfası döndürüyor."
        else -> State.HEALTHY to "Yayın yanıt verdi ve ilk veri baytı alındı."
    }

    internal fun latencyState(latencyMs: Long): State =
        if (latencyMs >= 2000) State.SLOW else State.HEALTHY
}

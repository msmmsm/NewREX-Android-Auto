// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.LibraryResult
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.newrex.auto.data.Kind
import com.newrex.auto.data.LiveReconnectPolicy
import com.newrex.auto.data.FmPresets
import com.newrex.auto.data.MediaEntry
import com.newrex.auto.data.Net
import com.newrex.auto.data.Prefs
import com.newrex.auto.data.PlaybackQueueSnapshot
import com.newrex.auto.data.PodcastDownloads
import com.newrex.auto.data.RadioApi
import com.newrex.auto.data.RadioRecovery
import com.newrex.auto.data.normName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Tek bir oynatıcı: TV, radyo ve podcast aynı oynatıcıyı paylaşır, sayfadan çıkınca ses devam eder. */
object PlayerHub {
    // Bazı IPTV sunucuları tarayıcı dışı istemcileri reddediyor
    private const val STREAM_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    // Radyolar ise tarayıcıya yayın değil web sayfası gönderiyor ya da birkaç saniye sonra kesiyor
    // (StreamTheWorld: Joy Türk, Süper FM, Metro FM, Virgin; Shoutcast sunucuları). Oynatıcı gibi tanıtılır.
    private const val AUDIO_USER_AGENT = Net.USER_AGENT

    private lateinit var tvSources: DefaultMediaSourceFactory
    private lateinit var audioSources: DefaultMediaSourceFactory
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var retries = 0

    /** Bu yayın en az bir kez çaldı mı: çalmış yayın kopunca uzun beklenir, hiç açılmayan için kısa. */
    private var playedOk = false
    private var healJob: Job? = null
    private var lastRadioFailureMessage: String? = null

    /** Bu istasyon için denenmiş ve çalmayan adresler. */
    private val dead = HashSet<String>()
    private val reconnect = Runnable {
        val active = exo ?: return@Runnable
        if (!active.playWhenReady || now.value?.kind == Kind.PODCAST) return@Runnable
        reload()
    }

    /** 30 sn sorunsuz çalan yayında yeniden bağlanma hakkı yenilenir. */
    private val stable = Runnable { retries = 0 }

    private var exo: ExoPlayer? = null
    private var session: MediaLibraryService.MediaLibrarySession? = null
    private var sessionPlayer: Player? = null
    /** UI denetimleri MediaController üzerinden oturuma gider; oynatıcı yalnızca servis sahibidir. */
    private var mediaController: MediaController? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var libraryObserver: Job? = null
    val networkAvailable = MutableStateFlow(false)
    private var queue: List<MediaEntry> = emptyList()
    private var index = 0

    val now = MutableStateFlow<MediaEntry?>(null)
    val playing = MutableStateFlow(false)
    val buffering = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    /** Podcast'lerde ilerleme (ms). */
    val position = MutableStateFlow(0L)
    val duration = MutableStateFlow(0L)

    private val handler = Handler(Looper.getMainLooper())
    private var ticks = 0
    private var queueTicks = 0
    private val ticker = object : Runnable {
        override fun run() {
            val p = exo ?: return
            val e = now.value
            if (e?.kind == Kind.PODCAST) {
                position.value = p.currentPosition
                duration.value = p.duration.coerceAtLeast(0)
                if (p.isPlaying && ++ticks % 10 == 0) Prefs.savePosition(e.url, p.currentPosition)
            }
            // Kuyruk ve seçili öğe 5 saniyede bir kalıcılaştırılır; canlı yayın konumu sıfırdır.
            if (p.isPlaying && ++queueTicks % 5 == 0) persistQueue()
            handler.postDelayed(this, 1000)
        }
    }

    val player: ExoPlayer get() = exo ?: error("OmniDriveMediaService henüz başlatılmadı")

    /** MediaLibraryService tarafından sunulan tek oturum. Yalnızca servis yaşam döngüsü yönetir. */
    internal val mediaSession: MediaLibraryService.MediaLibrarySession? get() = session

    /** Media library için oturumun anlık kuyruğunun güvenli kopyası. */
    internal fun libraryQueue(): List<MediaEntry> = queue.toList()

    internal fun attachController(controller: MediaController) {
        mediaController = controller
    }

    internal fun detachController(controller: MediaController) {
        if (mediaController === controller) mediaController = null
    }

    /** Media3 playback-resumption callback'i için kaydedilmiş kuyruğu belleğe alır. */
    internal fun restoreQueue(snapshot: com.newrex.auto.data.PlaybackQueueSnapshot) {
        queue = snapshot.entries
        index = snapshot.index
        now.value = queue.getOrNull(index)
        position.value = snapshot.positionMs
    }

    private fun persistQueue() {
        if (queue.isEmpty() || index !in queue.indices) return
        val current = now.value ?: return
        val activeItemMatches = exo?.currentMediaItem?.mediaId == current?.key
        val resumePosition = if (current?.kind == Kind.PODCAST) {
            if (activeItemMatches) exo?.currentPosition ?: 0L else Prefs.position(current.url)
        } else 0L
        Prefs.savePlaybackQueue(queue, index, resumePosition)
    }

    @OptIn(UnstableApi::class)
    fun init(context: Context) {
        if (exo != null) return
        fun sources(userAgent: String) = DefaultMediaSourceFactory(DefaultDataSource.Factory(context,
            DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                // HTTPS bağlantısı HTTP'ye düşürülmesin; yayın sahibi doğru TLS sertifikası sunmalıdır.
                .setAllowCrossProtocolRedirects(false)
                .setConnectTimeoutMs(10_000)
                .setReadTimeoutMs(15_000)))
            // Mobil veride kısa kopmalarda hemen hata vermesin
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(6))
        tvSources = sources(STREAM_USER_AGENT)
        audioSources = sources(AUDIO_USER_AGENT)
        PodcastDownloads.init(context)
        RadioApi.init(context)
        RadioRecovery.init(context)
        registerNetworkMonitor(context)
        val p = ExoPlayer.Builder(context)
            .setMediaSourceFactory(audioSources)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing.value = isPlaying
                if (!isPlaying) persistQueue()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // Kullanıcı veya Android Auto duraklattıysa bekleyen yeniden bağlanma iptal edilir.
                if (!playWhenReady) handler.removeCallbacks(reconnect)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId ?: return
                val nextIndex = queue.indexOfFirst { it.key == mediaId }
                if (nextIndex >= 0) {
                    index = nextIndex
                    now.value = queue[nextIndex]
                    persistQueue()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                buffering.value = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    now.value?.takeIf { it.kind == Kind.RADIO }?.let { RadioRecovery.record(it, success = true) }
                    error.value = null
                    lastRadioFailureMessage = null
                    playedOk = true
                    handler.postDelayed(stable, 30_000)
                }
                else handler.removeCallbacks(stable)
                if (state == Player.STATE_ENDED) {
                    val e = now.value
                    // Biten bölüm bir dahaki sefere baştan başlasın
                    if (e?.kind == Kind.PODCAST) Prefs.savePosition(e.url, 0)
                    // Canlı yayın "bitmez": sunucu bağlantıyı kapattıysa yeniden bağlan
                    else if (e != null) retry()
                }
            }

            override fun onPlayerError(e: PlaybackException) {
                if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    p.seekToDefaultPosition()
                    p.prepare()
                    return
                }
                val cur = now.value
                val radioMessage = if (cur?.kind == Kind.RADIO) radioFailureMessage(e) else null
                if (radioMessage != null) {
                    lastRadioFailureMessage = radioMessage
                    RadioRecovery.record(cur!!, success = false, state = "HATA_${e.errorCode}")
                }
                // Adres ölmüşse (404, 501…) beklemeden yedek yayına geç; diğer hatalarda önce yeniden bağlan
                // (bozuk yayın listesi, çözülemeyen ses gibi 3000+ hatalar da tekrar denemekle düzelmez)
                val gone = e.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                    e.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
                    e.errorCode >= PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED
                if (cur?.kind == Kind.RADIO && gone && heal(cur)) return
                // İnternet yokken art arda yayın/alternatif aramak yerine bağlantının geri gelmesini bekle.
                val networkFailure = e.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    e.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                // Yerel ağ IPTV'si internet erişimi olmadan da çalışabilir; yalnızca gerçek ağ hatasını çevrimdışı say.
                val offline = networkFailure && !networkAvailable.value
                if (offline) {
                    cur?.takeIf { it.kind == Kind.PODCAST }?.let { Prefs.savePosition(it.url, p.currentPosition) }
                    persistQueue()
                    buffering.value = true
                    error.value = "İnternet bağlantısı bekleniyor…"
                    return
                }
                if (cur != null && cur.kind != Kind.PODCAST && retry()) return
                if (cur?.kind == Kind.RADIO && heal(cur)) return
                buffering.value = false
                error.value = radioMessage ?: "Yayın açılamadı. Başka bir kanal/istasyon deneyin."
            }
        })

        // Direksiyondaki ileri/geri tuşları listede bir sonraki kanala/istasyona geçsin
        val sessionPlayer = object : ForwardingPlayer(p) {
            private val extra = intArrayOf(
                COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            )

            override fun getAvailableCommands(): Player.Commands =
                super.getAvailableCommands().buildUpon().addAll(*extra).build()

            override fun isCommandAvailable(command: Int) =
                command in extra || super.isCommandAvailable(command)

            override fun seekToNext() = if (isPodcast) seekBy(30_000) else advanceNext()
            override fun seekToNextMediaItem() = if (isPodcast) seekBy(30_000) else advanceNext()
            override fun seekToPrevious() = if (isPodcast) seekBy(-15_000) else advancePrevious()
            override fun seekToPreviousMediaItem() = if (isPodcast) seekBy(-15_000) else advancePrevious()
        }
        exo = p
        applyQuality()
        this.sessionPlayer = sessionPlayer
        // Oturum servis oluşturulurken hazırlanır; etkinlik kapanınca bırakılmaz.
        val libraryCallback = object : MediaLibraryService.MediaLibrarySession.Callback {
            @OptIn(UnstableApi::class)
            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                isForPlayback: Boolean,
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val snapshot = Prefs.playbackQueue()
                    ?: return Futures.immediateFailedFuture(IllegalStateException("Kayıtlı oynatma kuyruğu yok"))
                restoreQueue(snapshot)
                val items = if (isForPlayback) snapshot.entries else listOf(snapshot.entries[snapshot.index])
                val startIndex = if (isForPlayback) snapshot.index else 0
                val startPosition = if (isForPlayback && items[startIndex].kind == Kind.PODCAST) snapshot.positionMs else 0L
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(items.map(::itemFor), startIndex, startPosition)
                )
            }

            @OptIn(UnstableApi::class)
            override fun onGetLibraryRoot(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: MediaLibraryService.LibraryParams?,
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val root = MediaItem.Builder()
                    .setMediaId(LIBRARY_ROOT)
                    .setMediaMetadata(MediaMetadata.Builder()
                        .setTitle("OmniDrive")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build())
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(root, params))
            }

            @OptIn(UnstableApi::class)
            override fun onGetChildren(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: MediaLibraryService.LibraryParams?,
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val children = when (parentId) {
                    LIBRARY_ROOT -> libraryCategories()
                    LIBRARY_FAVORITES_TV -> externalEntries(Prefs.favorites(Kind.TV)).map(::itemFor)
                    LIBRARY_FAVORITES_RADIO -> externalEntries(Prefs.favorites(Kind.RADIO)).map(::itemFor)
                    LIBRARY_FAVORITES_PODCAST -> externalEntries(Prefs.favorites(Kind.PODCAST)).map(::itemFor)
                    LIBRARY_RECENTS -> externalEntries(Prefs.recents()).map(::itemFor)
                    LIBRARY_QUEUE -> externalEntries(libraryQueue()).map(::itemFor)
                    LIBRARY_NOW_PLAYING -> externalEntries(listOfNotNull(now.value)).map(::itemFor)
                    LIBRARY_DOWNLOADS -> externalEntries(PodcastDownloads.downloadedEntries()).map(::itemFor)
                    else -> emptyList()
                }
                val safePage = page.coerceAtLeast(0)
                val safePageSize = pageSize.coerceAtLeast(1)
                val from = (safePage.toLong() * safePageSize).coerceAtMost(children.size.toLong()).toInt()
                val to = (from.toLong() + safePageSize).coerceAtMost(children.size.toLong()).toInt()
                return Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.copyOf(children.subList(from, to)), params)
                )
            }

            @OptIn(UnstableApi::class)
            override fun onGetItem(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String,
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val item = when (mediaId) {
                    LIBRARY_ROOT -> libraryRootItem()
                    LIBRARY_FAVORITES_TV -> categoryItem(mediaId, "TV Favorileri")
                    LIBRARY_FAVORITES_RADIO -> categoryItem(mediaId, "Radyo Favorileri")
                    LIBRARY_FAVORITES_PODCAST -> categoryItem(mediaId, "Podcast Favorileri")
                    LIBRARY_RECENTS -> categoryItem(mediaId, "Son Dinlenenler")
                    LIBRARY_QUEUE -> categoryItem(mediaId, "Çalma Kuyruğu")
                    LIBRARY_NOW_PLAYING -> categoryItem(mediaId, "Şimdi Çalıyor")
                    LIBRARY_DOWNLOADS -> categoryItem(mediaId, "İndirilen Podcastler")
                    else -> externalEntries(Prefs.favorites() + Prefs.recents() + libraryQueue() + PodcastDownloads.downloadedEntries())
                        .firstOrNull { it.key == mediaId }?.let(::itemFor)
                }
                return if (item != null) Futures.immediateFuture(LibraryResult.ofItem(item, null))
                else Futures.immediateFuture(LibraryResult.ofError(SessionError(SessionError.ERROR_BAD_VALUE, "Medya öğesi bulunamadı")))
            }

            @OptIn(UnstableApi::class)
            override fun onSearch(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                params: MediaLibraryService.LibraryParams?,
            ): ListenableFuture<LibraryResult<Void>> {
                val normalized = query.trim().normName()
                if (normalized.isEmpty()) {
                    return Futures.immediateFuture(
                        LibraryResult.ofError(SessionError(SessionError.ERROR_BAD_VALUE, "Arama metni boş"))
                    )
                }
                val count = searchableEntries(normalized).size
                session.notifySearchResultChanged(browser, query, count, params)
                return Futures.immediateFuture(LibraryResult.ofVoid(params))
            }

            @OptIn(UnstableApi::class)
            override fun onGetSearchResult(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                page: Int,
                pageSize: Int,
                params: MediaLibraryService.LibraryParams?,
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val normalized = query.trim().normName()
                val matches = searchableEntries(normalized).map(::itemFor)
                val safePage = page.coerceAtLeast(0)
                val safePageSize = pageSize.coerceAtLeast(1)
                val from = (safePage.toLong() * safePageSize).coerceAtMost(matches.size.toLong()).toInt()
                val to = (from.toLong() + safePageSize).coerceAtMost(matches.size.toLong()).toInt()
                return Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.copyOf(matches.subList(from, to)), params)
                )
            }
        }
        session = MediaLibraryService.MediaLibrarySession.Builder(
            context.applicationContext, sessionPlayer, libraryCallback
        ).setId("omnidrive").build()
        libraryObserver?.cancel()
        libraryObserver = scope.launch {
            kotlinx.coroutines.flow.combine(now, Prefs.changes, PodcastDownloads.changes) { _, _, _ -> Unit }.collect {
                notifyLibraryChanges()
            }
        }
        handler.post(ticker)
    }

    fun play(list: List<MediaEntry>, startIndex: Int) {
        if (list.isEmpty()) return
        queue = list
        index = startIndex.coerceIn(0, list.lastIndex)
        start()
    }

    val isPodcast get() = now.value?.kind == Kind.PODCAST

    fun seekBy(ms: Long) {
        val active = mediaController ?: exo ?: return
        val d = active.duration
        val target = (active.currentPosition + ms).coerceAtLeast(0)
        active.seekTo(if (d > 0) target.coerceAtMost((d - 1000).coerceAtLeast(0)) else target)
        position.value = active.currentPosition
    }

    fun seekTo(ms: Long) {
        val target = ms.coerceAtLeast(0)
        (mediaController ?: exo)?.seekTo(target)
        position.value = target
    }

    /** Podcast'te direksiyon ve mini oynatıcı tuşları 30 sn ileri / 15 sn geri sarar. */
    fun next() {
        if (isPodcast) { seekBy(30_000); return }
        val controller = mediaController
        if (controller != null) controller.seekToNext() else advanceNext()
    }

    fun prev() {
        if (isPodcast) { seekBy(-15_000); return }
        val controller = mediaController
        if (controller != null) controller.seekToPrevious() else advancePrevious()
    }

    private fun advanceNext() {
        if (queue.isEmpty()) return
        index = (index + 1) % queue.size
        start()
    }

    private fun advancePrevious() {
        if (queue.isEmpty()) return
        index = (index - 1 + queue.size) % queue.size
        start()
    }

    fun toggle() {
        val controller = mediaController
        val active: Player = controller ?: exo ?: return
        if (active.isPlaying) {
            active.pause()
        } else {
            if (active.playbackState == Player.STATE_IDLE || active.playbackState == Player.STATE_ENDED) active.prepare()
            // Canlı yayında duraklatınca geride kalmamak için canlıya dön
            if (active.isCurrentMediaItemLive) active.seekToDefaultPosition()
            active.play()
        }
    }

    fun stop() {
        cancelRecovery()
        now.value?.takeIf { it.kind == Kind.PODCAST }?.let { e -> exo?.let { Prefs.savePosition(e.url, it.currentPosition) } }
        (mediaController ?: exo)?.stop()
        exo?.clearMediaItems()
        Prefs.clearPlaybackQueue()
        queue = emptyList()
        index = 0
        now.value = null
        error.value = null
        // Oturum servis yaşamı boyunca açık kalır; boş oynatma listesi medya bildirimini sonlandırır.
    }

    fun release() {
        persistQueue()
        cancelRecovery()
        now.value?.takeIf { it.kind == Kind.PODCAST }?.let { e -> exo?.let { Prefs.savePosition(e.url, it.currentPosition) } }
        handler.removeCallbacks(ticker)
        libraryObserver?.cancel()
        libraryObserver = null
        unregisterNetworkMonitor()
        session?.release()
        exo?.release()
        session = null
        sessionPlayer = null
        mediaController = null
        exo = null
        queue = emptyList()
        index = 0
        now.value = null
        playing.value = false
        buffering.value = false
        error.value = null
        position.value = 0
        duration.value = 0
    }

    /** TV görüntü kalitesi seçimi (Prefs.tvQuality): Otomatik en iyisini seçer, diğerleri üst sınırdır. */
    val QUALITIES = listOf("Otomatik" to 0, "1080p" to 1080, "720p" to 720, "480p" to 480)

    fun applyQuality() {
        val p = exo ?: return
        val h = QUALITIES.getOrNull(Prefs.tvQuality)?.second ?: 0
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .apply { if (h > 0) setMaxVideoSize(h * 16 / 9 + 1, h) else clearVideoSizeConstraints() }
            .build()
    }

    /** Kopan canlı yayına artan aralıklarla yeniden bağlanır. Bir süre düzgün çaldıysa sayaç sıfırlanır. */
    private fun retry(): Boolean {
        val active = exo ?: return false
        if (!active.playWhenReady || now.value?.kind == Kind.PODCAST) return false
        if (!LiveReconnectPolicy.shouldRetry(retries, playedOk)) return false
        buffering.value = true
        handler.removeCallbacks(reconnect)
        handler.postDelayed(reconnect, LiveReconnectPolicy.delayMs(retries))
        retries++
        return true
    }

    /** Yayın tamamen ölmüşse aynı adlı istasyonun başka bir yayın adresine geçer. */
    /** Radyo hatasını HTTP durum kodu ve medya ayrıştırma türüne göre kullanıcıya açıklar. */
    private fun radioFailureMessage(error: PlaybackException): String {
        var cause: Throwable? = error
        repeat(12) {
            val response = cause as? HttpDataSource.InvalidResponseCodeException
            if (response != null) {
                return when (response.responseCode) {
                    401, 403 -> "Radyo sunucusu erişimi reddetti (HTTP ${response.responseCode})."
                    404, 410 -> "Radyo yayını bulunamadı veya kaldırıldı (HTTP ${response.responseCode})."
                    429 -> "Radyo sunucusu istek sınırına ulaştı (HTTP 429). Daha sonra tekrar deneyin."
                    in 500..599 -> "Radyo sunucusunda geçici sorun var (HTTP ${response.responseCode})."
                    else -> "Radyo sunucusu isteği reddetti (HTTP ${response.responseCode})."
                }
            }
            cause = cause?.cause
        }
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
                "Adres açıldı ancak desteklenmeyen veya bozuk bir ses akışı döndürdü."
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "Radyo sunucusuna ulaşılamıyor. İnternet bağlantısını kontrol edin."
            else -> "Radyo yayını başlatılamadı. İstasyon adresi veya sunucusu hatalı olabilir."
        }
    }

    private fun heal(e: MediaEntry): Boolean {
        if (healJob?.isActive == true) return true
        dead += e.url
        val name = e.title.normName()
        if (name.isEmpty()) return false
        buffering.value = true
        healJob = scope.launch {
            val candidates = runCatching { RadioApi.search(e.title, country = e.countryCode ?: "TR", limit = 30) }
                .getOrNull().orEmpty()
                .filter { it.url !in dead && !RadioRecovery.isCoolingDown(it.url) }
                .filter { val n = it.title.normName(); n == name || n.startsWith("$name ") || name.startsWith("$n ") }
                .sortedByDescending { RadioRecovery.rank(e, it) }
            val alt = candidates.firstOrNull()
            if (now.value?.key != e.key) return@launch
            if (alt == null) {
                buffering.value = false
                error.value = lastRadioFailureMessage ?: "Radyo alternatifi bulunamadı. Başka bir istasyon deneyin."
                return@launch
            }
            val fixed = e.copy(url = alt.url, image = e.image ?: alt.image, home = e.home ?: alt.home)
            FmPresets.heal(e.url, fixed)
            if (index in queue.indices && queue[index].key == e.key) queue = queue.toMutableList().also { it[index] = fixed }
            retries = 0
            playedOk = false
            now.value = fixed
            reload()
        }
        return true
    }

    private fun cancelRecovery() {
        handler.removeCallbacks(reconnect)
        handler.removeCallbacks(stable)
        healJob?.cancel()
        retries = 0
    }

    private const val LIBRARY_ROOT = "omnidrive:root"
    private const val LIBRARY_FAVORITES_TV = "omnidrive:favorites:tv"
    private const val LIBRARY_FAVORITES_RADIO = "omnidrive:favorites:radio"
    private const val LIBRARY_FAVORITES_PODCAST = "omnidrive:favorites:podcast"
    private const val LIBRARY_RECENTS = "omnidrive:recents"
    private const val LIBRARY_QUEUE = "omnidrive:queue"
    private const val LIBRARY_NOW_PLAYING = "omnidrive:now-playing"
    private const val LIBRARY_DOWNLOADS = "omnidrive:downloads"

    /** Favori, son dinlenen, kuyruk veya geçerli öğe değiştiğinde bağlı araç tarayıcılarını yenile. */
    private fun notifyLibraryChanges() {
        val activeSession = session ?: return
        activeSession.notifyChildrenChanged(LIBRARY_ROOT, libraryCategories().size, null)
        activeSession.notifyChildrenChanged(LIBRARY_FAVORITES_TV, externalEntries(Prefs.favorites(Kind.TV)).size, null)
        activeSession.notifyChildrenChanged(LIBRARY_FAVORITES_RADIO, externalEntries(Prefs.favorites(Kind.RADIO)).size, null)
        activeSession.notifyChildrenChanged(LIBRARY_FAVORITES_PODCAST, externalEntries(Prefs.favorites(Kind.PODCAST)).size, null)
        activeSession.notifyChildrenChanged(LIBRARY_RECENTS, externalEntries(Prefs.recents()).size, null)
        activeSession.notifyChildrenChanged(LIBRARY_QUEUE, externalEntries(libraryQueue()).size, null)
        activeSession.notifyChildrenChanged(LIBRARY_NOW_PLAYING, if (externalEntries(listOfNotNull(now.value)).isEmpty()) 0 else 1, null)
        activeSession.notifyChildrenChanged(LIBRARY_DOWNLOADS, externalEntries(PodcastDownloads.downloadedEntries()).size, null)
    }

    private fun libraryRootItem() = MediaItem.Builder()
        .setMediaId(LIBRARY_ROOT)
        .setMediaMetadata(MediaMetadata.Builder().setTitle("OmniDrive")
            .setIsBrowsable(true).setIsPlayable(false).build())
        .build()

    private fun categoryItem(id: String, title: String) = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title)
            .setIsBrowsable(true).setIsPlayable(false).build())
        .build()

    private fun searchableEntries(normalizedQuery: String): List<MediaEntry> =
        if (normalizedQuery.isBlank()) emptyList()
        else externalEntries(Prefs.favorites() + Prefs.recents() + libraryQueue() + PodcastDownloads.downloadedEntries())
            .distinctBy { it.key }
            .filter {
                it.title.normName().contains(normalizedQuery) ||
                    it.group.normName().contains(normalizedQuery)
            }

    private fun externalEntries(entries: List<MediaEntry>): List<MediaEntry> =
        entries.filter { PlaybackQueueSnapshot.isSafeExternalUrl(it.url) }

    private fun libraryCategories(): List<MediaItem> = listOf(
        categoryItem(LIBRARY_FAVORITES_TV, "TV Favorileri"),
        categoryItem(LIBRARY_FAVORITES_RADIO, "Radyo Favorileri"),
        categoryItem(LIBRARY_FAVORITES_PODCAST, "Podcast Favorileri"),
        categoryItem(LIBRARY_RECENTS, "Son Dinlenenler"),
        categoryItem(LIBRARY_QUEUE, "Çalma Kuyruğu"),
        categoryItem(LIBRARY_NOW_PLAYING, "Şimdi Çalıyor"),
        categoryItem(LIBRARY_DOWNLOADS, "İndirilen Podcastler"),
    )

    private fun itemFor(e: MediaEntry) = MediaItem.Builder()
        .setMediaId(e.key)
        .setUri(e.url)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(e.title)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setArtist(when (e.kind) {
                    Kind.TV -> "OmniDrive TV"
                    Kind.RADIO -> "OmniDrive Radyo"
                    Kind.PODCAST -> e.group.ifBlank { "OmniDrive Podcast" }
                })
                .apply { e.image?.takeIf { PlaybackQueueSnapshot.isSafeExternalUrl(it) }?.let { setArtworkUri(android.net.Uri.parse(it)) } }
                .build()
        )
        .apply { if (e.url.contains(".m3u8", ignoreCase = true)) setMimeType(MimeTypes.APPLICATION_M3U8) }
        .build()

    private fun sourceFor(e: MediaEntry): androidx.media3.exoplayer.source.MediaSource {
        val mediaItem = itemFor(e)
        val playableItem = if (e.kind == Kind.PODCAST) {
            PodcastDownloads.localUri(e.url)?.let { mediaItem.buildUpon().setUri(it).build() } ?: mediaItem
        } else mediaItem
        return (if (e.kind == Kind.TV) tvSources else audioSources).createMediaSource(playableItem)
    }

    /** Aynı yayını baştan açar (yönlendirmeli adresler yeni sunucuya bağlanır). */
    private fun reload() {
        val p = exo ?: return
        // Yeniden bağlanma ile kullanıcı duraklatması aynı ana denk gelirse duraklatmayı koru.
        if (!p.playWhenReady) return
        val e = now.value ?: return
        if (e.kind == Kind.PODCAST) {
            Prefs.savePosition(e.url, p.currentPosition)
            p.setMediaSource(sourceFor(e), Prefs.position(e.url))
        } else {
            p.setMediaSource(sourceFor(e))
        }
        p.prepare()
        p.play()
    }

    /** Ağ kesintisinde gereksiz tekrar denemelerini durdurur; doğrulanmış bağlantı dönünce kaldığı yerden açar. */
    private fun registerNetworkMonitor(context: Context) {
        val manager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return
        connectivityManager = manager
        networkAvailable.value = manager.activeNetwork?.let { network ->
            manager.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } ?: false
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val wasOffline = !networkAvailable.value
                networkAvailable.value = true
                if (wasOffline) handler.postDelayed({
                    val p = exo ?: return@postDelayed
                    if (p.playWhenReady && !p.isPlaying && now.value != null) reload()
                }, 700)
            }

            override fun onLost(network: Network) {
                val active = manager.activeNetwork
                networkAvailable.value = active?.let {
                    manager.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                } ?: false
            }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }
            .onSuccess { networkCallback = callback }
    }

    private fun unregisterNetworkMonitor() {
        val manager = connectivityManager
        val callback = networkCallback
        if (manager != null && callback != null) runCatching { manager.unregisterNetworkCallback(callback) }
        networkCallback = null
        connectivityManager = null
        networkAvailable.value = false
    }

    private fun start() {
        cancelRecovery()
        dead.clear()
        playedOk = false
        now.value?.takeIf { it.kind == Kind.PODCAST }?.let { e -> exo?.let { Prefs.savePosition(e.url, it.currentPosition) } }
        val e = queue[index]
        now.value = e
        persistQueue()
        error.value = null
        Prefs.addRecent(e)
        if (e.kind == Kind.PODCAST) player.setMediaSource(sourceFor(e), Prefs.position(e.url)) else player.setMediaSource(sourceFor(e))
        position.value = 0
        duration.value = 0
        player.prepare()
        player.play()
    }
}

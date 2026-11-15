// OmniDrive — Media3 background playback service
package com.newrex.auto

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * TV, radyo ve podcast oynatıcısının sahibi.
 *
 * Media3 bu servisi oynatma sürerken foreground'a taşır ve medya bildirimi/oturum
 * yaşam döngüsünü yönetir. UI yalnızca MediaController bağlantısıyla servisi bağlı tutar.
 */
class OmniDriveMediaService : MediaSessionService() {

    override fun onCreate() {
        super.onCreate()
        com.newrex.auto.data.Prefs.init(applicationContext)
        PlayerHub.init(applicationContext)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        PlayerHub.mediaSession

    override fun onDestroy() {
        PlayerHub.release()
        super.onDestroy()
    }
}

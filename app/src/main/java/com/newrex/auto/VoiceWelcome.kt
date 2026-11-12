// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.newrex.auto.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

/**
 * Android Auto Bağlantısında Türkçe Sesli Karşılama Motoru
 *
 * Sürücü araca bindiğinde günün saatine göre hitap eder:
 * "Günaydın Murat, NewREX'e hoş geldin. İyi yolculuklar!"
 * Konuşurken radyo/müziği geçici olarak hafifçe kısar (Audio Focus Ducking).
 */
object VoiceWelcome {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    fun trigger(context: Context) {
        if (!Prefs.voiceWelcome) return

        // Günde tek seferlik kontrolü
        val today = LocalDate.now().toEpochDay()
        if (Prefs.voiceWelcomeOnceDaily && Prefs.lastWelcomeDay == today) return

        val appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        scope.launch {
            // Araç ekranı ve ses kanalı tamamen otursun diye kısa bir gecikme
            delay(2500)
            speakGreeting(appContext, today)
        }
    }

    private fun speakGreeting(context: Context, todayEpochDay: Long) {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "Günaydın"
            in 12..17 -> "Tünaydın"
            in 18..22 -> "İyi akşamlar"
            else -> "İyi geceler"
        }
        val name = Prefs.userName.ifBlank { "Kaptan" }
        val message = "$greeting $name, NewREX'e hoş geldin. İyi yolculuklar!"

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val tr = Locale("tr", "TR")
                    val result = engine.setLanguage(tr)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        isInitialized = true
                        requestDuckAudioFocus()

                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {}
                            override fun onDone(utteranceId: String?) {
                                abandonDuckAudioFocus()
                                Prefs.lastWelcomeDay = todayEpochDay
                            }
                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                abandonDuckAudioFocus()
                            }
                        })

                        engine.speak(message, TextToSpeech.QUEUE_FLUSH, null, "NEWREX_WELCOME")
                    }
                }
            }
        }
    }

    private fun requestDuckAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attr)
                .build()
            focusRequest?.let { am.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonDuckAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
    }
}


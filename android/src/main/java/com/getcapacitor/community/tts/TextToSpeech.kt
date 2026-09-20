package com.getcapacitor.community.tts

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech as AndroidTextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import java.util.Locale

public class TextToSpeech internal constructor(private val context: Context) : AndroidTextToSpeech.OnInitListener {
    // Stays null when the engine could not be created. Everything but isAvailable and onDestroy has always
    // answered that with a NullPointerException, which the plugin turns into a rejected call.
    private var tts: AndroidTextToSpeech? = null
    private var initializationStatus = 0
    private val requests: MutableMap<String?, SpeakResultCallback> = HashMap()

    init {
        try {
            tts =
                AndroidTextToSpeech(context, this).also {
                    it.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {}

                            override fun onDone(utteranceId: String?) {
                                val callback = requests[utteranceId]
                                if (callback != null) {
                                    callback.onDone()
                                    requests.remove(utteranceId)
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                val callback = requests[utteranceId]
                                if (callback != null) {
                                    callback.onError()
                                    requests.remove(utteranceId)
                                }
                            }

                            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                                requests[utteranceId]?.onRangeStart(start, end)
                            }
                        }
                    )
                }
        } catch (ex: Exception) {
            Log.d(LOG_TAG, ex.localizedMessage.orEmpty())
        }
    }

    override fun onInit(status: Int) {
        initializationStatus = status
    }

    @JvmOverloads
    public fun speak(
        text: String,
        lang: String,
        rate: Float,
        pitch: Float,
        volume: Float,
        voice: Int,
        callbackId: String?,
        resultCallback: SpeakResultCallback,
        queueStrategy: Int = AndroidTextToSpeech.QUEUE_FLUSH
    ) {
        if (queueStrategy != AndroidTextToSpeech.QUEUE_ADD) {
            stop()
        }
        requests[callbackId] = resultCallback

        val locale = Locale.forLanguageTag(lang)

        val ttsParams = Bundle()
        ttsParams.putSerializable(AndroidTextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, callbackId)
        ttsParams.putSerializable(AndroidTextToSpeech.Engine.KEY_PARAM_VOLUME, volume)

        val tts = tts!!
        tts.setLanguage(locale)
        tts.setSpeechRate(rate)
        tts.setPitch(pitch)

        if (voice >= 0) {
            val supportedVoices = getSupportedVoicesOrdered()
            if (voice < supportedVoices.size) {
                tts.setVoice(supportedVoices[voice])
            }
        }
        tts.speak(text, queueStrategy, ttsParams, callbackId)
    }

    public fun stop() {
        tts!!.stop()
        requests.clear()
    }

    public fun getSupportedLanguages(): JSArray? {
        val languages = tts!!.availableLanguages.map { it.toLanguageTag() }
        return JSArray.from(languages.toTypedArray())
    }

    /**
     * @return Ordered list of voices. The order is guaranteed to remain the same as long as the voices in tts.getVoices() do not change.
     */
    public fun getSupportedVoicesOrdered(): ArrayList<Voice> {
        val orderedVoices = ArrayList(tts!!.voices)

        // voice.getName() is guaranteed to be unique, so will be used for sorting.
        orderedVoices.sortWith { v1, v2 -> v1.name.compareTo(v2.name) }

        return orderedVoices
    }

    public fun getSupportedVoices(): JSArray? {
        val voices = getSupportedVoicesOrdered().map { convertVoiceToJSObject(it) }
        return JSArray.from(voices.toTypedArray())
    }

    public fun openInstall() {
        val packageManager = context.packageManager
        val installIntent = Intent()
        installIntent.setAction(AndroidTextToSpeech.Engine.ACTION_CHECK_TTS_DATA)

        val resolveInfo = packageManager.resolveActivity(installIntent, PackageManager.MATCH_DEFAULT_ONLY)

        if (resolveInfo != null) {
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(installIntent)
        }
    }

    public val isAvailable: Boolean
        get() = tts != null && initializationStatus == AndroidTextToSpeech.SUCCESS

    public fun isLanguageSupported(lang: String): Boolean {
        val locale = Locale.forLanguageTag(lang)
        val result = tts!!.isLanguageAvailable(locale)
        return result == AndroidTextToSpeech.LANG_AVAILABLE ||
            result == AndroidTextToSpeech.LANG_COUNTRY_AVAILABLE ||
            result == AndroidTextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    }

    public fun onDestroy() {
        val tts = tts ?: return
        tts.stop()
        tts.shutdown()
    }

    private fun convertVoiceToJSObject(voice: Voice): JSObject {
        val locale = voice.locale
        val obj = JSObject()
        obj.put("voiceURI", voice.name)
        obj.put("name", locale.displayLanguage + " " + locale.displayCountry)
        obj.put("lang", locale.toLanguageTag())
        obj.put("localService", !voice.isNetworkConnectionRequired)
        obj.put("default", false)
        return obj
    }

    public companion object {
        public const val LOG_TAG: String = "TextToSpeech"
    }
}

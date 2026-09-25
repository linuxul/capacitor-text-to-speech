package com.getcapacitor.community.tts

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginException
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "TextToSpeech")
public class TextToSpeechPlugin : Plugin() {
    private lateinit var implementation: TextToSpeech

    override fun load() {
        implementation = TextToSpeech(context)
    }

    @PluginMethod
    public fun speak(call: PluginCall) {
        if (!implementation.isAvailable) {
            throw PluginException("Not yet initialized or not available on this device.", "UNAVAILABLE")
        }

        val text = call.getString("text", "") ?: ""
        val lang = call.getString("lang", "en-US") ?: "en-US"
        val rate = call.getFloat("rate", 1.0f) ?: 1.0f
        val pitch = call.getFloat("pitch", 1.0f) ?: 1.0f
        val volume = call.getFloat("volume", 1.0f) ?: 1.0f
        val voice = call.getInt("voice", -1) ?: -1
        val queueStrategy = call.getInt("queueStrategy", 0) ?: 0

        if (!implementation.isLanguageSupported(lang)) {
            throw PluginException(ERROR_UNSUPPORTED_LANGUAGE)
        }

        val resultCallback =
            object : SpeakResultCallback {
                override fun onDone() {
                    call.resolve()
                }

                override fun onError() {
                    call.reject(ERROR_UTTERANCE)
                }

                override fun onRangeStart(start: Int, end: Int) {
                    val ret = JSObject()
                    ret.put("start", start)
                    ret.put("end", end)
                    ret.put("spokenWord", text.substring(start, end))
                    notifyListeners("onRangeStart", ret)
                }
            }

        try {
            implementation.speak(text, lang, rate, pitch, volume, voice, call.callbackId, resultCallback, queueStrategy)
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage)
        }
    }

    @PluginMethod
    public fun stop(call: PluginCall) {
        if (!implementation.isAvailable) {
            throw PluginException("Not yet initialized or not available on this device.", "UNAVAILABLE")
        }
        try {
            implementation.stop()
            call.resolve()
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage)
        }
    }

    @PluginMethod
    public fun getSupportedLanguages(call: PluginCall) {
        try {
            val ret = JSObject()
            ret.put("languages", implementation.getSupportedLanguages())
            call.resolve(ret)
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage)
        }
    }

    @PluginMethod
    public fun getSupportedVoices(call: PluginCall) {
        try {
            val ret = JSObject()
            ret.put("voices", implementation.getSupportedVoices())
            call.resolve(ret)
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage)
        }
    }

    @PluginMethod
    public fun isLanguageSupported(call: PluginCall) {
        val lang = call.getString("lang", "") ?: ""
        try {
            val ret = JSObject()
            ret.put("supported", implementation.isLanguageSupported(lang))
            call.resolve(ret)
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage)
        }
    }

    @PluginMethod
    public fun openInstall(call: PluginCall) {
        try {
            implementation.openInstall()
            call.resolve()
        } catch (ex: Exception) {
            call.reject(ex.localizedMessage)
        }
    }

    override fun handleOnDestroy() {
        implementation.onDestroy()
    }

    public companion object {
        public const val LOG_TAG: String = "TextToSpeechPlugin"

        public const val ERROR_UTTERANCE: String = "Failed to read text."
        public const val ERROR_UNSUPPORTED_LANGUAGE: String = "This language is not supported."
    }
}

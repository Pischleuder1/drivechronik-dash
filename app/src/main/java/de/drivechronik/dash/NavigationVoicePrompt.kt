package de.drivechronik.dash

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Handler
import android.os.Looper
import java.util.Locale

internal class NavigationVoicePrompt(
    context: Context
) {
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, () -> Unit>? = null
    private var callback: (() -> Unit)? = null
    private var closed = false

    init {
        engine = TextToSpeech(
            context.applicationContext
        ) { status ->
            main.post {
                if (closed) return@post
                if (status == TextToSpeech.SUCCESS) {
                    val tts = engine
                    ready = tts?.setLanguage(
                        Locale.GERMANY
                    )?.let { it >= 0 } == true

                    tts?.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(id: String?) {}

                            override fun onDone(id: String?) {
                                finish()
                            }

                            override fun onError(id: String?) {
                                finish()
                            }
                        }
                    )
                }

                pending?.let {
                    pending = null
                    speak(it.first, it.second)
                }
            }
        }
    }

    private fun finish() {
        main.post {
            val next = callback
            callback = null
            if (!closed) next?.invoke()
        }
    }

    fun speak(text: String, onFinished: () -> Unit) {
        if (closed) return

        if (engine == null) {
            pending = text to onFinished
            return
        }

        if (!ready) {
            onFinished()
            return
        }

        callback = onFinished
        val digits = listOf(
            "null", "eins", "zwei", "drei", "vier",
            "fünf", "sechs", "sieben", "acht", "neun"
        )

        // Fünfstellige PLZ einzeln aussprechen.
        val speechText = Regex(
            """(?<!\d)\d{5}(?!\d)"""
        ).replace(text) { match ->
            match.value.map { digit ->
                digits[digit - '0']
            }.joinToString(" ")
        }

        val result = engine?.speak(
            speechText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "drivechronik-navigation"
        )

        if (result != TextToSpeech.SUCCESS) finish()
    }

    fun close() {
        closed = true
        callback = null
        pending = null
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}

package de.drivechronik.dash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

internal class NavigationVoiceReply(
    private val context: Context
) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var callback: ((String?) -> Unit)? = null
    private var timeout: Runnable? = null

    fun listen(onAnswer: (String?) -> Unit) {
        cancel()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onAnswer(null)
            return
        }

        callback = onAnswer
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(results: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                finish(null)
            }

            override fun onResults(results: Bundle?) {
                val spoken = results
                    ?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )?.firstOrNull()

                finish(spoken)
            }
        })

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
        }

        val task = Runnable { finish(null) }
        timeout = task
        handler.postDelayed(task, 10_000L)

        recognizer?.startListening(intent)
    }

    private fun finish(answer: String?) {
        val next = callback ?: return
        callback = null
        release()
        next(answer)
    }

    private fun release() {
        timeout?.let(handler::removeCallbacks)
        timeout = null
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    fun cancel() {
        callback = null
        release()
    }
}

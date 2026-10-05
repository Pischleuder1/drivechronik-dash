package de.drivechronik.dash

import android.content.Context
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

internal class DriveChronikWakeWord(
    private val context: Context,
    private val onWake: () -> Unit,
    private val onStatus: (String) -> Unit
) {
    private var enabled = false
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var service: SpeechService? = null

    fun start() {
        if (enabled) return
        enabled = true
        onStatus("Sprachmodell wird geladen")

        StorageService.unpack(
            context.applicationContext,
            "drivechronik-voice",
            "drivechronik-voice",
            { loaded ->
                if (!enabled) {
                    loaded.close()
                } else {
                    try {
                        model = loaded
                        recognizer = Recognizer(
                            loaded,
                            16000.0f,
                            """["drive chronik","drivechronik",
                               "drei chronik","[unk]"]"""
                        )
                        service = SpeechService(
                            recognizer!!, 16000.0f
                        ).also {
                            it.startListening(listener)
                        }
                        onStatus("Warte auf DriveChronik")
                    } catch (e: Exception) {
                        stop()
                        onStatus("Mikrofonfehler: ${e.message}")
                    }
                }
            },
            { e ->
                if (enabled) {
                    stop()
                    onStatus("Modellfehler: ${e.message}")
                }
            }
        )
    }

    private val listener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {
            check(hypothesis, "partial")
        }

        override fun onResult(hypothesis: String?) {
            check(hypothesis, "text")
        }

        override fun onFinalResult(hypothesis: String?) {
            check(hypothesis, "text")
        }

        override fun onError(e: Exception?) {
            if (enabled) {
                stop()
                onStatus("Erkennungsfehler: ${e?.message}")
            }
        }

        override fun onTimeout() = Unit
    }

    private fun check(json: String?, field: String) {
        if (!enabled || json == null) return

        val words = try {
            JSONObject(json).optString(field).lowercase()
        } catch (_: Exception) {
            return
        }

        val recognized =
            Regex(
                """\b(?:drive\s*chronik|drei\s*chronik)\b"""
            ).containsMatchIn(words)

        if (recognized) {
            stop()
            onStatus("DriveChronik erkannt")
            onWake()
        }
    }

    fun stop() {
        enabled = false
        service?.cancel()
        service?.shutdown()
        service = null
        recognizer?.close()
        recognizer = null
        model?.close()
        model = null
    }
}

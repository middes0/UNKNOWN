package com.middes.launcher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import java.util.Locale

enum class NexaState { OFF, READY, LISTENING, PROCESSING, EXECUTING, SPEAKING }

class NexaController(
    private val context: Context,
    private val enabledProvider: () -> Boolean,
    private val setEnabled: (Boolean) -> Unit,
    private val onNeedPermission: () -> Unit,
    private val onCommand: (String) -> Unit,
    private val onStateChanged: (NexaState) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var active = false
    private var listening = false
    private var speaking = false
    private var bargeIn = false

    private val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1100L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1100L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
    }

    init {
        if (available()) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
                it.setRecognitionListener(createRecognitionListener())
            }
        }

        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale("pt", "BR")
                tts?.setSpeechRate(1.04f)
                tts?.setOnUtteranceProgressListener(createUtteranceListener())
            }
        }
    }

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun microphoneGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun toggle() {
        if (!available()) {
            onStateChanged(NexaState.OFF)
            return
        }
        if (!microphoneGranted()) {
            onNeedPermission()
            return
        }
        if (enabledProvider()) disable() else enableAfterPermission()
    }

    fun enableAfterPermission() {
        if (!available()) return
        if (!microphoneGranted()) {
            onNeedPermission()
            return
        }
        setEnabled(true)
        speak("Estou ouvindo.")
        if (active) startListening(220L)
    }

    fun disable() {
        setEnabled(false)
        stopListening()
        try { tts?.stop() } catch (_: Exception) {}
        speaking = false
        bargeIn = false
        onStateChanged(NexaState.OFF)
    }

    fun onResume() {
        active = true
        if (enabledProvider() && microphoneGranted()) startListening(300L)
        else if (!enabledProvider()) onStateChanged(NexaState.OFF)
    }

    fun onPause() {
        active = false
        stopListening()
    }

    fun showProcessing() {
        if (active && enabledProvider()) onStateChanged(NexaState.PROCESSING)
    }

    fun showExecuting() {
        if (active && enabledProvider()) onStateChanged(NexaState.EXECUTING)
    }

    fun speak(message: String) {
        if (!active || !ttsReady || message.isBlank()) return
        try {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "nexa-response")
            onStateChanged(NexaState.SPEAKING)
        } catch (_: Exception) {}
    }

    fun destroy() {
        active = false
        stopListening()
        handler.removeCallbacksAndMessages(null)
        try { tts?.stop() } catch (_: Exception) {}
        try { tts?.shutdown() } catch (_: Exception) {}
        recognizer = null
        tts = null
    }

    private fun startListening(delayMs: Long) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (!active || !enabledProvider() || !microphoneGranted()) return@postDelayed
            if (speaking && !bargeIn) return@postDelayed
            if (listening) return@postDelayed
            try {
                recognizer?.cancel()
                recognizer?.startListening(speechIntent)
            } catch (_: Exception) {
                restart(800L)
            }
        }, delayMs)
    }

    private fun stopListening() {
        listening = false
        bargeIn = false
        handler.removeCallbacksAndMessages(null)
        try { recognizer?.cancel() } catch (_: Exception) {}
    }

    private fun restart(delayMs: Long) {
        handler.removeCallbacksAndMessages(null)
        if (active && enabledProvider()) startListening(delayMs)
    }

    private fun createUtteranceListener() = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            speaking = true
            bargeIn = false
            onStateChanged(NexaState.SPEAKING)
            handler.postDelayed({
                if (active && enabledProvider() && speaking) {
                    bargeIn = true
                    startListening(0L)
                }
            }, 260L)
        }

        override fun onDone(utteranceId: String?) {
            speaking = false
            bargeIn = false
            onStateChanged(if (enabledProvider() && active) NexaState.READY else NexaState.OFF)
            restart(150L)
        }

        override fun onError(utteranceId: String?) {
            speaking = false
            bargeIn = false
            onStateChanged(if (enabledProvider() && active) NexaState.READY else NexaState.OFF)
            restart(180L)
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            listening = true
            onStateChanged(NexaState.LISTENING)
        }

        override fun onBeginningOfSpeech() {
            listening = true
        }

        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() { listening = false }

        override fun onError(error: Int) {
            listening = false
            onStateChanged(if (enabledProvider() && active) NexaState.READY else NexaState.OFF)
            if (active && enabledProvider()) restart(if (speaking && bargeIn) 120L else 360L)
        }

        override fun onResults(results: Bundle?) {
            listening = false
            if (!active) return
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            val phrase = matches.firstOrNull { hasWakeWord(it) }
                ?: if (bargeIn) "" else matches.firstOrNull().orEmpty()

            if (phrase.isBlank()) {
                restart(120L)
                return
            }

            if (bargeIn && !hasWakeWord(phrase)) {
                restart(150L)
                return
            }

            if (bargeIn) {
                bargeIn = false
                speaking = false
                try { tts?.stop() } catch (_: Exception) {}
            }

            onCommand(phrase)
            restart(120L)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!active || !bargeIn) return
            val phrase = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (phrase.isNotBlank() && hasWakeWord(phrase)) {
                try { recognizer?.cancel() } catch (_: Exception) {}
                bargeIn = false
                speaking = false
                try { tts?.stop() } catch (_: Exception) {}
                onCommand(phrase)
                restart(120L)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun hasWakeWord(value: String): Boolean {
        val normalized = normalize(value)
        return normalized.contains("nexa") || normalized.contains("nessa")
    }

    private fun normalize(value: String): String =
        java.text.Normalizer.normalize(value.lowercase(Locale("pt", "BR")), java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
package com.jarvis.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    private var onResult: ((String) -> Unit)? = null
    private var onError: ((Int) -> Unit)? = null
    private var onReady: (() -> Unit)? = null

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = onReady?.invoke() ?: Unit
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onError(error: Int) {
            onError?.invoke(error)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            onResult?.invoke(text)
        }
    }

    fun initialize(): Boolean {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return false
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(listener)
        return true
    }

    fun startListening(
        onReady: () -> Unit,
        onResult: (String) -> Unit,
        onError: (Int) -> Unit
    ) {
        this.onReady = onReady
        this.onResult = onResult
        this.onError = onError

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}

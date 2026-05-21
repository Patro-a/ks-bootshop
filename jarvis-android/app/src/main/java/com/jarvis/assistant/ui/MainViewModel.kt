package com.jarvis.assistant.ui

import android.app.Application
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.ai.AIService
import com.jarvis.assistant.commands.CommandHandler
import com.jarvis.assistant.model.ChatMessage
import com.jarvis.assistant.tts.ElevenLabsService
import com.jarvis.assistant.voice.VoiceManager
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    enum class State { IDLE, LISTENING, THINKING, SPEAKING }

    private val _state    = MutableLiveData(State.IDLE)
    val state: LiveData<State> = _state

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val voiceManager    = VoiceManager(app)
    private val aiService       = AIService()
    private val ttsService      = ElevenLabsService()
    private val commandHandler  = CommandHandler(app)

    private val cacheDir = app.cacheDir

    init {
        val available = voiceManager.initialize()
        if (!available) _error.value = "Speech recognition is not available on this device."
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun startListening() {
        if (_state.value == State.SPEAKING) ttsService.stopPlaying()
        _state.value = State.LISTENING

        voiceManager.startListening(
            onReady  = {},
            onResult = { text -> onSpeechResult(text) },
            onError  = { code -> onSpeechError(code) }
        )
    }

    fun stopListening() {
        voiceManager.stopListening()
        if (_state.value == State.LISTENING) _state.value = State.IDLE
    }

    fun stopSpeaking() {
        ttsService.stopPlaying()
        _state.value = State.IDLE
    }

    fun setVoice(voiceId: String) {
        ttsService.selectedVoiceId = voiceId
    }

    fun clearConversation() {
        aiService.clearHistory()
        _messages.value = emptyList()
    }

    // ── Internal flow ─────────────────────────────────────────────────────────

    private fun onSpeechResult(raw: String) {
        // Strip wake word if user said it while already in the app
        val text = raw.replace("hey jarvis", "", ignoreCase = true).trim().ifEmpty { raw }

        addMessage(ChatMessage("user", raw))

        // Fast-path: handle known device commands without calling the AI
        val cmd = commandHandler.handle(text)
        if (cmd.handled) {
            if (cmd.feedback.isNotEmpty()) {
                addMessage(ChatMessage("assistant", cmd.feedback))
                speak(cmd.feedback)
            } else {
                _state.value = State.IDLE
            }
            return
        }

        // General query — ask the AI
        fetchAIResponse(text)
    }

    private fun onSpeechError(code: Int) {
        val msg = when (code) {
            SpeechRecognizer.ERROR_NO_MATCH          -> "I did not catch that. Please try again."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT   -> "Network error. Check your connection."
            SpeechRecognizer.ERROR_AUDIO             -> "Microphone error. Check audio permissions."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
            else                                     -> "Speech recognition failed. Please try again."
        }
        _state.value = State.IDLE
        _error.value = msg
    }

    private fun fetchAIResponse(input: String) {
        _state.value = State.THINKING
        viewModelScope.launch {
            aiService.getResponse(input).fold(
                onSuccess = { reply ->
                    addMessage(ChatMessage("assistant", reply))
                    speak(reply)
                },
                onFailure = { ex ->
                    _state.value = State.IDLE
                    _error.value = "AI error: ${ex.message}"
                }
            )
        }
    }

    private fun speak(text: String) {
        _state.value = State.SPEAKING
        viewModelScope.launch {
            val result = ttsService.speak(text, cacheDir)
            _state.value = State.IDLE
            result.onFailure { _error.value = "Voice error: ${it.message}" }
        }
    }

    private fun addMessage(msg: ChatMessage) {
        val updated = (_messages.value ?: emptyList()) + msg
        _messages.value = updated
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
        ttsService.destroy()
    }
}

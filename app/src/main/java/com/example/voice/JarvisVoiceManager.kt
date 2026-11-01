package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "JarvisVoiceManager"

class JarvisVoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _rmsRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _rmsRms.asStateFlow()

    private val _isVoiceOutputEnabled = MutableStateFlow(true)
    val isVoiceOutputEnabled: StateFlow<Boolean> = _isVoiceOutputEnabled.asStateFlow()

    var onSpeechRecognized: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            textToSpeech?.let { tts ->
                // Prefer UK English for canonical Jarvis voice persona, fall back to US/default
                val result = tts.setLanguage(Locale.UK)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US)
                }
                tts.setPitch(0.92f) // slightly deeper, composed tone
                tts.setSpeechRate(1.02f)

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
            Log.d(TAG, "TTS initialized successfully.")
        } else {
            Log.w(TAG, "TTS init returned status $status")
        }
    }

    fun toggleVoiceOutput() {
        val newState = !_isVoiceOutputEnabled.value
        _isVoiceOutputEnabled.value = newState
        if (!newState) {
            stopSpeaking()
        }
    }

    fun startListening() {
        stopSpeaking()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError?.invoke("Speech recognition is not available on this device.")
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            _isListening.value = true
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Started speech listening.")
        } catch (e: Exception) {
            _isListening.value = false
            Log.e(TAG, "Failed to start listening", e)
            onError?.invoke("Could not activate voice input: ${e.message}")
        }
    }

    fun stopListening() {
        _isListening.value = false
        _rmsRms.value = 0f
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognizer", e)
        }
    }

    fun speak(text: String) {
        if (!_isVoiceOutputEnabled.value || !isTtsInitialized || text.isBlank()) return

        // Clean out excessive formatting characters before speech
        val cleanedText = text
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace("JARVIS:", "")
            .trim()

        try {
            _isSpeaking.value = true
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "JARVIS_RESPONSE_${System.currentTimeMillis()}")
            }
            textToSpeech?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, params, "JARVIS_RESPONSE")
        } catch (e: Exception) {
            _isSpeaking.value = false
            Log.e(TAG, "TTS speak failed", e)
        }
    }

    fun stopSpeaking() {
        _isSpeaking.value = false
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "TTS stop failed", e)
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Normalize roughly between 0f and 1f for the visualizer
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _rmsRms.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
                _rmsRms.value = 0f
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _rmsRms.value = 0f
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking closer to the mic."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    else -> "Voice recognition error ($error)"
                }
                Log.w(TAG, "Speech recognition error: $errorMessage")
                // Only alert user if it's not a silent timeout or no match
                if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    onError?.invoke(errorMessage)
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _rmsRms.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognized = matches[0]
                    Log.d(TAG, "Recognized speech: $recognized")
                    onSpeechRecognized?.invoke(recognized)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer", e)
        }
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}

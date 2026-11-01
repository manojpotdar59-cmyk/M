package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VoiceSpeechManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onPartialSpeech: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit,
    private val onSpeechError: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false
    private var isListening = false

    init {
        initTts()
        mainHandler.post { initSpeechRecognizer() }
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                val result = textToSpeech?.setLanguage(Locale.UK)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.setLanguage(Locale.US)
                }
                textToSpeech?.setPitch(0.95f)
                textToSpeech?.setSpeechRate(1.02f)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        mainHandler.post { onSpeakingStateChanged(true) }
                    }

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post { onSpeakingStateChanged(false) }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post { onSpeakingStateChanged(false) }
                    }
                })
            } else {
                Log.e("VoiceSpeechManager", "TTS initialization failed: $status")
            }
        }
    }

    private fun initSpeechRecognizer() {
        try {
            if (speechRecognizer != null) {
                speechRecognizer?.destroy()
                speechRecognizer = null
            }

            val isAvailable = SpeechRecognizer.isRecognitionAvailable(context)
            val isOnDeviceAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            } else false

            if (!isAvailable && !isOnDeviceAvailable) {
                Log.w("VoiceSpeechManager", "Speech recognition is not available on this device.")
                onSpeechError("Speech recognition engine unavailable on this device.")
                return
            }

            speechRecognizer = if (isOnDeviceAvailable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    mainHandler.post { onListeningStateChanged(true) }
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                    mainHandler.post { onListeningStateChanged(true) }
                }

                override fun onRmsChanged(rmsdB: Float) {
                    mainHandler.post { onRmsChanged(rmsdB) }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    mainHandler.post { onListeningStateChanged(false) }
                }

                override fun onError(error: Int) {
                    isListening = false
                    mainHandler.post {
                        onListeningStateChanged(false)
                        val errorMessage = getSpeechErrorMessage(error)
                        Log.d("VoiceSpeechManager", "Speech recognition error: $error -> $errorMessage")
                        onSpeechError(errorMessage)
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    mainHandler.post {
                        onListeningStateChanged(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            onSpeechResult(recognized)
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val partial = matches[0]
                        mainHandler.post { onPartialSpeech(partial) }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } catch (e: Exception) {
            Log.e("VoiceSpeechManager", "Failed to initialize SpeechRecognizer", e)
            onSpeechError("Speech engine initialization failed: ${e.localizedMessage}")
        }
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && SpeechRecognizer.isOnDeviceRecognitionAvailable(context))
    }

    fun startListening() {
        mainHandler.post {
            stopSpeaking()
            if (speechRecognizer == null) {
                initSpeechRecognizer()
            }

            val recognizer = speechRecognizer
            if (recognizer == null) {
                onSpeechError("Speech recognition engine unavailable on this device.")
                onListeningStateChanged(false)
                return@post
            }

            try {
                // Cancel any lingering recognition session to avoid ERROR_RECOGNIZER_BUSY
                recognizer.cancel()

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                }

                recognizer.startListening(intent)
                isListening = true
                onListeningStateChanged(true)
            } catch (e: Exception) {
                Log.e("VoiceSpeechManager", "Error calling startListening", e)
                isListening = false
                onListeningStateChanged(false)
                onSpeechError("Failed to engage audio sensors: ${e.localizedMessage}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            isListening = false
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e("VoiceSpeechManager", "Error stopping listening", e)
            }
            onListeningStateChanged(false)
        }
    }

    fun cancelListening() {
        mainHandler.post {
            isListening = false
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e("VoiceSpeechManager", "Error cancelling listening", e)
            }
            onListeningStateChanged(false)
        }
    }

    fun speak(text: String, pitch: Float = 0.95f, rate: Float = 1.02f) {
        if (!isTtsReady || textToSpeech == null) return
        stopListening()
        val cleaned = text
            .replace(Regex("[*#_`~>|]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isBlank()) return

        textToSpeech?.setPitch(pitch)
        textToSpeech?.setSpeechRate(rate)
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "jarvis_speech_${System.currentTimeMillis()}")
        }
        textToSpeech?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, params, "jarvis_speech")
    }

    fun stopSpeaking() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
            onSpeakingStateChanged(false)
        }
    }

    fun setVoiceParams(pitch: Float, rate: Float) {
        textToSpeech?.setPitch(pitch)
        textToSpeech?.setSpeechRate(rate)
    }

    private fun getSpeechErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording glitch detected."
            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            SpeechRecognizer.ERROR_NETWORK -> "Network link required for cloud speech."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech telemetry timed out."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly, sir."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Sensors busy. Recalibrating..."
            SpeechRecognizer.ERROR_SERVER -> "Remote recognition server error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No vocal directive detected."
            else -> "Speech recognition error (Code $errorCode)"
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e("VoiceSpeechManager", "Error destroying speech recognizer", e)
            }
            try {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
                textToSpeech = null
            } catch (e: Exception) {
                Log.e("VoiceSpeechManager", "Error shutting down TTS", e)
            }
        }
    }
}

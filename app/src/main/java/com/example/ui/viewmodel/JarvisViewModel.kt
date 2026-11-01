package com.example.ui.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.JarvisAiService
import com.example.data.local.JarvisPreferences
import com.example.data.model.ChatMessage
import com.example.data.model.DirectiveCategory
import com.example.data.model.JarvisDirective
import com.example.data.model.JarvisTab
import com.example.data.model.JarvisVoiceState
import com.example.data.model.MessageRole
import com.example.data.model.QuickCommand
import com.example.data.model.SystemDiagnostic
import com.example.voice.VoiceSpeechManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "JarvisViewModel"

class JarvisViewModel(
    private val context: Context,
    private val preferences: JarvisPreferences = JarvisPreferences(context),
    private val aiService: JarvisAiService = JarvisAiService()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _voiceState = MutableStateFlow(JarvisVoiceState.IDLE)
    val voiceState: StateFlow<JarvisVoiceState> = _voiceState.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _diagnostics = MutableStateFlow(SystemDiagnostic())
    val diagnostics: StateFlow<SystemDiagnostic> = _diagnostics.asStateFlow()

    private val _directives = MutableStateFlow<List<JarvisDirective>>(emptyList())
    val directives: StateFlow<List<JarvisDirective>> = _directives.asStateFlow()

    private val _currentTab = MutableStateFlow(JarvisTab.ARC_CORE)
    val currentTab: StateFlow<JarvisTab> = _currentTab.asStateFlow()

    private val _callsign = MutableStateFlow(preferences.callsign)
    val callsign: StateFlow<String> = _callsign.asStateFlow()

    private val _autoSpeak = MutableStateFlow(preferences.autoSpeak)
    val autoSpeak: StateFlow<Boolean> = _autoSpeak.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _voicePitch = MutableStateFlow(preferences.voicePitch)
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _voiceRate = MutableStateFlow(preferences.voiceRate)
    val voiceRate: StateFlow<Float> = _voiceRate.asStateFlow()

    private val _partialSpeechInput = MutableStateFlow("")
    val partialSpeechInput: StateFlow<String> = _partialSpeechInput.asStateFlow()

    private val _speechStatusMessage = MutableStateFlow<String?>(null)
    val speechStatusMessage: StateFlow<String?> = _speechStatusMessage.asStateFlow()

    val quickCommands = listOf(
        QuickCommand("diag", "Diagnostic Scan", "Run a complete diagnostic scan on all suit systems and device telemetry.", "diag"),
        QuickCommand("clean", "Protocol Clean Slate", "Execute Protocol Clean Slate. Reset system buffers and calibrate active sensors.", "clean"),
        QuickCommand("tactical", "Tactical Briefing", "Provide a tactical situational briefing of our active directives and priorities.", "tactical"),
        QuickCommand("power", "Arc Core Power Grid", "Analyze the current Arc Reactor output and clean energy distribution.", "power"),
        QuickCommand("mark85", "Suit Armor Specs", "Detail the technical specifications and defensive capabilities of the Mark 85 armor.", "mark85")
    )

    private var voiceManager: VoiceSpeechManager? = null
    private var streamingJob: Job? = null
    private var telemetryJob: Job? = null

    init {
        _directives.value = preferences.getDirectives()
        initVoiceManager()
        startTelemetryLoop()
        postJarvisGreeting()
    }

    private fun initVoiceManager() {
        voiceManager = VoiceSpeechManager(
            context = context,
            onSpeechResult = { recognizedText ->
                _partialSpeechInput.value = ""
                _voiceState.value = JarvisVoiceState.IDLE
                _speechStatusMessage.value = null
                if (recognizedText.isNotBlank()) {
                    sendDirective(recognizedText)
                }
            },
            onPartialSpeech = { partial ->
                _partialSpeechInput.value = partial
                _speechStatusMessage.value = "Recognizing: \"$partial\""
            },
            onRmsChanged = { rms ->
                _audioRms.value = rms
            },
            onListeningStateChanged = { listening ->
                if (listening) {
                    _voiceState.value = JarvisVoiceState.LISTENING
                    _speechStatusMessage.value = "Listening for directive..."
                } else if (_voiceState.value == JarvisVoiceState.LISTENING) {
                    _voiceState.value = JarvisVoiceState.IDLE
                }
            },
            onSpeakingStateChanged = { speaking ->
                if (speaking) {
                    _voiceState.value = JarvisVoiceState.SPEAKING
                } else if (_voiceState.value == JarvisVoiceState.SPEAKING) {
                    _voiceState.value = JarvisVoiceState.IDLE
                }
            },
            onSpeechError = { errorMessage ->
                _speechStatusMessage.value = errorMessage
                _voiceState.value = JarvisVoiceState.IDLE
                _partialSpeechInput.value = ""
            }
        )
    }

    private fun postJarvisGreeting() {
        val greeting = ChatMessage(
            role = MessageRole.JARVIS,
            content = "Online and at your service, ${_callsign.value}. All systems are operating within optimal parameters. How may I assist you today?"
        )
        _messages.value = listOf(greeting)
        if (_autoSpeak.value && !_isMuted.value) {
            voiceManager?.speak(greeting.content, _voicePitch.value, _voiceRate.value)
        }
    }

    private fun startTelemetryLoop() {
        telemetryJob = viewModelScope.launch {
            while (isActive) {
                updateHardwareDiagnostics()
                delay(3000L)
            }
        }
    }

    private fun updateHardwareDiagnostics() {
        try {
            // Read Battery
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, batteryFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 85
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 85

            // Read Memory
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val freeMb = memInfo.availMem / (1024 * 1024)
            val totalMb = memInfo.totalMem / (1024 * 1024)

            // Uptime
            val uptimeMillis = SystemClock.elapsedRealtime()
            val hours = (uptimeMillis / (1000 * 60 * 60)) % 24
            val minutes = (uptimeMillis / (1000 * 60)) % 60
            val seconds = (uptimeMillis / 1000) % 60
            val uptimeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

            // Dynamic Arc Reactor Telemetry Fluctuations
            val baseTemp = 98.2f + (System.currentTimeMillis() % 17) * 0.05f
            val basePower = 3.40f + (System.currentTimeMillis() % 13) * 0.02f

            _diagnostics.value = SystemDiagnostic(
                batteryPct = batteryPct,
                isCharging = isCharging,
                memoryFreeMb = freeMb,
                memoryTotalMb = totalMb,
                uptimeFormatted = uptimeStr,
                networkStatus = "Online (5G / StarkNet)",
                arcPowerGw = basePower,
                arcTempCelsius = baseTemp,
                coreIntegrityPct = 99
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading telemetry", e)
        }
    }

    fun selectTab(tab: JarvisTab) {
        _currentTab.value = tab
    }

    fun toggleVoiceListening() {
        if (_voiceState.value == JarvisVoiceState.LISTENING) {
            voiceManager?.stopListening()
            _voiceState.value = JarvisVoiceState.IDLE
            _speechStatusMessage.value = "Listening paused."
        } else {
            if (voiceManager?.isSpeechRecognitionAvailable() == false) {
                _speechStatusMessage.value = "Speech recognition is not available on this device."
                return
            }
            voiceManager?.stopSpeaking()
            _speechStatusMessage.value = "Listening for directive..."
            voiceManager?.startListening()
        }
    }

    fun onVoiceAction(hasPermission: Boolean, onRequestPermission: () -> Unit) {
        if (!hasPermission) {
            _speechStatusMessage.value = "Microphone access requested for Jarvis voice input."
            onRequestPermission()
            return
        }
        toggleVoiceListening()
    }

    fun clearSpeechStatus() {
        _speechStatusMessage.value = null
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        if (newMute) {
            voiceManager?.stopSpeaking()
            _voiceState.value = JarvisVoiceState.IDLE
        }
    }

    fun sendDirective(userPrompt: String) {
        if (userPrompt.isBlank()) return
        val trimmed = userPrompt.trim()

        // Append user message
        val userMsg = ChatMessage(role = MessageRole.USER, content = trimmed)
        val assistantPlaceholderId = UUID.randomUUID().toString()
        val assistantMsg = ChatMessage(
            id = assistantPlaceholderId,
            role = MessageRole.JARVIS,
            content = "Processing directive...",
            isStreaming = true
        )

        _messages.update { it + userMsg + assistantMsg }
        _voiceState.value = JarvisVoiceState.PROCESSING

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                aiService.streamJarvisResponse(trimmed, _callsign.value).collect { chunk ->
                    accumulated.append(chunk)
                    _messages.update { currentList ->
                        currentList.map { msg ->
                            if (msg.id == assistantPlaceholderId) {
                                msg.copy(content = accumulated.toString(), isStreaming = true)
                            } else msg
                        }
                    }
                }

                val finalOutput = accumulated.toString().ifBlank {
                    "Directive registered, ${_callsign.value}. Operating within normal parameters."
                }

                _messages.update { currentList ->
                    currentList.map { msg ->
                        if (msg.id == assistantPlaceholderId) {
                            msg.copy(content = finalOutput, isStreaming = false)
                        } else msg
                    }
                }

                _voiceState.value = JarvisVoiceState.IDLE
                if (_autoSpeak.value && !_isMuted.value) {
                    voiceManager?.speak(finalOutput, _voicePitch.value, _voiceRate.value)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error in streaming response", e)
                val fallbackError = "My apologies, ${_callsign.value}. An unexpected telemetry glitch occurred: ${e.localizedMessage ?: "Unknown error"}."
                _messages.update { currentList ->
                    currentList.map { msg ->
                        if (msg.id == assistantPlaceholderId) {
                            msg.copy(content = fallbackError, isStreaming = false)
                        } else msg
                    }
                }
                _voiceState.value = JarvisVoiceState.IDLE
            }
        }
    }

    fun addDirective(title: String, detail: String, category: DirectiveCategory, priority: String) {
        if (title.isBlank()) return
        val newDirective = JarvisDirective(
            title = title.trim(),
            detail = detail.trim(),
            category = category,
            priority = priority
        )
        val updated = listOf(newDirective) + _directives.value
        _directives.value = updated
        preferences.saveDirectives(updated)

        // Jarvis voice acknowledgement
        if (_autoSpeak.value && !_isMuted.value) {
            voiceManager?.speak("Directive $title recorded to mission memory, ${_callsign.value}.")
        }
    }

    fun toggleDirectiveCompleted(id: String) {
        val updated = _directives.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
        _directives.value = updated
        preferences.saveDirectives(updated)
    }

    fun deleteDirective(id: String) {
        val updated = _directives.value.filter { it.id != id }
        _directives.value = updated
        preferences.saveDirectives(updated)
    }

    fun setCallsign(newCallsign: String) {
        val cleaned = newCallsign.trim().ifEmpty { "Sir" }
        _callsign.value = cleaned
        preferences.callsign = cleaned
    }

    fun setAutoSpeak(enabled: Boolean) {
        _autoSpeak.value = enabled
        preferences.autoSpeak = enabled
        if (!enabled) {
            voiceManager?.stopSpeaking()
        }
    }

    fun setVoicePitch(pitch: Float) {
        _voicePitch.value = pitch
        preferences.voicePitch = pitch
        voiceManager?.setVoiceParams(pitch, _voiceRate.value)
    }

    fun setVoiceRate(rate: Float) {
        _voiceRate.value = rate
        preferences.voiceRate = rate
        voiceManager?.setVoiceParams(_voicePitch.value, rate)
    }

    fun testJarvisVoice() {
        voiceManager?.speak(
            "Speech synthesis operational, ${_callsign.value}. Vocal parameters calibrated to ${String.format("%.2f", _voicePitch.value)} pitch and ${String.format("%.2f", _voiceRate.value)} rate.",
            _voicePitch.value,
            _voiceRate.value
        )
    }

    fun clearChatSlate() {
        voiceManager?.stopSpeaking()
        streamingJob?.cancel()
        _messages.value = emptyList()
        postJarvisGreeting()
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
        telemetryJob?.cancel()
        voiceManager?.destroy()
    }
}

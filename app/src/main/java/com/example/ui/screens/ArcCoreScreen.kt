package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.JarvisVoiceState
import com.example.data.model.MessageRole
import com.example.ui.components.AcousticWaveIndicator
import com.example.ui.components.ArcReactorView
import com.example.ui.components.HudCard
import com.example.ui.components.HudStatusBadge
import com.example.ui.components.HudWaveformVisualizer
import com.example.ui.components.MicPulseHalo
import com.example.ui.components.QuickCommandChip
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.StarkAmber
import com.example.ui.theme.StarkGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun ArcCoreScreen(
    viewModel: JarvisViewModel,
    messages: List<ChatMessage>,
    voiceState: JarvisVoiceState,
    audioRms: Float,
    callsign: String,
    isMuted: Boolean,
    partialSpeech: String,
    speechStatusMessage: String?,
    onVoiceClick: () -> Unit,
    onDismissSpeechStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll when messages change or streaming updates
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 14.dp)
    ) {
        // Top HUD Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "J.A.R.V.I.S. MK-85",
                    color = ArcCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "DIRECTIVE LINK: $callsign",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HudStatusBadge(
                    text = when (voiceState) {
                        JarvisVoiceState.LISTENING -> "EARS OPEN"
                        JarvisVoiceState.PROCESSING -> "ANALYZING"
                        JarvisVoiceState.SPEAKING -> "TRANSMITTING"
                        JarvisVoiceState.IDLE -> "SYSTEM READY"
                    },
                    color = when (voiceState) {
                        JarvisVoiceState.LISTENING -> ArcCyan
                        JarvisVoiceState.PROCESSING -> StarkGold
                        JarvisVoiceState.SPEAKING -> ArcBlue
                        JarvisVoiceState.IDLE -> ArcCyan
                    }
                )

                IconButton(
                    onClick = { viewModel.toggleMute() },
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .testTag("mute_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Toggle Mute",
                        tint = if (isMuted) AlertRed else ArcCyan
                    )
                }
            }
        }

        // Central Arc Reactor Core Visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArcReactorView(
                    state = voiceState,
                    audioRms = audioRms,
                    sizeDp = 175.dp,
                    onClick = onVoiceClick,
                    modifier = Modifier.testTag("arc_reactor_touch_target")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Voice status label & Waveform
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    HudWaveformVisualizer(
                        isActive = voiceState == JarvisVoiceState.LISTENING || voiceState == JarvisVoiceState.SPEAKING,
                        audioRms = audioRms
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (voiceState) {
                            JarvisVoiceState.LISTENING -> "LISTENING FOR DIRECTIVE..."
                            JarvisVoiceState.PROCESSING -> "PROCESSING QUERY..."
                            JarvisVoiceState.SPEAKING -> "J.A.R.V.I.S. VOCAL ACTIVE"
                            JarvisVoiceState.IDLE -> "TAP REACTOR OR MIC TO SPEAK"
                        },
                        color = when (voiceState) {
                            JarvisVoiceState.LISTENING -> ArcCyan
                            JarvisVoiceState.PROCESSING -> StarkGold
                            JarvisVoiceState.SPEAKING -> ArcBlue
                            JarvisVoiceState.IDLE -> TextMuted
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                // Real-time Voice Recognition Banner (when listening or status active)
                AnimatedVisibility(
                    visible = voiceState == JarvisVoiceState.LISTENING || speechStatusMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SpeechRecognitionHudCard(
                        isListening = voiceState == JarvisVoiceState.LISTENING,
                        partialSpeech = partialSpeech,
                        statusMessage = speechStatusMessage,
                        callsign = callsign,
                        audioRms = audioRms,
                        onCancelListening = { viewModel.toggleVoiceListening() },
                        onDismiss = onDismissSpeechStatus
                    )
                }
            }
        }

        // Quick Command Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(viewModel.quickCommands) { cmd ->
                QuickCommandChip(
                    label = cmd.label,
                    prompt = cmd.prompt,
                    tag = cmd.tag,
                    onClick = { prompt -> viewModel.sendDirective(prompt) }
                )
            }
        }

        // Message Feed HUD
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(message = msg, callsign = callsign)
            }
        }

        // Bottom Directive Command Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = "Speak or enter command for Jarvis...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("directive_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ArcCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CyberSurface.copy(alpha = 0.9f),
                    unfocusedContainerColor = CyberSurface.copy(alpha = 0.7f)
                ),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendDirective(textInput)
                            textInput = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Mic button (triggers permission check and SpeechRecognizer) with pulsing acoustic halo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(54.dp)
            ) {
                MicPulseHalo(
                    isListening = voiceState == JarvisVoiceState.LISTENING,
                    audioRms = audioRms,
                    haloSize = 54.dp
                )

                Surface(
                    onClick = onVoiceClick,
                    shape = CircleShape,
                    color = if (voiceState == JarvisVoiceState.LISTENING) ArcCyan else CyberSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (voiceState == JarvisVoiceState.LISTENING) ArcCyan else CyberBorder
                    ),
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("voice_mic_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (voiceState == JarvisVoiceState.LISTENING) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Microphone",
                            tint = if (voiceState == JarvisVoiceState.LISTENING) CyberDark else ArcCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send button
            Surface(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendDirective(textInput)
                        textInput = ""
                    }
                },
                shape = CircleShape,
                color = ArcCyan,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_directive_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Directive",
                        tint = CyberDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpeechRecognitionHudCard(
    isListening: Boolean,
    partialSpeech: String,
    statusMessage: String?,
    callsign: String,
    audioRms: Float,
    onCancelListening: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CyberDark.copy(alpha = 0.95f))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        ArcCyan.copy(alpha = borderAlpha),
                        ArcBlue.copy(alpha = borderAlpha * 0.7f),
                        ArcCyan.copy(alpha = borderAlpha)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("speech_recognition_hud_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing recording indicator
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isListening) ArcCyan else StarkAmber)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = if (isListening) "VOICE SENSORS ACTIVE" else "SPEECH STATUS",
                            color = if (isListening) ArcCyan else StarkAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        val displayText = when {
                            partialSpeech.isNotBlank() -> "> \"$partialSpeech\""
                            isListening -> "Listening to $callsign... Speak directive now."
                            !statusMessage.isNullOrBlank() -> statusMessage
                            else -> "Ready for command"
                        }

                        Text(
                            text = displayText,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2
                        )
                    }
                }

                if (isListening) {
                    IconButton(
                        onClick = onCancelListening,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("stop_listening_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Listening",
                            tint = AlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("dismiss_speech_status_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Real-time animated acoustic wave indicator during speech recognition
            if (isListening) {
                Spacer(modifier = Modifier.height(6.dp))
                AcousticWaveIndicator(
                    isListening = true,
                    audioRms = audioRms,
                    heightDp = 48.dp
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    callsign: String,
    modifier: Modifier = Modifier
) {
    val isJarvis = message.role == MessageRole.JARVIS

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = if (isJarvis) Alignment.Start else Alignment.End
    ) {
        // Role header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 3.dp, start = if (isJarvis) 6.dp else 0.dp, end = if (isJarvis) 0.dp else 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isJarvis) ArcCyan else StarkGold)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isJarvis) "J.A.R.V.I.S." else callsign.uppercase(),
                color = if (isJarvis) ArcCyan else StarkGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Bubble card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isJarvis) 2.dp else 14.dp,
                        bottomEnd = if (isJarvis) 14.dp else 2.dp
                    )
                )
                .background(
                    if (isJarvis) CyberSurface.copy(alpha = 0.95f) else Color(0xFF132338)
                )
                .border(
                    width = 1.dp,
                    color = if (isJarvis) ArcCyan.copy(alpha = 0.35f) else ArcBlue.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isJarvis) 2.dp else 14.dp,
                        bottomEnd = if (isJarvis) 14.dp else 2.dp
                    )
                )
                .padding(14.dp)
        ) {
            Text(
                text = message.content,
                color = TextPrimary,
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            )
        }
    }
}

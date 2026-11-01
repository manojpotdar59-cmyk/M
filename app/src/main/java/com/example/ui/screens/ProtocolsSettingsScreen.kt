package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisTab
import com.example.ui.components.HudCard
import com.example.ui.components.HudStatusBadge
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.StarkGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun ProtocolsSettingsScreen(
    viewModel: JarvisViewModel,
    callsign: String,
    autoSpeak: Boolean,
    voicePitch: Float,
    voiceRate: Float,
    onTestVoiceRecognition: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var customCallsign by remember(callsign) { mutableStateOf(callsign) }
    var currentPitch by remember(voicePitch) { mutableFloatStateOf(voicePitch) }
    var currentRate by remember(voiceRate) { mutableFloatStateOf(voiceRate) }

    val presetCallsigns = listOf("Sir", "Tony", "Boss", "Commander")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 14.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PROTOCOLS & CALIBRATION",
                    color = ArcCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "J.A.R.V.I.S. VOCAL & SYSTEM PARAMETERS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            HudStatusBadge(text = "SECURE", color = ArcCyan)
        }

        // User Callsign Card
        HudCard(
            title = "USER CALLSIGN CALIBRATION",
            badgeText = callsign.uppercase(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Select or input how Jarvis addresses you during directives:",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Preset callsigns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetCallsigns.forEach { preset ->
                    val isSelected = callsign == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ArcCyan else CyberDark)
                            .border(1.dp, if (isSelected) ArcCyan else CyberBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.setCallsign(preset)
                                customCallsign = preset
                            }
                            .padding(vertical = 8.dp)
                            .testTag("callsign_preset_$preset"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset,
                            color = if (isSelected) CyberDark else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customCallsign,
                    onValueChange = { customCallsign = it },
                    placeholder = { Text("Custom Callsign...", color = TextMuted) },
                    modifier = Modifier.weight(1f).testTag("custom_callsign_field"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.setCallsign(customCallsign) },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcCyan, contentColor = CyberDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_callsign_button")
                ) {
                    Text("SAVE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Vocal Synthesis Calibration
        HudCard(
            title = "VOCAL SYNTHESIZER CALIBRATION",
            badgeText = "TEXT-TO-SPEECH",
            borderColor = ArcBlue,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Auto-speak toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vocalize Directive Responses",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Jarvis automatically speaks responses aloud",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = autoSpeak,
                    onCheckedChange = { viewModel.setAutoSpeak(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberDark,
                        checkedTrackColor = ArcCyan,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberDark
                    ),
                    modifier = Modifier.testTag("auto_speak_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pitch slider
            Text(
                text = "VOCAL PITCH: ${String.format("%.2f", currentPitch)}",
                color = ArcCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = currentPitch,
                onValueChange = { currentPitch = it },
                onValueChangeFinished = { viewModel.setVoicePitch(currentPitch) },
                valueRange = 0.7f..1.3f,
                colors = SliderDefaults.colors(
                    thumbColor = ArcCyan,
                    activeTrackColor = ArcCyan,
                    inactiveTrackColor = CyberBorder
                ),
                modifier = Modifier.testTag("voice_pitch_slider")
            )

            // Speed slider
            Text(
                text = "SPEECH RATE: ${String.format("%.2f", currentRate)}x",
                color = ArcCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = currentRate,
                onValueChange = { currentRate = it },
                onValueChangeFinished = { viewModel.setVoiceRate(currentRate) },
                valueRange = 0.7f..1.4f,
                colors = SliderDefaults.colors(
                    thumbColor = ArcBlue,
                    activeTrackColor = ArcBlue,
                    inactiveTrackColor = CyberBorder
                ),
                modifier = Modifier.testTag("voice_rate_slider")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { viewModel.testJarvisVoice() },
                colors = ButtonDefaults.buttonColors(containerColor = ArcBlue, contentColor = CyberDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("test_voice_button")
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = CyberDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text("TEST J.A.R.V.I.S. VOCAL RESPONSE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SpeechRecognizer Voice Directives Card
        HudCard(
            title = "VOICE COMMAND RECOGNITION (SPEECHRECOGNIZER)",
            badgeText = "AUDIO SENSORS",
            borderColor = ArcCyan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Android SpeechRecognizer listens for your spoken directives, extracts real-time phonetic audio RMS data, and transmits commands to the Jarvis neural processor.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recognition Service Engine",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    HudStatusBadge(text = "ARMED", color = ArcCyan)
                }

                Button(
                    onClick = {
                        onTestVoiceRecognition()
                        viewModel.selectTab(JarvisTab.ARC_CORE)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArcCyan, contentColor = CyberDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("test_speech_recognition_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = CyberDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ENGAGE VOICE SENSORS & SPEAK", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Security & AI Engine Architecture
        HudCard(
            title = "STARK SECURITY & ARCHITECTURE",
            badgeText = "VERIFIED",
            borderColor = StarkGold,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = StarkGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "App Check Attestation: Active",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "Zero-trust client verification with cryptographic tokens protects J.A.R.V.I.S. logic.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Emergency Protocol Clean Slate
        HudCard(
            title = "EMERGENCY PROTOCOLS",
            borderColor = AlertRed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Protocol Clean Slate resets all conversation buffers, clears temporary mission telemetry, and re-initializes primary systems.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    viewModel.clearChatSlate()
                    viewModel.selectTab(JarvisTab.ARC_CORE)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("protocol_clean_slate_button")
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXECUTE PROTOCOL CLEAN SLATE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

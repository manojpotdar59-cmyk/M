package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanGlow
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.StarkAmber
import com.example.ui.theme.StarkGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity acoustic oscilloscope wave & multi-band EQ indicator.
 * Reacts continuously and smoothly to audio RMS decibels delivered by Android SpeechRecognizer.
 */
@Composable
fun AcousticWaveIndicator(
    isListening: Boolean,
    audioRms: Float,
    modifier: Modifier = Modifier,
    heightDp: Dp = 64.dp
) {
    // Smoothly interpolate incoming RMS to prevent visual stuttering
    val smoothRms by animateFloatAsState(
        targetValue = if (isListening) audioRms.coerceIn(0f, 12f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "smoothRmsAnim"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "waveOscillator")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 1200 else 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .clip(RoundedCornerShape(12.dp))
            .background(CyberDark.copy(alpha = 0.85f))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        CyberBorder,
                        if (isListening) ArcCyan.copy(alpha = 0.6f) else CyberBorder,
                        CyberBorder
                    )
                ),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("acoustic_wave_indicator")
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(heightDp)) {
            val width = size.width
            val height = size.height
            val midY = height / 2f

            // Baseline guide line
            drawLine(
                color = CyberBorder.copy(alpha = 0.5f),
                start = Offset(0f, midY),
                end = Offset(width, midY),
                strokeWidth = 1.dp.toPx()
            )

            // Normalized amplitude scalar [0.08 .. 1.0]
            val baseAmplitude = if (isListening) {
                val normalizedVolume = (smoothRms / 12f).coerceIn(0f, 1f)
                (height * 0.12f) + (normalizedVolume * height * 0.40f) * ambientPulse
            } else {
                height * 0.05f
            }

            // 1. Draw Multi-Band Vertical Equalizer Bars (24 bars centered)
            val barCount = 27
            val totalBarWidth = width * 0.82f
            val barSpacing = totalBarWidth / barCount
            val startX = (width - totalBarWidth) / 2f

            for (i in 0 until barCount) {
                val x = startX + (i * barSpacing)
                // Center-weighted bell curve falloff
                val distFromCenter = abs(i - (barCount / 2f)) / (barCount / 2f)
                val bellCurve = cos(distFromCenter * (PI / 2.2f)).toFloat().coerceIn(0.15f, 1f)

                val harmonicOffset = sin(wavePhase + (i * 0.35f))
                val barHeight = if (isListening) {
                    val dynamicPart = (baseAmplitude * bellCurve * (1f + 0.35f * harmonicOffset)).coerceAtLeast(3.dp.toPx())
                    dynamicPart
                } else {
                    (height * 0.08f * bellCurve).coerceAtLeast(2.dp.toPx())
                }

                val barColor = when {
                    smoothRms > 7.5f && distFromCenter < 0.25f -> StarkGold
                    i % 2 == 0 -> ArcCyan
                    else -> ArcBlue
                }

                val barAlpha = if (isListening) {
                    (0.45f + 0.55f * (smoothRms / 10f)).coerceIn(0.4f, 1.0f)
                } else 0.25f

                // Mirrored bars top and bottom from center
                drawLine(
                    color = barColor.copy(alpha = barAlpha),
                    start = Offset(x, midY - barHeight),
                    end = Offset(x, midY + barHeight),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 2. Continuous Oscilloscope Sine Wave
            if (isListening) {
                val wavePath = Path()
                val wavePath2 = Path()
                val points = 60
                val dx = width / points

                for (p in 0..points) {
                    val px = p * dx
                    val angle = (px / width) * 4 * PI + wavePhase
                    val waveY = midY + (sin(angle).toFloat() * baseAmplitude * 0.85f)
                    val waveY2 = midY + (cos(angle * 1.3f).toFloat() * baseAmplitude * 0.55f)

                    if (p == 0) {
                        wavePath.moveTo(px, waveY)
                        wavePath2.moveTo(px, waveY2)
                    } else {
                        wavePath.lineTo(px, waveY)
                        wavePath2.lineTo(px, waveY2)
                    }
                }

                // Primary glowing wave
                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            ArcCyan.copy(alpha = 0.1f),
                            ArcCyan.copy(alpha = 0.85f),
                            StarkGold.copy(alpha = 0.9f),
                            ArcCyan.copy(alpha = 0.85f),
                            ArcCyan.copy(alpha = 0.1f)
                        )
                    ),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Secondary harmonic wave
                drawPath(
                    path = wavePath2,
                    color = ArcBlue.copy(alpha = 0.45f),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Top HUD Telemetry Overlay inside wave box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isListening) ArcCyan else TextMuted)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isListening) "SPEECH ACOUSTICS ACTIVE" else "ACOUSTIC SENSORS IDLE",
                    color = if (isListening) ArcCyan else TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            Text(
                text = if (isListening) {
                    val db = String.format("+%.1f dB", smoothRms)
                    "BAND: 16 kHz • RMS: $db"
                } else {
                    "STANDBY"
                },
                color = if (isListening) StarkGold else TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Pulsing acoustic halo ring that surrounds buttons or the Arc Reactor core.
 */
@Composable
fun MicPulseHalo(
    isListening: Boolean,
    audioRms: Float,
    modifier: Modifier = Modifier,
    haloSize: Dp = 56.dp
) {
    val smoothRms by animateFloatAsState(
        targetValue = if (isListening) audioRms.coerceIn(0f, 12f) else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "haloRms"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "haloPulse")
    val pulseProg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloProg"
    )

    if (isListening) {
        Canvas(modifier = modifier.size(haloSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.width / 2f

            // Dynamic expansion based on speech level
            val dynamicBonus = (smoothRms / 12f) * 10.dp.toPx()
            val rippleRadius = (baseRadius * 0.7f) + (pulseProg * (baseRadius * 0.5f + dynamicBonus))
            val rippleAlpha = ((1f - pulseProg) * (0.6f + (smoothRms / 20f))).coerceIn(0.1f, 0.85f)

            drawCircle(
                color = ArcCyan.copy(alpha = rippleAlpha),
                radius = rippleRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Second lagging ripple
            val secondProg = (pulseProg + 0.5f) % 1f
            val secondRadius = (baseRadius * 0.7f) + (secondProg * (baseRadius * 0.5f + dynamicBonus))
            val secondAlpha = ((1f - secondProg) * (0.4f + (smoothRms / 25f))).coerceIn(0.05f, 0.6f)

            drawCircle(
                color = ArcBlue.copy(alpha = secondAlpha),
                radius = secondRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

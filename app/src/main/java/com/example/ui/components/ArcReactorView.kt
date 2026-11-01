package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.JarvisVoiceState
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanGlow
import com.example.ui.theme.StarkGold
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ArcReactorView(
    state: JarvisVoiceState,
    audioRms: Float = 0f,
    sizeDp: Dp = 220.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorTransitions")

    // Rotation speeds adapt based on state:
    val rotationOuterDuration = when (state) {
        JarvisVoiceState.PROCESSING -> 3000
        JarvisVoiceState.SPEAKING -> 4000
        JarvisVoiceState.LISTENING -> 2500
        JarvisVoiceState.IDLE -> 12000
    }

    val rotationOuter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationOuterDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRingRotation"
    )

    val rotationInner by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (rotationOuterDuration * 0.7f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRingRotation"
    )

    // Pulse core
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = if (state == JarvisVoiceState.LISTENING) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == JarvisVoiceState.LISTENING) 600 else 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (state == JarvisVoiceState.IDLE) 0.55f else 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val primaryColor = when (state) {
        JarvisVoiceState.LISTENING -> ArcCyan
        JarvisVoiceState.PROCESSING -> StarkGold
        JarvisVoiceState.SPEAKING -> ArcBlue
        JarvisVoiceState.IDLE -> ArcCyan
    }

    val interactionSource = remember { MutableInteractionSource() }

    val smoothRms by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (state == JarvisVoiceState.LISTENING) audioRms.coerceIn(0f, 12f) else 0f,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessHigh),
        label = "arcSmoothRms"
    )

    val acousticRipplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "acousticRipple"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(sizeDp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = min(size.width, size.height) / 2f - 16.dp.toPx()

            // 1. Ambient Background Glow (flares dynamically with speech volume)
            val rmsBloom = if (state == JarvisVoiceState.LISTENING) (smoothRms / 10f) * 0.45f else 0f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = (glowAlpha * 0.45f + rmsBloom).coerceIn(0.1f, 0.95f)),
                        primaryColor.copy(alpha = (glowAlpha * 0.12f + rmsBloom * 0.4f).coerceIn(0.05f, 0.5f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * (1.35f + (if (state == JarvisVoiceState.LISTENING) (smoothRms / 12f) * 0.25f else 0f))
                ),
                radius = baseRadius * 1.55f,
                center = center
            )

            // 2. Audio Wave / Expanding Acoustic Ripple Rings while actively listening
            if (state == JarvisVoiceState.LISTENING) {
                val voiceBonus = (smoothRms / 12f) * 22.dp.toPx()

                // Triple expanding acoustic ripples
                for (r in 0 until 3) {
                    val phase = (acousticRipplePhase + (r * 0.33f)) % 1f
                    val rippleR = (baseRadius * 0.75f) + (phase * (baseRadius * 0.45f + voiceBonus))
                    val rippleAlpha = ((1f - phase) * (0.65f + (smoothRms / 15f))).coerceIn(0.08f, 0.85f)
                    drawCircle(
                        color = if (r % 2 == 0) ArcCyan.copy(alpha = rippleAlpha) else ArcBlue.copy(alpha = rippleAlpha * 0.8f),
                        radius = rippleR,
                        center = center,
                        style = Stroke(width = (2f - phase).coerceAtLeast(0.8f).dp.toPx())
                    )
                }
            } else {
                val reactiveBonus = (smoothRms / 10f) * 14.dp.toPx()
                drawCircle(
                    color = ArcCyanGlow.copy(alpha = 0.25f),
                    radius = (baseRadius * 0.95f) + reactiveBonus,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 3. Outer Segmented Ring with Rotation
            rotate(rotationOuter, pivot = center) {
                val segments = 12
                val segmentAngle = 360f / segments
                val gap = 6f
                for (i in 0 until segments) {
                    val startAngle = i * segmentAngle + gap / 2f
                    val sweepAngle = segmentAngle - gap
                    drawArc(
                        color = primaryColor.copy(alpha = 0.85f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 2, baseRadius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Small radial notch on segment
                    val rad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
                    val notchInner = baseRadius - 8.dp.toPx()
                    val notchOuter = baseRadius + 3.dp.toPx()
                    val p1 = Offset(center.x + (notchInner * cos(rad)).toFloat(), center.y + (notchInner * sin(rad)).toFloat())
                    val p2 = Offset(center.x + (notchOuter * cos(rad)).toFloat(), center.y + (notchOuter * sin(rad)).toFloat())
                    drawLine(
                        color = StarkGold.copy(alpha = 0.9f),
                        start = p1,
                        end = p2,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 4. Middle Counter-Rotating Ring
            rotate(rotationInner, pivot = center) {
                val midRadius = baseRadius * 0.72f
                drawCircle(
                    color = ArcBlue.copy(alpha = 0.6f),
                    radius = midRadius,
                    center = center,
                    style = Stroke(width = 1.8.dp.toPx())
                )

                // 8 Nodes along middle ring
                val nodeCount = 8
                for (j in 0 until nodeCount) {
                    val angleDeg = j * (360f / nodeCount)
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val nx = center.x + (midRadius * cos(rad)).toFloat()
                    val ny = center.y + (midRadius * sin(rad)).toFloat()
                    drawCircle(
                        color = StarkGold,
                        radius = 2.5.dp.toPx(),
                        center = Offset(nx, ny)
                    )
                }
            }

            val dynamicCoreScale = if (state == JarvisVoiceState.LISTENING) {
                pulseScale * (1f + (smoothRms / 12f) * 0.26f)
            } else pulseScale
            val coreRadius = baseRadius * 0.44f * dynamicCoreScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColor.copy(alpha = 0.85f),
                        primaryColor.copy(alpha = 0.15f)
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            drawCircle(
                color = primaryColor,
                radius = coreRadius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 6. Central Stark Triangle / Core Geometry
            val triSize = coreRadius * 0.62f
            val triPath = Path().apply {
                moveTo(center.x, center.y - triSize)
                lineTo(center.x + (triSize * 0.866f), center.y + (triSize * 0.5f))
                lineTo(center.x - (triSize * 0.866f), center.y + (triSize * 0.5f))
                close()
            }
            drawPath(
                path = triPath,
                color = Color(0xFF071220),
                alpha = 0.85f
            )
            drawPath(
                path = triPath,
                color = ArcCyan,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Core center light
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = center
            )
        }
    }
}

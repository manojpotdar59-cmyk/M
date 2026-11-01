package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DirectiveCategory
import com.example.data.model.JarvisDirective
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanGlow
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.StarkAmber
import com.example.ui.theme.StarkGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HudCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    badgeText: String? = null,
    borderColor: Color = CyberBorder,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        borderColor.copy(alpha = 0.8f),
                        CyberBorder.copy(alpha = 0.3f),
                        borderColor.copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            if (title != null || badgeText != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title != null) {
                        Text(
                            text = title.uppercase(),
                            color = ArcCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                    }
                    if (badgeText != null) {
                        HudStatusBadge(text = badgeText)
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun HudStatusBadge(
    text: String,
    color: Color = ArcCyan
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun HudTelemetryGauge(
    title: String,
    value: String,
    unit: String = "",
    fraction: Float,
    statusText: String = "NOMINAL",
    gaugeColor: Color = ArcCyan,
    modifier: Modifier = Modifier
) {
    HudCard(
        title = title,
        badgeText = statusText,
        borderColor = gaugeColor.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        color = gaugeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            Text(
                text = "${(fraction.coerceIn(0f, 1f) * 100).toInt()}%",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = gaugeColor,
            trackColor = CyberDark,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun HudWaveformVisualizer(
    isActive: Boolean,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveformAnim")
    val heights = (0 until 9).map { index ->
        val duration = 280 + (index * 60)
        val anim by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = if (isActive) 1.0f else 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "waveBar$index"
        )
        anim
    }

    val rmsMultiplier = if (isActive) (1f + (audioRms.coerceIn(0f, 12f) / 6f)).coerceIn(1f, 2.5f) else 1f

    Row(
        modifier = modifier
            .height(28.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { idx, heightFactor ->
            val barHeight = ((24 * heightFactor * rmsMultiplier).coerceIn(4f, 28f)).dp
            val barColor = when {
                audioRms > 6f && idx in 3..5 -> StarkGold
                idx % 2 == 0 -> ArcCyan
                else -> ArcBlue
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun QuickCommandChip(
    label: String,
    prompt: String,
    tag: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onClick(prompt) },
        color = CyberSurface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ArcCyan.copy(alpha = 0.35f)),
        modifier = modifier.testTag("quick_command_$tag")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(StarkGold)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = ArcCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun HudDirectiveCard(
    directive: JarvisDirective,
    onToggleCompleted: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (directive.category) {
        DirectiveCategory.TACTICAL -> ArcCyan
        DirectiveCategory.DEFENSE -> AlertRed
        DirectiveCategory.RESEARCH -> ArcBlue
        DirectiveCategory.ROUTINE -> StarkGold
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurface.copy(alpha = 0.9f))
            .border(1.dp, categoryColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("directive_card_${directive.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = directive.isCompleted,
                onCheckedChange = { onToggleCompleted(directive.id) },
                colors = CheckboxDefaults.colors(
                    checkedColor = ArcCyan,
                    uncheckedColor = TextMuted,
                    checkmarkColor = CyberDark
                ),
                modifier = Modifier.testTag("directive_check_${directive.id}")
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HudStatusBadge(text = directive.category.label, color = categoryColor)
                    if (directive.priority == "CRITICAL" || directive.priority == "HIGH") {
                        HudStatusBadge(text = directive.priority, color = if (directive.priority == "CRITICAL") AlertRed else StarkAmber)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = directive.title,
                    color = if (directive.isCompleted) TextMuted else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (directive.detail.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = directive.detail,
                        color = if (directive.isCompleted) TextMuted.copy(alpha = 0.6f) else TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = { onDelete(directive.id) },
                modifier = Modifier.testTag("directive_delete_${directive.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Directive",
                    tint = TextMuted
                )
            }
        }
    }
}

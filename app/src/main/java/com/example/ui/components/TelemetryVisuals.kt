package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BackHand
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.TableRestaurant
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DurationFormatter
import com.example.model.GripType
import com.example.model.HandType
import com.example.model.HourlyHandStat
import com.example.ui.theme.LocalGripAccents
import com.example.ui.theme.MonoMetricMedium
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun iconForGrip(gripType: GripType): ImageVector = when (gripType) {
    GripType.ONE_HAND_THUMB -> Icons.Rounded.TouchApp
    GripType.TWO_HAND_PORTRAIT -> Icons.Rounded.Smartphone
    GripType.CRADLE_SUPPORT -> Icons.Rounded.BackHand
    GripType.TWO_HAND_LANDSCAPE -> Icons.Rounded.ScreenRotation
    GripType.UPRIGHT_CALL -> Icons.Rounded.Call
    GripType.RESTING -> Icons.Rounded.TableRestaurant
}

fun iconForHand(handType: HandType): ImageVector = when (handType) {
    HandType.LEFT -> Icons.Rounded.BackHand
    HandType.RIGHT -> Icons.Rounded.PanTool
    HandType.BOTH -> Icons.Rounded.Smartphone
    HandType.RESTING -> Icons.Rounded.TableRestaurant
}

/**
 * Real-time gyroscopic attitude instrument rendered on Canvas.
 * Smoothly rotates a minimalist smartphone silhouette based on actual hardware roll & pitch.
 */
@Composable
fun LivePhoneTiltVisualizer(
    rollDeg: Float,
    pitchDeg: Float,
    hand: HandType,
    modifier: Modifier = Modifier
) {
    val accents = LocalGripAccents.current
    val activeColor = accents.colorForHand(hand)
    val ringMuted = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    val animatedRoll by animateFloatAsState(
        targetValue = rollDeg.coerceIn(-65f, 65f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "roll_anim"
    )
    val animatedPitch by animateFloatAsState(
        targetValue = pitchDeg.coerceIn(0f, 90f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pitch_anim"
    )

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension * 0.45f

        // Outer precision bezel ring
        drawCircle(
            color = ringMuted,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )

        // Subtle inner crosshairs
        drawLine(
            color = ringMuted.copy(alpha = 0.35f),
            start = Offset(cx - radius * 0.85f, cy),
            end = Offset(cx + radius * 0.85f, cy),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = ringMuted.copy(alpha = 0.35f),
            start = Offset(cx, cy - radius * 0.85f),
            end = Offset(cx, cy + radius * 0.85f),
            strokeWidth = 1.dp.toPx()
        )

        // Precision degree tick marks around perimeter
        for (angle in 0 until 360 step 15) {
            val rad = angle * PI / 180.0
            val isMajor = angle % 45 == 0
            val tickLen = if (isMajor) 7.dp.toPx() else 3.5.dp.toPx()
            val innerR = radius - tickLen
            drawLine(
                color = if (isMajor) ringMuted else ringMuted.copy(alpha = 0.4f),
                start = Offset(
                    cx + (innerR * cos(rad)).toFloat(),
                    cy + (innerR * sin(rad)).toFloat()
                ),
                end = Offset(
                    cx + (radius * cos(rad)).toFloat(),
                    cy + (radius * sin(rad)).toFloat()
                ),
                strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
            )
        }

        // Active hand directional side glow arc
        val arcTopLeft = Offset(cx - radius, cy - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        when (hand) {
            HandType.LEFT -> {
                drawArc(
                    color = accents.leftHand,
                    startAngle = 125f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            HandType.RIGHT -> {
                drawArc(
                    color = accents.rightHand,
                    startAngle = -55f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            HandType.BOTH -> {
                drawArc(
                    color = accents.bothHands,
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = accents.bothHands,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            HandType.RESTING -> {
                drawArc(
                    color = accents.resting.copy(alpha = 0.5f),
                    startAngle = 45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Rotated phone silhouette reacting to live roll & pitch perspective
        val pitchScaleY = (0.55f + (animatedPitch / 90f) * 0.45f).coerceIn(0.55f, 1.0f)
        val phoneW = radius * 0.66f
        val phoneH = radius * 1.18f * pitchScaleY

        rotate(degrees = -animatedRoll, pivot = Offset(cx, cy)) {
            val left = cx - phoneW / 2f
            val top = cy - phoneH / 2f
            // Ambient glow behind phone
            drawRoundRect(
                color = activeColor.copy(alpha = 0.16f),
                topLeft = Offset(left - 4.dp.toPx(), top - 4.dp.toPx()),
                size = Size(phoneW + 8.dp.toPx(), phoneH + 8.dp.toPx()),
                cornerRadius = CornerRadius(14.dp.toPx())
            )
            // Phone chassis
            drawRoundRect(
                color = surfaceColor,
                topLeft = Offset(left, top),
                size = Size(phoneW, phoneH),
                cornerRadius = CornerRadius(10.dp.toPx())
            )
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(left, top),
                size = Size(phoneW, phoneH),
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
            // Top speaker pill
            drawLine(
                color = onSurface.copy(alpha = 0.45f),
                start = Offset(cx - phoneW * 0.18f, top + 6.dp.toPx()),
                end = Offset(cx + phoneW * 0.18f, top + 6.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Center balance horizon bar
            drawLine(
                color = activeColor.copy(alpha = 0.9f),
                start = Offset(cx - phoneW * 0.25f, cy),
                end = Offset(cx + phoneW * 0.25f, cy),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = activeColor,
                radius = 3.5.dp.toPx(),
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Segmented Donut Chart for Daily Hand Distribution (Left vs Right vs Both).
 */
@Composable
fun HandSplitRingChart(
    leftSeconds: Long,
    rightSeconds: Long,
    bothSeconds: Long,
    modifier: Modifier = Modifier
) {
    val accents = LocalGripAccents.current
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val total = leftSeconds + rightSeconds + bothSeconds

    val leftSweep by animateFloatAsState(
        targetValue = if (total > 0) (leftSeconds.toFloat() / total) * 360f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "left_arc"
    )
    val rightSweep by animateFloatAsState(
        targetValue = if (total > 0) (rightSeconds.toFloat() / total) * 360f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "right_arc"
    )
    val bothSweep by animateFloatAsState(
        targetValue = if (total > 0) (bothSeconds.toFloat() / total) * 360f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "both_arc"
    )

    Box(
        modifier = modifier.size(156.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 16.dp.toPx()
            val inset = strokePx / 2f + 4.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)

            // Base track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            if (total > 0L) {
                val gap = 4f
                var cursor = -90f

                if (leftSweep > 1f) {
                    drawArc(
                        color = accents.leftHand,
                        startAngle = cursor,
                        sweepAngle = (leftSweep - gap).coerceAtLeast(1.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    cursor += leftSweep
                }
                if (rightSweep > 1f) {
                    drawArc(
                        color = accents.rightHand,
                        startAngle = cursor,
                        sweepAngle = (rightSweep - gap).coerceAtLeast(1.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    cursor += rightSweep
                }
                if (bothSweep > 1f) {
                    drawArc(
                        color = accents.bothHands,
                        startAngle = cursor,
                        sweepAngle = (bothSweep - gap).coerceAtLeast(1.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (total > 0L) DurationFormatter.formatCompact(total) else "0s",
                style = MonoMetricMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "HOLD TIME",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 24-Hour Stacked Bar Chart showing hourly hand holding rhythm.
 */
@Composable
fun HourlyHandChart(
    hourlyStats: List<HourlyHandStat>,
    modifier: Modifier = Modifier
) {
    val accents = LocalGripAccents.current
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val maxTotal = (hourlyStats.maxOfOrNull { it.totalSeconds } ?: 0L).coerceAtLeast(10L)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(116.dp)
    ) {
        val barCount = 24
        val horizontalPad = 4.dp.toPx()
        val usableWidth = size.width - horizontalPad * 2f
        val slotWidth = usableWidth / barCount
        val barWidth = (slotWidth * 0.58f).coerceAtLeast(4.dp.toPx())
        val chartHeight = size.height - 8.dp.toPx()

        // Horizontal subtle reference lines
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
            val y = chartHeight * (1f - frac)
            drawLine(
                color = gridColor.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        for (i in 0 until barCount) {
            val stat = hourlyStats.getOrNull(i)
            val x = horizontalPad + i * slotWidth + (slotWidth - barWidth) / 2f

            // Background slot pillar
            drawRoundRect(
                color = emptyBarColor.copy(alpha = 0.45f),
                topLeft = Offset(x, 0f),
                size = Size(barWidth, chartHeight),
                cornerRadius = CornerRadius(3.dp.toPx())
            )

            if (stat != null && stat.totalSeconds > 0L) {
                val totalFraction = (stat.totalSeconds.toFloat() / maxTotal.toFloat()).coerceIn(0.06f, 1f)
                val totalH = chartHeight * totalFraction
                var bottomY = chartHeight

                val segments = listOf(
                    stat.leftSeconds to accents.leftHand,
                    stat.rightSeconds to accents.rightHand,
                    stat.bothSeconds to accents.bothHands
                )

                for ((sec, color) in segments) {
                    if (sec <= 0L) continue
                    val segH = totalH * (sec.toFloat() / stat.totalSeconds.toFloat())
                    val topY = (bottomY - segH).coerceAtLeast(0f)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, (bottomY - topY).coerceAtLeast(2.dp.toPx())),
                        cornerRadius = CornerRadius(2.5.dp.toPx())
                    )
                    bottomY = topY
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AdaptiveModelStats
import com.example.model.DailyStatsSummary
import com.example.model.DurationFormatter
import com.example.model.GripType
import com.example.model.HandType
import com.example.model.LiveGripSnapshot
import com.example.ui.GripPulseUiState
import com.example.ui.components.HandSplitRingChart
import com.example.ui.components.HourlyHandChart
import com.example.ui.components.LivePhoneTiltVisualizer
import com.example.ui.components.iconForGrip
import com.example.ui.theme.ExpressiveHeroNumeral
import com.example.ui.theme.LocalGripAccents
import com.example.ui.theme.MonoCaption
import com.example.ui.theme.MonoMetricMedium
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardScreen(
    state: GripPulseUiState,
    onToggleBackgroundTracking: () -> Unit,
    onTeachCurrentHand: (HandType) -> Unit,
    onOpenGripsTab: () -> Unit,
    onOpenWidgetsTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accents = LocalGripAccents.current
    val summary = state.dailySummary
    val live = state.liveSnapshot

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Expressive Header Bar
        item {
            DashboardHeader(
                isBackgroundTracking = state.isBackgroundTracking,
                adaptiveStats = state.adaptiveModelStats,
                onToggleBackgroundTracking = onToggleBackgroundTracking
            )
        }

        // 2. Material 3 Expressive Live Sensor Hero Card + Instant 1-Tap AI Teaching
        item {
            LiveSensorHeroCard(
                live = live,
                adaptiveStats = state.adaptiveModelStats,
                isBackgroundTracking = state.isBackgroundTracking,
                onTeachCurrentHand = onTeachCurrentHand,
                onToggleBackgroundTracking = onToggleBackgroundTracking
            )
        }

        // 3. Expressive Sculptural Bento Row using Google MaterialShapes.Cookie9Sided
        item {
            ExpressiveSummaryBentoRow(summary = summary)
        }

        // 4. Daily Hand Time Distribution Card with Expressive Wavy Progress Indicators
        item {
            DailyHandSplitCard(summary = summary)
        }

        // 5. Most Frequent Grip Spotlight Card with MaterialShapes.SoftBurst
        item {
            MostFrequentGripCard(
                summary = summary,
                liveGrip = live.gripType,
                onExploreGrips = onOpenGripsTab
            )
        }

        // 6. 24-Hour Hand Rhythm Timeline
        item {
            HourlyRhythmCard(summary = summary)
        }

        // 7. Expressive Floating Widgets Callout Pill Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .clickable(onClick = onOpenWidgetsTab)
                    .testTag("dashboard_widgets_cta"),
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (accents.isDark) 0.55f else 0.72f),
                border = BorderStroke(1.dp, accents.bothHands.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(MaterialShapes.Clover4Leaf.toShape())
                            .background(accents.bothHands.copy(alpha = 0.24f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Widgets,
                            contentDescription = "Home screen widgets",
                            tint = accents.bothHands,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Real-Time Home Widgets",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pin live 2×2 or 4×2 grip cards straight to your home screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accents.bothHands.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Open widgets hub",
                            tint = accents.bothHands,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    isBackgroundTracking: Boolean,
    adaptiveStats: AdaptiveModelStats,
    onToggleBackgroundTracking: () -> Unit
) {
    val accents = LocalGripAccents.current
    val pulseTransition = rememberInfiniteTransition(label = "live_dot")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "GripPulse",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .background(accents.livePulse)
                )
            }
            Text(
                text = "${adaptiveStats.stageLabel} · ${adaptiveStats.totalSamples} patterns studied",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onToggleBackgroundTracking)
                .testTag("toggle_background_tracking_pill"),
            shape = CircleShape,
            color = if (isBackgroundTracking) {
                accents.livePulse.copy(alpha = 0.20f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            border = BorderStroke(
                1.25.dp,
                if (isBackgroundTracking) accents.livePulse else accents.cardBorder
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isBackgroundTracking) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                    contentDescription = if (isBackgroundTracking) "Pause background tracking" else "Start background tracking",
                    tint = if (isBackgroundTracking) accents.livePulse else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBackgroundTracking) "LIVE BG" else "START BG",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isBackgroundTracking) accents.livePulse else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LiveSensorHeroCard(
    live: LiveGripSnapshot,
    adaptiveStats: AdaptiveModelStats,
    isBackgroundTracking: Boolean,
    onTeachCurrentHand: (HandType) -> Unit,
    onToggleBackgroundTracking: () -> Unit
) {
    val accents = LocalGripAccents.current
    val handColor by animateColorAsState(
        targetValue = accents.colorForHand(live.hand),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "hand_color_anim"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_sensor_hero_card"),
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.25.dp, handColor.copy(alpha = 0.48f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            handColor.copy(alpha = if (accents.isDark) 0.20f else 0.12f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LivePhoneTiltVisualizer(
                        rollDeg = live.rollDeg,
                        pitchDeg = live.pitchDeg,
                        hand = live.hand,
                        modifier = Modifier.size(130.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = handColor.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (live.usedAdaptiveModel) Icons.Rounded.AutoAwesome else Icons.Rounded.Sensors,
                                        contentDescription = "Detection engine status",
                                        tint = handColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (live.usedAdaptiveModel) {
                                            "AI · ${(live.confidence * 100).roundToInt()}%"
                                        } else if (live.isHolding) {
                                            "HOLDING"
                                        } else {
                                            "RESTING"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = handColor
                                    )
                                }
                            }
                        }

                        Text(
                            text = live.hand.displayName,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = iconForGrip(live.gripType),
                                contentDescription = live.gripType.displayName,
                                tint = handColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = live.gripType.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = handColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Expressive pill telemetry chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExpressiveTelemetryChip(
                                label = "ROLL",
                                value = String.format(Locale.US, "%+.0f°", live.rollDeg),
                                modifier = Modifier.weight(1f)
                            )
                            ExpressiveTelemetryChip(
                                label = "PITCH",
                                value = String.format(Locale.US, "%.0f°", live.pitchDeg),
                                modifier = Modifier.weight(1f)
                            )
                            ExpressiveTelemetryChip(
                                label = "STB",
                                value = "${live.stabilityScore}%",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Instant One-Tap Pattern Teaching Strip so users can train & improve detection live
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.62f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Teach Pattern Now (improves accuracy)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${adaptiveStats.maturityPercent}% trained",
                            style = MonoCaption,
                            color = accents.leftHand,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TeachHandChip(
                            label = "Left",
                            selected = live.hand == HandType.LEFT,
                            accent = accents.leftHand,
                            onClick = { onTeachCurrentHand(HandType.LEFT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("teach_left_chip")
                        )
                        TeachHandChip(
                            label = "Right",
                            selected = live.hand == HandType.RIGHT,
                            accent = accents.rightHand,
                            onClick = { onTeachCurrentHand(HandType.RIGHT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("teach_right_chip")
                        )
                        TeachHandChip(
                            label = "2-Hand",
                            selected = live.hand == HandType.BOTH,
                            accent = accents.bothHands,
                            onClick = { onTeachCurrentHand(HandType.BOTH) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("teach_both_chip")
                        )
                    }
                }

                // Tactile Expressive Pill Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBackgroundTracking) "Live Widget Engine Active" else "Always-On Background Sync",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBackgroundTracking) {
                                "Recording across apps & pushing live widget updates"
                            } else {
                                "Track hand holds across apps & keep widgets live"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onToggleBackgroundTracking,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = handColor,
                            contentColor = if (accents.isDark) Color(0xFF06111B) else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("hero_tracking_button")
                    ) {
                        Text(
                            text = if (isBackgroundTracking) "Pause" else "Start",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeachHandChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected) accent.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) accent else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = if (selected) 1.25.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        modifier = modifier.height(38.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun ExpressiveTelemetryChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MonoCaption,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveSummaryBentoRow(summary: DailyStatsSummary) {
    val accents = LocalGripAccents.current
    val dominant = summary.dominantHand
    val dominantColor = dominant?.let { accents.colorForHand(it) } ?: accents.leftHand

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1.25f),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = if (accents.isDark) 0.55f else 0.75f
                )
            ),
            border = BorderStroke(1.dp, accents.leftHand.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "TODAY'S HOLD TIME",
                    style = MaterialTheme.typography.labelSmall,
                    color = accents.leftHand
                )
                Text(
                    text = DurationFormatter.formatCompact(summary.totalHoldSeconds),
                    style = ExpressiveHeroNumeral,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dominant?.let { "Lead: ${it.displayName}" } ?: "Ready to measure",
                    style = MaterialTheme.typography.labelMedium,
                    color = dominantColor
                )
            }
        }

        Card(
            modifier = Modifier.weight(0.88f),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, accents.cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(accents.highlight.copy(alpha = if (accents.isDark) 0.22f else 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${summary.balanceScore}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = accents.highlight,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DailyHandSplitCard(summary: DailyStatsSummary) {
    val accents = LocalGripAccents.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_hand_split_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accents.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "Hand Hold Split",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Time held across Left, Right, and Two-Handed postures",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HandSplitRingChart(
                    leftSeconds = summary.leftSeconds,
                    rightSeconds = summary.rightSeconds,
                    bothSeconds = summary.bothSeconds
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HandBreakdownRow(
                        label = "Left Hand",
                        durationText = DurationFormatter.formatCompact(summary.leftSeconds),
                        percent = summary.leftPercent,
                        accent = accents.leftHand
                    )
                    HandBreakdownRow(
                        label = "Right Hand",
                        durationText = DurationFormatter.formatCompact(summary.rightSeconds),
                        percent = summary.rightPercent,
                        accent = accents.rightHand
                    )
                    HandBreakdownRow(
                        label = "Two Hands",
                        durationText = DurationFormatter.formatCompact(summary.bothSeconds),
                        percent = summary.bothPercent,
                        accent = accents.bothHands
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Ergonomic insight",
                    tint = accents.highlight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = summary.ergonomicInsight,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HandBreakdownRow(
    label: String,
    durationText: String,
    percent: Int,
    accent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "$durationText · $percent%",
                style = MonoCaption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearWavyProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MostFrequentGripCard(
    summary: DailyStatsSummary,
    liveGrip: GripType,
    onExploreGrips: () -> Unit
) {
    val accents = LocalGripAccents.current
    val topGrip = summary.topGrip

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onExploreGrips)
            .testTag("most_frequent_grip_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accents.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = "Most frequent grip",
                        tint = accents.highlight,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Most Frequent Grip",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "All Grips →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (topGrip == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(MaterialShapes.SoftBurst.toShape())
                            .background(accents.highlight.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconForGrip(liveGrip),
                            contentDescription = "Ready to detect grip",
                            tint = accents.highlight,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Detecting your #1 grip...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hold and tilt your phone naturally for a few seconds to rank your top daily grip.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(accents.highlight.copy(alpha = if (accents.isDark) 0.16f else 0.14f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(MaterialShapes.SoftBurst.toShape())
                            .background(accents.highlight.copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconForGrip(topGrip.gripType),
                            contentDescription = topGrip.gripType.displayName,
                            tint = accents.highlight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#1 TODAY · ${topGrip.gripType.strainLevel.label.uppercase(Locale.US)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = accents.highlight
                        )
                        Text(
                            text = topGrip.gripType.displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = topGrip.gripType.ergonomicTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${topGrip.percentage}%",
                            style = MonoMetricMedium,
                            color = accents.highlight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = DurationFormatter.formatShort(topGrip.durationSeconds),
                            style = MonoCaption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val runnersUp = summary.gripBreakdown.drop(1).take(3)
                if (runnersUp.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        runnersUp.forEach { stat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = iconForGrip(stat.gripType),
                                    contentDescription = stat.gripType.displayName,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stat.gripType.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${DurationFormatter.formatShort(stat.durationSeconds)} · ${stat.percentage}%",
                                    style = MonoCaption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyRhythmCard(summary: DailyStatsSummary) {
    val accents = LocalGripAccents.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hourly_rhythm_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accents.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoGraph,
                        contentDescription = "Hourly timeline",
                        tint = accents.bothHands,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "24-Hour Hand Rhythm",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = "Hand switches",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${summary.switchCount} switches",
                            style = MonoCaption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HourlyHandChart(hourlyStats = summary.hourlyBreakdown)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("12a", "6a", "12p", "6p", "11p").forEach { label ->
                    Text(
                        text = label,
                        style = MonoCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

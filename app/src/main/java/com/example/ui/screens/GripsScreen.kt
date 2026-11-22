package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.GripSegmentEntity
import com.example.model.DayHistoryItem
import com.example.model.DurationFormatter
import com.example.model.GripType
import com.example.model.GripUsageStat
import com.example.model.HandType
import com.example.model.StrainLevel
import com.example.ui.GripPulseUiState
import com.example.ui.components.iconForGrip
import com.example.ui.theme.LocalGripAccents
import com.example.ui.theme.MonoCaption
import com.example.ui.theme.MonoMetricMedium
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GripsScreen(
    state: GripPulseUiState,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accents = LocalGripAccents.current
    val summary = state.dailySummary
    val liveGrip = state.liveSnapshot.gripType

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .testTag("grips_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Grip Analytics & History",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Compare posture frequency, thumb strain, and 7-day hold trends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 7-Day Selector Strip
        item {
            WeeklyHistoryStrip(
                history = state.weeklyHistory,
                selectedDateKey = state.selectedDateKey,
                onSelectDate = onSelectDate
            )
        }

        // Ranked Grip Frequency Cards
        item {
            Text(
                text = if (state.selectedDateKey == state.todayDateKey) {
                    "Today's Grip Frequency"
                } else {
                    "Grip Breakdown · ${state.selectedDateKey}"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        val breakdown = if (summary.gripBreakdown.isNotEmpty()) {
            summary.gripBreakdown
        } else {
            GripType.activeGrips.map { GripUsageStat(it, 0L, 0) }
        }

        items(breakdown, key = { it.gripType.name }) { stat ->
            val isCurrentlyActive =
                state.selectedDateKey == state.todayDateKey && stat.gripType == liveGrip
            GripDetailCard(
                stat = stat,
                isActiveNow = isCurrentlyActive
            )
        }

        // Recent Real Telemetry Switches Log
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = "Recent hold sessions",
                    tint = accents.bothHands,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Recorded Holds",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (state.recentSegments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, accents.cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = "Waiting for telemetry",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No hold segments logged yet. Hold or tilt your device to start recording live grip sessions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.recentSegments.take(15), key = { it.id }) { seg ->
                RecentSegmentRow(segment = seg)
            }
        }
    }
}

@Composable
private fun WeeklyHistoryStrip(
    history: List<DayHistoryItem>,
    selectedDateKey: String,
    onSelectDate: (String) -> Unit
) {
    val accents = LocalGripAccents.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_history_strip")
    ) {
        items(history, key = { it.dateKey }) { day ->
            val selected = day.dateKey == selectedDateKey
            val dominantColor = when {
                day.totalHoldSeconds <= 0L -> MaterialTheme.colorScheme.outline
                day.rightSeconds >= day.leftSeconds && day.rightSeconds >= day.bothSeconds -> accents.rightHand
                day.leftSeconds >= day.rightSeconds && day.leftSeconds >= day.bothSeconds -> accents.leftHand
                else -> accents.bothHands
            }

            Surface(
                modifier = Modifier
                    .width(88.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { onSelectDate(day.dateKey) }
                    .testTag("history_day_${day.dateKey}"),
                shape = RoundedCornerShape(26.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else accents.cardBorder
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = if (day.totalHoldSeconds > 0L) {
                            DurationFormatter.formatShort(day.totalHoldSeconds)
                        } else {
                            "—"
                        },
                        style = MonoCaption,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dominantColor)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GripDetailCard(
    stat: GripUsageStat,
    isActiveNow: Boolean
) {
    val accents = LocalGripAccents.current
    val strainColor = when (stat.gripType.strainLevel) {
        StrainLevel.LOW -> accents.leftHand
        StrainLevel.MODERATE -> accents.bothHands
        StrainLevel.ELEVATED -> accents.rightHand
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grip_card_${stat.gripType.name}"),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isActiveNow) 1.5.dp else 1.dp,
            color = if (isActiveNow) accents.livePulse else accents.cardBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(strainColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForGrip(stat.gripType),
                        contentDescription = stat.gripType.displayName,
                        tint = strainColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stat.gripType.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isActiveNow) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = accents.livePulse.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "NOW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accents.livePulse,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = stat.gripType.strainLevel.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = strainColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stat.percentage}%",
                        style = MonoMetricMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = DurationFormatter.formatCompact(stat.durationSeconds),
                        style = MonoCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearWavyProgressIndicator(
                progress = { (stat.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = strainColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            Text(
                text = stat.gripType.ergonomicTip,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentSegmentRow(segment: GripSegmentEntity) {
    val accents = LocalGripAccents.current
    val hand = HandType.fromName(segment.hand)
    val grip = GripType.fromName(segment.gripType)
    val handColor = accents.colorForHand(hand)
    val timeLabel = remember(segment.timestampMillis) {
        SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(segment.timestampMillis))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accents.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(handColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${hand.displayName} · ${grip.displayName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "%s · Roll %+.0f° · Pitch %.0f°",
                        timeLabel,
                        segment.avgRollDeg,
                        segment.avgPitchDeg
                    ),
                    style = MonoCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "+${DurationFormatter.formatCompact(segment.durationSeconds)}",
                style = MonoCaption,
                color = handColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

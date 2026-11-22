package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AdaptiveModelStats
import com.example.model.HandType
import com.example.ui.AiStudyUiState
import com.example.ui.CalibrationCaptureState
import com.example.ui.GripPulseUiState
import com.example.ui.components.iconForHand
import com.example.ui.theme.LocalGripAccents
import com.example.ui.theme.MonoCaption
import com.example.ui.theme.MonoMetricMedium
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalibrateScreen(
    state: GripPulseUiState,
    onCaptureCalibration: (HandType) -> Unit,
    onRunAiPatternStudy: () -> Unit,
    onUpdateDeadzone: (Float) -> Unit,
    onResetCalibration: () -> Unit,
    onToggleBackgroundTracking: () -> Unit,
    onResetTodayStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accents = LocalGripAccents.current
    val cal = state.calibrationProfile
    val live = state.liveSnapshot
    val adaptive = state.adaptiveModelStats
    val isCapturing = state.calibrationCaptureState is CalibrationCaptureState.Capturing
    val capturingHand = (state.calibrationCaptureState as? CalibrationCaptureState.Capturing)?.targetHand

    var showResetConfirm by remember { mutableStateOf(false) }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Clear Today's Stats?") },
            text = {
                Text("This will erase today's recorded hand-hold time and grip frequency statistics from local storage.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetTodayStats()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear Today")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .testTag("calibrate_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "AI Learning & Calibration",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Multi-axis pattern engine that studies your grip and improves over time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Adaptive Pattern Learning Engine Card + AI Pattern Study Report
        item {
            AdaptiveIntelligenceCard(
                adaptive = adaptive,
                aiStudyState = state.aiStudyState,
                onRunAiPatternStudy = onRunAiPatternStudy
            )
        }

        // 2. Guided 3-Posture Multi-Axis Calibration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Multi-axis calibration",
                                tint = accents.leftHand,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Multi-Axis Hand Capture",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (adaptive.isAdaptiveActive) {
                                accents.livePulse.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        ) {
                            Text(
                                text = if (adaptive.isAdaptiveActive) "ADAPTIVE" else "BASE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (adaptive.isAdaptiveActive) {
                                    accents.livePulse
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Hold your phone naturally in each hand and tap Capture (2.2s). GripPulse records full 6-axis gravity, pitch-independent roll, and gyro spread.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    CalibrationStepRow(
                        hand = HandType.LEFT,
                        learnedRollDeg = adaptive.learnedLeftRollDeg ?: cal.leftRollBaselineDeg,
                        sampleCount = adaptive.leftSamples,
                        accent = accents.leftHand,
                        isCapturingThis = capturingHand == HandType.LEFT,
                        anyCapturing = isCapturing,
                        onCapture = { onCaptureCalibration(HandType.LEFT) }
                    )

                    CalibrationStepRow(
                        hand = HandType.RIGHT,
                        learnedRollDeg = adaptive.learnedRightRollDeg ?: cal.rightRollBaselineDeg,
                        sampleCount = adaptive.rightSamples,
                        accent = accents.rightHand,
                        isCapturingThis = capturingHand == HandType.RIGHT,
                        anyCapturing = isCapturing,
                        onCapture = { onCaptureCalibration(HandType.RIGHT) }
                    )

                    CalibrationStepRow(
                        hand = HandType.BOTH,
                        learnedRollDeg = adaptive.learnedBothRollDeg ?: cal.twoHandRollBaselineDeg,
                        sampleCount = adaptive.bothSamples,
                        accent = accents.bothHands,
                        isCapturingThis = capturingHand == HandType.BOTH,
                        anyCapturing = isCapturing,
                        onCapture = { onCaptureCalibration(HandType.BOTH) }
                    )

                    // Neutral Two-Hand Deadzone Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Base Two-Hand Deadzone",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "±${cal.deadzoneDeg.roundToInt()}°",
                                style = MonoCaption,
                                color = accents.bothHands,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = cal.deadzoneDeg,
                            onValueChange = onUpdateDeadzone,
                            valueRange = 2.5f..12f,
                            modifier = Modifier.testTag("deadzone_slider")
                        )
                    }

                    OutlinedButton(
                        onClick = onResetCalibration,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_calibration_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Learned Patterns & Calibration")
                    }
                }
            }
        }

        // 3. Live Raw Hardware Sensor Telemetry Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, accents.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Sensors,
                            contentDescription = "Raw hardware telemetry",
                            tint = accents.highlight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Pitch-Independent Sensors",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticTile(
                            label = "GRAVITY X",
                            value = String.format(Locale.US, "%+.2f", live.accelX),
                            unit = "m/s²",
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticTile(
                            label = "GRAVITY Y",
                            value = String.format(Locale.US, "%+.2f", live.accelY),
                            unit = "m/s²",
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticTile(
                            label = "GRAVITY Z",
                            value = String.format(Locale.US, "%+.2f", live.accelZ),
                            unit = "m/s²",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticTile(
                            label = "ROLL TILT",
                            value = String.format(Locale.US, "%+.1f°", live.rollDeg),
                            unit = "YZ-stable",
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticTile(
                            label = "PITCH",
                            value = String.format(Locale.US, "%.1f°", live.pitchDeg),
                            unit = "upright",
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticTile(
                            label = "SPREAD",
                            value = String.format(Locale.US, "%.1f°", live.rollSpreadDeg),
                            unit = "std dev",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SensorChip(
                            label = if (live.hasAccelerometer) "Gravity / Accel OK" else "Accel Missing",
                            active = live.hasAccelerometer
                        )
                        SensorChip(
                            label = if (live.hasGyroscope) "Gyroscope OK" else "Gyro Fallback",
                            active = live.hasGyroscope
                        )
                    }
                }
            }
        }

        // 4. Background Tracking & Data Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, accents.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Background Telemetry Service",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Continuously monitors hand holds across apps and refreshes widgets in real time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = state.isBackgroundTracking,
                            onCheckedChange = { onToggleBackgroundTracking() },
                            modifier = Modifier.testTag("settings_bg_switch")
                        )
                    }

                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_today_stats_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Today's Hold Stats")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AdaptiveIntelligenceCard(
    adaptive: AdaptiveModelStats,
    aiStudyState: AiStudyUiState,
    onRunAiPatternStudy: () -> Unit
) {
    val accents = LocalGripAccents.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adaptive_ai_card"),
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.25.dp, accents.leftHand.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(MaterialShapes.SoftBurst.toShape())
                        .background(accents.leftHand.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = "Adaptive pattern engine",
                        tint = accents.leftHand,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ADAPTIVE PATTERN MODEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = accents.leftHand
                    )
                    Text(
                        text = adaptive.stageLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${adaptive.maturityPercent}%",
                    style = MonoMetricMedium,
                    color = accents.leftHand,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearWavyProgressIndicator(
                progress = { (adaptive.maturityPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = accents.leftHand,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagnosticTile(
                    label = "VERIFIED",
                    value = "${adaptive.userVerifiedSamples}",
                    unit = "user taught",
                    modifier = Modifier.weight(1f)
                )
                DiagnosticTile(
                    label = "SELF-LEARNED",
                    value = "${adaptive.autoLearnedSamples}",
                    unit = "steady holds",
                    modifier = Modifier.weight(1f)
                )
                DiagnosticTile(
                    label = "TOTAL",
                    value = "${adaptive.totalSamples}",
                    unit = "patterns",
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = onRunAiPatternStudy,
                enabled = aiStudyState !is AiStudyUiState.Analyzing,
                shape = CircleShape,
                contentPadding = PaddingValues(vertical = 13.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("run_ai_study_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (aiStudyState is AiStudyUiState.Analyzing) {
                        "Studying Patterns..."
                    } else {
                        "Study My Grip Patterns (AI Report)"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }

            when (aiStudyState) {
                AiStudyUiState.Idle -> Unit
                AiStudyUiState.Analyzing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
                is AiStudyUiState.Ready -> {
                    val report = aiStudyState.report
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = report.headline,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = report.calibrationAdvice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        report.ergonomicRecommendations.forEach { rec ->
                            Text(
                                text = "• $rec",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (report.isPlaceholder) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accents.highlight.copy(alpha = 0.14f))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "Firebase notice",
                                    tint = accents.highlight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = report.notice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accents.highlight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CalibrationStepRow(
    hand: HandType,
    learnedRollDeg: Float,
    sampleCount: Int,
    accent: Color,
    isCapturingThis: Boolean,
    anyCapturing: Boolean,
    onCapture: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(accent.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForHand(hand),
                    contentDescription = hand.displayName,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hand.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "Cluster %+.1f° · %d samples",
                        learnedRollDeg,
                        sampleCount
                    ),
                    style = MonoCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onCapture,
                enabled = !anyCapturing,
                shape = CircleShape,
                modifier = Modifier.testTag("capture_${hand.name.lowercase(Locale.US)}_btn"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (isCapturingThis) {
                    LoadingIndicator(modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hold...")
                } else {
                    Text("Capture")
                }
            }
        }
    }
}

@Composable
private fun DiagnosticTile(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MonoMetricMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SensorChip(label: String, active: Boolean) {
    val accents = LocalGripAccents.current
    val tint = if (active) accents.livePulse else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = CircleShape,
        color = tint.copy(alpha = 0.14f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

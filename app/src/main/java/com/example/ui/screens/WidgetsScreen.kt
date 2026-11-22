package com.example.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DurationFormatter
import com.example.ui.GripPulseUiState
import com.example.ui.theme.LocalGripAccents
import com.example.ui.theme.MonoCaption
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.widget.GripCompactWidgetProvider
import com.example.widget.GripInsightsWidgetProvider

@Composable
fun WidgetsScreen(
    state: GripPulseUiState,
    onToggleBackgroundTracking: () -> Unit,
    onSyncWidgetsNow: () -> Unit,
    onPinResultMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accents = LocalGripAccents.current
    val summary = state.dailySummary
    val live = state.liveSnapshot

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .testTag("widgets_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Hero Art Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, accents.cardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_grip_ergonomics_hero_1795344122965),
                        contentDescription = "Ergonomic hand grip telemetry artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ObsidianBg.copy(alpha = 0.2f),
                                        ObsidianBg.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Widgets,
                                contentDescription = "Home widgets",
                                tint = accents.leftHand,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "REAL-TIME HOME SCREEN WIDGETS",
                                style = MaterialTheme.typography.labelSmall,
                                color = accents.leftHand
                            )
                        }
                        Text(
                            text = "Live Grip Insights on Your Home Screen",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SlateTextPrimary
                        )
                        Text(
                            text = "Pin either widget below to watch your hand split and #1 grip update in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )
                    }
                }
            }
        }

        // Widget 1: Compact Live (2x2) Preview & Pin
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, accents.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Compact Live Widget",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "2×2 · Current hand, grip & split summary",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "2×2",
                            style = MonoCaption,
                            color = accents.leftHand,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Faithful live Compose preview of the 2x2 Android Widget
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.width(196.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, ObsidianOutline)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "GRIPPULSE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateTextSecondary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = accents.leftHand.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, accents.leftHand)
                                    ) {
                                        Text(
                                            text = if (state.isBackgroundTracking) "LIVE" else "TAP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accents.leftHand,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = live.hand.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SlateTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = live.gripType.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accents.rightHand,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = ObsidianSurfaceHigh,
                                    border = BorderStroke(1.dp, ObsidianOutline)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(9.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Today",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SlateTextSecondary
                                            )
                                            Text(
                                                text = DurationFormatter.formatCompact(summary.totalHoldSeconds),
                                                style = MonoCaption,
                                                color = SlateTextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "L ${summary.leftPercent}% · R ${summary.rightPercent}% · 2H ${summary.bothPercent}%",
                                            style = MonoCaption,
                                            color = SlateTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Top: ${summary.topGrip?.gripType?.shortName ?: "Ready"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accents.leftHand,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            requestPinWidget(
                                context = context,
                                providerClass = GripCompactWidgetProvider::class.java,
                                widgetTitle = "Compact Live Widget",
                                onMessage = onPinResultMessage
                            )
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_compact_widget_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddToHomeScreen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pin Compact Widget to Home Screen")
                    }
                }
            }
        }

        // Widget 2: Detailed Daily Insights (4x2) Preview & Pin
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, accents.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Insights Widget",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "4×2 · Full Left / Right / 2H bars + Top Grip card",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "4×2",
                            style = MonoCaption,
                            color = accents.rightHand,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Faithful live Compose preview of the 4x2 Android Widget
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, ObsidianOutline)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "GRIPPULSE · TODAY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateTextSecondary
                                    )
                                    Text(
                                        text = "Now: ${live.hand.displayName} · ${DurationFormatter.formatCompact(summary.totalHoldSeconds)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = SlateTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = accents.leftHand.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, accents.leftHand)
                                ) {
                                    Text(
                                        text = if (state.isBackgroundTracking) "LIVE ●" else "START",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accents.leftHand,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1.25f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    WidgetPreviewBar(
                                        label = "Left Hand",
                                        value = "${DurationFormatter.formatShort(summary.leftSeconds)} · ${summary.leftPercent}%",
                                        percent = summary.leftPercent,
                                        color = accents.leftHand
                                    )
                                    WidgetPreviewBar(
                                        label = "Right Hand",
                                        value = "${DurationFormatter.formatShort(summary.rightSeconds)} · ${summary.rightPercent}%",
                                        percent = summary.rightPercent,
                                        color = accents.rightHand
                                    )
                                    WidgetPreviewBar(
                                        label = "Two Hands",
                                        value = "${DurationFormatter.formatShort(summary.bothSeconds)} · ${summary.bothPercent}%",
                                        percent = summary.bothPercent,
                                        color = accents.bothHands
                                    )
                                }

                                Surface(
                                    modifier = Modifier.weight(0.95f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = ObsidianSurfaceHigh,
                                    border = BorderStroke(1.dp, ObsidianOutline)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "TOP GRIP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SlateTextSecondary
                                        )
                                        Text(
                                            text = summary.topGrip?.gripType?.displayName ?: "Hold phone",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = SlateTextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (summary.topGrip != null) {
                                                "${DurationFormatter.formatShort(summary.topGrip!!.durationSeconds)} · ${summary.topGrip!!.percentage}%"
                                            } else {
                                                "0s · 0% share"
                                            },
                                            style = MonoCaption,
                                            color = accents.highlight,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            requestPinWidget(
                                context = context,
                                providerClass = GripInsightsWidgetProvider::class.java,
                                widgetTitle = "Daily Insights Widget",
                                onMessage = onPinResultMessage
                            )
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_insights_widget_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddToHomeScreen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pin Daily Insights Widget (4×2)")
                    }
                }
            }
        }

        // Real-Time Widget Sync Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, accents.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Live Widget Sync Engine",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Keep the background telemetry service enabled so both home screen widgets update automatically whenever your hand or grip changes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onToggleBackgroundTracking,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("widgets_toggle_service_btn")
                        ) {
                            Icon(
                                imageVector = if (state.isBackgroundTracking) Icons.Rounded.StopCircle else Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.isBackgroundTracking) "Pause Live" else "Start Live")
                        }
                        OutlinedButton(
                            onClick = onSyncWidgetsNow,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("widgets_sync_now_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPreviewBar(
    label: String,
    value: String,
    percent: Int,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MonoCaption,
                color = SlateTextPrimary
            )
        }
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = ObsidianSurfaceHigh,
            strokeCap = StrokeCap.Round
        )
    }
}

private fun requestPinWidget(
    context: Context,
    providerClass: Class<*>,
    widgetTitle: String,
    onMessage: (String) -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, providerClass)
        if (manager.isRequestPinAppWidgetSupported) {
            val accepted = manager.requestPinAppWidget(provider, null, null)
            if (accepted) {
                onMessage("Follow your home screen prompt to place $widgetTitle")
            } else {
                onMessage("Long-press your home screen → Widgets → GripPulse to add $widgetTitle")
            }
        } else {
            onMessage("Long-press your home screen → Widgets → GripPulse to add $widgetTitle")
        }
    } else {
        onMessage("Long-press your home screen → Widgets → GripPulse to add $widgetTitle")
    }
}

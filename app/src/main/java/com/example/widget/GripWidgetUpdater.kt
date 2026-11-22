package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.model.DurationFormatter
import com.example.model.WidgetDisplayState

object GripWidgetUpdater {

    const val ACTION_WIDGET_REFRESH = "com.example.action.WIDGET_REFRESH"
    const val ACTION_WIDGET_TOGGLE_TRACKING = "com.example.action.WIDGET_TOGGLE_TRACKING"

    fun updateAllWidgets(context: Context, state: WidgetDisplayState) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val compactComponent = ComponentName(context, GripCompactWidgetProvider::class.java)
        val compactIds = appWidgetManager.getAppWidgetIds(compactComponent)
        if (compactIds.isNotEmpty()) {
            val compactViews = buildCompactViews(context, state)
            compactIds.forEach { id ->
                appWidgetManager.updateAppWidget(id, compactViews)
            }
        }

        val insightsComponent = ComponentName(context, GripInsightsWidgetProvider::class.java)
        val insightsIds = appWidgetManager.getAppWidgetIds(insightsComponent)
        if (insightsIds.isNotEmpty()) {
            val insightsViews = buildInsightsViews(context, state)
            insightsIds.forEach { id ->
                appWidgetManager.updateAppWidget(id, insightsViews)
            }
        }
    }

    fun buildCompactViews(context: Context, state: WidgetDisplayState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_grip_compact)

        views.setTextViewText(
            R.id.widget_compact_status_pill,
            if (state.isTrackingServiceRunning) "LIVE" else "TAP TO TRACK"
        )
        views.setTextViewText(
            R.id.widget_compact_live_hand,
            state.liveHand.displayName
        )
        views.setTextViewText(
            R.id.widget_compact_live_grip,
            state.liveGrip.displayName
        )
        views.setTextViewText(
            R.id.widget_compact_total_time,
            DurationFormatter.formatCompact(state.totalHoldSeconds)
        )
        views.setTextViewText(
            R.id.widget_compact_split_summary,
            "L ${state.leftPercent}% · R ${state.rightPercent}% · 2H ${state.bothPercent}%"
        )
        views.setTextViewText(
            R.id.widget_compact_top_grip,
            "Top: ${state.topGripName}"
        )

        views.setOnClickPendingIntent(
            R.id.widget_compact_root,
            openAppPendingIntent(context, requestCode = 101)
        )
        views.setOnClickPendingIntent(
            R.id.widget_compact_status_pill,
            toggleTrackingPendingIntent(context, GripCompactWidgetProvider::class.java, requestCode = 102)
        )
        return views
    }

    fun buildInsightsViews(context: Context, state: WidgetDisplayState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_grip_insights)

        val totalFormatted = DurationFormatter.formatCompact(state.totalHoldSeconds)
        views.setTextViewText(
            R.id.widget_insights_live_state,
            "Now: ${state.liveHand.displayName} · $totalFormatted today"
        )
        views.setTextViewText(
            R.id.widget_insights_toggle_btn,
            if (state.isTrackingServiceRunning) "LIVE ●" else "START"
        )

        views.setTextViewText(
            R.id.widget_left_value,
            "${DurationFormatter.formatShort(state.leftSeconds)} · ${state.leftPercent}%"
        )
        views.setProgressBar(R.id.widget_left_progress, 100, state.leftPercent, false)

        views.setTextViewText(
            R.id.widget_right_value,
            "${DurationFormatter.formatShort(state.rightSeconds)} · ${state.rightPercent}%"
        )
        views.setProgressBar(R.id.widget_right_progress, 100, state.rightPercent, false)

        views.setTextViewText(
            R.id.widget_both_value,
            "${DurationFormatter.formatShort(state.bothSeconds)} · ${state.bothPercent}%"
        )
        views.setProgressBar(R.id.widget_both_progress, 100, state.bothPercent, false)

        views.setTextViewText(R.id.widget_top_grip_name, state.topGripName)
        val topStatsText = if (state.topGripSeconds > 0L) {
            "${DurationFormatter.formatShort(state.topGripSeconds)} · ${state.topGripPercent}% share"
        } else {
            "0s · No hold yet"
        }
        views.setTextViewText(R.id.widget_top_grip_stats, topStatsText)
        views.setTextViewText(R.id.widget_updated_at, state.updatedTimeText)

        views.setOnClickPendingIntent(
            R.id.widget_insights_root,
            openAppPendingIntent(context, requestCode = 201)
        )
        views.setOnClickPendingIntent(
            R.id.widget_insights_toggle_btn,
            toggleTrackingPendingIntent(context, GripInsightsWidgetProvider::class.java, requestCode = 202)
        )
        return views
    }

    private fun openAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun toggleTrackingPendingIntent(
        context: Context,
        providerClass: Class<*>,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, providerClass).apply {
            action = ACTION_WIDGET_TOGGLE_TRACKING
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

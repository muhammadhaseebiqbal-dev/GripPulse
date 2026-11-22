package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.example.GripPulseApp
import com.example.sensor.GripTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GripInsightsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as? GripPulseApp ?: return
        val cached = app.preferences.readCachedWidgetState()
        val views = GripWidgetUpdater.buildInsightsViews(context, cached)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
        CoroutineScope(Dispatchers.IO).launch {
            app.sensorEngine.pushWidgetUpdate()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val app = context.applicationContext as? GripPulseApp ?: return
        when (intent.action) {
            GripWidgetUpdater.ACTION_WIDGET_TOGGLE_TRACKING -> {
                if (app.sensorEngine.isServiceRunning.value) {
                    GripTrackingService.stop(context)
                } else {
                    GripTrackingService.start(context)
                }
            }
            GripWidgetUpdater.ACTION_WIDGET_REFRESH -> {
                CoroutineScope(Dispatchers.IO).launch {
                    app.sensorEngine.pushWidgetUpdate()
                }
            }
        }
    }
}

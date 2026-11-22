package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CalibrationProfile
import com.example.model.GripType
import com.example.model.HandType
import com.example.model.WidgetDisplayState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gripDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "grippulse_prefs"
)

class GripPreferencesRepository(private val context: Context) {

    private val widgetPrefs = context.getSharedPreferences(
        "grippulse_widget_cache",
        Context.MODE_PRIVATE
    )

    private object Keys {
        val LEFT_ROLL = floatPreferencesKey("cal_left_roll")
        val RIGHT_ROLL = floatPreferencesKey("cal_right_roll")
        val TWO_HAND_ROLL = floatPreferencesKey("cal_two_hand_roll")
        val DEADZONE = floatPreferencesKey("cal_deadzone")
        val IS_CALIBRATED = booleanPreferencesKey("cal_is_calibrated")
        val BACKGROUND_TRACKING = booleanPreferencesKey("background_tracking_enabled")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_one_hand_limit_minutes")
    }

    val calibrationProfile: Flow<CalibrationProfile> = context.gripDataStore.data.map { prefs ->
        CalibrationProfile(
            leftRollBaselineDeg = prefs[Keys.LEFT_ROLL] ?: 11.5f,
            rightRollBaselineDeg = prefs[Keys.RIGHT_ROLL] ?: -11.5f,
            twoHandRollBaselineDeg = prefs[Keys.TWO_HAND_ROLL] ?: 0.0f,
            deadzoneDeg = prefs[Keys.DEADZONE] ?: 5.5f,
            isCalibrated = prefs[Keys.IS_CALIBRATED] ?: false
        )
    }

    val backgroundTrackingEnabled: Flow<Boolean> = context.gripDataStore.data.map { prefs ->
        prefs[Keys.BACKGROUND_TRACKING] ?: false
    }

    val dailyOneHandLimitMinutes: Flow<Int> = context.gripDataStore.data.map { prefs ->
        prefs[Keys.DAILY_GOAL_MINUTES] ?: 120
    }

    suspend fun saveCalibration(profile: CalibrationProfile) {
        context.gripDataStore.edit { prefs ->
            prefs[Keys.LEFT_ROLL] = profile.leftRollBaselineDeg
            prefs[Keys.RIGHT_ROLL] = profile.rightRollBaselineDeg
            prefs[Keys.TWO_HAND_ROLL] = profile.twoHandRollBaselineDeg
            prefs[Keys.DEADZONE] = profile.deadzoneDeg
            prefs[Keys.IS_CALIBRATED] = profile.isCalibrated
        }
    }

    suspend fun resetCalibration() {
        saveCalibration(CalibrationProfile())
    }

    suspend fun setBackgroundTrackingEnabled(enabled: Boolean) {
        context.gripDataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_TRACKING] = enabled
        }
        widgetPrefs.edit().putBoolean("is_tracking", enabled).apply()
    }

    suspend fun setDailyOneHandLimitMinutes(minutes: Int) {
        context.gripDataStore.edit { prefs ->
            prefs[Keys.DAILY_GOAL_MINUTES] = minutes.coerceIn(30, 360)
        }
    }

    fun cacheWidgetState(state: WidgetDisplayState) {
        widgetPrefs.edit()
            .putString("live_hand", state.liveHand.name)
            .putString("live_grip", state.liveGrip.name)
            .putBoolean("is_tracking", state.isTrackingServiceRunning)
            .putLong("total_sec", state.totalHoldSeconds)
            .putLong("left_sec", state.leftSeconds)
            .putLong("right_sec", state.rightSeconds)
            .putLong("both_sec", state.bothSeconds)
            .putInt("left_pct", state.leftPercent)
            .putInt("right_pct", state.rightPercent)
            .putInt("both_pct", state.bothPercent)
            .putString("top_grip_name", state.topGripName)
            .putLong("top_grip_sec", state.topGripSeconds)
            .putInt("top_grip_pct", state.topGripPercent)
            .putString("updated_text", state.updatedTimeText)
            .apply()
    }

    fun readCachedWidgetState(): WidgetDisplayState {
        return WidgetDisplayState(
            liveHand = HandType.fromName(widgetPrefs.getString("live_hand", HandType.RESTING.name)),
            liveGrip = GripType.fromName(widgetPrefs.getString("live_grip", GripType.RESTING.name)),
            isTrackingServiceRunning = widgetPrefs.getBoolean("is_tracking", false),
            totalHoldSeconds = widgetPrefs.getLong("total_sec", 0L),
            leftSeconds = widgetPrefs.getLong("left_sec", 0L),
            rightSeconds = widgetPrefs.getLong("right_sec", 0L),
            bothSeconds = widgetPrefs.getLong("both_sec", 0L),
            leftPercent = widgetPrefs.getInt("left_pct", 0),
            rightPercent = widgetPrefs.getInt("right_pct", 0),
            bothPercent = widgetPrefs.getInt("both_pct", 0),
            topGripName = widgetPrefs.getString("top_grip_name", "Ready to track") ?: "Ready to track",
            topGripSeconds = widgetPrefs.getLong("top_grip_sec", 0L),
            topGripPercent = widgetPrefs.getInt("top_grip_pct", 0),
            updatedTimeText = widgetPrefs.getString("updated_text", "Tap to open") ?: "Tap to open"
        )
    }
}

package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.GripAiAdvisor
import com.example.data.GripPreferencesRepository
import com.example.data.GripRepository
import com.example.data.GripSegmentEntity
import com.example.data.PatternSource
import com.example.model.AdaptiveModelStats
import com.example.model.AiPatternStudyReport
import com.example.model.CalibrationProfile
import com.example.model.DailyStatsSummary
import com.example.model.DayHistoryItem
import com.example.model.GripType
import com.example.model.HandType
import com.example.model.LiveGripSnapshot
import com.example.sensor.GripSensorEngine
import com.example.sensor.GripTrackingService
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CalibrationCaptureState {
    data object Idle : CalibrationCaptureState
    data class Capturing(val targetHand: HandType) : CalibrationCaptureState
}

sealed interface AiStudyUiState {
    data object Idle : AiStudyUiState
    data object Analyzing : AiStudyUiState
    data class Ready(val report: AiPatternStudyReport) : AiStudyUiState
}

data class GripPulseUiState(
    val liveSnapshot: LiveGripSnapshot = LiveGripSnapshot(),
    val adaptiveModelStats: AdaptiveModelStats = AdaptiveModelStats(),
    val aiStudyState: AiStudyUiState = AiStudyUiState.Idle,
    val selectedDateKey: String = "",
    val todayDateKey: String = "",
    val dailySummary: DailyStatsSummary = DailyStatsSummary(dateKey = ""),
    val weeklyHistory: List<DayHistoryItem> = emptyList(),
    val recentSegments: List<GripSegmentEntity> = emptyList(),
    val calibrationProfile: CalibrationProfile = CalibrationProfile(),
    val isBackgroundTracking: Boolean = false,
    val dailyOneHandLimitMinutes: Int = 120,
    val calibrationCaptureState: CalibrationCaptureState = CalibrationCaptureState.Idle,
    val statusMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class GripPulseViewModel(
    private val repository: GripRepository,
    private val preferences: GripPreferencesRepository,
    private val sensorEngine: GripSensorEngine,
    private val aiAdvisor: GripAiAdvisor
) : ViewModel() {

    private val todayKey = repository.todayDateKey()
    private val selectedDateKeyFlow = MutableStateFlow(todayKey)
    private val captureStateFlow = MutableStateFlow<CalibrationCaptureState>(CalibrationCaptureState.Idle)
    private val aiStudyStateFlow = MutableStateFlow<AiStudyUiState>(AiStudyUiState.Idle)
    private val statusMessageFlow = MutableStateFlow<String?>(null)

    private val selectedDailySummaryFlow = selectedDateKeyFlow.flatMapLatest { dateKey ->
        repository.observeDailySummary(dateKey)
    }

    private val coreTelemetryFlow = combine(
        sensorEngine.liveSnapshot,
        selectedDateKeyFlow,
        selectedDailySummaryFlow,
        repository.observeWeeklyHistory(),
        repository.observeRecentSegments(25)
    ) { live, selectedDate, summary, history, recent ->
        CoreTelemetryBundle(live, selectedDate, summary, history, recent)
    }

    private val adaptiveBundleFlow = combine(
        sensorEngine.adaptiveStats,
        aiStudyStateFlow,
        preferences.calibrationProfile,
        sensorEngine.isServiceRunning,
        preferences.dailyOneHandLimitMinutes
    ) { adaptiveStats, aiStudy, calibration, isServiceRunning, limitMinutes ->
        AdaptiveBundle(adaptiveStats, aiStudy, calibration, isServiceRunning, limitMinutes)
    }

    val uiState: StateFlow<GripPulseUiState> = combine(
        coreTelemetryFlow,
        adaptiveBundleFlow,
        captureStateFlow,
        statusMessageFlow
    ) { core, adaptive, captureState, statusMsg ->
        GripPulseUiState(
            liveSnapshot = core.live,
            adaptiveModelStats = adaptive.stats,
            aiStudyState = adaptive.aiStudy,
            selectedDateKey = core.selectedDate,
            todayDateKey = todayKey,
            dailySummary = core.summary,
            weeklyHistory = core.history,
            recentSegments = core.recent,
            calibrationProfile = adaptive.calibration,
            isBackgroundTracking = adaptive.isServiceRunning,
            dailyOneHandLimitMinutes = adaptive.limitMinutes,
            calibrationCaptureState = captureState,
            statusMessage = statusMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GripPulseUiState(
            selectedDateKey = todayKey,
            todayDateKey = todayKey,
            dailySummary = DailyStatsSummary(dateKey = todayKey)
        )
    )

    fun selectDate(dateKey: String) {
        selectedDateKeyFlow.value = dateKey
    }

    fun toggleBackgroundTracking(context: Context) {
        if (sensorEngine.isServiceRunning.value) {
            GripTrackingService.stop(context)
            statusMessageFlow.value = "Background tracking paused"
        } else {
            GripTrackingService.start(context)
            statusMessageFlow.value = "Live background tracking active — widgets updating in real time"
        }
    }

    /**
     * One-tap live correction/confirmation: feeds the current multi-axis sensor vector
     * into the adaptive pattern model so classification immediately improves.
     */
    fun teachCurrentHand(hand: HandType) {
        viewModelScope.launch {
            sensorEngine.teachCurrentPosture(hand)
            statusMessageFlow.value = "Pattern learned for ${hand.displayName} — detection accuracy updated"
        }
    }

    fun captureCalibrationFor(targetHand: HandType) {
        if (captureStateFlow.value is CalibrationCaptureState.Capturing) return
        viewModelScope.launch {
            captureStateFlow.value = CalibrationCaptureState.Capturing(targetHand)
            val measured = sensorEngine.captureFeatureWindow(2200L)

            val gripForCal = when (targetHand) {
                HandType.BOTH -> GripType.TWO_HAND_PORTRAIT
                HandType.LEFT, HandType.RIGHT -> if (measured.pitchDeg <= 38f) {
                    GripType.CRADLE_SUPPORT
                } else {
                    GripType.ONE_HAND_THUMB
                }
                HandType.RESTING -> GripType.RESTING
            }

            // Persist a high-weight multi-axis calibration pattern sample into Room
            repository.savePatternSample(
                hand = targetHand,
                gripType = gripForCal,
                features = measured,
                source = PatternSource.CALIBRATION,
                weight = 3.0f
            )

            // Also sync scalar roll baseline in preferences for diagnostics display
            val current = uiState.value.calibrationProfile
            val updated = when (targetHand) {
                HandType.LEFT -> current.copy(
                    leftRollBaselineDeg = measured.rollDeg,
                    isCalibrated = true
                )
                HandType.RIGHT -> current.copy(
                    rightRollBaselineDeg = measured.rollDeg,
                    isCalibrated = true
                )
                HandType.BOTH -> current.copy(
                    twoHandRollBaselineDeg = measured.rollDeg,
                    isCalibrated = true
                )
                HandType.RESTING -> current
            }
            preferences.saveCalibration(updated)

            captureStateFlow.value = CalibrationCaptureState.Idle
            statusMessageFlow.value = String.format(
                Locale.US,
                "Learned %s pattern (Roll %+.1f° · Pitch %.0f°)",
                targetHand.displayName,
                measured.rollDeg,
                measured.pitchDeg
            )
        }
    }

    fun runAiPatternStudy() {
        if (aiStudyStateFlow.value is AiStudyUiState.Analyzing) return
        viewModelScope.launch {
            aiStudyStateFlow.value = AiStudyUiState.Analyzing
            val snapshot = uiState.value
            val samples = repository.getPatternSamples(120)
            val report = aiAdvisor.studyPatterns(
                summary = snapshot.dailySummary,
                modelStats = snapshot.adaptiveModelStats,
                samples = samples
            )
            aiStudyStateFlow.value = AiStudyUiState.Ready(report)
        }
    }

    fun updateDeadzone(deadzoneDeg: Float) {
        viewModelScope.launch {
            val current = uiState.value.calibrationProfile
            preferences.saveCalibration(current.copy(deadzoneDeg = deadzoneDeg))
        }
    }

    fun resetCalibration() {
        viewModelScope.launch {
            preferences.resetCalibration()
            repository.clearPatternSamples()
            aiStudyStateFlow.value = AiStudyUiState.Idle
            statusMessageFlow.value = "Adaptive model & calibration reset to base physics"
        }
    }

    fun resetTodayStats() {
        viewModelScope.launch {
            repository.resetDate(todayKey)
            sensorEngine.pushWidgetUpdate()
            statusMessageFlow.value = "Today's hold statistics cleared"
        }
    }

    fun syncWidgetsNow() {
        viewModelScope.launch {
            sensorEngine.pushWidgetUpdate()
            statusMessageFlow.value = "Home screen widgets synced with latest stats"
        }
    }

    fun postMessage(message: String) {
        statusMessageFlow.value = message
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    private data class CoreTelemetryBundle(
        val live: LiveGripSnapshot,
        val selectedDate: String,
        val summary: DailyStatsSummary,
        val history: List<DayHistoryItem>,
        val recent: List<GripSegmentEntity>
    )

    private data class AdaptiveBundle(
        val stats: AdaptiveModelStats,
        val aiStudy: AiStudyUiState,
        val calibration: CalibrationProfile,
        val isServiceRunning: Boolean,
        val limitMinutes: Int
    )

    companion object {
        fun factory(
            repository: GripRepository,
            preferences: GripPreferencesRepository,
            sensorEngine: GripSensorEngine,
            aiAdvisor: GripAiAdvisor
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GripPulseViewModel(repository, preferences, sensorEngine, aiAdvisor) as T
            }
        }
    }
}

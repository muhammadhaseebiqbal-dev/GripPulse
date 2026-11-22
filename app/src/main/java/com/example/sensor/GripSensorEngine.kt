package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.data.GripPreferencesRepository
import com.example.data.GripRepository
import com.example.data.PatternSource
import com.example.model.AdaptiveModelStats
import com.example.model.CalibrationProfile
import com.example.model.GripType
import com.example.model.HandType
import com.example.model.LiveGripSnapshot
import com.example.model.SensorFeatureVector
import com.example.model.WidgetDisplayState
import com.example.widget.GripWidgetUpdater
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GripSensorEngine(
    private val context: Context,
    private val repository: GripRepository,
    private val preferences: GripPreferencesRepository
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscopeSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _liveSnapshot = MutableStateFlow(
        LiveGripSnapshot(
            hasAccelerometer = (gravitySensor != null || accelerometerSensor != null),
            hasGyroscope = (gyroscopeSensor != null)
        )
    )
    val liveSnapshot: StateFlow<LiveGripSnapshot> = _liveSnapshot.asStateFlow()

    private val _adaptiveStats = MutableStateFlow(AdaptiveModelStats())
    val adaptiveStats: StateFlow<AdaptiveModelStats> = _adaptiveStats.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    @Volatile
    private var calibrationProfile: CalibrationProfile = CalibrationProfile()

    @Volatile
    private var adaptiveModel: AdaptiveGripModel = AdaptiveGripModel()

    @Volatile
    private var latestFeatures: SensorFeatureVector = SensorFeatureVector()

    private var activeConsumers = 0
    private var isRegistered = false

    // Filtered sensor state
    private var filteredGx = 0f
    private var filteredGy = 0f
    private var filteredGz = 9.81f
    private var hasGravitySample = false
    private var latestGyroMag = 0f
    private var isNearProximity = false

    // Rolling windows for smooth classification and micro-motion spread
    private val handWindow = ArrayDeque<HandType>()
    private val gripWindow = ArrayDeque<GripType>()
    private val rollHistory = ArrayDeque<Float>()
    private val windowCapacity = 9
    private val rollHistoryCapacity = 14

    // Active hold accumulator flushed every 5s or on posture transition
    private var currentTrackedHand: HandType = HandType.RESTING
    private var currentTrackedGrip: GripType = GripType.RESTING
    private var segmentStartMillis: Long = 0L
    private var postureHoldStartMillis: Long = 0L
    private var lastAutoLearnMillis: Long = 0L
    private var rollSum = 0f
    private var pitchSum = 0f
    private var sampleCount = 0
    private var lastUiEmitMillis = 0L

    // Live multi-axis calibration capture buffer
    @Volatile
    private var isCapturingCalibration = false
    private val calibrationFeatureSamples = mutableListOf<SensorFeatureVector>()

    init {
        scope.launch {
            preferences.calibrationProfile.collectLatest { profile ->
                calibrationProfile = profile
            }
        }
        scope.launch {
            repository.observePatternSamples(180).collectLatest { samples ->
                val rebuilt = AdaptiveGripModel(samples)
                adaptiveModel = rebuilt
                _adaptiveStats.value = rebuilt.stats
            }
        }
    }

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
        scope.launch {
            preferences.setBackgroundTrackingEnabled(running)
            pushWidgetUpdate()
        }
    }

    @Synchronized
    fun acquireListener() {
        activeConsumers++
        if (!isRegistered) {
            registerSensors()
        }
    }

    @Synchronized
    fun releaseListener() {
        activeConsumers = (activeConsumers - 1).coerceAtLeast(0)
        if (activeConsumers == 0 && isRegistered) {
            flushPendingSegment(System.currentTimeMillis())
            unregisterSensors()
        }
    }

    private fun registerSensors() {
        val primaryAccel = gravitySensor ?: accelerometerSensor
        primaryAccel?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscopeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        isRegistered = true
        val now = System.currentTimeMillis()
        segmentStartMillis = now
        postureHoldStartMillis = now
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
        isRegistered = false
        handWindow.clear()
        gripWindow.clear()
        rollHistory.clear()
    }

    /**
     * Captures a multi-axis sensor fingerprint window over ~2.2 seconds while the user
     * holds the phone in a target hand posture.
     */
    suspend fun captureFeatureWindow(durationMs: Long = 2200L): SensorFeatureVector {
        synchronized(calibrationFeatureSamples) {
            calibrationFeatureSamples.clear()
            isCapturingCalibration = true
        }
        delay(durationMs)
        return synchronized(calibrationFeatureSamples) {
            isCapturingCalibration = false
            if (calibrationFeatureSamples.isEmpty()) {
                latestFeatures
            } else {
                val n = calibrationFeatureSamples.size.toFloat()
                val avgNx = calibrationFeatureSamples.sumOf { it.nx.toDouble() }.toFloat() / n
                val avgNy = calibrationFeatureSamples.sumOf { it.ny.toDouble() }.toFloat() / n
                val avgNz = calibrationFeatureSamples.sumOf { it.nz.toDouble() }.toFloat() / n
                val avgRoll = calibrationFeatureSamples.sumOf { it.rollDeg.toDouble() }.toFloat() / n
                val avgPitch = calibrationFeatureSamples.sumOf { it.pitchDeg.toDouble() }.toFloat() / n
                val avgGyro = calibrationFeatureSamples.sumOf { it.gyroMag.toDouble() }.toFloat() / n
                val rollStd = sqrt(
                    calibrationFeatureSamples.sumOf { (it.rollDeg - avgRoll).toDouble().pow(2) } / n
                ).toFloat()
                SensorFeatureVector(
                    nx = avgNx,
                    ny = avgNy,
                    nz = avgNz,
                    rollDeg = avgRoll,
                    pitchDeg = avgPitch,
                    gyroMag = avgGyro,
                    rollSpreadDeg = rollStd
                )
            }
        }
    }

    /**
     * Immediately teaches the adaptive pattern model that the user's current real-time
     * sensor posture belongs to [correctedHand], updating live detection within milliseconds.
     */
    suspend fun teachCurrentPosture(correctedHand: HandType) {
        if (correctedHand == HandType.RESTING) return
        val features = latestFeatures
        val inferredGrip = when {
            correctedHand == HandType.BOTH && features.pitchDeg < 22f -> GripType.TWO_HAND_LANDSCAPE
            correctedHand == HandType.BOTH -> GripType.TWO_HAND_PORTRAIT
            features.pitchDeg >= 78f -> GripType.UPRIGHT_CALL
            features.pitchDeg <= 38f && features.gyroMag < 0.48f -> GripType.CRADLE_SUPPORT
            else -> GripType.ONE_HAND_THUMB
        }
        repository.savePatternSample(
            hand = correctedHand,
            gripType = inferredGrip,
            features = features,
            source = PatternSource.USER_CORRECTION,
            weight = 3.5f
        )
        // Clear smoothing window so user sees immediate adaptive response
        handWindow.clear()
        handWindow.addLast(correctedHand)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                filteredGx = event.values[0]
                filteredGy = event.values[1]
                filteredGz = event.values[2]
                hasGravitySample = true
                processGravityFrame()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (gravitySensor == null) {
                    val alpha = if (hasGravitySample) 0.76f else 0f
                    filteredGx = alpha * filteredGx + (1f - alpha) * event.values[0]
                    filteredGy = alpha * filteredGy + (1f - alpha) * event.values[1]
                    filteredGz = alpha * filteredGz + (1f - alpha) * event.values[2]
                    hasGravitySample = true
                    processGravityFrame()
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val wx = event.values[0]
                val wy = event.values[1]
                val wz = event.values[2]
                val mag = sqrt(wx * wx + wy * wy + wz * wz)
                latestGyroMag = 0.65f * latestGyroMag + 0.35f * mag
            }
            Sensor.TYPE_PROXIMITY -> {
                val maxRange = event.sensor.maximumRange
                isNearProximity = event.values[0] < minOf(maxRange, 3.0f)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun processGravityFrame() {
        val now = System.currentTimeMillis()
        val prelimFeatures = GripClassifier.extractFeatures(
            gx = filteredGx,
            gy = filteredGy,
            gz = filteredGz,
            gyroMag = latestGyroMag
        )

        if (rollHistory.size >= rollHistoryCapacity) rollHistory.removeFirst()
        rollHistory.addLast(prelimFeatures.rollDeg)
        val rollSpread = computeStdDev(rollHistory)

        val raw = GripClassifier.classify(
            gx = filteredGx,
            gy = filteredGy,
            gz = filteredGz,
            gyroMag = latestGyroMag,
            isNearProximity = isNearProximity,
            profile = calibrationProfile,
            adaptiveModel = adaptiveModel,
            rollSpreadDeg = rollSpread
        )
        latestFeatures = raw.featureVector

        if (isCapturingCalibration) {
            synchronized(calibrationFeatureSamples) {
                calibrationFeatureSamples.add(raw.featureVector)
            }
        }

        // Smooth hand & grip predictions via rolling window majority vote
        if (handWindow.size >= windowCapacity) handWindow.removeFirst()
        handWindow.addLast(raw.hand)
        val smoothedHand = majorityVote(handWindow, raw.hand)

        if (gripWindow.size >= windowCapacity) gripWindow.removeFirst()
        gripWindow.addLast(raw.gripType)
        val smoothedGrip = if (smoothedHand == HandType.RESTING) {
            GripType.RESTING
        } else {
            majorityVote(gripWindow, raw.gripType)
        }

        // Accumulate active hold duration and flush every 5s or on hand/grip change
        val postureChanged = (smoothedHand != currentTrackedHand || smoothedGrip != currentTrackedGrip)
        if (postureChanged) {
            flushPendingSegment(now)
            currentTrackedHand = smoothedHand
            currentTrackedGrip = smoothedGrip
            segmentStartMillis = now
            postureHoldStartMillis = now
            rollSum = raw.rollDeg
            pitchSum = raw.pitchDeg
            sampleCount = 1
        } else {
            rollSum += raw.rollDeg
            pitchSum += raw.pitchDeg
            sampleCount++
            val elapsedMs = now - segmentStartMillis
            if (elapsedMs >= 5_000L && currentTrackedHand != HandType.RESTING) {
                flushPendingSegment(now)
                segmentStartMillis = now
                rollSum = raw.rollDeg
                pitchSum = raw.pitchDeg
                sampleCount = 1
            }

            // Continuous self-improvement: when the user holds steadily with high confidence
            // and has at least 2 verified anchors, record a lightweight auto-learned sample
            val steadyHoldMs = now - postureHoldStartMillis
            if (currentTrackedHand != HandType.RESTING &&
                adaptiveModel.stats.userVerifiedSamples >= 2 &&
                raw.confidence >= 0.87f &&
                rollSpread < 4.5f &&
                steadyHoldMs >= 8_000L &&
                now - lastAutoLearnMillis >= 30_000L
            ) {
                lastAutoLearnMillis = now
                val snapshotFeatures = raw.featureVector
                val learnedHand = currentTrackedHand
                val learnedGrip = currentTrackedGrip
                scope.launch {
                    repository.savePatternSample(
                        hand = learnedHand,
                        gripType = learnedGrip,
                        features = snapshotFeatures,
                        source = PatternSource.AUTO_LEARNED,
                        weight = 0.65f
                    )
                }
            }
        }

        // Emit live HUD state at ~10Hz for smooth Canvas tilt gauge
        if (now - lastUiEmitMillis >= 100L || postureChanged) {
            lastUiEmitMillis = now
            _liveSnapshot.value = LiveGripSnapshot(
                hand = smoothedHand,
                gripType = smoothedGrip,
                confidence = raw.confidence,
                rollDeg = raw.rollDeg,
                pitchDeg = raw.pitchDeg,
                accelX = filteredGx,
                accelY = filteredGy,
                accelZ = filteredGz,
                gyroMagnitude = latestGyroMag,
                rollSpreadDeg = rollSpread,
                stabilityScore = raw.stabilityScore,
                isHolding = smoothedHand != HandType.RESTING,
                usedAdaptiveModel = raw.usedAdaptiveModel,
                hasAccelerometer = true,
                hasGyroscope = (gyroscopeSensor != null),
                timestampMillis = now
            )
            if (postureChanged) {
                scope.launch { pushWidgetUpdate() }
            }
        }
    }

    private fun flushPendingSegment(nowMillis: Long) {
        val hand = currentTrackedHand
        val grip = currentTrackedGrip
        if (hand == HandType.RESTING || grip == GripType.RESTING || segmentStartMillis <= 0L) {
            return
        }
        val durationSec = ((nowMillis - segmentStartMillis) / 1000L)
        if (durationSec <= 0L) return

        val count = sampleCount.coerceAtLeast(1)
        val avgRoll = rollSum / count
        val avgPitch = pitchSum / count

        scope.launch {
            repository.recordSegment(
                hand = hand,
                gripType = grip,
                durationSeconds = durationSec,
                avgRollDeg = avgRoll,
                avgPitchDeg = avgPitch,
                timestampMillis = nowMillis
            )
            pushWidgetUpdate()
        }
    }

    suspend fun pushWidgetUpdate() {
        val summary = repository.getDailySummarySnapshot()
        val live = _liveSnapshot.value
        val timeFmt = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        val topGrip = summary.topGrip
        val state = WidgetDisplayState(
            liveHand = live.hand,
            liveGrip = live.gripType,
            isTrackingServiceRunning = _isServiceRunning.value,
            totalHoldSeconds = summary.totalHoldSeconds,
            leftSeconds = summary.leftSeconds,
            rightSeconds = summary.rightSeconds,
            bothSeconds = summary.bothSeconds,
            leftPercent = summary.leftPercent,
            rightPercent = summary.rightPercent,
            bothPercent = summary.bothPercent,
            topGripName = topGrip?.gripType?.displayName ?: "Hold phone to start",
            topGripSeconds = topGrip?.durationSeconds ?: 0L,
            topGripPercent = topGrip?.percentage ?: 0,
            updatedTimeText = "Updated ${timeFmt.format(Date())}"
        )
        preferences.cacheWidgetState(state)
        GripWidgetUpdater.updateAllWidgets(context, state)
    }

    private fun computeStdDev(values: ArrayDeque<Float>): Float {
        if (values.size < 2) return 0f
        val mean = values.average()
        var sumSq = 0.0
        for (v in values) {
            val d = v - mean
            sumSq += d * d
        }
        return sqrt(sumSq / values.size).toFloat()
    }

    private fun <T> majorityVote(window: ArrayDeque<T>, fallback: T): T {
        if (window.isEmpty()) return fallback
        val counts = HashMap<T, Int>(4)
        var best = fallback
        var bestCount = 0
        for (item in window) {
            val next = (counts[item] ?: 0) + 1
            counts[item] = next
            if (next > bestCount) {
                bestCount = next
                best = item
            }
        }
        return best
    }
}

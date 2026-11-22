package com.example.model

import kotlin.math.roundToInt

enum class HandType(
    val displayName: String,
    val shortName: String,
    val description: String
) {
    LEFT("Left Hand", "Left", "Single-handed left grip"),
    RIGHT("Right Hand", "Right", "Single-handed right grip"),
    BOTH("Two Hands", "2-Hand", "Balanced bilateral hold"),
    RESTING("Resting Flat", "Flat", "Placed on a surface, not held");

    companion object {
        fun fromName(name: String?): HandType =
            entries.firstOrNull { it.name == name } ?: RESTING
    }
}

enum class StrainLevel(val label: String) {
    LOW("Low Strain"),
    MODERATE("Moderate"),
    ELEVATED("Elevated Thumb Load")
}

enum class GripType(
    val displayName: String,
    val shortName: String,
    val strainLevel: StrainLevel,
    val ergonomicTip: String
) {
    ONE_HAND_THUMB(
        displayName = "One-Hand Thumb",
        shortName = "1H Thumb",
        strainLevel = StrainLevel.ELEVATED,
        ergonomicTip = "High reach stretches thumb tendons. Alternate hands every 15–20 minutes."
    ),
    TWO_HAND_PORTRAIT(
        displayName = "Two-Hand Typing",
        shortName = "2H Typing",
        strainLevel = StrainLevel.LOW,
        ergonomicTip = "Most balanced portrait posture. Distributes phone weight evenly across both wrists."
    ),
    CRADLE_SUPPORT(
        displayName = "Cradle & Index",
        shortName = "Cradle",
        strainLevel = StrainLevel.LOW,
        ergonomicTip = "Low wrist torque. Great for reading or browsing while resting thumb tendons."
    ),
    TWO_HAND_LANDSCAPE(
        displayName = "Landscape Hold",
        shortName = "Landscape",
        strainLevel = StrainLevel.MODERATE,
        ergonomicTip = "Keep elbows supported during extended gaming or video sessions to avoid wrist flexion."
    ),
    UPRIGHT_CALL(
        displayName = "Upright / Call",
        shortName = "Upright",
        strainLevel = StrainLevel.MODERATE,
        ergonomicTip = "Avoid clamping the phone tightly when vertical; keep your shoulder relaxed."
    ),
    RESTING(
        displayName = "Resting Flat",
        shortName = "Resting",
        strainLevel = StrainLevel.LOW,
        ergonomicTip = "Phone is resting on a table or flat surface. Zero hand fatigue."
    );

    companion object {
        val activeGrips: List<GripType> = listOf(
            ONE_HAND_THUMB,
            TWO_HAND_PORTRAIT,
            CRADLE_SUPPORT,
            TWO_HAND_LANDSCAPE,
            UPRIGHT_CALL
        )

        fun fromName(name: String?): GripType =
            entries.firstOrNull { it.name == name } ?: RESTING
    }
}

data class SensorFeatureVector(
    val nx: Float = 0f,
    val ny: Float = 0f,
    val nz: Float = 1f,
    val rollDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val gyroMag: Float = 0f,
    val rollSpreadDeg: Float = 0f
)

data class CalibrationProfile(
    val leftRollBaselineDeg: Float = 8.5f,
    val rightRollBaselineDeg: Float = -8.5f,
    val twoHandRollBaselineDeg: Float = 0.0f,
    val deadzoneDeg: Float = 4.2f,
    val isCalibrated: Boolean = false
)

data class AdaptiveModelStats(
    val totalSamples: Int = 0,
    val userVerifiedSamples: Int = 0,
    val autoLearnedSamples: Int = 0,
    val leftSamples: Int = 0,
    val rightSamples: Int = 0,
    val bothSamples: Int = 0,
    val maturityPercent: Int = 0,
    val stageLabel: String = "Initial Physics",
    val learnedLeftRollDeg: Float? = null,
    val learnedRightRollDeg: Float? = null,
    val learnedBothRollDeg: Float? = null
) {
    val isAdaptiveActive: Boolean
        get() = listOf(leftSamples, rightSamples, bothSamples).count { it > 0 } >= 2
}

data class AiPatternStudyReport(
    val headline: String,
    val calibrationAdvice: String,
    val ergonomicRecommendations: List<String>,
    val isPlaceholder: Boolean = true,
    val notice: String = "Sample AI report — connect Firebase AI Logic to enable live Gemini pattern analysis."
)

data class LiveGripSnapshot(
    val hand: HandType = HandType.RESTING,
    val gripType: GripType = GripType.RESTING,
    val confidence: Float = 0f,
    val rollDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 9.81f,
    val gyroMagnitude: Float = 0f,
    val rollSpreadDeg: Float = 0f,
    val stabilityScore: Int = 100,
    val isHolding: Boolean = false,
    val usedAdaptiveModel: Boolean = false,
    val hasAccelerometer: Boolean = true,
    val hasGyroscope: Boolean = true,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class GripUsageStat(
    val gripType: GripType,
    val durationSeconds: Long,
    val percentage: Int
)

data class HourlyHandStat(
    val hour: Int,
    val leftSeconds: Long,
    val rightSeconds: Long,
    val bothSeconds: Long
) {
    val totalSeconds: Long get() = leftSeconds + rightSeconds + bothSeconds
}

data class DailyStatsSummary(
    val dateKey: String,
    val totalHoldSeconds: Long = 0L,
    val leftSeconds: Long = 0L,
    val rightSeconds: Long = 0L,
    val bothSeconds: Long = 0L,
    val gripBreakdown: List<GripUsageStat> = emptyList(),
    val hourlyBreakdown: List<HourlyHandStat> = (0..23).map { HourlyHandStat(it, 0, 0, 0) },
    val switchCount: Int = 0
) {
    val leftPercent: Int
        get() = if (totalHoldSeconds <= 0) 0 else ((leftSeconds * 100.0) / totalHoldSeconds).roundToInt()

    val rightPercent: Int
        get() = if (totalHoldSeconds <= 0) 0 else ((rightSeconds * 100.0) / totalHoldSeconds).roundToInt()

    val bothPercent: Int
        get() = if (totalHoldSeconds <= 0) 0 else (100 - leftPercent - rightPercent).coerceIn(0, 100)

    val dominantHand: HandType?
        get() {
            if (totalHoldSeconds <= 0) return null
            return when {
                rightSeconds >= leftSeconds && rightSeconds >= bothSeconds -> HandType.RIGHT
                leftSeconds >= rightSeconds && leftSeconds >= bothSeconds -> HandType.LEFT
                else -> HandType.BOTH
            }
        }

    val topGrip: GripUsageStat?
        get() = gripBreakdown.firstOrNull { it.durationSeconds > 0 }

    val balanceScore: Int
        get() {
            if (totalHoldSeconds <= 0) return 100
            val oneHandMaxShare = maxOf(leftSeconds, rightSeconds).toFloat() / totalHoldSeconds.toFloat()
            val bothShare = bothSeconds.toFloat() / totalHoldSeconds.toFloat()
            val raw = ((1f - (oneHandMaxShare - 0.35f).coerceAtLeast(0f) * 1.15f) * 85f + bothShare * 25f)
            return raw.roundToInt().coerceIn(20, 100)
        }

    val ergonomicInsight: String
        get() = when {
            totalHoldSeconds < 10 ->
                "Hold your phone naturally — GripPulse studies your tilt and gyro patterns in real time."
            bothPercent >= 45 ->
                "Great ergonomic balance! Using two hands reduces single-thumb tendon load."
            rightPercent >= 75 ->
                "Heavy right-hand bias (${rightPercent}%). Try switching to your left hand or cradling to ease thumb fatigue."
            leftPercent >= 75 ->
                "Heavy left-hand bias (${leftPercent}%). Try balancing with your right hand or two-handed typing."
            else ->
                "Well-distributed hold pattern today across ${leftPercent}% Left, ${rightPercent}% Right, and ${bothPercent}% Two-Handed."
        }
}

data class DayHistoryItem(
    val dateKey: String,
    val label: String,
    val totalHoldSeconds: Long,
    val leftSeconds: Long,
    val rightSeconds: Long,
    val bothSeconds: Long,
    val topGrip: GripType?
)

data class WidgetDisplayState(
    val liveHand: HandType = HandType.RESTING,
    val liveGrip: GripType = GripType.RESTING,
    val isTrackingServiceRunning: Boolean = false,
    val totalHoldSeconds: Long = 0L,
    val leftSeconds: Long = 0L,
    val rightSeconds: Long = 0L,
    val bothSeconds: Long = 0L,
    val leftPercent: Int = 0,
    val rightPercent: Int = 0,
    val bothPercent: Int = 0,
    val topGripName: String = "Ready to track",
    val topGripSeconds: Long = 0L,
    val topGripPercent: Int = 0,
    val updatedTimeText: String = "Live"
)

object DurationFormatter {
    fun formatCompact(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun formatShort(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}

package com.example.sensor

import com.example.model.CalibrationProfile
import com.example.model.GripType
import com.example.model.HandType
import com.example.model.SensorFeatureVector
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

data class ClassificationResult(
    val hand: HandType,
    val gripType: GripType,
    val confidence: Float,
    val rollDeg: Float,
    val pitchDeg: Float,
    val stabilityScore: Int,
    val isHolding: Boolean,
    val usedAdaptiveModel: Boolean,
    val featureVector: SensorFeatureVector
)

/**
 * Pitch-independent physics & adaptive-pattern grip classifier.
 * Computes lateral roll relative to the YZ plane (eliminating gimbal blow-up at upright angles)
 * and fuses personalized multi-axis pattern learning when available.
 */
object GripClassifier {

    fun extractFeatures(
        gx: Float,
        gy: Float,
        gz: Float,
        gyroMag: Float,
        rollSpreadDeg: Float = 0f
    ): SensorFeatureVector {
        val gNorm = hypot(hypot(gx.toDouble(), gy.toDouble()), gz.toDouble()).toFloat()
            .coerceAtLeast(0.01f)
        val nx = gx / gNorm
        val ny = gy / gNorm
        val nz = gz / gNorm

        // Pitch: 0 deg = flat horizontal, 90 deg = completely vertical upright
        val pitchDeg = Math.toDegrees(
            atan2(abs(ny).toDouble(), hypot(nx.toDouble(), nz.toDouble()))
        ).toFloat().coerceIn(0f, 90f)

        // Pitch-independent lateral roll in portrait: atan2(nx, hypot(ny, nz))
        // Stable even when phone is held at 85-90 deg upright!
        val yzMag = hypot(ny.toDouble(), nz.toDouble()).coerceAtLeast(0.05)
        val rollDeg = Math.toDegrees(
            atan2(nx.toDouble(), yzMag)
        ).toFloat().coerceIn(-90f, 90f)

        return SensorFeatureVector(
            nx = nx,
            ny = ny,
            nz = nz,
            rollDeg = rollDeg,
            pitchDeg = pitchDeg,
            gyroMag = gyroMag,
            rollSpreadDeg = rollSpreadDeg
        )
    }

    fun classify(
        gx: Float,
        gy: Float,
        gz: Float,
        gyroMag: Float,
        isNearProximity: Boolean,
        profile: CalibrationProfile,
        adaptiveModel: AdaptiveGripModel? = null,
        rollSpreadDeg: Float = 0f
    ): ClassificationResult {
        val features = extractFeatures(gx, gy, gz, gyroMag, rollSpreadDeg)
        val stabilityScore = (100f - (gyroMag * 36f) - (rollSpreadDeg * 2.2f))
            .roundToInt()
            .coerceIn(15, 100)

        // 1. Resting flat on a surface (face-up or face-down, minimal motion)
        val isFlat = abs(features.nz) > 0.94f &&
            hypot(features.nx.toDouble(), features.ny.toDouble()) < 0.34 &&
            gyroMag < 0.14f
        if (isFlat && !isNearProximity) {
            return ClassificationResult(
                hand = HandType.RESTING,
                gripType = GripType.RESTING,
                confidence = 0.97f,
                rollDeg = features.rollDeg,
                pitchDeg = features.pitchDeg,
                stabilityScore = stabilityScore,
                isHolding = false,
                usedAdaptiveModel = false,
                featureVector = features
            )
        }

        // 2. Landscape hold detection (sideways orientation)
        val isLandscape = abs(features.nx) > 0.76f && abs(features.ny) < 0.46f
        if (isLandscape) {
            val conf = (0.85f + (abs(features.nx) - 0.76f).coerceAtLeast(0f) * 0.5f).coerceAtMost(0.98f)
            return ClassificationResult(
                hand = HandType.BOTH,
                gripType = GripType.TWO_HAND_LANDSCAPE,
                confidence = conf,
                rollDeg = features.rollDeg,
                pitchDeg = features.pitchDeg,
                stabilityScore = stabilityScore,
                isHolding = true,
                usedAdaptiveModel = false,
                featureVector = features
            )
        }

        // 3. Hand prediction: prioritize on-device Adaptive Pattern Model when trained,
        // otherwise fall back cleanly to calibrated or default biomechanical thresholds.
        val adaptivePrediction = adaptiveModel?.predictHand(features)
        val (hand, handConfidence, usedAdaptive) = if (adaptivePrediction != null) {
            Triple(adaptivePrediction.first, adaptivePrediction.second, true)
        } else if (profile.isCalibrated) {
            val (h, c) = classifyCalibratedHand(features.rollDeg, profile)
            Triple(h, c, false)
        } else {
            val (h, c) = classifyDefaultHand(features.rollDeg, profile.deadzoneDeg)
            Triple(h, c, false)
        }

        // 4. Grip posture inference using pitch, hand, proximity, and gyro micro-dynamics
        val gripType = when {
            isNearProximity || features.pitchDeg >= 78f -> GripType.UPRIGHT_CALL
            hand == HandType.BOTH -> GripType.TWO_HAND_PORTRAIT
            features.pitchDeg <= 38f && gyroMag < 0.48f -> GripType.CRADLE_SUPPORT
            else -> GripType.ONE_HAND_THUMB
        }

        val finalConfidence = (handConfidence * 0.80f + (if (gyroMag < 2.0f) 0.18f else 0.08f))
            .coerceIn(0.55f, 0.99f)

        return ClassificationResult(
            hand = hand,
            gripType = gripType,
            confidence = finalConfidence,
            rollDeg = features.rollDeg,
            pitchDeg = features.pitchDeg,
            stabilityScore = stabilityScore,
            isHolding = true,
            usedAdaptiveModel = usedAdaptive,
            featureVector = features
        )
    }

    private fun classifyDefaultHand(rollDeg: Float, deadzoneDeg: Float): Pair<HandType, Float> {
        val dz = deadzoneDeg.coerceIn(2.0f, 14f)
        return when {
            rollDeg > dz -> {
                val margin = ((rollDeg - dz) / 14f).coerceIn(0f, 1f)
                HandType.LEFT to (0.72f + margin * 0.25f)
            }
            rollDeg < -dz -> {
                val margin = ((abs(rollDeg) - dz) / 14f).coerceIn(0f, 1f)
                HandType.RIGHT to (0.72f + margin * 0.25f)
            }
            else -> {
                val centerCloseness = 1f - (abs(rollDeg) / dz).coerceIn(0f, 1f)
                HandType.BOTH to (0.70f + centerCloseness * 0.24f)
            }
        }
    }

    private fun classifyCalibratedHand(
        rollDeg: Float,
        profile: CalibrationProfile
    ): Pair<HandType, Float> {
        val distLeft = abs(rollDeg - profile.leftRollBaselineDeg)
        val distRight = abs(rollDeg - profile.rightRollBaselineDeg)
        val distBoth = abs(rollDeg - profile.twoHandRollBaselineDeg)

        val candidates = listOf(
            HandType.LEFT to distLeft,
            HandType.RIGHT to distRight,
            HandType.BOTH to distBoth
        ).sortedBy { it.second }

        val best = candidates[0]
        val second = candidates[1]
        val separation = (second.second - best.second).coerceAtLeast(0f)
        val confidence = (0.70f + (separation / 12f).coerceIn(0f, 0.28f))
        return best.first to confidence
    }
}

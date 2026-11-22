package com.example.sensor

import com.example.data.GripPatternSampleEntity
import com.example.data.PatternSource
import com.example.model.AdaptiveModelStats
import com.example.model.HandType
import com.example.model.SensorFeatureVector
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class HandPrototype(
    val hand: HandType,
    val meanRoll: Float,
    val meanPitch: Float,
    val meanNx: Float,
    val meanGyro: Float,
    val stdRoll: Float,
    val stdPitch: Float,
    val sampleCount: Int
)

/**
 * On-device adaptive pattern-learning model that studies multi-axis sensor samples
 * (pitch-independent roll, pitch, normalized gravity X, and gyro dynamics) and
 * continuously improves classification accuracy via weighted k-NN + Gaussian prototypes.
 */
class AdaptiveGripModel(
    samples: List<GripPatternSampleEntity> = emptyList()
) {
    private val activeSamples: List<GripPatternSampleEntity> = samples.filter {
        val h = HandType.fromName(it.hand)
        h == HandType.LEFT || h == HandType.RIGHT || h == HandType.BOTH
    }

    val prototypes: Map<HandType, HandPrototype> = buildPrototypes(activeSamples)
    val stats: AdaptiveModelStats = computeStats(activeSamples, prototypes)

    fun predictHand(feature: SensorFeatureVector): Pair<HandType, Float>? {
        if (prototypes.isEmpty()) return null

        // If user has trained only 1 one-handed posture so far, synthesize a symmetric
        // mirror prototype for the opposite hand + neutral two-hand anchor so calibration
        // never skews everything into a single hand.
        val effectivePrototypes = expandPartialPrototypes(prototypes)
        if (effectivePrototypes.size < 2) return null

        // 1. Gaussian Prototype log-likelihood score per hand
        val protoScores = mutableMapOf<HandType, Float>()
        for ((hand, proto) in effectivePrototypes) {
            val zRoll = (feature.rollDeg - proto.meanRoll) / proto.stdRoll.coerceIn(3.5f, 16f)
            val zPitch = (feature.pitchDeg - proto.meanPitch) / proto.stdPitch.coerceIn(9f, 28f)
            val zNx = (feature.nx - proto.meanNx) / 0.14f
            val distSq = (zRoll * zRoll * 1.45f) + (zPitch * zPitch * 0.35f) + (zNx * zNx * 0.85f)
            protoScores[hand] = exp(-0.5f * distSq)
        }

        // 2. Weighted k-NN voting across the user's real recorded samples (k = 5)
        val knnScores = mutableMapOf(
            HandType.LEFT to 0f,
            HandType.RIGHT to 0f,
            HandType.BOTH to 0f
        )
        if (activeSamples.size >= 3 && prototypes.size >= 2) {
            val neighbors = activeSamples.map { sample ->
                val dRoll = (feature.rollDeg - sample.rollDeg) / 6.5f
                val dPitch = (feature.pitchDeg - sample.pitchDeg) / 16.0f
                val dNx = (feature.nx - sample.nx) / 0.14f
                val dGyro = (feature.gyroMag - sample.gyroMag) / 0.85f
                val dist = sqrt(
                    dRoll * dRoll * 1.5f + dPitch * dPitch * 0.35f + dNx * dNx * 0.8f + dGyro * dGyro * 0.15f
                )
                sample to dist
            }.sortedBy { it.second }.take(5)

            for ((sample, dist) in neighbors) {
                val hand = HandType.fromName(sample.hand)
                val affinity = sample.weight * exp(-dist * dist * 0.55f)
                knnScores[hand] = (knnScores[hand] ?: 0f) + affinity
            }
        }

        val protoSum = protoScores.values.sum().coerceAtLeast(1e-5f)
        val knnSum = knnScores.values.sum()

        // Fuse prototype likelihood + k-NN affinity
        val fused = listOf(HandType.LEFT, HandType.RIGHT, HandType.BOTH).associateWith { hand ->
            val pNorm = (protoScores[hand] ?: 0f) / protoSum
            if (knnSum > 1e-4f) {
                val kNorm = (knnScores[hand] ?: 0f) / knnSum
                0.42f * pNorm + 0.58f * kNorm
            } else {
                pNorm
            }
        }

        val bestEntry = fused.maxByOrNull { it.value } ?: return null
        val confidence = (0.58f + bestEntry.value * 0.40f).coerceIn(0.55f, 0.99f)
        return bestEntry.key to confidence
    }

    private fun expandPartialPrototypes(
        raw: Map<HandType, HandPrototype>
    ): Map<HandType, HandPrototype> {
        if (raw.size >= 3) return raw
        val result = raw.toMutableMap()

        val left = raw[HandType.LEFT]
        val right = raw[HandType.RIGHT]
        val both = raw[HandType.BOTH]

        if (left != null && right == null) {
            result[HandType.RIGHT] = left.copy(
                hand = HandType.RIGHT,
                meanRoll = -left.meanRoll,
                meanNx = -left.meanNx
            )
        } else if (right != null && left == null) {
            result[HandType.LEFT] = right.copy(
                hand = HandType.LEFT,
                meanRoll = -right.meanRoll,
                meanNx = -right.meanNx
            )
        }

        if (both == null && result.containsKey(HandType.LEFT) && result.containsKey(HandType.RIGHT)) {
            val l = result[HandType.LEFT]!!
            val r = result[HandType.RIGHT]!!
            result[HandType.BOTH] = HandPrototype(
                hand = HandType.BOTH,
                meanRoll = (l.meanRoll + r.meanRoll) / 2f,
                meanPitch = (l.meanPitch + r.meanPitch) / 2f,
                meanNx = (l.meanNx + r.meanNx) / 2f,
                meanGyro = (l.meanGyro + r.meanGyro) / 2f,
                stdRoll = 4.5f,
                stdPitch = 16f,
                sampleCount = 0
            )
        }
        return result
    }

    private fun buildPrototypes(samples: List<GripPatternSampleEntity>): Map<HandType, HandPrototype> {
        if (samples.isEmpty()) return emptyMap()
        val now = System.currentTimeMillis()
        val grouped = samples.groupBy { HandType.fromName(it.hand) }
        val result = mutableMapOf<HandType, HandPrototype>()

        for ((hand, list) in grouped) {
            if (hand == HandType.RESTING || list.isEmpty()) continue
            var wSum = 0f
            var rollSum = 0f
            var pitchSum = 0f
            var nxSum = 0f
            var gyroSum = 0f

            // Time-decay weighting (recent habits carry higher weight)
            val weighted = list.map { s ->
                val ageDays = ((now - s.timestampMillis).coerceAtLeast(0L) / 86_400_000.0).toFloat()
                val recencyFactor = exp(-ageDays * 0.08f).coerceAtLeast(0.35f)
                val w = s.weight * recencyFactor
                wSum += w
                rollSum += s.rollDeg * w
                pitchSum += s.pitchDeg * w
                nxSum += s.nx * w
                gyroSum += s.gyroMag * w
                s to w
            }

            if (wSum <= 0f) continue
            val meanRoll = rollSum / wSum
            val meanPitch = pitchSum / wSum
            val meanNx = nxSum / wSum
            val meanGyro = gyroSum / wSum

            var rollVar = 0f
            var pitchVar = 0f
            for ((s, w) in weighted) {
                rollVar += w * (s.rollDeg - meanRoll).pow(2)
                pitchVar += w * (s.pitchDeg - meanPitch).pow(2)
            }
            val stdRoll = sqrt(rollVar / wSum).coerceIn(4.0f, 14.0f)
            val stdPitch = sqrt(pitchVar / wSum).coerceIn(10.0f, 24.0f)

            result[hand] = HandPrototype(
                hand = hand,
                meanRoll = meanRoll,
                meanPitch = meanPitch,
                meanNx = meanNx,
                meanGyro = meanGyro,
                stdRoll = stdRoll,
                stdPitch = stdPitch,
                sampleCount = list.size
            )
        }
        return result
    }

    private fun computeStats(
        samples: List<GripPatternSampleEntity>,
        protos: Map<HandType, HandPrototype>
    ): AdaptiveModelStats {
        val userVerified = samples.count {
            it.source == PatternSource.CALIBRATION || it.source == PatternSource.USER_CORRECTION
        }
        val autoLearned = samples.count { it.source == PatternSource.AUTO_LEARNED }
        val leftCount = samples.count { HandType.fromName(it.hand) == HandType.LEFT }
        val rightCount = samples.count { HandType.fromName(it.hand) == HandType.RIGHT }
        val bothCount = samples.count { HandType.fromName(it.hand) == HandType.BOTH }

        val coveredHands = listOf(leftCount, rightCount, bothCount).count { it > 0 }
        val coverageScore = (coveredHands / 3f) * 45f
        val verifiedScore = (userVerified.coerceAtMost(10) / 10f) * 35f
        val autoScore = (autoLearned.coerceAtMost(25) / 25f) * 20f
        val maturity = (coverageScore + verifiedScore + autoScore).roundToInt().coerceIn(0, 100)

        val stage = when {
            maturity >= 80 -> "Highly Personalized"
            maturity >= 45 -> "Actively Adapting"
            userVerified > 0 -> "Learning Your Grip"
            else -> "Base Physics"
        }

        return AdaptiveModelStats(
            totalSamples = samples.size,
            userVerifiedSamples = userVerified,
            autoLearnedSamples = autoLearned,
            leftSamples = leftCount,
            rightSamples = rightCount,
            bothSamples = bothCount,
            maturityPercent = maturity,
            stageLabel = stage,
            learnedLeftRollDeg = protos[HandType.LEFT]?.meanRoll,
            learnedRightRollDeg = protos[HandType.RIGHT]?.meanRoll,
            learnedBothRollDeg = protos[HandType.BOTH]?.meanRoll
        )
    }
}

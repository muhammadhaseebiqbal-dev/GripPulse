package com.example.ai

import com.example.data.GripPatternSampleEntity
import com.example.model.AdaptiveModelStats
import com.example.model.AiPatternStudyReport
import com.example.model.DailyStatsSummary
import com.example.model.DurationFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Single AI advisor interface so cloud Gemini coaching via Firebase AI Logic can be plugged in
 * cleanly without touching UI or sensor layers.
 */
interface GripAiAdvisor {
    suspend fun studyPatterns(
        summary: DailyStatsSummary,
        modelStats: AdaptiveModelStats,
        samples: List<GripPatternSampleEntity>
    ): AiPatternStudyReport
}

/**
 * Placeholder advisor used when Firebase AI Logic is not enabled.
 * Clearly labels every report as a sample preview while still surfacing real on-device metrics.
 */
class PlaceholderGripAiAdvisor : GripAiAdvisor {

    override suspend fun studyPatterns(
        summary: DailyStatsSummary,
        modelStats: AdaptiveModelStats,
        samples: List<GripPatternSampleEntity>
    ): AiPatternStudyReport {
        delay(650L)

        val totalFormatted = DurationFormatter.formatCompact(summary.totalHoldSeconds)
        val topGripName = summary.topGrip?.gripType?.displayName ?: "not yet dominant"
        val headline = if (modelStats.isAdaptiveActive) {
            "Sample Pattern Study · On-device model trained on ${modelStats.totalSamples} real sensor patterns (${modelStats.maturityPercent}% maturity)"
        } else {
            "Sample Pattern Study · ${modelStats.userVerifiedSamples} calibration anchors recorded ($totalFormatted held today)"
        }

        val leftRollStr = modelStats.learnedLeftRollDeg?.let { String.format(Locale.US, "%+.1f°", it) } ?: "untrained"
        val rightRollStr = modelStats.learnedRightRollDeg?.let { String.format(Locale.US, "%+.1f°", it) } ?: "untrained"
        val bothRollStr = modelStats.learnedBothRollDeg?.let { String.format(Locale.US, "%+.1f°", it) } ?: "untrained"

        val calibrationAdvice =
            "Learned lateral tilt clusters — Left: $leftRollStr, Right: $rightRollStr, Two-Hand: $bothRollStr. " +
                if (modelStats.userVerifiedSamples < 6) {
                    "Tap the quick feedback chips on Today or run 3-Hand Capture to sharpen personal boundaries."
                } else {
                    "Your personal multi-axis clusters are well-separated and continue refining automatically during steady holds."
                }

        val recommendations = buildList {
            add("Sample Insight: Primary grip today is $topGripName (${summary.topGrip?.percentage ?: 0}% share) with a balance score of ${summary.balanceScore}/100.")
            if (summary.rightPercent >= 65) {
                add("Sample Tip: Right-hand hold time (${summary.rightPercent}%) is elevated. Switching to Cradle or Two-Hand Typing for 10 minutes eases thumb tendon tension.")
            } else if (summary.leftPercent >= 65) {
                add("Sample Tip: Left-hand hold time (${summary.leftPercent}%) is elevated. Balancing with your right hand reduces asymmetric wrist torque.")
            } else {
                add("Sample Tip: Your hand distribution (${summary.leftPercent}% L / ${summary.rightPercent}% R / ${summary.bothPercent}% 2H) shows healthy load sharing.")
            }
            add("Sample Learning Note: Each one-tap correction adds a 3.5x-weighted 6-axis fingerprint so detection accuracy improves every day.")
        }

        return AiPatternStudyReport(
            headline = headline,
            calibrationAdvice = calibrationAdvice,
            ergonomicRecommendations = recommendations,
            isPlaceholder = true,
            notice = "Sample AI report — Gemini cloud pattern coaching requires Firebase AI Logic."
        )
    }
}

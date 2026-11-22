package com.example.data

import com.example.model.DailyStatsSummary
import com.example.model.DayHistoryItem
import com.example.model.GripType
import com.example.model.GripUsageStat
import com.example.model.HandType
import com.example.model.HourlyHandStat
import com.example.model.SensorFeatureVector
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GripRepository(
    private val gripDao: GripDao
) {
    fun todayDateKey(): String = formatDateKey(System.currentTimeMillis())

    fun formatDateKey(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(millis))
    }

    fun recentDateKeys(days: Int = 7): List<Pair<String, String>> {
        val cal = Calendar.getInstance()
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("EEE", Locale.US)
        val result = mutableListOf<Pair<String, String>>()
        for (i in 0 until days) {
            val key = keyFmt.format(cal.time)
            val label = if (i == 0) "Today" else labelFmt.format(cal.time)
            result.add(key to label)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return result.reversed()
    }

    suspend fun recordSegment(
        hand: HandType,
        gripType: GripType,
        durationSeconds: Long,
        avgRollDeg: Float,
        avgPitchDeg: Float,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        if (durationSeconds <= 0L || hand == HandType.RESTING || gripType == GripType.RESTING) return
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val entity = GripSegmentEntity(
            dateKey = formatDateKey(timestampMillis),
            hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
            hand = hand.name,
            gripType = gripType.name,
            durationSeconds = durationSeconds,
            avgRollDeg = avgRollDeg,
            avgPitchDeg = avgPitchDeg,
            timestampMillis = timestampMillis
        )
        gripDao.insertSegment(entity)
    }

    suspend fun savePatternSample(
        hand: HandType,
        gripType: GripType,
        features: SensorFeatureVector,
        source: String,
        weight: Float
    ) {
        if (hand == HandType.RESTING) return
        val sample = GripPatternSampleEntity(
            hand = hand.name,
            gripType = gripType.name,
            nx = features.nx,
            ny = features.ny,
            nz = features.nz,
            rollDeg = features.rollDeg,
            pitchDeg = features.pitchDeg,
            gyroMag = features.gyroMag,
            rollSpreadDeg = features.rollSpreadDeg,
            source = source,
            weight = weight,
            timestampMillis = System.currentTimeMillis()
        )
        gripDao.insertPatternSample(sample)
        if (source == PatternSource.AUTO_LEARNED) {
            gripDao.pruneAutoLearnedSamples(100)
        }
    }

    fun observePatternSamples(limit: Int = 180): Flow<List<GripPatternSampleEntity>> {
        return gripDao.observePatternSamples(limit)
    }

    suspend fun getPatternSamples(limit: Int = 180): List<GripPatternSampleEntity> {
        return gripDao.getPatternSamples(limit)
    }

    suspend fun clearPatternSamples() {
        gripDao.clearPatternSamples()
    }

    fun observeDailySummary(dateKey: String = todayDateKey()): Flow<DailyStatsSummary> {
        return gripDao.observeSegmentsForDate(dateKey).map { segments ->
            aggregateDailySummary(dateKey, segments)
        }
    }

    suspend fun getDailySummarySnapshot(dateKey: String = todayDateKey()): DailyStatsSummary {
        val segments = gripDao.getSegmentsForDate(dateKey)
        return aggregateDailySummary(dateKey, segments)
    }

    fun observeRecentSegments(limit: Int = 25): Flow<List<GripSegmentEntity>> {
        return gripDao.observeRecentSegments(limit)
    }

    fun observeWeeklyHistory(): Flow<List<DayHistoryItem>> {
        val dayPairs = recentDateKeys(7)
        val keys = dayPairs.map { it.first }
        return gripDao.observeSegmentsForDates(keys).map { allSegments ->
            val grouped = allSegments.groupBy { it.dateKey }
            dayPairs.map { (dateKey, label) ->
                val daySegments = grouped[dateKey].orEmpty()
                val summary = aggregateDailySummary(dateKey, daySegments)
                DayHistoryItem(
                    dateKey = dateKey,
                    label = label,
                    totalHoldSeconds = summary.totalHoldSeconds,
                    leftSeconds = summary.leftSeconds,
                    rightSeconds = summary.rightSeconds,
                    bothSeconds = summary.bothSeconds,
                    topGrip = summary.topGrip?.gripType
                )
            }
        }
    }

    suspend fun resetDate(dateKey: String = todayDateKey()) {
        gripDao.deleteSegmentsForDate(dateKey)
    }

    suspend fun clearAllHistory() {
        gripDao.clearAll()
    }

    private fun aggregateDailySummary(
        dateKey: String,
        segments: List<GripSegmentEntity>
    ): DailyStatsSummary {
        var leftSec = 0L
        var rightSec = 0L
        var bothSec = 0L
        val gripTotals = mutableMapOf<GripType, Long>()
        val hourLeft = LongArray(24)
        val hourRight = LongArray(24)
        val hourBoth = LongArray(24)

        var switches = 0
        var prevKey: String? = null

        for (seg in segments) {
            val hand = HandType.fromName(seg.hand)
            val grip = GripType.fromName(seg.gripType)
            if (hand == HandType.RESTING || grip == GripType.RESTING) continue

            val dur = seg.durationSeconds.coerceAtLeast(0L)
            val h = seg.hourOfDay.coerceIn(0, 23)
            when (hand) {
                HandType.LEFT -> {
                    leftSec += dur
                    hourLeft[h] += dur
                }
                HandType.RIGHT -> {
                    rightSec += dur
                    hourRight[h] += dur
                }
                HandType.BOTH -> {
                    bothSec += dur
                    hourBoth[h] += dur
                }
                HandType.RESTING -> Unit
            }
            gripTotals[grip] = (gripTotals[grip] ?: 0L) + dur

            val currentKey = "${seg.hand}_${seg.gripType}"
            if (prevKey != null && prevKey != currentKey) {
                switches++
            }
            prevKey = currentKey
        }

        val total = leftSec + rightSec + bothSec
        val gripStats = GripType.activeGrips.map { type ->
            val sec = gripTotals[type] ?: 0L
            val pct = if (total > 0L) ((sec * 100.0) / total).roundToInt() else 0
            GripUsageStat(type, sec, pct)
        }.sortedByDescending { it.durationSeconds }

        val hourlyStats = (0..23).map { hour ->
            HourlyHandStat(
                hour = hour,
                leftSeconds = hourLeft[hour],
                rightSeconds = hourRight[hour],
                bothSeconds = hourBoth[hour]
            )
        }

        return DailyStatsSummary(
            dateKey = dateKey,
            totalHoldSeconds = total,
            leftSeconds = leftSec,
            rightSeconds = rightSec,
            bothSeconds = bothSec,
            gripBreakdown = gripStats,
            hourlyBreakdown = hourlyStats,
            switchCount = switches
        )
    }
}

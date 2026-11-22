package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object PatternSource {
    const val CALIBRATION = "CALIBRATION"
    const val USER_CORRECTION = "USER_CORRECTION"
    const val AUTO_LEARNED = "AUTO_LEARNED"
}

/**
 * Multi-axis sensor pattern fingerprint stored in Room so the on-device adaptive
 * engine learns the user's real grip geometry and continuously improves over time.
 */
@Entity(
    tableName = "grip_pattern_samples",
    indices = [Index(value = ["hand"]), Index(value = ["timestampMillis"])]
)
data class GripPatternSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hand: String,
    val gripType: String,
    val nx: Float,
    val ny: Float,
    val nz: Float,
    val rollDeg: Float,
    val pitchDeg: Float,
    val gyroMag: Float,
    val rollSpreadDeg: Float,
    val source: String,
    val weight: Float,
    val timestampMillis: Long = System.currentTimeMillis()
)

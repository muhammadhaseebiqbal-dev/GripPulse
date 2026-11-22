package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grip_segments",
    indices = [Index(value = ["dateKey"]), Index(value = ["timestampMillis"])]
)
data class GripSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val hourOfDay: Int,
    val hand: String,
    val gripType: String,
    val durationSeconds: Long,
    val avgRollDeg: Float,
    val avgPitchDeg: Float,
    val timestampMillis: Long = System.currentTimeMillis()
)

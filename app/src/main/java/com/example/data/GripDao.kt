package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: GripSegmentEntity)

    @Query("SELECT * FROM grip_segments WHERE dateKey = :dateKey ORDER BY timestampMillis DESC")
    fun observeSegmentsForDate(dateKey: String): Flow<List<GripSegmentEntity>>

    @Query("SELECT * FROM grip_segments WHERE dateKey = :dateKey ORDER BY timestampMillis DESC")
    suspend fun getSegmentsForDate(dateKey: String): List<GripSegmentEntity>

    @Query("SELECT * FROM grip_segments WHERE dateKey IN (:dateKeys) ORDER BY dateKey DESC, timestampMillis DESC")
    fun observeSegmentsForDates(dateKeys: List<String>): Flow<List<GripSegmentEntity>>

    @Query("SELECT * FROM grip_segments ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecentSegments(limit: Int = 30): Flow<List<GripSegmentEntity>>

    @Query("DELETE FROM grip_segments WHERE dateKey = :dateKey")
    suspend fun deleteSegmentsForDate(dateKey: String)

    @Query("DELETE FROM grip_segments")
    suspend fun clearAll()

    // Adaptive Pattern Learning Memory
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatternSample(sample: GripPatternSampleEntity)

    @Query("SELECT * FROM grip_pattern_samples ORDER BY timestampMillis DESC LIMIT :limit")
    fun observePatternSamples(limit: Int = 180): Flow<List<GripPatternSampleEntity>>

    @Query("SELECT * FROM grip_pattern_samples ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun getPatternSamples(limit: Int = 180): List<GripPatternSampleEntity>

    @Query("DELETE FROM grip_pattern_samples WHERE source = 'AUTO_LEARNED' AND id NOT IN (SELECT id FROM grip_pattern_samples WHERE source = 'AUTO_LEARNED' ORDER BY timestampMillis DESC LIMIT :keepLimit)")
    suspend fun pruneAutoLearnedSamples(keepLimit: Int = 100)

    @Query("DELETE FROM grip_pattern_samples")
    suspend fun clearPatternSamples()
}

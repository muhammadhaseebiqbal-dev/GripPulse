package com.example

import com.example.data.GripPatternSampleEntity
import com.example.data.PatternSource
import com.example.model.CalibrationProfile
import com.example.model.GripType
import com.example.model.HandType
import com.example.sensor.AdaptiveGripModel
import com.example.sensor.GripClassifier
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GripClassifierTest {

    private val defaultProfile = CalibrationProfile()

    @Test
    fun flatPhoneOnDeskIsClassifiedAsResting() {
        val result = GripClassifier.classify(
            gx = 0.1f,
            gy = 0.2f,
            gz = 9.8f,
            gyroMag = 0.02f,
            isNearProximity = false,
            profile = defaultProfile
        )
        assertEquals(HandType.RESTING, result.hand)
        assertEquals(GripType.RESTING, result.gripType)
        assertFalse(result.isHolding)
    }

    @Test
    fun uprightPhoneRollRemainsStableWithoutGimbalBlowup() {
        // Nearly vertical phone (87 deg pitch, tiny 5 deg lateral tilt):
        // old atan2(nx, nz) exploded to ~60 deg; new YZ-plane roll stays ~5 deg.
        val result = GripClassifier.classify(
            gx = 0.85f,
            gy = 9.75f,
            gz = 0.25f,
            gyroMag = 0.1f,
            isNearProximity = false,
            profile = defaultProfile
        )
        assertTrue("Expected roll near +5 deg, got ${result.rollDeg}", abs(result.rollDeg - 5f) < 1.5f)
        assertTrue("Expected upright pitch > 80 deg, got ${result.pitchDeg}", result.pitchDeg > 80f)
    }

    @Test
    fun rightTiltedPortraitIsRightHandOneHandThumb() {
        val result = GripClassifier.classify(
            gx = -2.4f,
            gy = 7.4f,
            gz = 6.0f,
            gyroMag = 0.35f,
            isNearProximity = false,
            profile = defaultProfile
        )
        assertEquals(HandType.RIGHT, result.hand)
        assertEquals(GripType.ONE_HAND_THUMB, result.gripType)
        assertTrue(result.isHolding)
    }

    @Test
    fun levelPortraitIsTwoHandTyping() {
        val result = GripClassifier.classify(
            gx = 0.2f,
            gy = 6.9f,
            gz = 6.9f,
            gyroMag = 0.20f,
            isNearProximity = false,
            profile = defaultProfile
        )
        assertEquals(HandType.BOTH, result.hand)
        assertEquals(GripType.TWO_HAND_PORTRAIT, result.gripType)
        assertTrue(result.isHolding)
    }

    @Test
    fun rotatedSidewaysIsLandscapeTwoHandHold() {
        val result = GripClassifier.classify(
            gx = 9.2f,
            gy = 1.0f,
            gz = 3.2f,
            gyroMag = 0.15f,
            isNearProximity = false,
            profile = defaultProfile
        )
        assertEquals(HandType.BOTH, result.hand)
        assertEquals(GripType.TWO_HAND_LANDSCAPE, result.gripType)
        assertTrue(result.isHolding)
    }

    @Test
    fun adaptiveModelLearnsInvertedPersonalTiltHabit() {
        // User who naturally tilts POSITIVE roll when holding in RIGHT hand (opposite of default)
        val fRight = GripClassifier.extractFeatures(gx = 2.0f, gy = 7.2f, gz = 6.3f, gyroMag = 0.25f)
        val fLeft = GripClassifier.extractFeatures(gx = -2.1f, gy = 7.1f, gz = 6.3f, gyroMag = 0.25f)
        val fBoth = GripClassifier.extractFeatures(gx = 0.0f, gy = 7.0f, gz = 6.8f, gyroMag = 0.15f)

        val samples = listOf(
            GripPatternSampleEntity(
                hand = HandType.RIGHT.name,
                gripType = GripType.ONE_HAND_THUMB.name,
                nx = fRight.nx, ny = fRight.ny, nz = fRight.nz,
                rollDeg = fRight.rollDeg, pitchDeg = fRight.pitchDeg,
                gyroMag = fRight.gyroMag, rollSpreadDeg = 1.0f,
                source = PatternSource.CALIBRATION, weight = 3.0f
            ),
            GripPatternSampleEntity(
                hand = HandType.LEFT.name,
                gripType = GripType.ONE_HAND_THUMB.name,
                nx = fLeft.nx, ny = fLeft.ny, nz = fLeft.nz,
                rollDeg = fLeft.rollDeg, pitchDeg = fLeft.pitchDeg,
                gyroMag = fLeft.gyroMag, rollSpreadDeg = 1.0f,
                source = PatternSource.CALIBRATION, weight = 3.0f
            ),
            GripPatternSampleEntity(
                hand = HandType.BOTH.name,
                gripType = GripType.TWO_HAND_PORTRAIT.name,
                nx = fBoth.nx, ny = fBoth.ny, nz = fBoth.nz,
                rollDeg = fBoth.rollDeg, pitchDeg = fBoth.pitchDeg,
                gyroMag = fBoth.gyroMag, rollSpreadDeg = 0.8f,
                source = PatternSource.CALIBRATION, weight = 3.0f
            )
        )

        val model = AdaptiveGripModel(samples)
        val result = GripClassifier.classify(
            gx = 1.9f,
            gy = 7.3f,
            gz = 6.2f,
            gyroMag = 0.22f,
            isNearProximity = false,
            profile = defaultProfile,
            adaptiveModel = model
        )
        assertTrue(result.usedAdaptiveModel)
        assertEquals(HandType.RIGHT, result.hand)
    }
}

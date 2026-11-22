package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.HandType

@Immutable
data class GripAccents(
    val leftHand: Color,
    val rightHand: Color,
    val bothHands: Color,
    val resting: Color,
    val highlight: Color,
    val livePulse: Color,
    val cardBorder: Color,
    val isDark: Boolean
) {
    fun colorForHand(hand: HandType): Color = when (hand) {
        HandType.LEFT -> leftHand
        HandType.RIGHT -> rightHand
        HandType.BOTH -> bothHands
        HandType.RESTING -> resting
    }
}

val LocalGripAccents = staticCompositionLocalOf {
    GripAccents(
        leftHand = TealLeftDark,
        rightHand = CoralRightDark,
        bothHands = CobaltBothDark,
        resting = NeutralRestingDark,
        highlight = AmberHighlightDark,
        livePulse = EmeraldLiveDark,
        cardBorder = ObsidianOutline,
        isDark = true
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = TealLeftDark,
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF083530),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = CoralRightDark,
    onSecondary = Color(0xFF2B0B03),
    secondaryContainer = Color(0xFF421A10),
    onSecondaryContainer = Color(0xFFFFD8CC),
    tertiary = CobaltBothDark,
    onTertiary = Color(0xFF06163B),
    tertiaryContainer = Color(0xFF14244E),
    onTertiaryContainer = Color(0xFFD6E2FF),
    background = ObsidianBg,
    onBackground = SlateTextPrimary,
    surface = ObsidianSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = SlateTextSecondary,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF1D273B),
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = Color(0xFF212C42)
)

private val LightColorScheme = lightColorScheme(
    primary = TealLeftLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F5EE),
    onPrimaryContainer = Color(0xFF003830),
    secondary = CoralRightLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0D6),
    onSecondaryContainer = Color(0xFF4A1303),
    tertiary = CobaltBothLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCE6FF),
    onTertiaryContainer = Color(0xFF0C205C),
    background = AlabasterBg,
    onBackground = InkTextPrimary,
    surface = AlabasterSurface,
    onSurface = InkTextPrimary,
    surfaceVariant = AlabasterSurfaceHigh,
    onSurfaceVariant = InkTextSecondary,
    outline = AlabasterOutline,
    outlineVariant = Color(0xFFE2E8F0),
    surfaceContainerLow = AlabasterSurface,
    surfaceContainer = AlabasterSurfaceHigh,
    surfaceContainerHigh = Color(0xFFDFE7F2)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GripPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val accents = if (darkTheme) {
        GripAccents(
            leftHand = TealLeftDark,
            rightHand = CoralRightDark,
            bothHands = CobaltBothDark,
            resting = NeutralRestingDark,
            highlight = AmberHighlightDark,
            livePulse = EmeraldLiveDark,
            cardBorder = ObsidianOutline,
            isDark = true
        )
    } else {
        GripAccents(
            leftHand = TealLeftLight,
            rightHand = CoralRightLight,
            bothHands = CobaltBothLight,
            resting = NeutralRestingLight,
            highlight = AmberHighlightLight,
            livePulse = EmeraldLiveLight,
            cardBorder = AlabasterOutline,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalGripAccents provides accents) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

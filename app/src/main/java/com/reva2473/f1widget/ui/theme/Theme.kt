package com.reva2473.f1widget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.domain.model.F1TeamId

object P1TColors {
    val BackgroundPrimary = Color(0xFF0B0B0E)
    val BackgroundLight = Color(0xFFF6F6F9)
    val HeaderCrimsonGlow = Color(0xFF581313)
    val SurfaceCard = Color(0xFF16161A)
    val SurfaceCardLight = Color(0xFFFFFFFF)
    val SurfaceElevated = Color(0xFF1E1E24)
    val SurfaceInnerCard = Color(0xFF121216)
    val BorderSubtle = Color(0xFF2A2A32)
    val BorderLight = Color(0xFFE2E2EA)
    val AccentRed = Color(0xFFE1251B)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9A9AA6)
    val TextMuted = Color(0xFF8E8E9B)

    // Timing & Sector Colors
    val SectorPurple = Color(0xFFB855FF)
    val SectorGreen = Color(0xFF2CE580)
    val TrackClearBg = Color(0xFF103321)
    val SectorYellow = Color(0xFFF5C518)
    val SectorInactive = Color(0xFF3A3A44)
    val SpeedTrapMagenta = Color(0xFFE01A9F)
    val InPitBlue = Color(0xFF2A75D3)
    val GoldTrophy = Color(0xFFF5C518)
    val SilverTrophy = Color(0xFFC0C0C8)
    val BronzeTrophy = Color(0xFFD97B29)
}

data class F1AccentPalette(
    val primaryAccent: Color,
    val headerGlow: Color,
    val isDark: Boolean
)

val LocalF1Accent = staticCompositionLocalOf {
    F1AccentPalette(
        primaryAccent = P1TColors.AccentRed,
        headerGlow = P1TColors.HeaderCrimsonGlow,
        isDark = true
    )
}

val F1Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp
    )
)

@Composable
fun F1WidgetTheme(
    teamAccent: F1TeamId = F1TeamId.FERRARI,
    useTeamAccent: Boolean = true,
    themeMode: String = "DARK",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "LIGHT" -> false
        "SYSTEM" -> isSystemInDarkTheme()
        else -> true
    }

    val accentColor = if (useTeamAccent) Color(teamAccent.colorHex) else P1TColors.AccentRed
    val glowColor = if (useTeamAccent) Color(teamAccent.secondaryHex) else P1TColors.HeaderCrimsonGlow
    val onPrimaryColor = P1TTheme.onColor(accentColor)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accentColor,
            secondary = P1TColors.SectorGreen,
            tertiary = P1TColors.SectorPurple,
            background = P1TColors.BackgroundPrimary,
            surface = P1TColors.SurfaceCard,
            surfaceVariant = P1TColors.SurfaceElevated,
            onPrimary = onPrimaryColor,
            onBackground = P1TColors.TextPrimary,
            onSurface = P1TColors.TextPrimary,
            onSurfaceVariant = P1TColors.TextSecondary,
            outline = P1TColors.BorderSubtle
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            secondary = Color(0xFF0E9F57),
            tertiary = P1TColors.SectorPurple,
            background = P1TColors.BackgroundLight,
            surface = P1TColors.SurfaceCardLight,
            surfaceVariant = Color(0xFFEEEEF4),
            onPrimary = onPrimaryColor,
            onBackground = Color(0xFF111116),
            onSurface = Color(0xFF111116),
            onSurfaceVariant = Color(0xFF5A5A66),
            outline = P1TColors.BorderLight
        )
    }

    val palette = F1AccentPalette(
        primaryAccent = accentColor,
        headerGlow = glowColor,
        isDark = isDark
    )

    CompositionLocalProvider(LocalF1Accent provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = F1Typography,
            content = content
        )
    }
}

/**
 * Theme-aware semantic color helpers for Light & Dark modes.
 * Preserves Dark mode's obsidian/carbon palette while ensuring high contrast in Light mode.
 */
object P1TTheme {
    val isDark: Boolean
        @Composable get() = LocalF1Accent.current.isDark

    val surfaceInnerCard: Color
        @Composable get() = if (isDark) P1TColors.SurfaceInnerCard else Color(0xFFF1F1F6)

    val borderInnerCard: Color
        @Composable get() = if (isDark) Color(0xFF24242C) else Color(0xFFE0E0EA)

    val surfaceStandingCard: Color
        @Composable get() = if (isDark) Color(0xFF19191B) else Color(0xFFFFFFFF)

    val borderStandingCard: Color
        @Composable get() = if (isDark) Color.White.copy(alpha = 0.07f) else P1TColors.BorderLight

    val dividerSubtle: Color
        @Composable get() = if (isDark) Color(0xFF2E2E38) else Color(0xFFDCDCE6)

    val textMuted: Color
        @Composable get() = if (isDark) P1TColors.TextMuted else Color(0xFF6B6B78)

    val pillBg: Color
        @Composable get() = if (isDark) Color(0xFF1E1E26) else Color(0xFFEBEBF2)

    val pillBorder: Color
        @Composable get() = if (isDark) Color(0xFF383844) else Color(0xFFD0D0DC)

    val pillSelectedBg: Color
        @Composable get() = if (isDark) Color(0xFF282834) else Color(0xFFE2E2EC)

    val trackClearBg: Color
        @Composable get() = if (isDark) P1TColors.TrackClearBg else Color(0xFFE3F7EC)

    val trackClearBorder: Color
        @Composable get() = if (isDark) Color(0xFF1E5B37) else Color(0xFF81D4A4)

    val sectorGreen: Color
        @Composable get() = if (isDark) P1TColors.SectorGreen else Color(0xFF0A8A47)

    val sectorYellow: Color
        @Composable get() = if (isDark) P1TColors.SectorYellow else Color(0xFFA67800)

    val sectorYellowBg: Color
        @Composable get() = if (isDark) Color(0xFF2E2A14) else Color(0xFFFFF6D6)

    val sectorYellowBorder: Color
        @Composable get() = if (isDark) Color(0xFF6B5A18) else Color(0xFFE6C24D)

    val sectorPurple: Color
        @Composable get() = if (isDark) P1TColors.SectorPurple else Color(0xFF8E24AA)

    val redFlagBg: Color
        @Composable get() = if (isDark) Color(0xFF3B1214) else Color(0xFFFDE8E8)

    val redFlagBorder: Color
        @Composable get() = if (isDark) Color(0xFF7A2226) else Color(0xFFF5A3A6)

    val poleGold: Color
        @Composable get() = if (isDark) Color(0xFFF5C518) else Color(0xFF7D5D00)

    val accentRed: Color
        @Composable get() = if (isDark) P1TColors.AccentRed else Color(0xFFC61D15)

    val inPitText: Color
        @Composable get() = if (isDark) Color(0xFF64B5F6) else Color(0xFF1565C0)

    @Composable
    fun accentPillBg(accent: Color): Color =
        if (isDark) accent.copy(alpha = 0.18f) else accent.copy(alpha = 0.12f)

    fun onColor(background: Color): Color {
        val lum = background.luminance()
        val whiteContrast = 1.05f / (lum + 0.05f)
        val blackContrast = (lum + 0.05f) / 0.05f
        return if (blackContrast > whiteContrast) Color.Black else Color.White
    }

    @Composable
    fun adaptiveTeamText(teamColor: Color): Color {
        val lum = teamColor.luminance()
        if (isDark) {
            if (lum >= 0.15f) return teamColor
            val blend = (0.50f - (lum / 0.15f) * 0.30f).coerceIn(0.20f, 0.50f)
            return lerp(teamColor, Color.White.copy(alpha = teamColor.alpha), blend)
        }
        if (lum <= 0.12f) return teamColor
        val scale = Math.pow(0.11 / lum.toDouble(), 1.0 / 2.2)
            .toFloat()
            .coerceIn(0.28f, 0.88f)
        return Color(
            red = teamColor.red * scale,
            green = teamColor.green * scale,
            blue = teamColor.blue * scale,
            alpha = teamColor.alpha
        )
    }
}

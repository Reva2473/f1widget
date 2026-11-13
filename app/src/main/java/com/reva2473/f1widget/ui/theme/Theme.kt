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
    val TextMuted = Color(0xFF686873)

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
    val onPrimaryColor = if (accentColor.luminance() > 0.45f) Color.Black else Color.White

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

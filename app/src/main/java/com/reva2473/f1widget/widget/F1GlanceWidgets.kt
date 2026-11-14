package com.reva2473.f1widget.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.reva2473.f1widget.MainActivity
import com.reva2473.f1widget.data.local.UserPreferencesStore
import com.reva2473.f1widget.data.repository.F1Repository
import com.reva2473.f1widget.data.seed.SeedDataProvider
import com.reva2473.f1widget.domain.model.ConstructorDriverPoints
import com.reva2473.f1widget.domain.model.ConstructorStanding
import com.reva2473.f1widget.domain.model.DriverStanding
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.LiveSessionState
import com.reva2473.f1widget.domain.model.RaceDriverResult
import com.reva2473.f1widget.domain.model.RaceWeekend
import com.reva2473.f1widget.domain.model.SessionScheduleItem
import com.reva2473.f1widget.domain.model.UserPreferencesState
import com.reva2473.f1widget.ui.components.getDriverHeadshotRes
import com.reva2473.f1widget.ui.components.getTeamCarResOrNull
import java.time.Year
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val WIDGET_NETWORK_TIMEOUT_MS = 8_000L
private val DefaultCrimsonRed = Color(0xFFE1251B)

val DriverStanding.driverCode: String
    get() = code

private val TargetScreenActionKey = ActionParameters.Key<String>("EXTRA_TARGET_SCREEN")

fun launchAppAction(context: Context, targetScreen: String = "HOME"): Action {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("EXTRA_TARGET_SCREEN", targetScreen)
        data = Uri.parse("apexgrid://widget/$targetScreen")
    }
    return actionStartActivity(
        intent = intent,
        parameters = actionParametersOf(TargetScreenActionKey to targetScreen)
    )
}

// ============================================================================
// Theme, Contrast & Formatting Helpers
// ============================================================================

private data class WidgetPalette(
    val isDark: Boolean,
    val background: Color,
    val surfaceElevated: Color,
    val badgeSurface: Color,
    val avatarBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentFill: Color,
    val accentText: Color,
    val onAccent: Color,
    val liveRed: Color,
    val sectorGreen: Color,
    val gold: Color,
    val silver: Color,
    val bronze: Color
) {
    fun adaptiveTeamText(teamColor: Color): Color =
        ensureContrast(teamColor, surfaceElevated)
}

private fun contrastRatio(a: Color, b: Color): Double {
    val lumA = a.luminance().toDouble()
    val lumB = b.luminance().toDouble()
    val lighter = max(lumA, lumB)
    val darker = min(lumA, lumB)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun ensureContrast(
    foreground: Color,
    background: Color,
    minRatio: Double = 4.5
): Color {
    if (contrastRatio(foreground, background) >= minRatio) return foreground
    val target = if (background.luminance() < 0.5f) Color.White else Color(0xFF0E0E12)
    var low = 0f
    var high = 1f
    repeat(16) {
        val mid = (low + high) / 2f
        val candidate = lerp(foreground, target, mid)
        if (contrastRatio(candidate, background) >= minRatio) {
            high = mid
        } else {
            low = mid
        }
    }
    return lerp(foreground, target, high)
}

private fun bestOnColor(background: Color): Color {
    val darkText = Color(0xFF111115)
    return if (contrastRatio(Color.White, background) >= contrastRatio(darkText, background)) {
        Color.White
    } else {
        darkText
    }
}

private fun buildWidgetPalette(context: Context, prefs: UserPreferencesState): WidgetPalette {
    val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    val isSystemDark = nightMask == Configuration.UI_MODE_NIGHT_YES
    val isDark = when (prefs.themeMode.uppercase(Locale.ROOT)) {
        "LIGHT" -> false
        "SYSTEM" -> isSystemDark
        else -> true
    }

    val background = if (isDark) Color(0xFF16161A) else Color(0xFFF2F2F7)
    val surfaceElevated = if (isDark) Color(0xFF202026) else Color(0xFFFFFFFF)
    val badgeSurface = if (isDark) Color(0xFF2B2B34) else Color(0xFFE2E2EC)
    val avatarBg = badgeSurface
    val textPrimary = if (isDark) Color(0xFFF5F5F7) else Color(0xFF141418)
    val textSecondary = if (isDark) Color(0xFFB4B4C0) else Color(0xFF42424E)
    val textMuted = if (isDark) Color(0xFFA0A0AE) else Color(0xFF525260)

    val rawAccent = if (prefs.useTeamColorAccent) Color(prefs.favoriteTeam.colorHex) else DefaultCrimsonRed
    val accentRefSurface = if (isDark) badgeSurface else badgeSurface
    val accentText = ensureContrast(rawAccent, accentRefSurface)
    val onAccent = bestOnColor(rawAccent)
    val liveRed = ensureContrast(DefaultCrimsonRed, accentRefSurface)
    val sectorGreen = if (isDark) Color(0xFF2CE580) else Color(0xFF096E38)
    val gold = if (isDark) Color(0xFFF5C518) else Color(0xFF7D5D00)
    val silver = if (isDark) Color(0xFFD0D0D8) else Color(0xFF4A4D58)
    val bronze = if (isDark) Color(0xFFE88D3F) else Color(0xFF8C460A)

    return WidgetPalette(
        isDark = isDark,
        background = background,
        surfaceElevated = surfaceElevated,
        badgeSurface = badgeSurface,
        avatarBg = avatarBg,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textMuted = textMuted,
        accentFill = rawAccent,
        accentText = accentText,
        onAccent = onAccent,
        liveRed = liveRed,
        sectorGreen = sectorGreen,
        gold = gold,
        silver = silver,
        bronze = bronze
    )
}

private fun formatWidgetPoints(points: Double): String {
    return if (points % 1.0 == 0.0) {
        points.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", points).trimEnd('0').trimEnd('.')
    }
}

// ============================================================================
// Async Data & Timeout Helpers
// ============================================================================

private suspend fun loadUserPreferences(context: Context): UserPreferencesState {
    return try {
        withTimeoutOrNull(WIDGET_NETWORK_TIMEOUT_MS) {
            UserPreferencesStore(context.applicationContext).preferencesFlow.first()
        } ?: UserPreferencesState()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        UserPreferencesState()
    }
}

private suspend inline fun <T> withWidgetTimeout(
    timeoutMs: Long = WIDGET_NETWORK_TIMEOUT_MS,
    crossinline block: suspend () -> T
): T? {
    return try {
        withTimeoutOrNull(timeoutMs) {
            block()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        null
    }
}

private suspend fun resolveUpcomingRace(repo: F1Repository, season: Int): RaceWeekend {
    val schedule = withWidgetTimeout { repo.getSeasonSchedule(season) }.orEmpty()
    val nowMillis = System.currentTimeMillis()
    val completionBufferMillis = 3L * 60L * 60L * 1000L
    val upcoming = schedule.firstOrNull { weekend ->
        val raceSession = weekend.sessions.firstOrNull { it.isRace } ?: weekend.sessions.lastOrNull()
        val utcMillis = raceSession?.utcEpochMillis
        when {
            utcMillis != null -> utcMillis + completionBufferMillis > nowMillis
            weekend.daysToGo > 0 -> true
            raceSession != null -> !raceSession.countdownBadge.contains("Completed", ignoreCase = true)
            else -> false
        }
    }
    return upcoming
        ?: schedule.lastOrNull()
        ?: withWidgetTimeout { repo.getNextRaceWeekend() }
        ?: SeedDataProvider.getNextRaceWeekend()
}

// ============================================================================
// Scaled Bitmap & Circular Avatar Caches (IPC-Safe)
// ============================================================================

private val scaledWidgetBitmapCache = LruCache<String, Bitmap>(32)
private val circularAvatarCache = LruCache<String, Bitmap>(32)

private fun computePowerOfTwoSampleSize(
    rawWidth: Int,
    rawHeight: Int,
    targetWidthPx: Int,
    targetHeightPx: Int
): Int {
    var sampleSize = 1
    while (rawWidth / (sampleSize * 2) >= targetWidthPx &&
        rawHeight / (sampleSize * 2) >= targetHeightPx
    ) {
        sampleSize *= 2
    }
    return sampleSize
}

/**
 * Decodes and scales a drawable resource to fit within [targetWidthPx] x [targetHeightPx]
 * so large PNGs are never sent raw across RemoteViews Binder IPC.
 */
private fun loadScaledWidgetBitmap(
    context: Context,
    @DrawableRes resId: Int,
    targetWidthPx: Int,
    targetHeightPx: Int
): Bitmap? {
    if (targetWidthPx <= 0 || targetHeightPx <= 0) return null
    val cacheKey = "$resId:${targetWidthPx}x$targetHeightPx"
    scaledWidgetBitmapCache.get(cacheKey)?.let { cached ->
        if (!cached.isRecycled) return cached
    }

    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, boundsOpts)
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        val sampleSize = computePowerOfTwoSampleSize(
            rawWidth = boundsOpts.outWidth,
            rawHeight = boundsOpts.outHeight,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx
        )
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeResource(context.resources, resId, decodeOpts)
            ?: return null

        val output = if (decoded.width > targetWidthPx || decoded.height > targetHeightPx) {
            val scale = minOf(
                targetWidthPx.toFloat() / decoded.width.toFloat(),
                targetHeightPx.toFloat() / decoded.height.toFloat()
            )
            val scaledWidth = (decoded.width * scale).roundToInt().coerceAtLeast(1)
            val scaledHeight = (decoded.height * scale).roundToInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(decoded, scaledWidth, scaledHeight, true)
            if (scaled !== decoded && !decoded.isRecycled) {
                decoded.recycle()
            }
            scaled
        } else {
            decoded
        }

        scaledWidgetBitmapCache.put(cacheKey, output)
        output
    } catch (_: Throwable) {
        null
    }
}

private fun loadCircularAvatarBitmap(
    context: Context,
    @DrawableRes resId: Int,
    sizePx: Int
): Bitmap? {
    if (sizePx <= 0) return null
    val cacheKey = "$resId:$sizePx"
    circularAvatarCache.get(cacheKey)?.let { cached ->
        if (!cached.isRecycled) return cached
    }

    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, boundsOpts)
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        val sampleSize = computePowerOfTwoSampleSize(
            rawWidth = boundsOpts.outWidth,
            rawHeight = boundsOpts.outHeight,
            targetWidthPx = sizePx,
            targetHeightPx = sizePx
        )
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeResource(context.resources, resId, decodeOpts)
            ?: return null

        val minDim = minOf(decoded.width, decoded.height)
        val srcLeft = (decoded.width - minDim) / 2
        val srcTop = (decoded.height - minDim) / 2
        val srcRect = Rect(srcLeft, srcTop, srcLeft + minDim, srcTop + minDim)
        val dstRect = Rect(0, 0, sizePx, sizePx)

        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
        val radius = sizePx / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(decoded, srcRect, dstRect, paint)

        if (decoded !== output && !decoded.isRecycled) {
            decoded.recycle()
        }

        circularAvatarCache.put(cacheKey, output)
        output
    } catch (_: Throwable) {
        null
    }
}

@Composable
private fun DriverAvatar(
    driverCode: String,
    size: Dp,
    palette: WidgetPalette,
    modifier: GlanceModifier = GlanceModifier
) {
    val headshotRes = getDriverHeadshotRes(driverCode)
    val radius = size / 2
    val context = LocalContext.current
    val sizePx = (size.value * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
    val circularBitmap = headshotRes?.let { loadCircularAvatarBitmap(context, it, sizePx) }
    Box(
        modifier = modifier
            .size(size)
            .background(ColorProvider(palette.avatarBg))
            .cornerRadius(radius),
        contentAlignment = Alignment.Center
    ) {
        if (circularBitmap != null) {
            Image(
                provider = ImageProvider(circularBitmap),
                contentDescription = driverCode,
                modifier = GlanceModifier
                    .size(size)
                    .cornerRadius(radius)
            )
        } else {
            Text(
                text = driverCode.take(3).uppercase(Locale.ROOT),
                style = TextStyle(
                    color = ColorProvider(palette.textPrimary),
                    fontSize = if (size >= 32.dp) 11.sp else 8.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun TeamCarThumbnail(
    team: F1TeamId,
    displayName: String,
    width: Dp,
    height: Dp,
    palette: WidgetPalette,
    modifier: GlanceModifier = GlanceModifier
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val widthPx = (width.value * density).roundToInt().coerceAtLeast(1)
    val heightPx = (height.value * density).roundToInt().coerceAtLeast(1)
    val carRes = getTeamCarResOrNull(team)
    val carBitmap = carRes?.let { loadScaledWidgetBitmap(context, it, widthPx, heightPx) }

    if (carBitmap != null) {
        Image(
            provider = ImageProvider(carBitmap),
            contentDescription = displayName,
            modifier = modifier.width(width).height(height)
        )
    } else {
        val badgeText = if (team != F1TeamId.OTHER) {
            team.shortCode
        } else {
            displayName.trim()
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercase(Locale.ROOT) }
                .ifBlank { "F1" }
        }
        val badgeTextColor = ensureContrast(Color(team.colorHex), palette.badgeSurface)
        Box(
            modifier = modifier
                .width(width)
                .height(height)
                .background(ColorProvider(palette.badgeSurface))
                .cornerRadius(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(badgeTextColor),
                    fontSize = if (height >= 24.dp) 10.sp else 8.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// ============================================================================
// 1. Next Race Countdown Widget
// ============================================================================
class NextRaceCountdownWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val palette = buildWidgetPalette(context, prefs)
        val repo = F1Repository(context.applicationContext)
        val activeYear = Year.now().value
        val race = resolveUpcomingRace(repo, activeYear)
        val raceSession = race.sessions.find {
            it.isRace || it.fullName.equals("Race", ignoreCase = true) || it.shortName.equals("Race", ignoreCase = true)
        } ?: race.sessions.lastOrNull()
        val qualiSession = race.sessions.find {
            it.shortName.equals("Quali", ignoreCase = true) || it.fullName.contains("Qualifying", ignoreCase = true)
        }

        provideContent {
            val size = LocalSize.current
            val isCompactWidth = size.width < 220.dp
            val isCompactHeight = size.height < 130.dp
            val isTall = size.height >= 190.dp

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(palette.background))
                        .cornerRadius(16.dp)
                        .padding(if (isCompactHeight) 8.dp else 10.dp)
                        .clickable(launchAppAction(context, "NEXT_RACE"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isCompactWidth) "${race.countryFlagEmoji} NEXT GP" else "${race.countryFlagEmoji} NEXT GRAND PRIX",
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(
                                    color = ColorProvider(palette.accentText),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Box(
                                modifier = GlanceModifier
                                    .background(ColorProvider(palette.accentFill))
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                val badgeText = if (race.daysToGo == 0) {
                                    "R${race.round} · RACE WEEK"
                                } else if (isCompactWidth) {
                                    "R${race.round} · ${race.daysToGo}d"
                                } else {
                                    "R${race.round} · in ${race.daysToGo}d"
                                }
                                Text(
                                    text = badgeText,
                                    style = TextStyle(
                                        color = ColorProvider(palette.onAccent),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(if (isCompactHeight) 2.dp else 4.dp))

                        Text(
                            text = race.grandPrixName,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(palette.textPrimary),
                                fontSize = if (isCompactWidth) 13.sp else 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            text = if (isCompactWidth) race.dateRangeText else "${race.circuitName} · ${race.dateRangeText}",
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(palette.textSecondary),
                                fontSize = 10.sp
                            )
                        )

                        if (isTall && qualiSession != null) {
                            Spacer(modifier = GlanceModifier.height(6.dp))
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(ColorProvider(palette.surfaceElevated))
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "QUALIFYING",
                                    style = TextStyle(
                                        color = ColorProvider(palette.textMuted),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                Text(
                                    text = "${qualiSession.dayOfWeek} ${qualiSession.dateDay} ${qualiSession.dateMonth} · ${qualiSession.localTime}",
                                    style = TextStyle(
                                        color = ColorProvider(palette.textPrimary),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorProvider(palette.surfaceElevated))
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = if (isCompactHeight) 3.dp else 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "LAPS",
                                    style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 8.sp)
                                )
                                Text(
                                    text = "${race.totalLaps}",
                                    style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            if (!isCompactWidth) {
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = "CIRCUIT",
                                        style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 8.sp)
                                    )
                                    Text(
                                        text = "${race.circuitLengthKm} km",
                                        style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = "CORNERS",
                                        style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 8.sp)
                                    )
                                    Text(
                                        text = "${race.totalCorners}",
                                        style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "RACE TIME",
                                    style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 8.sp)
                                )
                                Text(
                                    text = raceSession?.localTime ?: "TBD",
                                    style = TextStyle(color = ColorProvider(palette.sectorGreen), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class NextRaceCountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextRaceCountdownWidget()
}

// ============================================================================
// 2. Weekend Session Schedule Widget
// ============================================================================
class WeekendScheduleWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val palette = buildWidgetPalette(context, prefs)
        val repo = F1Repository(context.applicationContext)
        val activeYear = Year.now().value
        val race = resolveUpcomingRace(repo, activeYear)
        val allSessions: List<SessionScheduleItem> = race.sessions

        provideContent {
            val size = LocalSize.current
            val isCompactWidth = size.width < 230.dp
            val maxRows = when {
                size.height < 140.dp -> 2
                size.height < 190.dp -> 3
                else -> 5
            }
            val sessions = if (allSessions.size > maxRows) {
                allSessions.takeLast(maxRows)
            } else {
                allSessions
            }

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(palette.background))
                        .cornerRadius(16.dp)
                        .padding(10.dp)
                        .clickable(launchAppAction(context, "WEEKEND_SCHEDULE"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${race.countryFlagEmoji} ${race.grandPrixName}",
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(
                                    color = ColorProvider(palette.textPrimary),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = if (isCompactWidth) "SCHEDULE" else "WEEKEND SCHEDULE",
                                style = TextStyle(
                                    color = ColorProvider(palette.accentText),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        if (sessions.isEmpty()) {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .background(ColorProvider(palette.surfaceElevated))
                                    .cornerRadius(8.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Session times pending (${race.dateRangeText})",
                                    maxLines = 2,
                                    style = TextStyle(
                                        color = ColorProvider(palette.textSecondary),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        } else {
                            sessions.forEachIndexed { index, session ->
                                val isRace = session.isRace
                                val rowBg = if (isRace) palette.surfaceElevated else palette.background
                                val labelColor = if (isRace) palette.accentText else palette.textPrimary
                                val badgeBg = if (isRace) palette.accentFill else palette.badgeSurface
                                val badgeTextColor = if (isRace) palette.onAccent else palette.textPrimary

                                Row(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .background(ColorProvider(rowBg))
                                        .cornerRadius(6.dp)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = GlanceModifier
                                            .width(36.dp)
                                            .background(ColorProvider(badgeBg))
                                            .cornerRadius(4.dp)
                                            .padding(vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = session.shortName,
                                            style = TextStyle(
                                                color = ColorProvider(badgeTextColor),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Spacer(modifier = GlanceModifier.width(6.dp))

                                    Text(
                                        text = session.fullName,
                                        maxLines = 1,
                                        modifier = GlanceModifier.defaultWeight(),
                                        style = TextStyle(
                                            color = ColorProvider(labelColor),
                                            fontSize = 10.sp,
                                            fontWeight = if (isRace) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )

                                    if (!isCompactWidth) {
                                        Text(
                                            text = "${session.dayOfWeek} ${session.dateDay} ${session.dateMonth}",
                                            style = TextStyle(
                                                color = ColorProvider(palette.textSecondary),
                                                fontSize = 9.sp
                                            )
                                        )
                                        Spacer(modifier = GlanceModifier.width(6.dp))
                                    }

                                    Text(
                                        text = session.localTime,
                                        style = TextStyle(
                                            color = ColorProvider(if (isRace) palette.gold else palette.textPrimary),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                if (index < sessions.size - 1) {
                                    Spacer(modifier = GlanceModifier.height(2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class WeekendScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekendScheduleWidget()
}

// ============================================================================
// 3. Driver Standings Widget (WDC)
// ============================================================================
class DriverStandingsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val palette = buildWidgetPalette(context, prefs)
        val repo = F1Repository(context.applicationContext)
        val currentYear = Year.now().value
        val standingsList = withWidgetTimeout { repo.getDriverStandings(currentYear) }.orEmpty()
        val displayYear = if (standingsList.isNotEmpty()) {
            currentYear
        } else {
            SeedDataProvider.getNextRaceWeekend().season
        }
        val standings: List<DriverStanding> = standingsList
            .ifEmpty { SeedDataProvider.getDriverStandings2026() }
            .take(5)

        provideContent {
            val size = LocalSize.current
            val isCompactWidth = size.width < 220.dp
            val isCompactHeight = size.height < 190.dp
            val maxRows = when {
                size.height < 140.dp -> 2
                isCompactHeight -> 3
                else -> 5
            }
            val visibleStandings = standings.take(maxRows)

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(palette.background))
                        .cornerRadius(16.dp)
                        .padding(8.dp)
                        .clickable(launchAppAction(context, "DRIVER_STANDINGS"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isCompactWidth) "DRIVERS" else "DRIVERS CHAMPIONSHIP",
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(
                                    color = ColorProvider(palette.accentText),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = "WDC $displayYear",
                                style = TextStyle(
                                    color = ColorProvider(palette.textMuted),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        visibleStandings.forEachIndexed { index, driver ->
                            val teamColor = Color(driver.team.colorHex)
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(ColorProvider(palette.surfaceElevated))
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "P${driver.position}",
                                    style = TextStyle(
                                        color = ColorProvider(if (driver.position == 1) palette.gold else palette.textPrimary),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                Box(
                                    modifier = GlanceModifier
                                        .width(3.dp)
                                        .height(14.dp)
                                        .background(ColorProvider(teamColor))
                                        .cornerRadius(1.dp)
                                ) {}

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                if (!isCompactWidth) {
                                    DriverAvatar(
                                        driverCode = driver.code,
                                        size = 20.dp,
                                        palette = palette
                                    )
                                    Spacer(modifier = GlanceModifier.width(6.dp))
                                }

                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = if (isCompactWidth) driver.code else driver.fullName,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = ColorProvider(palette.textPrimary),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    if (!isCompactHeight) {
                                        Text(
                                            text = driver.team.displayName,
                                            maxLines = 1,
                                            style = TextStyle(
                                                color = ColorProvider(palette.textSecondary),
                                                fontSize = 8.sp
                                            )
                                        )
                                    }
                                }

                                if (!isCompactWidth && driver.wins > 0) {
                                    Text(
                                        text = "${driver.wins}W",
                                        style = TextStyle(
                                            color = ColorProvider(palette.gold),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = GlanceModifier.width(6.dp))
                                }

                                Text(
                                    text = "${formatWidgetPoints(driver.points)} PTS",
                                    style = TextStyle(
                                        color = ColorProvider(palette.textPrimary),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            if (index < visibleStandings.size - 1) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

class DriverStandingsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DriverStandingsWidget()
}

// ============================================================================
// 4. Constructor Standings Widget (WCC)
// ============================================================================
class ConstructorStandingsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val palette = buildWidgetPalette(context, prefs)
        val repo = F1Repository(context.applicationContext)
        val currentYear = Year.now().value
        val standingsList = withWidgetTimeout { repo.getConstructorStandings(currentYear) }.orEmpty()
        val displayYear = if (standingsList.isNotEmpty()) {
            currentYear
        } else {
            SeedDataProvider.getNextRaceWeekend().season
        }
        val standings: List<ConstructorStanding> = standingsList
            .ifEmpty { SeedDataProvider.getConstructorStandings2026() }
            .take(5)

        provideContent {
            val size = LocalSize.current
            val isCompactWidth = size.width < 220.dp
            val showGap = size.width >= 260.dp
            val maxRows = when {
                size.height < 140.dp -> 2
                size.height < 190.dp -> 3
                else -> 5
            }
            val visibleStandings = standings.take(maxRows)

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(palette.background))
                        .cornerRadius(16.dp)
                        .padding(8.dp)
                        .clickable(launchAppAction(context, "CONSTRUCTOR_STANDINGS"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isCompactWidth) "CONSTRUCTORS" else "CONSTRUCTORS CHAMPIONSHIP",
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(
                                    color = ColorProvider(palette.accentText),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = "WCC $displayYear",
                                style = TextStyle(
                                    color = ColorProvider(palette.textMuted),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        visibleStandings.forEachIndexed { index, constructor ->
                            val teamColor = Color(constructor.team.colorHex)
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(ColorProvider(palette.surfaceElevated))
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "P${constructor.position}",
                                    style = TextStyle(
                                        color = ColorProvider(if (constructor.position == 1) palette.gold else palette.textPrimary),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                Box(
                                    modifier = GlanceModifier
                                        .width(3.dp)
                                        .height(14.dp)
                                        .background(ColorProvider(teamColor))
                                        .cornerRadius(1.dp)
                                ) {}

                                Spacer(modifier = GlanceModifier.width(6.dp))

                                if (!isCompactWidth) {
                                    TeamCarThumbnail(
                                        team = constructor.team,
                                        displayName = constructor.resolvedConstructorName,
                                        width = 32.dp,
                                        height = 16.dp,
                                        palette = palette
                                    )
                                    Spacer(modifier = GlanceModifier.width(6.dp))
                                }

                                Text(
                                    text = constructor.resolvedConstructorName,
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight(),
                                    style = TextStyle(
                                        color = ColorProvider(palette.textPrimary),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                val gap = constructor.gapToP1
                                if (showGap && gap != null) {
                                    Text(
                                        text = gap,
                                        style = TextStyle(
                                            color = ColorProvider(palette.textSecondary),
                                            fontSize = 8.sp
                                        )
                                    )
                                    Spacer(modifier = GlanceModifier.width(4.dp))
                                }

                                Text(
                                    text = "${formatWidgetPoints(constructor.points)} PTS",
                                    style = TextStyle(
                                        color = ColorProvider(palette.textPrimary),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            if (index < visibleStandings.size - 1) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

class ConstructorStandingsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ConstructorStandingsWidget()
}

// ============================================================================
// 5. Favorite Driver Widget (2x2 Small)
// ============================================================================
class FavoriteDriverWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val palette = buildWidgetPalette(context, prefs)
        val repo = F1Repository(context.applicationContext)
        val currentYear = Year.now().value
        val standingsList = withWidgetTimeout { repo.getDriverStandings(currentYear) }.orEmpty()
        val standings = standingsList.ifEmpty { SeedDataProvider.getDriverStandings2026() }
        val targetCode = prefs.favoriteDriverCode
        val driver: DriverStanding = standings.firstOrNull {
            it.code.equals(targetCode, ignoreCase = true) ||
                it.driverId.equals(targetCode, ignoreCase = true) ||
                it.fullName.contains(targetCode, ignoreCase = true)
        } ?: standings.firstOrNull() ?: SeedDataProvider.getDriverStandings2026().first()
        val rawTeamColor = Color(driver.team.colorHex)
        val teamAccentColor = palette.adaptiveTeamText(rawTeamColor)

        provideContent {
            val size = LocalSize.current
            val isCompactHeight = size.height < 130.dp
            val isWide = size.width >= 230.dp
            val isTall = size.height >= 190.dp
            val avatarSize = when {
                isCompactHeight -> 28.dp
                isWide || isTall -> 40.dp
                else -> 34.dp
            }

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(palette.background))
                        .cornerRadius(16.dp)
                        .padding(8.dp)
                        .clickable(launchAppAction(context, "DRIVER_STANDINGS"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .width(3.dp)
                                    .height(12.dp)
                                    .background(ColorProvider(if (prefs.useTeamColorAccent) palette.accentFill else rawTeamColor))
                                    .cornerRadius(1.dp)
                            ) {}
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = driver.team.displayName.uppercase(Locale.ROOT),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(
                                    color = ColorProvider(teamAccentColor),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "P${driver.position}",
                                style = TextStyle(
                                    color = ColorProvider(palette.gold),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = driver.code,
                                    style = TextStyle(
                                        color = ColorProvider(palette.textPrimary),
                                        fontSize = if (isCompactHeight) 15.sp else 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = driver.fullName,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(palette.textSecondary),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            DriverAvatar(
                                driverCode = driver.code,
                                size = avatarSize,
                                palette = palette
                            )
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(ColorProvider(palette.surfaceElevated))
                                .cornerRadius(6.dp)
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "POINTS",
                                    style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 8.sp)
                                )
                                Text(
                                    text = formatWidgetPoints(driver.points),
                                    style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            if (isWide) {
                                Column(
                                    modifier = GlanceModifier.defaultWeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "PODIUMS",
                                        style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 8.sp)
                                    )
                                    Text(
                                        text = "${driver.podiums}",
                                        style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "WINS",
                                    style = TextStyle(color = ColorProvider(palette.textMuted), fontSize = 8.sp)
                                )
                                Text(
                                    text = "${driver.wins}",
                                    style = TextStyle(color = ColorProvider(palette.gold), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        if (!isCompactHeight) {
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = driver.last3Summary,
                                maxLines = 1,
                                style = TextStyle(
                                    color = ColorProvider(palette.sectorGreen),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

class FavoriteDriverWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FavoriteDriverWidget()
}

// ============================================================================
// 6. Favorite Team Widget (2x2 Small)
// ============================================================================
class FavoriteTeamWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val palette = buildWidgetPalette(context, prefs)
        val repo = F1Repository(context.applicationContext)
        val currentYear = Year.now().value

        val (standingsList, networkDrivers) = withWidgetTimeout {
            val constructors = repo.getConstructorStandings(currentYear)
            val target = findFavoriteConstructor(constructors, prefs.favoriteTeam)
            val drivers = if (target?.drivers.isNullOrEmpty()) {
                repo.getDriverStandings(currentYear)
            } else {
                emptyList()
            }
            constructors to drivers
        } ?: (emptyList<ConstructorStanding>() to emptyList())

        val standings = standingsList.ifEmpty { SeedDataProvider.getConstructorStandings2026() }
        val constructor = findFavoriteConstructor(standings, prefs.favoriteTeam)
            ?: standings.firstOrNull()
            ?: SeedDataProvider.getConstructorStandings2026().first()

        val driverPair: List<ConstructorDriverPoints> = if (constructor.drivers.isNotEmpty()) {
            constructor.drivers.take(2)
        } else {
            val derivedFromNetwork = networkDrivers
                .filter { it.team == constructor.team }
                .take(2)
                .map { ConstructorDriverPoints(fullName = it.fullName, points = it.points) }
            derivedFromNetwork.ifEmpty {
                SeedDataProvider.getDriverStandings2026()
                    .filter { it.team == constructor.team }
                    .take(2)
                    .map { ConstructorDriverPoints(fullName = it.fullName, points = it.points) }
            }
        }
        val rawTeamColor = Color(constructor.team.colorHex)
        val teamAccentColor = palette.adaptiveTeamText(rawTeamColor)

        provideContent {
            val size = LocalSize.current
            val isCompactHeight = size.height < 130.dp
            val isTall = size.height >= 190.dp
            val carWidth = (size.width - 16.dp).coerceAtLeast(64.dp)
            val carHeight = when {
                isCompactHeight -> 22.dp
                isTall -> 36.dp
                else -> 26.dp
            }
            val visibleDrivers = if (isCompactHeight) driverPair.take(1) else driverPair.take(2)

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(palette.background))
                        .cornerRadius(16.dp)
                        .padding(8.dp)
                        .clickable(launchAppAction(context, "FAVORITE_TEAM"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .width(3.dp)
                                    .height(12.dp)
                                    .background(ColorProvider(if (prefs.useTeamColorAccent) palette.accentFill else rawTeamColor))
                                    .cornerRadius(1.dp)
                            ) {}
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Text(
                                text = "GARAGE",
                                style = TextStyle(
                                    color = ColorProvider(palette.accentText),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "P${constructor.position}",
                                style = TextStyle(
                                    color = ColorProvider(palette.gold),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        Text(
                            text = constructor.resolvedConstructorName,
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(palette.textPrimary),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            text = "${formatWidgetPoints(constructor.points)} PTS · ${constructor.wins} WINS",
                            maxLines = 1,
                            style = TextStyle(
                                color = ColorProvider(teamAccentColor),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        TeamCarThumbnail(
                            team = constructor.team,
                            displayName = constructor.resolvedConstructorName,
                            width = carWidth,
                            height = carHeight,
                            palette = palette,
                            modifier = GlanceModifier.fillMaxWidth()
                        )

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        if (visibleDrivers.isNotEmpty()) {
                            Column(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(ColorProvider(palette.surfaceElevated))
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                visibleDrivers.forEachIndexed { i, drv ->
                                    Row(
                                        modifier = GlanceModifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = drv.fullName,
                                            maxLines = 1,
                                            modifier = GlanceModifier.defaultWeight(),
                                            style = TextStyle(color = ColorProvider(palette.textPrimary), fontSize = 9.sp)
                                        )
                                        Text(
                                            text = "${formatWidgetPoints(drv.points)} pts",
                                            style = TextStyle(
                                                color = ColorProvider(palette.textSecondary),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    if (i == 0 && visibleDrivers.size > 1) {
                                        Spacer(modifier = GlanceModifier.height(1.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun findFavoriteConstructor(
    standings: List<ConstructorStanding>,
    targetTeam: F1TeamId
): ConstructorStanding? {
    return standings.firstOrNull {
        it.team == targetTeam ||
            it.team.name.equals(targetTeam.name, ignoreCase = true) ||
            it.resolvedConstructorName.contains(targetTeam.displayName, ignoreCase = true)
    }
}

class FavoriteTeamWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FavoriteTeamWidget()
}

// ============================================================================
// 7. Last Race Podium Widget (4x2 Medium)
// ============================================================================
class LastRacePodiumWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val palette = buildWidgetPalette(context, prefs)
        val repo = F1Repository(context.applicationContext)
        val activeYear = Year.now().value
        val latestRace: RaceWeekend? = withWidgetTimeout { repo.getLatestCompletedRace(activeYear) }
        val results: List<RaceDriverResult> = latestRace?.results.orEmpty().take(5)

        provideContent {
            val size = LocalSize.current
            val isCompactWidth = size.width < 230.dp
            val showStints = size.width >= 270.dp && size.height >= 120.dp
            val maxRows = when {
                size.height < 115.dp -> 1
                size.height < 190.dp -> 3
                else -> 5
            }
            val visibleResults = results.take(maxRows)

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(palette.background))
                        .cornerRadius(16.dp)
                        .padding(8.dp)
                        .clickable(launchAppAction(context, "LAST_RACE_RESULTS"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val titleRace = latestRace?.grandPrixName?.uppercase(Locale.ROOT)
                            Text(
                                text = if (titleRace != null) "LAST RACE PODIUM · $titleRace" else "LAST RACE PODIUM",
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(
                                    color = ColorProvider(palette.accentText),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (!isCompactWidth) {
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Text(
                                    text = if (results.isEmpty()) "PENDING" else "FINAL CLASSIFICATION",
                                    style = TextStyle(
                                        color = ColorProvider(palette.textMuted),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        if (visibleResults.isEmpty()) {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .background(ColorProvider(palette.surfaceElevated))
                                    .cornerRadius(8.dp)
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Awaiting Race Result",
                                        style = TextStyle(
                                            color = ColorProvider(palette.textPrimary),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = GlanceModifier.height(2.dp))
                                    Text(
                                        text = "Official classification will appear after the race",
                                        maxLines = 2,
                                        style = TextStyle(
                                            color = ColorProvider(palette.textSecondary),
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        } else {
                            visibleResults.forEachIndexed { index, driver ->
                                val posColor = when (driver.position) {
                                    1 -> palette.gold
                                    2 -> palette.silver
                                    3 -> palette.bronze
                                    else -> palette.textPrimary
                                }
                                val teamColor = Color(driver.team.colorHex)

                                Row(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .background(ColorProvider(palette.surfaceElevated))
                                        .cornerRadius(6.dp)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "P${driver.position}",
                                        style = TextStyle(
                                            color = ColorProvider(posColor),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )

                                    Spacer(modifier = GlanceModifier.width(6.dp))

                                    Box(
                                        modifier = GlanceModifier
                                            .width(3.dp)
                                            .height(14.dp)
                                            .background(ColorProvider(teamColor))
                                            .cornerRadius(1.dp)
                                    ) {}

                                    Spacer(modifier = GlanceModifier.width(6.dp))

                                    if (!isCompactWidth) {
                                        DriverAvatar(
                                            driverCode = driver.driverCode,
                                            size = 20.dp,
                                            palette = palette
                                        )
                                        Spacer(modifier = GlanceModifier.width(6.dp))
                                    }

                                    Column(modifier = GlanceModifier.defaultWeight()) {
                                        Text(
                                            text = if (isCompactWidth) driver.driverCode else "${driver.driverCode} · ${driver.driverFullName}",
                                            maxLines = 1,
                                            style = TextStyle(
                                                color = ColorProvider(palette.textPrimary),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "${driver.team.displayName} · ${driver.formattedPoints} PTS",
                                            maxLines = 1,
                                            style = TextStyle(
                                                color = ColorProvider(palette.textSecondary),
                                                fontSize = 8.sp
                                            )
                                        )
                                    }

                                    if (showStints && driver.stints.isNotEmpty()) {
                                        val stintSummary = driver.stints.joinToString(" ➔ ") { "${it.compound.code}(${it.lapsCount})" }
                                        Text(
                                            text = stintSummary,
                                            style = TextStyle(
                                                color = ColorProvider(palette.textMuted),
                                                fontSize = 8.sp
                                            )
                                        )
                                        Spacer(modifier = GlanceModifier.width(6.dp))
                                    }

                                    Text(
                                        text = driver.finishTimeOrGap,
                                        style = TextStyle(
                                            color = ColorProvider(if (driver.position == 1) palette.sectorGreen else palette.textPrimary),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                if (index < visibleResults.size - 1) {
                                    Spacer(modifier = GlanceModifier.height(2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class LastRacePodiumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LastRacePodiumWidget()
}

// ============================================================================
// 8. Live Timing Banner Widget
// ============================================================================
class LiveTimingBannerWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = loadUserPreferences(context)
        val palette = buildWidgetPalette(context, prefs)
        val repo = F1Repository(context.applicationContext)
        val liveSession: LiveSessionState = withWidgetTimeout { repo.getLiveSessionState() }
            ?: SeedDataProvider.getLiveSessionState()

        provideContent {
            val size = LocalSize.current
            val isCompactWidth = size.width < 220.dp
            val maxDrivers = when {
                size.height < 130.dp -> 1
                size.height < 190.dp -> 2
                else -> 3
            }
            val topDrivers = liveSession.drivers.take(maxDrivers)
            val statusColor = when {
                liveSession.trackStatusText.contains("Red", ignoreCase = true) -> palette.liveRed
                liveSession.trackStatusText.contains("Yellow", ignoreCase = true) ||
                    liveSession.trackStatusText.contains("Safety", ignoreCase = true) ||
                    liveSession.trackStatusText.contains("VSC", ignoreCase = true) -> palette.gold
                else -> palette.sectorGreen
            }

            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(palette.background))
                        .cornerRadius(16.dp)
                        .padding(10.dp)
                        .clickable(launchAppAction(context, "LIVE_TIMING"))
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE · ${liveSession.shortSessionCode}",
                                style = TextStyle(
                                    color = ColorProvider(if (prefs.useTeamColorAccent) palette.accentText else palette.liveRed),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Box(
                                modifier = GlanceModifier
                                    .background(ColorProvider(palette.surfaceElevated))
                                    .cornerRadius(4.dp)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isCompactWidth) {
                                        liveSession.timeRemaining
                                    } else {
                                        "${liveSession.timeRemaining} · ${liveSession.trackStatusText.uppercase(Locale.ROOT)}"
                                    },
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(statusColor),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            if (!isCompactWidth) {
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                Text(
                                    text = liveSession.grandPrixName,
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight(),
                                    style = TextStyle(
                                        color = ColorProvider(palette.textSecondary),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        if (topDrivers.isEmpty()) {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .background(ColorProvider(palette.surfaceElevated))
                                    .cornerRadius(6.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Awaiting live timing feed",
                                    style = TextStyle(
                                        color = ColorProvider(palette.textSecondary),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        } else {
                            topDrivers.forEachIndexed { index, driver ->
                                Row(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .background(ColorProvider(palette.surfaceElevated))
                                        .cornerRadius(6.dp)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "P${driver.position}",
                                        style = TextStyle(
                                            color = ColorProvider(if (driver.position == 1) palette.gold else palette.textPrimary),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = GlanceModifier.width(6.dp))
                                    DriverAvatar(
                                        driverCode = driver.code,
                                        size = 20.dp,
                                        palette = palette
                                    )
                                    Spacer(modifier = GlanceModifier.width(6.dp))
                                    Column(modifier = GlanceModifier.defaultWeight()) {
                                        Text(
                                            text = if (isCompactWidth) driver.code else "${driver.code} · ${driver.fullName}",
                                            maxLines = 1,
                                            style = TextStyle(
                                                color = ColorProvider(palette.textPrimary),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "BEST: ${driver.bestLapTime} · LAST: ${driver.lastLapTime}",
                                            maxLines = 1,
                                            style = TextStyle(
                                                color = ColorProvider(palette.textSecondary),
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                    if (!isCompactWidth) {
                                        Spacer(modifier = GlanceModifier.width(4.dp))
                                        Text(
                                            text = if (driver.position == 1) "LEADER" else driver.gapToLeader,
                                            style = TextStyle(
                                                color = ColorProvider(if (driver.position == 1) palette.sectorGreen else palette.textMuted),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                                if (index < topDrivers.size - 1) {
                                    Spacer(modifier = GlanceModifier.height(3.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class LiveTimingBannerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiveTimingBannerWidget()
}

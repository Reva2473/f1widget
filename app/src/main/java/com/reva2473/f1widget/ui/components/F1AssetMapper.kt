package com.reva2473.f1widget.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.R
import com.reva2473.f1widget.domain.model.F1TeamId

private data class DriverAssetSpec(
    val code: String,
    val firstNames: Set<String>,
    val lastNames: Set<String>,
    @DrawableRes val headshotRes: Int,
    @DrawableRes val cutoutRes: Int
)

private val DRIVER_NAME_SPLIT_REGEX = Regex("[\\s_\\-.]+")

private val DRIVER_ASSET_SPECS = listOf(
    DriverAssetSpec("VER", setOf("max"), setOf("verstappen"), R.drawable.driver_ver, R.drawable.driver_full_ver),
    DriverAssetSpec("HAM", setOf("lewis"), setOf("hamilton"), R.drawable.driver_ham, R.drawable.driver_full_ham),
    DriverAssetSpec("LEC", setOf("charles"), setOf("leclerc"), R.drawable.driver_lec, R.drawable.driver_full_lec),
    DriverAssetSpec("NOR", setOf("lando"), setOf("norris"), R.drawable.driver_nor, R.drawable.driver_full_nor),
    DriverAssetSpec("PIA", setOf("oscar"), setOf("piastri"), R.drawable.driver_pia, R.drawable.driver_full_pia),
    DriverAssetSpec("RUS", setOf("george"), setOf("russell"), R.drawable.driver_rus, R.drawable.driver_full_rus),
    DriverAssetSpec("ANT", setOf("andrea", "kimi"), setOf("antonelli"), R.drawable.driver_ant, R.drawable.driver_full_ant),
    DriverAssetSpec("ALO", setOf("fernando"), setOf("alonso"), R.drawable.driver_alo, R.drawable.driver_full_alo),
    DriverAssetSpec("STR", setOf("lance"), setOf("stroll"), R.drawable.driver_str, R.drawable.driver_full_str),
    DriverAssetSpec("GAS", setOf("pierre"), setOf("gasly"), R.drawable.driver_gas, R.drawable.driver_full_gas),
    DriverAssetSpec("OCO", setOf("esteban"), setOf("ocon"), R.drawable.driver_oco, R.drawable.driver_full_oco),
    DriverAssetSpec("ALB", setOf("alex", "alexander"), setOf("albon"), R.drawable.driver_alb, R.drawable.driver_full_alb),
    DriverAssetSpec("SAI", setOf("carlos"), setOf("sainz"), R.drawable.driver_sai, R.drawable.driver_full_sai),
    DriverAssetSpec("LAW", setOf("liam"), setOf("lawson"), R.drawable.driver_law, R.drawable.driver_full_law),
    DriverAssetSpec("HAD", setOf("isack"), setOf("hadjar"), R.drawable.driver_had, R.drawable.driver_full_had),
    DriverAssetSpec("COL", setOf("franco"), setOf("colapinto"), R.drawable.driver_col, R.drawable.driver_full_col),
    DriverAssetSpec("BEA", setOf("oliver", "ollie"), setOf("bearman"), R.drawable.driver_bea, R.drawable.driver_full_bea),
    DriverAssetSpec("HUL", setOf("nico"), setOf("hulkenberg", "hülkenberg"), R.drawable.driver_hul, R.drawable.driver_full_hul),
    DriverAssetSpec("BOR", setOf("gabriel"), setOf("bortoleto"), R.drawable.driver_bor, R.drawable.driver_full_bor),
    DriverAssetSpec("TSU", setOf("yuki"), setOf("tsunoda"), R.drawable.driver_tsu, R.drawable.driver_full_tsu),
    DriverAssetSpec("DOO", setOf("jack"), setOf("doohan"), R.drawable.driver_doo, R.drawable.driver_full_doo),
    DriverAssetSpec("BOT", setOf("valtteri"), setOf("bottas"), R.drawable.driver_bot, R.drawable.driver_full_bot),
    DriverAssetSpec("LIN", setOf("arvid"), setOf("lindblad"), R.drawable.driver_lin, R.drawable.driver_full_lin),
    DriverAssetSpec("PER", setOf("sergio"), setOf("perez", "pérez"), R.drawable.driver_per, R.drawable.driver_full_per)
)

private fun resolveDriverAssetSpec(codeOrName: String): DriverAssetSpec? {
    val trimmed = codeOrName.trim()
    if (trimmed.isEmpty()) return null

    // 1. Exact 3-letter driver code match first
    val upper = trimmed.uppercase()
    if (upper.length == 3) {
        DRIVER_ASSET_SPECS.firstOrNull { it.code == upper }?.let { return it }
    }

    // 2. Tokenized full name / driverId match (guards against historical relatives like Jos Verstappen)
    val tokens = trimmed.lowercase().split(DRIVER_NAME_SPLIT_REGEX).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null

    if (tokens.size == 1) {
        val single = tokens[0]
        return DRIVER_ASSET_SPECS.firstOrNull { single in it.lastNames }
    }

    return DRIVER_ASSET_SPECS.firstOrNull { spec ->
        tokens.any { it in spec.lastNames } && tokens.any { it in spec.firstNames }
    }
}

/**
 * Resolves real 2025/2026 F1 driver circular headshot drawable resource.
 */
@DrawableRes
fun getDriverHeadshotRes(codeOrName: String): Int? =
    resolveDriverAssetSpec(codeOrName)?.headshotRes

/**
 * Resolves real 2025/2026 F1 driver full upper-body cutout drawable resource.
 */
@DrawableRes
fun getDriverCutoutRes(codeOrName: String): Int? =
    resolveDriverAssetSpec(codeOrName)?.cutoutRes

/**
 * Resolves real 2025/2026 F1 team car side-profile cutout drawable resource.
 */
@DrawableRes
fun getTeamCarRes(teamIdOrName: String): Int {
    return getTeamCarRes(resolveTeamId(teamIdOrName))
}

internal fun resolveTeamId(teamIdOrName: String): F1TeamId {
    val trimmed = teamIdOrName.trim()
    val byShortCode = F1TeamId.entries.firstOrNull {
        it != F1TeamId.OTHER && it.shortCode.equals(trimmed, ignoreCase = true)
    }
    return byShortCode ?: F1TeamId.fromName(trimmed)
}

@DrawableRes
fun getTeamCarResOrNull(team: F1TeamId): Int? {
    return when (team) {
        F1TeamId.MERCEDES -> R.drawable.car_mercedes
        F1TeamId.FERRARI -> R.drawable.car_ferrari
        F1TeamId.RED_BULL -> R.drawable.car_red_bull
        F1TeamId.MCLAREN -> R.drawable.car_mclaren
        F1TeamId.RACING_BULLS -> R.drawable.car_racing_bulls
        F1TeamId.ALPINE -> R.drawable.car_alpine
        F1TeamId.ASTON_MARTIN -> R.drawable.car_aston_martin
        F1TeamId.HAAS -> R.drawable.car_haas
        F1TeamId.WILLIAMS -> R.drawable.car_williams
        F1TeamId.AUDI_SAUBER -> R.drawable.car_audi_sauber
        F1TeamId.CADILLAC -> R.drawable.car_cadillac
        F1TeamId.OTHER -> null
    }
}

@DrawableRes
fun getTeamCarRes(team: F1TeamId): Int {
    return getTeamCarResOrNull(team) ?: R.drawable.car_haas
}

@DrawableRes
fun getTeamCarResByColorOrNull(color: Color): Int? {
    val targetArgb = color.toArgb()
    val matched = F1TeamId.entries.firstOrNull {
        Color(it.colorHex).toArgb() == targetArgb || Color(it.secondaryHex).toArgb() == targetArgb
    }
    if (matched != null) return getTeamCarResOrNull(matched)

    return when {
        // Cadillac gold
        color.red > 0.65f && color.green in 0.50f..0.85f && color.blue < 0.45f -> R.drawable.car_cadillac
        // Ferrari crimson vs Audi neon red-orange
        color.red > 0.85f && color.green in 0.12f..0.32f && color.blue < 0.12f -> R.drawable.car_audi_sauber
        color.red > 0.7f && color.green < 0.2f && color.blue < 0.3f -> R.drawable.car_ferrari
        // Mercedes teal
        color.green > 0.7f && color.blue > 0.6f && color.red < 0.3f -> R.drawable.car_mercedes
        // McLaren papaya
        color.red > 0.8f && color.green in 0.4f..0.6f && color.blue < 0.25f -> R.drawable.car_mclaren
        // Williams bright sky blue vs Racing Bulls royal blue vs Red Bull cobalt blue
        color.blue > 0.85f && color.green > 0.68f && color.red in 0.25f..0.55f -> R.drawable.car_williams
        color.blue > 0.85f && color.green in 0.45f..0.68f && color.red in 0.25f..0.55f -> R.drawable.car_racing_bulls
        color.blue > 0.65f && color.red < 0.45f -> R.drawable.car_red_bull
        // Aston Martin emerald green
        color.green > 0.5f && color.blue in 0.3f..0.6f -> R.drawable.car_aston_martin
        // Alpine pink
        color.red > 0.8f && color.blue > 0.6f -> R.drawable.car_alpine
        // Audi green fallback
        color.green > 0.8f && color.blue < 0.4f -> R.drawable.car_audi_sauber
        else -> null
    }
}

@DrawableRes
fun getTeamCarResByColor(color: Color): Int {
    return getTeamCarResByColorOrNull(color) ?: R.drawable.car_haas
}

private fun computeDriverFallbackLabel(driverCodeOrName: String, driverCode: String?): String {
    val explicitCode = driverCode?.trim()?.takeIf { it.isNotEmpty() }
    if (explicitCode != null) {
        return explicitCode.take(3).uppercase()
    }
    val trimmed = driverCodeOrName.trim()
    if (trimmed.isEmpty()) return "?"
    val words = trimmed.split(DRIVER_NAME_SPLIT_REGEX).filter { it.isNotEmpty() }
    return when {
        words.size >= 2 -> "${words.first().first()}${words.last().first()}".uppercase()
        trimmed.length <= 3 -> trimmed.uppercase()
        else -> trimmed.first().uppercase()
    }
}

/**
 * High-craft driver portrait avatar component.
 * Displays real driver headshot/cutout inside a dark #232325 circular frame
 * with subtle team-colored border and optional position badge (P1, P2, etc.).
 */
@Composable
fun DriverPortraitAvatar(
    driverCodeOrName: String,
    teamColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    positionBadge: String? = null,
    useFullCutout: Boolean = false,
    driverCode: String? = null
) {
    val trimmedQuery = driverCodeOrName.trim()
    val queryTokens = trimmedQuery.split(DRIVER_NAME_SPLIT_REGEX).filter { it.isNotEmpty() }
    val hasFullNameOrSurname = queryTokens.size >= 2 || (queryTokens.size == 1 && trimmedQuery.length > 3)

    val resId = if (hasFullNameOrSurname) {
        if (useFullCutout) {
            getDriverCutoutRes(trimmedQuery) ?: getDriverHeadshotRes(trimmedQuery)
        } else {
            getDriverHeadshotRes(trimmedQuery) ?: getDriverCutoutRes(trimmedQuery)
        }
    } else {
        val fallbackCode = driverCode?.trim()?.takeIf { it.isNotEmpty() }
        if (useFullCutout) {
            getDriverCutoutRes(trimmedQuery)
                ?: fallbackCode?.let(::getDriverCutoutRes)
                ?: getDriverHeadshotRes(trimmedQuery)
                ?: fallbackCode?.let(::getDriverHeadshotRes)
        } else {
            getDriverHeadshotRes(trimmedQuery)
                ?: fallbackCode?.let(::getDriverHeadshotRes)
                ?: getDriverCutoutRes(trimmedQuery)
                ?: fallbackCode?.let(::getDriverCutoutRes)
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF232325))
                .border(1.5.dp, teamColor.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (resId != null) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = driverCodeOrName,
                    contentScale = if (useFullCutout) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = computeDriverFallbackLabel(driverCodeOrName, driverCode),
                    color = Color.White,
                    fontSize = (size.value * 0.32f).sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (positionBadge != null) {
            val badgeBg = when (positionBadge.uppercase()) {
                "P1", "1" -> Color(0xFFF5C518)
                "P2", "2" -> Color(0xFFC0C0C8)
                "P3", "3" -> Color(0xFFD97B29)
                else -> Color(0xFF1E1E24)
            }
            val badgeTextColor = if (positionBadge.uppercase() in listOf("P1", "P2", "P3", "1", "2", "3")) {
                Color.Black
            } else {
                Color.White
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .border(0.8.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = positionBadge,
                    color = badgeTextColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * Clean vector side-profile F1 car silhouette used as fallback when no team car PNG is available
 * (e.g., historical or unlisted constructors mapped to F1TeamId.OTHER).
 */
@Composable
fun GenericF1CarSideVector(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Carbon floor & diffuser
        val floorPath = Path().apply {
            moveTo(0.06f * w, 0.72f * h)
            lineTo(0.94f * w, 0.72f * h)
            lineTo(0.96f * w, 0.65f * h)
            lineTo(0.86f * w, 0.65f * h)
            lineTo(0.14f * w, 0.67f * h)
            close()
        }
        drawPath(floorPath, color = Color(0xFF1E1E24))

        // Main chassis / engine cover / nose silhouette (facing right)
        val bodyPath = Path().apply {
            moveTo(0.10f * w, 0.56f * h) // Rear wing pylon join
            lineTo(0.24f * w, 0.34f * h) // Airbox / shark fin top
            lineTo(0.42f * w, 0.34f * h)
            lineTo(0.50f * w, 0.46f * h) // Cockpit rim
            lineTo(0.62f * w, 0.46f * h) // Nose bridge
            lineTo(0.91f * w, 0.62f * h) // Low nose tip
            lineTo(0.91f * w, 0.67f * h)
            lineTo(0.12f * w, 0.67f * h)
            close()
        }
        drawPath(bodyPath, color = accentColor)
        drawPath(
            bodyPath,
            color = Color.White.copy(alpha = 0.25f),
            style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round)
        )

        // Sidepod undercut highlight
        val sidepodPath = Path().apply {
            moveTo(0.32f * w, 0.52f * h)
            lineTo(0.60f * w, 0.52f * h)
            lineTo(0.56f * w, 0.65f * h)
            lineTo(0.28f * w, 0.65f * h)
            close()
        }
        drawPath(sidepodPath, color = Color.Black.copy(alpha = 0.24f))

        // Halo hoop over cockpit
        drawLine(
            color = Color(0xFF282832),
            start = Offset(0.43f * w, 0.38f * h),
            end = Offset(0.58f * w, 0.45f * h),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Rear wing
        drawRect(
            color = Color(0xFF22222A),
            topLeft = Offset(0.05f * w, 0.28f * h),
            size = Size(0.10f * w, 0.28f * h)
        )
        drawRect(
            color = accentColor,
            topLeft = Offset(0.04f * w, 0.28f * h),
            size = Size(0.12f * w, 0.06f * h)
        )

        // Front wing
        drawRect(
            color = accentColor,
            topLeft = Offset(0.84f * w, 0.64f * h),
            size = Size(0.13f * w, 0.06f * h)
        )

        // Rear & Front Tyres
        val tyreRadius = (0.145f * h).coerceAtMost(0.09f * w)
        val rearWheelCenter = Offset(0.22f * w, 0.64f * h)
        val frontWheelCenter = Offset(0.77f * w, 0.64f * h)
        listOf(rearWheelCenter, frontWheelCenter).forEach { center ->
            drawCircle(color = Color(0xFF111116), radius = tyreRadius, center = center)
            drawCircle(
                color = accentColor.copy(alpha = 0.85f),
                radius = tyreRadius * 0.62f,
                center = center,
                style = Stroke(width = 1.6.dp.toPx())
            )
            drawCircle(color = Color(0xFF3A3A44), radius = tyreRadius * 0.28f, center = center)
        }
    }
}

/**
 * Reusable F1 team car cutout image component.
 */
@Composable
fun TeamCarShowcaseImage(
    teamIdOrName: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val team = resolveTeamId(teamIdOrName)
    TeamCarShowcaseImage(
        team = team,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
fun TeamCarShowcaseImage(
    team: F1TeamId,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val carRes = getTeamCarResOrNull(team)
    if (carRes != null) {
        Image(
            painter = painterResource(id = carRes),
            contentDescription = "${team.displayName} Car",
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        GenericF1CarSideVector(
            accentColor = Color(team.colorHex),
            modifier = modifier
        )
    }
}

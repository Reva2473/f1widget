package com.reva2473.f1widget.ui.components

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.graphics.Color
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
    val trimmed = teamIdOrName.trim()
    val byShortCode = F1TeamId.entries.firstOrNull {
        it != F1TeamId.OTHER && it.shortCode.equals(trimmed, ignoreCase = true)
    }
    return getTeamCarRes(byShortCode ?: F1TeamId.fromName(trimmed))
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
fun getTeamCarResByColor(color: Color): Int {
    val targetArgb = color.toArgb()
    val matched = F1TeamId.entries.firstOrNull {
        it != F1TeamId.OTHER && (
            Color(it.colorHex).toArgb() == targetArgb || Color(it.secondaryHex).toArgb() == targetArgb
            )
    }
    if (matched != null) return getTeamCarRes(matched)

    return when {
        color.red > 0.7f && color.green < 0.2f && color.blue < 0.3f -> R.drawable.car_ferrari
        color.green > 0.7f && color.blue > 0.6f -> R.drawable.car_mercedes
        color.red > 0.8f && color.green in 0.4f..0.6f -> R.drawable.car_mclaren
        color.blue > 0.7f && color.red < 0.4f -> R.drawable.car_red_bull
        color.green > 0.5f && color.blue in 0.3f..0.6f -> R.drawable.car_aston_martin
        color.red > 0.8f && color.blue > 0.6f -> R.drawable.car_alpine
        color.green > 0.8f && color.blue < 0.4f -> R.drawable.car_audi_sauber
        else -> R.drawable.car_haas
    }
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
    val resId = if (useFullCutout) {
        driverCode?.let(::getDriverCutoutRes)
            ?: getDriverCutoutRes(driverCodeOrName)
            ?: driverCode?.let(::getDriverHeadshotRes)
            ?: getDriverHeadshotRes(driverCodeOrName)
    } else {
        driverCode?.let(::getDriverHeadshotRes)
            ?: getDriverHeadshotRes(driverCodeOrName)
            ?: driverCode?.let(::getDriverCutoutRes)
            ?: getDriverCutoutRes(driverCodeOrName)
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
 * Reusable F1 team car cutout image component.
 */
@Composable
fun TeamCarShowcaseImage(
    teamIdOrName: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val carRes = getTeamCarRes(teamIdOrName)
    Image(
        painter = painterResource(id = carRes),
        contentDescription = "$teamIdOrName Car",
        contentScale = contentScale,
        modifier = modifier
    )
}

@Composable
fun TeamCarShowcaseImage(
    team: F1TeamId,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val carRes = getTeamCarRes(team)
    Image(
        painter = painterResource(id = carRes),
        contentDescription = "${team.displayName} Car",
        contentScale = contentScale,
        modifier = modifier
    )
}

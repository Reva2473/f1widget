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

/**
 * Resolves real 2025/2026 F1 driver circular headshot drawable resource.
 */
@DrawableRes
fun getDriverHeadshotRes(codeOrName: String): Int? {
    val key = codeOrName.trim().uppercase()
    val lower = codeOrName.trim().lowercase()

    return when {
        key == "VER" || lower.contains("verstappen") -> R.drawable.driver_ver
        key == "HAM" || lower.contains("hamilton") -> R.drawable.driver_ham
        key == "LEC" || lower.contains("leclerc") -> R.drawable.driver_lec
        key == "NOR" || lower.contains("norris") -> R.drawable.driver_nor
        key == "PIA" || lower.contains("piastri") -> R.drawable.driver_pia
        key == "RUS" || lower.contains("russell") -> R.drawable.driver_rus
        key == "ANT" || lower.contains("antonelli") -> R.drawable.driver_ant
        key == "ALO" || lower.contains("alonso") -> R.drawable.driver_alo
        key == "STR" || lower.contains("stroll") -> R.drawable.driver_str
        key == "GAS" || lower.contains("gasly") -> R.drawable.driver_gas
        key == "OCO" || lower.contains("ocon") -> R.drawable.driver_oco
        key == "ALB" || lower.contains("albon") -> R.drawable.driver_alb
        key == "SAI" || lower.contains("sainz") -> R.drawable.driver_sai
        key == "LAW" || lower.contains("lawson") -> R.drawable.driver_law
        key == "HAD" || lower.contains("hadjar") -> R.drawable.driver_had
        key == "COL" || lower.contains("colapinto") -> R.drawable.driver_col
        key == "BEA" || lower.contains("bearman") -> R.drawable.driver_bea
        key == "HUL" || lower.contains("hulkenberg") || lower.contains("hülkenberg") -> R.drawable.driver_hul
        key == "BOR" || lower.contains("bortoleto") -> R.drawable.driver_bor
        key == "TSU" || lower.contains("tsunoda") -> R.drawable.driver_tsu
        key == "DOO" || lower.contains("doohan") -> R.drawable.driver_doo
        key == "BOT" || key == "VB" || lower.contains("bottas") -> R.drawable.driver_bot
        key == "LIN" || key == "AL" || lower.contains("lindblad") -> R.drawable.driver_lin
        key == "PER" || key == "SP" || lower.contains("perez") || lower.contains("pérez") -> R.drawable.driver_per
        else -> null
    }
}

/**
 * Resolves real 2025/2026 F1 driver full upper-body cutout drawable resource.
 */
@DrawableRes
fun getDriverCutoutRes(codeOrName: String): Int? {
    val key = codeOrName.trim().uppercase()
    val lower = codeOrName.trim().lowercase()

    return when {
        key == "VER" || lower.contains("verstappen") -> R.drawable.driver_full_ver
        key == "HAM" || lower.contains("hamilton") -> R.drawable.driver_full_ham
        key == "LEC" || lower.contains("leclerc") -> R.drawable.driver_full_lec
        key == "NOR" || lower.contains("norris") -> R.drawable.driver_full_nor
        key == "PIA" || lower.contains("piastri") -> R.drawable.driver_full_pia
        key == "RUS" || lower.contains("russell") -> R.drawable.driver_full_rus
        key == "ANT" || lower.contains("antonelli") -> R.drawable.driver_full_ant
        key == "ALO" || lower.contains("alonso") -> R.drawable.driver_full_alo
        key == "STR" || lower.contains("stroll") -> R.drawable.driver_full_str
        key == "GAS" || lower.contains("gasly") -> R.drawable.driver_full_gas
        key == "OCO" || lower.contains("ocon") -> R.drawable.driver_full_oco
        key == "ALB" || lower.contains("albon") -> R.drawable.driver_full_alb
        key == "SAI" || lower.contains("sainz") -> R.drawable.driver_full_sai
        key == "LAW" || lower.contains("lawson") -> R.drawable.driver_full_law
        key == "HAD" || lower.contains("hadjar") -> R.drawable.driver_full_had
        key == "COL" || lower.contains("colapinto") -> R.drawable.driver_full_col
        key == "BEA" || lower.contains("bearman") -> R.drawable.driver_full_bea
        key == "HUL" || lower.contains("hulkenberg") || lower.contains("hülkenberg") -> R.drawable.driver_full_hul
        key == "BOR" || lower.contains("bortoleto") -> R.drawable.driver_full_bor
        key == "TSU" || lower.contains("tsunoda") -> R.drawable.driver_full_tsu
        key == "DOO" || lower.contains("doohan") -> R.drawable.driver_full_doo
        key == "BOT" || key == "VB" || lower.contains("bottas") -> R.drawable.driver_full_bot
        key == "LIN" || key == "AL" || lower.contains("lindblad") -> R.drawable.driver_full_lin
        key == "PER" || key == "SP" || lower.contains("perez") || lower.contains("pérez") -> R.drawable.driver_full_per
        else -> null
    }
}

/**
 * Resolves real 2025/2026 F1 team car side-profile cutout drawable resource.
 */
@DrawableRes
fun getTeamCarRes(teamIdOrName: String): Int {
    val lower = teamIdOrName.trim().lowercase()

    return when {
        lower.contains("mercedes") || lower == "mer" -> R.drawable.car_mercedes
        lower.contains("ferrari") || lower == "fer" -> R.drawable.car_ferrari
        lower.contains("red bull") || lower.contains("red_bull") || lower == "rbr" -> R.drawable.car_red_bull
        lower.contains("mclaren") || lower == "mcl" -> R.drawable.car_mclaren
        lower.contains("racing bull") || lower.contains("vcarb") || lower.contains("alphatauri") ||
            lower == "rb" || lower == "vrb" || lower.contains("cash app") -> R.drawable.car_racing_bulls
        lower.contains("alpine") || lower == "alp" -> R.drawable.car_alpine
        lower.contains("aston") || lower == "amr" -> R.drawable.car_aston_martin
        lower.contains("haas") || lower == "haa" -> R.drawable.car_haas
        lower.contains("williams") || lower == "wil" -> R.drawable.car_williams
        lower.contains("cadillac") || lower == "cad" -> R.drawable.car_cadillac
        lower.contains("sauber") || lower.contains("audi") || lower == "sau" -> R.drawable.car_audi_sauber
        else -> R.drawable.car_ferrari
    }
}

@DrawableRes
fun getTeamCarRes(team: F1TeamId): Int {
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
    }
}

@DrawableRes
fun getTeamCarResByColor(color: Color): Int {
    val targetArgb = color.toArgb()
    val matched = F1TeamId.entries.firstOrNull {
        Color(it.colorHex).toArgb() == targetArgb || Color(it.secondaryHex).toArgb() == targetArgb
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
        else -> R.drawable.car_ferrari
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
    useFullCutout: Boolean = false
) {
    val resId = if (useFullCutout) {
        getDriverCutoutRes(driverCodeOrName) ?: getDriverHeadshotRes(driverCodeOrName)
    } else {
        getDriverHeadshotRes(driverCodeOrName) ?: getDriverCutoutRes(driverCodeOrName)
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
                    text = driverCodeOrName.take(3).uppercase(),
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

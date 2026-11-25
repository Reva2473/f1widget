package com.reva2473.f1widget

import androidx.compose.ui.graphics.Color
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.ui.components.getDriverCutoutRes
import com.reva2473.f1widget.ui.components.getDriverHeadshotRes
import com.reva2473.f1widget.ui.components.getTeamCarRes
import com.reva2473.f1widget.ui.components.getTeamCarResByColor
import com.reva2473.f1widget.ui.components.getTeamCarResByColorOrNull
import com.reva2473.f1widget.ui.components.getTeamCarResOrNull
import com.reva2473.f1widget.ui.components.resolveTeamId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests verifying [F1AssetMapper] driver code/name resolution without substring collisions,
 * full 24-driver 2026 asset coverage, color-based team car resolution, and [F1TeamId.OTHER] null safety.
 */
class F1AssetMapperTest {

    @Test
    fun testExactThreeLetterDriverCodesMatchExpectedDrawables() {
        val expectedHeadshots = mapOf(
            "VER" to R.drawable.driver_ver,
            "HAM" to R.drawable.driver_ham,
            "LEC" to R.drawable.driver_lec,
            "NOR" to R.drawable.driver_nor,
            "PIA" to R.drawable.driver_pia,
            "RUS" to R.drawable.driver_rus,
            "ANT" to R.drawable.driver_ant,
            "ALO" to R.drawable.driver_alo,
            "STR" to R.drawable.driver_str,
            "GAS" to R.drawable.driver_gas,
            "OCO" to R.drawable.driver_oco,
            "ALB" to R.drawable.driver_alb,
            "SAI" to R.drawable.driver_sai,
            "LAW" to R.drawable.driver_law,
            "HAD" to R.drawable.driver_had,
            "COL" to R.drawable.driver_col,
            "BEA" to R.drawable.driver_bea,
            "HUL" to R.drawable.driver_hul,
            "BOR" to R.drawable.driver_bor,
            "TSU" to R.drawable.driver_tsu,
            "DOO" to R.drawable.driver_doo,
            "BOT" to R.drawable.driver_bot,
            "LIN" to R.drawable.driver_lin,
            "PER" to R.drawable.driver_per
        )
        assertEquals(24, expectedHeadshots.size)
        expectedHeadshots.forEach { (code, res) ->
            assertEquals("Headshot mismatch for $code", res, getDriverHeadshotRes(code))
            assertEquals("Lowercase/whitespace headshot mismatch for $code", res, getDriverHeadshotRes(" ${code.lowercase()} "))
        }

        val expectedCutouts = mapOf(
            "VER" to R.drawable.driver_full_ver,
            "HAM" to R.drawable.driver_full_ham,
            "LEC" to R.drawable.driver_full_lec,
            "NOR" to R.drawable.driver_full_nor,
            "PIA" to R.drawable.driver_full_pia,
            "RUS" to R.drawable.driver_full_rus,
            "ANT" to R.drawable.driver_full_ant,
            "ALO" to R.drawable.driver_full_alo,
            "STR" to R.drawable.driver_full_str,
            "GAS" to R.drawable.driver_full_gas,
            "OCO" to R.drawable.driver_full_oco,
            "ALB" to R.drawable.driver_full_alb,
            "SAI" to R.drawable.driver_full_sai,
            "LAW" to R.drawable.driver_full_law,
            "HAD" to R.drawable.driver_full_had,
            "COL" to R.drawable.driver_full_col,
            "BEA" to R.drawable.driver_full_bea,
            "HUL" to R.drawable.driver_full_hul,
            "BOR" to R.drawable.driver_full_bor,
            "TSU" to R.drawable.driver_full_tsu,
            "DOO" to R.drawable.driver_full_doo,
            "BOT" to R.drawable.driver_full_bot,
            "LIN" to R.drawable.driver_full_lin,
            "PER" to R.drawable.driver_full_per
        )
        assertEquals(24, expectedCutouts.size)
        expectedCutouts.forEach { (code, res) ->
            assertEquals("Cutout mismatch for $code", res, getDriverCutoutRes(code))
            assertEquals("Lowercase/whitespace cutout mismatch for $code", res, getDriverCutoutRes(" ${code.lowercase()} "))
        }
    }

    @Test
    fun testDriverNameResolutionAvoidsSubstringAndRelativeCollisions() {
        // Valid full names & hyphen/underscore driverIds
        assertEquals(R.drawable.driver_ver, getDriverHeadshotRes("Max Verstappen"))
        assertEquals(R.drawable.driver_per, getDriverHeadshotRes("Sergio Pérez"))
        assertEquals(R.drawable.driver_per, getDriverHeadshotRes("sergio_perez"))
        assertEquals(R.drawable.driver_bot, getDriverHeadshotRes("Valtteri Bottas"))
        assertEquals(R.drawable.driver_lin, getDriverHeadshotRes("Arvid Lindblad"))
        assertEquals(R.drawable.driver_ant, getDriverHeadshotRes("Andrea Kimi Antonelli"))
        assertEquals(R.drawable.driver_ant, getDriverHeadshotRes("Kimi Antonelli"))
        assertEquals(R.drawable.driver_had, getDriverHeadshotRes("Isack Hadjar"))
        assertEquals(R.drawable.driver_hul, getDriverHeadshotRes("Nico Hülkenberg"))
        assertEquals(R.drawable.driver_bea, getDriverHeadshotRes("Ollie Bearman"))

        // Single-token last names match, single-token first names do not
        assertEquals(R.drawable.driver_had, getDriverHeadshotRes("hadjar"))
        assertNull(getDriverHeadshotRes("Max"))
        assertNull(getDriverHeadshotRes("Kimi"))

        // Relatives and substring lookalikes must NOT falsely match
        assertNull(getDriverHeadshotRes("Jos Verstappen"))
        assertNull(getDriverCutoutRes("Jos Verstappen"))
        assertNull(getDriverHeadshotRes("Antonio Perez"))
        assertNull(getDriverHeadshotRes("Lindqvist"))
        assertNull(getDriverHeadshotRes("Anton"))
        assertNull(getDriverHeadshotRes("Botas"))
        assertNull(getDriverHeadshotRes("Hadjarov"))
        assertNull(getDriverHeadshotRes(""))
        assertNull(getDriverHeadshotRes("   "))
    }

    @Test
    fun testHistoricalDriverPortraitResolutionChecksFullNameBeforeThreeLetterCode() {
        // Historical drivers whose 3-letter code ("VER", "ALB") collides with 2026 drivers
        assertEquals(R.drawable.driver_ver, getDriverHeadshotRes("VER"))
        assertNull(getDriverHeadshotRes("Jos Verstappen"))
        assertNull(getDriverCutoutRes("Jos Verstappen"))
        assertNull(getDriverHeadshotRes("jos_verstappen"))

        assertEquals(R.drawable.driver_alb, getDriverHeadshotRes("ALB"))
        assertNull(getDriverHeadshotRes("Michele Alboreto"))
        assertNull(getDriverCutoutRes("Michele Alboreto"))
        assertNull(getDriverHeadshotRes("Alboreto"))
        assertNull(getDriverHeadshotRes("michele-alboreto"))

        // Current 2026 drivers resolve via both full name and 3-letter code
        assertEquals(R.drawable.driver_ver, getDriverHeadshotRes("Max Verstappen"))
        assertEquals(R.drawable.driver_full_ver, getDriverCutoutRes("Max Verstappen"))
        assertEquals(R.drawable.driver_alb, getDriverHeadshotRes("Alexander Albon"))
        assertEquals(R.drawable.driver_full_alb, getDriverCutoutRes("Alex Albon"))

        // Other historical legends also do not falsely resolve
        assertNull(getDriverHeadshotRes("Ayrton Senna"))
        assertNull(getDriverHeadshotRes("Michael Schumacher"))
        assertNull(getDriverHeadshotRes("Nino Farina"))
    }

    @Test
    fun testTeamCarResOrNullReturnsNullForOtherAndMatchesAllTeams() {
        assertNull(getTeamCarResOrNull(F1TeamId.OTHER))
        assertEquals(R.drawable.car_haas, getTeamCarRes(F1TeamId.OTHER))

        assertEquals(R.drawable.car_mercedes, getTeamCarResOrNull(F1TeamId.MERCEDES))
        assertEquals(R.drawable.car_ferrari, getTeamCarResOrNull(F1TeamId.FERRARI))
        assertEquals(R.drawable.car_red_bull, getTeamCarResOrNull(F1TeamId.RED_BULL))
        assertEquals(R.drawable.car_mclaren, getTeamCarResOrNull(F1TeamId.MCLAREN))
        assertEquals(R.drawable.car_racing_bulls, getTeamCarResOrNull(F1TeamId.RACING_BULLS))
        assertEquals(R.drawable.car_alpine, getTeamCarResOrNull(F1TeamId.ALPINE))
        assertEquals(R.drawable.car_aston_martin, getTeamCarResOrNull(F1TeamId.ASTON_MARTIN))
        assertEquals(R.drawable.car_haas, getTeamCarResOrNull(F1TeamId.HAAS))
        assertEquals(R.drawable.car_williams, getTeamCarResOrNull(F1TeamId.WILLIAMS))
        assertEquals(R.drawable.car_audi_sauber, getTeamCarResOrNull(F1TeamId.AUDI_SAUBER))
        assertEquals(R.drawable.car_cadillac, getTeamCarResOrNull(F1TeamId.CADILLAC))

        // String overload resolves short codes and historical names
        assertEquals(R.drawable.car_racing_bulls, getTeamCarRes("VRB"))
        assertEquals(R.drawable.car_red_bull, getTeamCarRes("RBR"))
        assertEquals(R.drawable.car_racing_bulls, getTeamCarRes("toro_rosso"))

        // Unknown / unmapped historical teams resolve to F1TeamId.OTHER and null via getTeamCarResOrNull
        assertEquals(F1TeamId.OTHER, resolveTeamId("lotus"))
        assertNull(getTeamCarResOrNull(resolveTeamId("lotus")))
        assertNull(getTeamCarResOrNull(resolveTeamId("unknown_constructor")))
    }

    @Test
    fun testTeamCarResByColorAndOrNullForPrimarySecondaryAndHeuristicColors() {
        val exactTeamColors = listOf(
            F1TeamId.WILLIAMS to R.drawable.car_williams,
            F1TeamId.RACING_BULLS to R.drawable.car_racing_bulls,
            F1TeamId.CADILLAC to R.drawable.car_cadillac,
            F1TeamId.FERRARI to R.drawable.car_ferrari,
            F1TeamId.MCLAREN to R.drawable.car_mclaren,
            F1TeamId.MERCEDES to R.drawable.car_mercedes,
            F1TeamId.RED_BULL to R.drawable.car_red_bull
        )

        exactTeamColors.forEach { (team, expectedDrawable) ->
            assertEquals("Primary color mismatch for $team", expectedDrawable, getTeamCarResByColorOrNull(Color(team.colorHex)))
            assertEquals("Secondary color mismatch for $team", expectedDrawable, getTeamCarResByColorOrNull(Color(team.secondaryHex)))
            assertEquals("getTeamCarResByColor mismatch for $team", expectedDrawable, getTeamCarResByColor(Color(team.colorHex)))
        }

        // Heuristic color range differentiation (Williams vs Racing Bulls vs Red Bull, and Cadillac gold)
        assertEquals(R.drawable.car_williams, getTeamCarResByColorOrNull(Color(0.35f, 0.78f, 0.96f)))
        assertEquals(R.drawable.car_racing_bulls, getTeamCarResByColorOrNull(Color(0.35f, 0.56f, 0.96f)))
        assertEquals(R.drawable.car_red_bull, getTeamCarResByColorOrNull(Color(0.20f, 0.42f, 0.78f)))
        assertEquals(R.drawable.car_cadillac, getTeamCarResByColorOrNull(Color(0.82f, 0.68f, 0.22f)))

        // Unmatched color returns null from OrNull variant and falls back to car_haas in non-null variant
        assertNull(getTeamCarResByColorOrNull(Color.Black))
        assertEquals(R.drawable.car_haas, getTeamCarResByColor(Color.Black))
    }
}

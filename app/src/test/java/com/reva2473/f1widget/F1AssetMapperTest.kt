package com.reva2473.f1widget

import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.ui.components.getDriverCutoutRes
import com.reva2473.f1widget.ui.components.getDriverHeadshotRes
import com.reva2473.f1widget.ui.components.getTeamCarRes
import com.reva2473.f1widget.ui.components.getTeamCarResOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests verifying [F1AssetMapper] driver code/name resolution without substring collisions
 * and team car drawable resolution including [F1TeamId.OTHER] null safety.
 */
class F1AssetMapperTest {

    @Test
    fun testExactThreeLetterDriverCodesMatchExpectedDrawables() {
        val expectedHeadshots = mapOf(
            "VER" to R.drawable.driver_ver,
            "PER" to R.drawable.driver_per,
            "BOT" to R.drawable.driver_bot,
            "LIN" to R.drawable.driver_lin,
            "ANT" to R.drawable.driver_ant,
            "HAD" to R.drawable.driver_had,
            "HAM" to R.drawable.driver_ham,
            "LEC" to R.drawable.driver_lec,
            "NOR" to R.drawable.driver_nor,
            "PIA" to R.drawable.driver_pia,
            "RUS" to R.drawable.driver_rus,
            "ALO" to R.drawable.driver_alo
        )
        expectedHeadshots.forEach { (code, res) ->
            assertEquals(res, getDriverHeadshotRes(code))
            assertEquals(res, getDriverHeadshotRes(" ${code.lowercase()} "))
        }

        val expectedCutouts = mapOf(
            "VER" to R.drawable.driver_full_ver,
            "PER" to R.drawable.driver_full_per,
            "BOT" to R.drawable.driver_full_bot,
            "LIN" to R.drawable.driver_full_lin,
            "ANT" to R.drawable.driver_full_ant,
            "HAD" to R.drawable.driver_full_had
        )
        expectedCutouts.forEach { (code, res) ->
            assertEquals(res, getDriverCutoutRes(code))
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
        assertEquals(R.drawable.car_haas, getTeamCarRes("lotus"))
    }
}

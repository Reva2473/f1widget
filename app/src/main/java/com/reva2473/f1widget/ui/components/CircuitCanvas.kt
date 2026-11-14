package com.reva2473.f1widget.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.LiveDriverTiming
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Normalized circuit geometry descriptor.
 */
data class CircuitGeometryData(
    val sector1Points: List<Offset>,
    val sector2Points: List<Offset>,
    val sector3Points: List<Offset>,
    val allPoints: List<Offset>,
    val corners: List<Pair<Int, Offset>>,
    val startFinishPos: Offset,
    val speedTrapPos: Offset,
    val speedTrapBadgePos: Offset,
    val s1CutoffProgress: Float = 0.33f,
    val s2CutoffProgress: Float = 0.68f
)

/**
 * Normalized geometry for Sepang International Circuit.
 * Coords in range 0.05f .. 0.95f
 */
object SepangCircuitGeometry {
    // Sector 1: Start/Finish straight through Turn 1, 2, 3, 4
    val sector1Points = listOf(
        Offset(0.24f, 0.82f), // Start/Finish
        Offset(0.48f, 0.82f),
        Offset(0.72f, 0.82f),
        Offset(0.88f, 0.82f), // Turn 1 entry
        Offset(0.93f, 0.73f), // Turn 1 apex
        Offset(0.87f, 0.63f), // Turn 2 apex
        Offset(0.78f, 0.62f),
        Offset(0.68f, 0.55f), // Turn 3
        Offset(0.60f, 0.50f),
        Offset(0.55f, 0.40f)  // Turn 4
    )

    // Sector 2: Turn 5 through Turn 11
    val sector2Points = listOf(
        Offset(0.55f, 0.40f),
        Offset(0.47f, 0.32f), // Turn 5
        Offset(0.50f, 0.20f), // Turn 6
        Offset(0.64f, 0.16f), // Turn 7
        Offset(0.76f, 0.22f), // Turn 8
        Offset(0.72f, 0.34f), // Turn 9
        Offset(0.62f, 0.38f), // Turn 10
        Offset(0.56f, 0.48f), // Turn 11
        Offset(0.48f, 0.52f)
    )

    // Sector 3: Turn 12, 13, 14, Back Straight, Turn 15 Hairpin back to Start/Finish
    val sector3Points = listOf(
        Offset(0.48f, 0.52f),
        Offset(0.36f, 0.48f), // Turn 12
        Offset(0.30f, 0.56f), // Turn 13
        Offset(0.36f, 0.68f), // Turn 14 entry to back straight
        Offset(0.56f, 0.72f), // Back straight
        Offset(0.76f, 0.72f),
        Offset(0.86f, 0.72f),
        Offset(0.90f, 0.77f), // Turn 15 Hairpin
        Offset(0.86f, 0.82f),
        Offset(0.24f, 0.82f)  // Rejoin Start/Finish
    )

    val allPoints: List<Offset> by lazy {
        val list = mutableListOf<Offset>()
        list.addAll(sector1Points)
        list.addAll(sector2Points.drop(1))
        list.addAll(sector3Points.drop(1))
        list
    }

    val corners = listOf(
        Pair(1, Offset(0.92f, 0.75f)),
        Pair(2, Offset(0.87f, 0.62f)),
        Pair(3, Offset(0.70f, 0.56f)),
        Pair(4, Offset(0.55f, 0.39f)),
        Pair(5, Offset(0.46f, 0.31f)),
        Pair(6, Offset(0.50f, 0.19f)),
        Pair(7, Offset(0.65f, 0.15f)),
        Pair(8, Offset(0.77f, 0.22f)),
        Pair(9, Offset(0.73f, 0.35f)),
        Pair(10, Offset(0.61f, 0.37f)),
        Pair(11, Offset(0.55f, 0.49f)),
        Pair(12, Offset(0.35f, 0.47f)),
        Pair(13, Offset(0.29f, 0.56f)),
        Pair(14, Offset(0.37f, 0.69f)),
        Pair(15, Offset(0.90f, 0.76f))
    )
}

/**
 * Normalized geometry for Autódromo José Carlos Pace (Interlagos, São Paulo).
 * Counter-clockwise iconic 15-turn circuit:
 * S1: Start/Finish, Senna S (T1-T3), Reta Oposta to Descida do Lago (T4)
 * S2: Descida do Lago (T4-T5), Ferradura (T6-T7), Laranjinha (T8), Pinheirinho (T9), Bico de Pato (T10), Mergulho (T11)
 * S3: Junção (T12), Subida dos Boxes (T13-T15) back to Start/Finish
 */
object InterlagosCircuitGeometry {
    // S1: Main straight right-to-left, downhill Senna S T1-T3, Reta Oposta
    val sector1Points = listOf(
        Offset(0.65f, 0.20f), // Start/Finish Line
        Offset(0.45f, 0.20f), // Main Straight
        Offset(0.26f, 0.20f), // Approach T1
        Offset(0.15f, 0.23f), // Turn 1 (Senna S entry left)
        Offset(0.18f, 0.33f), // Turn 2 (Senna S switchback right)
        Offset(0.13f, 0.42f), // Turn 3 (Curva do Sol left)
        Offset(0.13f, 0.54f),
        Offset(0.14f, 0.66f), // Reta Oposta
        Offset(0.15f, 0.78f),
        Offset(0.17f, 0.88f)  // Turn 4 entry (Descida do Lago)
    )

    // S2: Descida do Lago (T4-T5) through Ferradura (T6-T7), Laranjinha, Pinheirinho, Bico de Pato, Mergulho
    val sector2Points = listOf(
        Offset(0.17f, 0.88f), // T4
        Offset(0.24f, 0.90f), // T4 apex
        Offset(0.32f, 0.88f), // T5 exit
        Offset(0.42f, 0.82f),
        Offset(0.48f, 0.72f), // Turn 6 (Ferradura entry uphill)
        Offset(0.56f, 0.62f), // Turn 7 (Ferradura exit)
        Offset(0.66f, 0.54f), // Turn 8 (Curva da Laranjinha)
        Offset(0.70f, 0.46f),
        Offset(0.62f, 0.40f), // Turn 9 (Pinheirinho left hook)
        Offset(0.53f, 0.43f),
        Offset(0.48f, 0.50f), // Turn 10 (Bico de Pato tight hairpin)
        Offset(0.54f, 0.57f),
        Offset(0.62f, 0.63f), // Turn 11 (Mergulho downhill left)
        Offset(0.70f, 0.72f)
    )

    // S3: Junção (T12), Subida dos Boxes (T13-T15) back to Main Straight
    val sector3Points = listOf(
        Offset(0.70f, 0.72f),
        Offset(0.76f, 0.78f), // Turn 12 (Junção left)
        Offset(0.83f, 0.74f),
        Offset(0.86f, 0.62f), // Turn 13 (Subida dos Boxes)
        Offset(0.87f, 0.48f), // Turn 14 (Curva do Café)
        Offset(0.85f, 0.33f), // Turn 15 (Arquibancadas)
        Offset(0.78f, 0.22f), // Sweeping onto Main Straight
        Offset(0.65f, 0.20f)  // Rejoin Start/Finish
    )

    val allPoints: List<Offset> by lazy {
        val list = mutableListOf<Offset>()
        list.addAll(sector1Points)
        list.addAll(sector2Points.drop(1))
        list.addAll(sector3Points.drop(1))
        list
    }

    val corners = listOf(
        Pair(1, Offset(0.15f, 0.23f)), // T1 Senna S
        Pair(2, Offset(0.18f, 0.33f)), // T2 Senna S
        Pair(3, Offset(0.13f, 0.42f)), // T3 Curva do Sol
        Pair(4, Offset(0.24f, 0.90f)), // T4 Descida do Lago
        Pair(5, Offset(0.32f, 0.88f)), // T5 Descida do Lago exit
        Pair(6, Offset(0.48f, 0.72f)), // T6 Ferradura entry
        Pair(7, Offset(0.56f, 0.62f)), // T7 Ferradura exit
        Pair(8, Offset(0.70f, 0.46f)), // T8 Laranjinha
        Pair(9, Offset(0.53f, 0.43f)), // T9 Pinheirinho
        Pair(10, Offset(0.48f, 0.50f)), // T10 Bico de Pato
        Pair(11, Offset(0.62f, 0.63f)), // T11 Mergulho
        Pair(12, Offset(0.76f, 0.78f)), // T12 Junção
        Pair(13, Offset(0.86f, 0.62f)), // T13 Subida dos Boxes
        Pair(14, Offset(0.87f, 0.48f)), // T14 Curva do Café
        Pair(15, Offset(0.85f, 0.33f))  // T15 Arquibancadas
    )
}

/**
 * Normalized geometry for Yas Marina Circuit (Abu Dhabi).
 */
object YasMarinaCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.35f, 0.85f),
        Offset(0.20f, 0.85f),
        Offset(0.14f, 0.76f),
        Offset(0.18f, 0.62f),
        Offset(0.24f, 0.50f),
        Offset(0.28f, 0.38f),
        Offset(0.32f, 0.24f)
    )

    val sector2Points = listOf(
        Offset(0.32f, 0.24f),
        Offset(0.48f, 0.16f),
        Offset(0.66f, 0.16f),
        Offset(0.78f, 0.22f),
        Offset(0.72f, 0.34f),
        Offset(0.62f, 0.42f),
        Offset(0.68f, 0.56f)
    )

    val sector3Points = listOf(
        Offset(0.68f, 0.56f),
        Offset(0.78f, 0.64f),
        Offset(0.84f, 0.74f),
        Offset(0.76f, 0.84f),
        Offset(0.62f, 0.86f),
        Offset(0.48f, 0.85f),
        Offset(0.35f, 0.85f)
    )

    val allPoints: List<Offset> by lazy {
        val list = mutableListOf<Offset>()
        list.addAll(sector1Points)
        list.addAll(sector2Points.drop(1))
        list.addAll(sector3Points.drop(1))
        list
    }

    val corners = listOf(
        Pair(1, Offset(0.14f, 0.76f)),
        Pair(2, Offset(0.18f, 0.62f)),
        Pair(3, Offset(0.24f, 0.50f)),
        Pair(4, Offset(0.28f, 0.38f)),
        Pair(5, Offset(0.32f, 0.24f)),
        Pair(6, Offset(0.48f, 0.16f)),
        Pair(7, Offset(0.78f, 0.22f)),
        Pair(8, Offset(0.72f, 0.34f)),
        Pair(9, Offset(0.62f, 0.42f)),
        Pair(10, Offset(0.68f, 0.56f)),
        Pair(11, Offset(0.78f, 0.64f)),
        Pair(12, Offset(0.84f, 0.74f)),
        Pair(13, Offset(0.76f, 0.84f)),
        Pair(14, Offset(0.62f, 0.86f)),
        Pair(15, Offset(0.48f, 0.85f)),
        Pair(16, Offset(0.38f, 0.85f))
    )
}

/**
 * Normalized geometry for Bahrain International Circuit (Sakhir).
 */
object BahrainCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.22f, 0.80f), // Start/Finish
        Offset(0.55f, 0.84f), // Main Straight
        Offset(0.84f, 0.86f), // Turn 1 Hairpin
        Offset(0.76f, 0.72f), // Turn 2
        Offset(0.80f, 0.62f), // Turn 3
        Offset(0.72f, 0.42f)  // Turn 4 Hairpin
    )

    val sector2Points = listOf(
        Offset(0.72f, 0.42f),
        Offset(0.60f, 0.46f), // Turn 5
        Offset(0.52f, 0.40f), // Turn 6
        Offset(0.44f, 0.48f), // Turn 7
        Offset(0.36f, 0.36f), // Turn 8 Hairpin
        Offset(0.46f, 0.24f), // Turn 9
        Offset(0.26f, 0.16f)  // Turn 10 Hairpin
    )

    val sector3Points = listOf(
        Offset(0.26f, 0.16f),
        Offset(0.24f, 0.38f), // Back Straight
        Offset(0.20f, 0.52f), // Turn 11
        Offset(0.34f, 0.56f), // Turn 12
        Offset(0.48f, 0.58f), // Turn 13
        Offset(0.30f, 0.72f), // Turn 14
        Offset(0.14f, 0.78f), // Turn 15
        Offset(0.22f, 0.80f)  // Rejoin Start/Finish
    )

    val allPoints: List<Offset> by lazy {
        val list = mutableListOf<Offset>()
        list.addAll(sector1Points)
        list.addAll(sector2Points.drop(1))
        list.addAll(sector3Points.drop(1))
        list
    }

    val corners = listOf(
        Pair(1, Offset(0.85f, 0.86f)),
        Pair(2, Offset(0.75f, 0.72f)),
        Pair(4, Offset(0.73f, 0.41f)),
        Pair(6, Offset(0.52f, 0.39f)),
        Pair(8, Offset(0.35f, 0.35f)),
        Pair(10, Offset(0.25f, 0.15f)),
        Pair(11, Offset(0.19f, 0.52f)),
        Pair(13, Offset(0.49f, 0.58f)),
        Pair(14, Offset(0.29f, 0.73f)),
        Pair(15, Offset(0.13f, 0.78f))
    )
}

/**
 * Normalized geometry for Albert Park Circuit (Melbourne, Australia).
 */
object MelbourneCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.24f, 0.78f), // Start/Finish
        Offset(0.48f, 0.84f), // Turn 1
        Offset(0.62f, 0.78f), // Turn 3
        Offset(0.64f, 0.66f), // Turn 4
        Offset(0.76f, 0.64f), // Turn 5
        Offset(0.84f, 0.46f)  // Turn 6-7
    )

    val sector2Points = listOf(
        Offset(0.84f, 0.46f),
        Offset(0.78f, 0.26f), // Turn 8 Lakeside
        Offset(0.56f, 0.16f), // Turn 9-10 Chicane
        Offset(0.36f, 0.18f), // Turn 11
        Offset(0.24f, 0.28f)  // Turn 12
    )

    val sector3Points = listOf(
        Offset(0.24f, 0.28f),
        Offset(0.26f, 0.46f), // Turn 13
        Offset(0.16f, 0.56f), // Turn 14
        Offset(0.14f, 0.72f), // Turn 15-16
        Offset(0.24f, 0.78f)  // Rejoin Start/Finish
    )

    val allPoints: List<Offset> by lazy {
        val list = mutableListOf<Offset>()
        list.addAll(sector1Points)
        list.addAll(sector2Points.drop(1))
        list.addAll(sector3Points.drop(1))
        list
    }

    val corners = listOf(
        Pair(1, Offset(0.48f, 0.85f)),
        Pair(3, Offset(0.63f, 0.79f)),
        Pair(5, Offset(0.77f, 0.65f)),
        Pair(6, Offset(0.85f, 0.46f)),
        Pair(9, Offset(0.56f, 0.15f)),
        Pair(11, Offset(0.36f, 0.17f)),
        Pair(13, Offset(0.27f, 0.46f)),
        Pair(14, Offset(0.15f, 0.56f))
    )
}

/**
 * Normalized geometry for Silverstone Circuit (Great Britain).
 */
object SilverstoneCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.44f, 0.84f), // Hamilton Straight
        Offset(0.62f, 0.82f), // Abbey T1
        Offset(0.68f, 0.70f), // Farm T2
        Offset(0.56f, 0.64f), // Village T3
        Offset(0.62f, 0.54f), // The Loop T4
        Offset(0.74f, 0.56f), // Aintree T5
        Offset(0.84f, 0.36f)  // Wellington Straight
    )

    val sector2Points = listOf(
        Offset(0.84f, 0.36f),
        Offset(0.84f, 0.20f), // Brooklands T6
        Offset(0.74f, 0.16f), // Luffield T7
        Offset(0.66f, 0.26f), // Woodcote T8
        Offset(0.52f, 0.20f), // Copse T9
        Offset(0.38f, 0.24f), // Maggotts
        Offset(0.32f, 0.34f), // Becketts
        Offset(0.24f, 0.42f)  // Chapel T14
    )

    val sector3Points = listOf(
        Offset(0.24f, 0.42f),
        Offset(0.16f, 0.62f), // Hangar Straight
        Offset(0.18f, 0.76f), // Stowe T15
        Offset(0.28f, 0.86f), // Vale T16
        Offset(0.36f, 0.84f), // Club T17-18
        Offset(0.44f, 0.84f)  // Rejoin Start/Finish
    )

    val allPoints: List<Offset> by lazy {
        val list = mutableListOf<Offset>()
        list.addAll(sector1Points)
        list.addAll(sector2Points.drop(1))
        list.addAll(sector3Points.drop(1))
        list
    }

    val corners = listOf(
        Pair(1, Offset(0.63f, 0.83f)),
        Pair(3, Offset(0.55f, 0.64f)),
        Pair(4, Offset(0.62f, 0.53f)),
        Pair(6, Offset(0.85f, 0.19f)),
        Pair(9, Offset(0.52f, 0.19f)),
        Pair(11, Offset(0.38f, 0.23f)),
        Pair(13, Offset(0.31f, 0.34f)),
        Pair(15, Offset(0.17f, 0.76f)),
        Pair(18, Offset(0.36f, 0.85f))
    )
}

/**
 * Normalized geometry for Circuit de Monaco (Monte Carlo).
 */
object MonacoCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.24f, 0.68f), // Start/Finish
        Offset(0.26f, 0.48f), // Sainte-Devote T1
        Offset(0.36f, 0.30f), // Beau Rivage
        Offset(0.54f, 0.18f), // Casino Square T4
        Offset(0.68f, 0.24f)  // Mirabeau T5
    )

    val sector2Points = listOf(
        Offset(0.68f, 0.24f),
        Offset(0.84f, 0.26f), // Fairmont Hairpin T6
        Offset(0.76f, 0.36f), // Mirabeau Bas T7
        Offset(0.86f, 0.46f), // Portier T8
        Offset(0.72f, 0.58f), // Tunnel
        Offset(0.56f, 0.60f)  // Nouvelle Chicane T10
    )

    val sector3Points = listOf(
        Offset(0.56f, 0.60f),
        Offset(0.48f, 0.68f), // Tabac T12
        Offset(0.44f, 0.78f), // Swimming Pool
        Offset(0.36f, 0.82f),
        Offset(0.22f, 0.84f), // La Rascasse T17
        Offset(0.18f, 0.76f), // Anthony Noghes T18
        Offset(0.24f, 0.68f)  // Rejoin Start/Finish
    )

    val allPoints: List<Offset> by lazy {
        val list = mutableListOf<Offset>()
        list.addAll(sector1Points)
        list.addAll(sector2Points.drop(1))
        list.addAll(sector3Points.drop(1))
        list
    }

    val corners = listOf(
        Pair(1, Offset(0.25f, 0.48f)),
        Pair(3, Offset(0.54f, 0.17f)),
        Pair(5, Offset(0.68f, 0.23f)),
        Pair(6, Offset(0.85f, 0.26f)),
        Pair(8, Offset(0.87f, 0.46f)),
        Pair(10, Offset(0.56f, 0.59f)),
        Pair(12, Offset(0.48f, 0.69f)),
        Pair(15, Offset(0.44f, 0.79f)),
        Pair(17, Offset(0.21f, 0.85f)),
        Pair(18, Offset(0.17f, 0.76f))
    )
}

/**
 * Normalized geometry for Autodromo Nazionale Monza (Italy).
 */
object MonzaCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.44f, 0.84f), // Main Straight
        Offset(0.76f, 0.84f), // Rettifilo Chicane T1-T2
        Offset(0.86f, 0.68f), // Curva Grande T3
        Offset(0.76f, 0.50f)  // Variante della Roggia T4-T5
    )

    val sector2Points = listOf(
        Offset(0.76f, 0.50f),
        Offset(0.80f, 0.26f), // Lesmo 1 T6
        Offset(0.66f, 0.16f), // Lesmo 2 T7
        Offset(0.48f, 0.22f), // Serraglio
        Offset(0.34f, 0.34f), // Ascari T8-T10
        Offset(0.28f, 0.44f)
    )

    val sector3Points = listOf(
        Offset(0.28f, 0.44f),
        Offset(0.20f, 0.64f), // Back Straight
        Offset(0.18f, 0.80f), // Parabolica T11
        Offset(0.28f, 0.86f),
        Offset(0.44f, 0.84f)  // Rejoin Start/Finish
    )

    val allPoints: List<Offset> by lazy {
        val list = mutableListOf<Offset>()
        list.addAll(sector1Points)
        list.addAll(sector2Points.drop(1))
        list.addAll(sector3Points.drop(1))
        list
    }

    val corners = listOf(
        Pair(1, Offset(0.76f, 0.85f)),
        Pair(3, Offset(0.87f, 0.68f)),
        Pair(4, Offset(0.75f, 0.50f)),
        Pair(6, Offset(0.81f, 0.25f)),
        Pair(7, Offset(0.66f, 0.15f)),
        Pair(8, Offset(0.34f, 0.33f)),
        Pair(11, Offset(0.17f, 0.80f))
    )
}

/**
 * Normalized geometry for Suzuka International Racing Course (Japan).
 */
object SuzukaCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.62f, 0.78f), // Start/Finish
        Offset(0.82f, 0.74f), // Turn 1-2
        Offset(0.76f, 0.54f), // S-Curves
        Offset(0.84f, 0.44f),
        Offset(0.74f, 0.34f),
        Offset(0.68f, 0.22f)  // Dunlop T7
    )

    val sector2Points = listOf(
        Offset(0.68f, 0.22f),
        Offset(0.52f, 0.18f), // Degner 1
        Offset(0.50f, 0.30f), // Degner 2
        Offset(0.44f, 0.40f), // Crossover Underpass
        Offset(0.36f, 0.32f), // Hairpin T11
        Offset(0.22f, 0.38f), // 200R
        Offset(0.14f, 0.28f), // Spoon Curve entry
        Offset(0.14f, 0.44f)  // Spoon Curve exit
    )

    val sector3Points = listOf(
        Offset(0.14f, 0.44f),
        Offset(0.34f, 0.56f), // Back Straight (Crossover Overpass)
        Offset(0.52f, 0.66f), // 130R T15
        Offset(0.56f, 0.80f), // Casio Triangle T16-T18
        Offset(0.62f, 0.78f)  // Rejoin Start/Finish
    )

    val allPoints: List<Offset> by lazy {
        val list = mutableListOf<Offset>()
        list.addAll(sector1Points)
        list.addAll(sector2Points.drop(1))
        list.addAll(sector3Points.drop(1))
        list
    }

    val corners = listOf(
        Pair(1, Offset(0.83f, 0.74f)),
        Pair(4, Offset(0.85f, 0.44f)),
        Pair(7, Offset(0.68f, 0.21f)),
        Pair(8, Offset(0.52f, 0.17f)),
        Pair(11, Offset(0.36f, 0.31f)),
        Pair(13, Offset(0.13f, 0.28f)),
        Pair(15, Offset(0.52f, 0.67f)),
        Pair(16, Offset(0.56f, 0.81f))
    )
}

/**
 * Normalized geometry for Circuit de Spa-Francorchamps (Belgium).
 */
object SpaCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.24f, 0.44f), // Start/Finish
        Offset(0.14f, 0.32f), // La Source T1 Hairpin
        Offset(0.24f, 0.20f), // Eau Rouge / Raidillon
        Offset(0.48f, 0.16f), // Kemmel Straight
        Offset(0.72f, 0.18f)  // Les Combes T5-T7
    )

    val sector2Points = listOf(
        Offset(0.72f, 0.18f),
        Offset(0.84f, 0.28f), // Bruxelles T8
        Offset(0.72f, 0.36f), // Speaker's Corner T9
        Offset(0.86f, 0.48f), // Pouhon T10-T11
        Offset(0.76f, 0.64f), // Fagnes T12-T13
        Offset(0.84f, 0.80f)  // Stavelot T14-T15
    )

    val sector3Points = listOf(
        Offset(0.84f, 0.80f),
        Offset(0.62f, 0.84f), // Paul Frere
        Offset(0.44f, 0.74f), // Blanchimont T16-T17
        Offset(0.32f, 0.60f), // Bus Stop Chicane T18-T19
        Offset(0.24f, 0.44f)  // Rejoin Start/Finish
    )

    val allPoints: List<Offset> by lazy {
        val list = mutableListOf<Offset>()
        list.addAll(sector1Points)
        list.addAll(sector2Points.drop(1))
        list.addAll(sector3Points.drop(1))
        list
    }

    val corners = listOf(
        Pair(1, Offset(0.13f, 0.32f)),
        Pair(3, Offset(0.24f, 0.19f)),
        Pair(5, Offset(0.72f, 0.17f)),
        Pair(8, Offset(0.85f, 0.28f)),
        Pair(10, Offset(0.87f, 0.48f)),
        Pair(12, Offset(0.75f, 0.64f)),
        Pair(14, Offset(0.85f, 0.80f)),
        Pair(17, Offset(0.44f, 0.75f)),
        Pair(18, Offset(0.32f, 0.61f))
    )
}

/**
 * Resolves circuit geometry by circuit type or circuitId/name string.
 */
fun getCircuitGeometry(circuitType: String): CircuitGeometryData {
    val key = circuitType.trim().uppercase()
    return when {
        key in setOf("INTERLAGOS", "BRAZIL", "SAO_PAULO", "SAO PAULO") ||
            key.contains("INTERLAGOS") || key.contains("SAO_PAULO") -> CircuitGeometryData(
            sector1Points = InterlagosCircuitGeometry.sector1Points,
            sector2Points = InterlagosCircuitGeometry.sector2Points,
            sector3Points = InterlagosCircuitGeometry.sector3Points,
            allPoints = InterlagosCircuitGeometry.allPoints,
            corners = InterlagosCircuitGeometry.corners,
            startFinishPos = Offset(0.65f, 0.20f),
            speedTrapPos = Offset(0.14f, 0.68f),
            speedTrapBadgePos = Offset(0.26f, 0.64f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("YAS_MARINA", "YAS MARINA", "ABU_DHABI", "ABU DHABI") ||
            key.contains("YAS") || key.contains("ABU") -> CircuitGeometryData(
            sector1Points = YasMarinaCircuitGeometry.sector1Points,
            sector2Points = YasMarinaCircuitGeometry.sector2Points,
            sector3Points = YasMarinaCircuitGeometry.sector3Points,
            allPoints = YasMarinaCircuitGeometry.allPoints,
            corners = YasMarinaCircuitGeometry.corners,
            startFinishPos = Offset(0.35f, 0.85f),
            speedTrapPos = Offset(0.52f, 0.16f),
            speedTrapBadgePos = Offset(0.52f, 0.25f),
            s1CutoffProgress = 0.32f,
            s2CutoffProgress = 0.66f
        )
        key in setOf("BAHRAIN", "SAKHIR") ||
            key.contains("BAHRAIN") || key.contains("SAKHIR") -> CircuitGeometryData(
            sector1Points = BahrainCircuitGeometry.sector1Points,
            sector2Points = BahrainCircuitGeometry.sector2Points,
            sector3Points = BahrainCircuitGeometry.sector3Points,
            allPoints = BahrainCircuitGeometry.allPoints,
            corners = BahrainCircuitGeometry.corners,
            startFinishPos = BahrainCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.58f, 0.84f),
            speedTrapBadgePos = Offset(0.58f, 0.74f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("MELBOURNE", "ALBERT_PARK", "ALBERT PARK", "AUSTRALIA") ||
            key.contains("MELBOURNE") || key.contains("ALBERT") -> CircuitGeometryData(
            sector1Points = MelbourneCircuitGeometry.sector1Points,
            sector2Points = MelbourneCircuitGeometry.sector2Points,
            sector3Points = MelbourneCircuitGeometry.sector3Points,
            allPoints = MelbourneCircuitGeometry.allPoints,
            corners = MelbourneCircuitGeometry.corners,
            startFinishPos = MelbourneCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.68f, 0.21f),
            speedTrapBadgePos = Offset(0.62f, 0.30f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("SILVERSTONE", "GREAT_BRITAIN", "BRITAIN", "UK") ||
            key.contains("SILVERSTONE") -> CircuitGeometryData(
            sector1Points = SilverstoneCircuitGeometry.sector1Points,
            sector2Points = SilverstoneCircuitGeometry.sector2Points,
            sector3Points = SilverstoneCircuitGeometry.sector3Points,
            allPoints = SilverstoneCircuitGeometry.allPoints,
            corners = SilverstoneCircuitGeometry.corners,
            startFinishPos = SilverstoneCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.19f, 0.54f),
            speedTrapBadgePos = Offset(0.33f, 0.56f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("MONACO", "MONTE_CARLO", "MONTE CARLO") ||
            key.contains("MONACO") || key.contains("MONTE") -> CircuitGeometryData(
            sector1Points = MonacoCircuitGeometry.sector1Points,
            sector2Points = MonacoCircuitGeometry.sector2Points,
            sector3Points = MonacoCircuitGeometry.sector3Points,
            allPoints = MonacoCircuitGeometry.allPoints,
            corners = MonacoCircuitGeometry.corners,
            startFinishPos = MonacoCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.70f, 0.58f),
            speedTrapBadgePos = Offset(0.66f, 0.48f),
            s1CutoffProgress = 0.33f,
            s2CutoffProgress = 0.67f
        )
        key in setOf("MONZA", "ITALY", "ITALIAN") ||
            key.contains("MONZA") -> CircuitGeometryData(
            sector1Points = MonzaCircuitGeometry.sector1Points,
            sector2Points = MonzaCircuitGeometry.sector2Points,
            sector3Points = MonzaCircuitGeometry.sector3Points,
            allPoints = MonzaCircuitGeometry.allPoints,
            corners = MonzaCircuitGeometry.corners,
            startFinishPos = MonzaCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.62f, 0.84f),
            speedTrapBadgePos = Offset(0.60f, 0.74f),
            s1CutoffProgress = 0.33f,
            s2CutoffProgress = 0.67f
        )
        key in setOf("SUZUKA", "JAPAN", "JAPANESE") ||
            key.contains("SUZUKA") -> CircuitGeometryData(
            sector1Points = SuzukaCircuitGeometry.sector1Points,
            sector2Points = SuzukaCircuitGeometry.sector2Points,
            sector3Points = SuzukaCircuitGeometry.sector3Points,
            allPoints = SuzukaCircuitGeometry.allPoints,
            corners = SuzukaCircuitGeometry.corners,
            startFinishPos = SuzukaCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.46f, 0.62f),
            speedTrapBadgePos = Offset(0.46f, 0.52f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("SPA", "SPA_FRANCORCHAMPS", "SPA-FRANCORCHAMPS", "BELGIUM", "BELGIAN") ||
            key.contains("SPA") -> CircuitGeometryData(
            sector1Points = SpaCircuitGeometry.sector1Points,
            sector2Points = SpaCircuitGeometry.sector2Points,
            sector3Points = SpaCircuitGeometry.sector3Points,
            allPoints = SpaCircuitGeometry.allPoints,
            corners = SpaCircuitGeometry.corners,
            startFinishPos = SpaCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.56f, 0.16f),
            speedTrapBadgePos = Offset(0.56f, 0.25f),
            s1CutoffProgress = 0.33f,
            s2CutoffProgress = 0.68f
        )
        else -> CircuitGeometryData(
            sector1Points = SepangCircuitGeometry.sector1Points,
            sector2Points = SepangCircuitGeometry.sector2Points,
            sector3Points = SepangCircuitGeometry.sector3Points,
            allPoints = SepangCircuitGeometry.allPoints,
            corners = SepangCircuitGeometry.corners,
            startFinishPos = SepangCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.68f, 0.72f),
            speedTrapBadgePos = Offset(0.68f, 0.63f),
            s1CutoffProgress = 0.35f,
            s2CutoffProgress = 0.70f
        )
    }
}

/**
 * Builds a smooth Catmull-Rom cubic Bezier path from normalized waypoints.
 * When close = true, computes smooth cyclic Catmull-Rom tangents across the start/end boundary
 * so there is zero sharp geometric cusp.
 */
fun buildCircuitPath(points: List<Offset>, width: Float, height: Float, close: Boolean = false): Path {
    val path = Path()
    if (points.isEmpty()) return path

    val rawScaled = points.map { Offset(it.x * width, it.y * height) }
    // If closed loop and endpoints coincide, deduplicate for cyclic tangent calculation
    val scaledPoints = if (close && rawScaled.size > 1 &&
        rawScaled.first().x == rawScaled.last().x &&
        rawScaled.first().y == rawScaled.last().y
    ) {
        rawScaled.dropLast(1)
    } else {
        rawScaled
    }

    val n = scaledPoints.size
    if (n < 2) return path

    path.moveTo(scaledPoints.first().x, scaledPoints.first().y)

    if (close && n >= 3) {
        // Smooth cyclic Catmull-Rom spline across start/end boundary
        for (i in 0 until n) {
            val p0 = scaledPoints[(i - 1 + n) % n]
            val p1 = scaledPoints[i]
            val p2 = scaledPoints[(i + 1) % n]
            val p3 = scaledPoints[(i + 2) % n]

            val cp1X = p1.x + (p2.x - p0.x) / 6f
            val cp1Y = p1.y + (p2.y - p0.y) / 6f
            val cp2X = p2.x - (p3.x - p1.x) / 6f
            val cp2Y = p2.y - (p3.y - p1.y) / 6f

            path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p2.x, p2.y)
        }
        path.close()
    } else {
        // Open sub-sector spline
        for (i in 0 until n - 1) {
            val p0 = if (i > 0) scaledPoints[i - 1] else scaledPoints[i]
            val p1 = scaledPoints[i]
            val p2 = scaledPoints[i + 1]
            val p3 = if (i + 2 < n) scaledPoints[i + 2] else p2

            val cp1X = p1.x + (p2.x - p0.x) / 6f
            val cp1Y = p1.y + (p2.y - p0.y) / 6f
            val cp2X = p2.x - (p3.x - p1.x) / 6f
            val cp2Y = p2.y - (p3.y - p1.y) / 6f

            path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p2.x, p2.y)
        }
        if (close) {
            path.close()
        }
    }
    return path
}

private class CachedCircuitPath(
    var width: Float = -1f,
    var height: Float = -1f,
    var circuitType: String = "",
    var path: Path = Path(),
    var s1Path: Path = Path(),
    var s2Path: Path = Path(),
    var s3Path: Path = Path(),
    var pathLength: Float = 0f
)

/**
 * Generic glowing white circuit outline canvas for any circuit type (Sepang, Interlagos, Yas Marina).
 */
@Composable
fun CircuitOutlineCanvas(
    modifier: Modifier = Modifier,
    circuitType: String = "SEPANG"
) {
    val geometry = remember(circuitType) { getCircuitGeometry(circuitType) }
    val cached = remember(circuitType) { CachedCircuitPath() }
    val isDark = LocalF1Accent.current.isDark
    val outlineColor = if (isDark) Color.White else Color(0xFF1A1A22)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        if (cached.width != w || cached.height != h || cached.circuitType != circuitType) {
            cached.path = buildCircuitPath(geometry.allPoints, w, h, close = true)
            cached.width = w
            cached.height = h
            cached.circuitType = circuitType
        }
        val circuitPath = cached.path

        // Outer glow layer
        drawPath(
            path = circuitPath,
            color = outlineColor.copy(alpha = if (isDark) 0.18f else 0.12f),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Mid glow layer
        drawPath(
            path = circuitPath,
            color = outlineColor.copy(alpha = if (isDark) 0.40f else 0.25f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Crisp inner stroke
        drawPath(
            path = circuitPath,
            color = outlineColor,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Start / Finish Line marker
        val sfX = geometry.startFinishPos.x * w
        val sfY = geometry.startFinishPos.y * h
        drawLine(
            color = Color(0xFFE1251B),
            start = Offset(sfX, sfY - 6.dp.toPx()),
            end = Offset(sfX, sfY + 6.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Square
        )
    }
}

/**
 * 1. SepangOutlineCanvas
 * Draws the double-stroke glowing white circuit outline of Sepang International Circuit.
 */
@Composable
fun SepangOutlineCanvas(modifier: Modifier = Modifier) {
    CircuitOutlineCanvas(modifier = modifier, circuitType = "SEPANG")
}

/**
 * 2. MultiSectorCircuitMap
 * Draws the 3-sector colored circuit map (Sector 1 Red #E1251B, Sector 2 Cyan #00B4D8, Sector 3 Yellow #FFD100)
 * with numbered corner markers, start/finish checkered square, SECTOR 1 / 2 / 3 labels,
 * and the magenta SPEED TRAP badge with callout line.
 * Supports "SEPANG", "INTERLAGOS", "YAS_MARINA".
 */
@Composable
fun MultiSectorCircuitMap(
    modifier: Modifier = Modifier,
    circuitType: String = "SEPANG"
) {
    val cornerPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val sectorTextPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val geometry = remember(circuitType) { getCircuitGeometry(circuitType) }
    val cached = remember(circuitType) { CachedCircuitPath() }
    val isDark = LocalF1Accent.current.isDark
    val trackShadowColor = if (isDark) Color(0xFF1E1E26) else Color(0xFFD6D6E2)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        if (cached.width != w || cached.height != h || cached.circuitType != circuitType) {
            cached.s1Path = buildCircuitPath(geometry.sector1Points, w, h)
            cached.s2Path = buildCircuitPath(geometry.sector2Points, w, h)
            cached.s3Path = buildCircuitPath(geometry.sector3Points, w, h)
            cached.path = buildCircuitPath(geometry.allPoints, w, h, close = true)
            cached.width = w
            cached.height = h
            cached.circuitType = circuitType
        }
        val s1Path = cached.s1Path
        val s2Path = cached.s2Path
        val s3Path = cached.s3Path
        val fullPath = cached.path

        val strokeWidth = 5.5.dp.toPx()

        // Background shadow track ribbon
        drawPath(
            path = fullPath,
            color = trackShadowColor,
            style = Stroke(width = strokeWidth + 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Sector 1 - Red (#E1251B)
        drawPath(
            path = s1Path,
            color = Color(0xFFE1251B),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Sector 2 - Cyan (#00B4D8)
        drawPath(
            path = s2Path,
            color = Color(0xFF00B4D8),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Sector 3 - Yellow (#FFD100)
        drawPath(
            path = s3Path,
            color = Color(0xFFFFD100),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Corner Markers
        cornerPaint.textSize = 9.dp.toPx()
        geometry.corners.forEach { (num, pos) ->
            val cx = pos.x * w
            val cy = pos.y * h

            drawCircle(
                color = Color(0xFF141418),
                radius = 8.5.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF383844),
                radius = 8.5.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )

            cornerPaint.color = android.graphics.Color.WHITE
            val text = if (num < 10) "0$num" else "$num"
            drawContext.canvas.nativeCanvas.drawText(
                text,
                cx,
                cy + 3.dp.toPx(),
                cornerPaint
            )
        }

        // Start / Finish Line (checkered pattern)
        val sfX = geometry.startFinishPos.x * w
        val sfY = geometry.startFinishPos.y * h
        val checkH = 5.dp.toPx()
        val checkW = 3.dp.toPx()
        for (row in 0..1) {
            for (col in 0..2) {
                val isWhite = (row + col) % 2 == 0
                drawRect(
                    color = if (isWhite) Color.White else Color.Black,
                    topLeft = Offset(sfX + col * checkW - 4.5.dp.toPx(), sfY - checkH + row * checkH),
                    size = Size(checkW, checkH)
                )
            }
        }

        // Sector Badges
        val s1Center = Offset(geometry.sector1Points[geometry.sector1Points.size / 2].x * w, (geometry.sector1Points[geometry.sector1Points.size / 2].y + 0.08f) * h)
        val s2Center = Offset(geometry.sector2Points[geometry.sector2Points.size / 2].x * w, (geometry.sector2Points[geometry.sector2Points.size / 2].y - 0.08f) * h)
        val s3Center = Offset(geometry.sector3Points[geometry.sector3Points.size / 2].x * w, (geometry.sector3Points[geometry.sector3Points.size / 2].y + 0.08f) * h)

        drawSectorLabel("SECTOR 1", Color(0xFFE1251B), s1Center, sectorTextPaint)
        drawSectorLabel("SECTOR 2", Color(0xFF00B4D8), s2Center, sectorTextPaint)
        drawSectorLabel("SECTOR 3", Color(0xFFFFD100), s3Center, sectorTextPaint)

        // SPEED TRAP Callout & Badge
        val trapTarget = Offset(geometry.speedTrapPos.x * w, geometry.speedTrapPos.y * h)
        val trapBadgePos = Offset(geometry.speedTrapBadgePos.x * w, geometry.speedTrapBadgePos.y * h)

        val trapBadgeWidth = 68.dp.toPx()
        val trapBadgeHeight = 16.dp.toPx()
        val pad = 2.dp.toPx()
        val trapLeft = (trapBadgePos.x - trapBadgeWidth / 2f).coerceIn(pad, (w - trapBadgeWidth - pad).coerceAtLeast(pad))
        val trapTop = (trapBadgePos.y - trapBadgeHeight).coerceIn(pad, (h - trapBadgeHeight - pad).coerceAtLeast(pad))
        val clampedBadgeAnchor = Offset(trapLeft + trapBadgeWidth / 2f, trapTop + trapBadgeHeight)

        drawLine(
            color = P1TColors.SpeedTrapMagenta,
            start = clampedBadgeAnchor,
            end = trapTarget,
            strokeWidth = 1.2.dp.toPx()
        )
        drawCircle(
            color = P1TColors.SpeedTrapMagenta,
            radius = 3.dp.toPx(),
            center = trapTarget
        )

        drawRoundRect(
            color = P1TColors.SpeedTrapMagenta,
            topLeft = Offset(trapLeft, trapTop),
            size = Size(trapBadgeWidth, trapBadgeHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        cornerPaint.color = android.graphics.Color.WHITE
        cornerPaint.textSize = 8.dp.toPx()
        drawContext.canvas.nativeCanvas.drawText(
            "SPEED TRAP",
            trapLeft + trapBadgeWidth / 2f,
            trapTop + 11.5.dp.toPx(),
            cornerPaint
        )
    }
}

private fun DrawScope.drawSectorLabel(
    text: String,
    color: Color,
    center: Offset,
    paint: android.graphics.Paint
) {
    paint.textSize = 9.dp.toPx()
    paint.color = android.graphics.Color.WHITE

    val badgeW = 60.dp.toPx()
    val badgeH = 17.dp.toPx()
    val pad = 2.dp.toPx()
    val left = (center.x - badgeW / 2f).coerceIn(pad, (size.width - badgeW - pad).coerceAtLeast(pad))
    val top = (center.y - badgeH / 2f).coerceIn(pad, (size.height - badgeH - pad).coerceAtLeast(pad))

    drawRoundRect(
        color = Color(0xFF14141A),
        topLeft = Offset(left, top),
        size = Size(badgeW, badgeH),
        cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx())
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(badgeW, badgeH),
        cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx()),
        style = Stroke(width = 1.2.dp.toPx())
    )

    drawContext.canvas.nativeCanvas.drawText(
        text,
        left + badgeW / 2f,
        top + 12.dp.toPx(),
        paint
    )
}

private class DriverTapTarget(
    val code: String,
    val dotPos: Offset,
    val pillRect: Rect
)

private class TapTargetSnapshot {
    @Volatile
    var targets: List<DriverTapTarget> = emptyList()
}

/**
 * 3. LiveTrackMapCanvas
 * Draws the grey ribbon circuit with white sector divider ticks (S1, S2, S3),
 * start/finish checkered line, corner numbers (1..15), and moving team-colored driver dots
 * with dark rounded pill labels (VER, ANT, HAM, etc.).
 *
 * Performance fixes:
 * - Pre-allocates PathMeasure and Paint objects via `remember` to eliminate 12.5 Hz GC churn.
 * - Uses `.pointerInput(Unit)` with `rememberUpdatedState` so the 80ms ticker never cancels tap gestures.
 */
@Composable
fun LiveTrackMapCanvas(
    drivers: List<LiveDriverTiming>,
    modifier: Modifier = Modifier,
    selectedDriverCode: String? = null,
    onDriverTap: ((String) -> Unit)? = null,
    circuitType: String = "INTERLAGOS"
) {
    val currentDrivers by rememberUpdatedState(drivers)
    val currentOnDriverTap by rememberUpdatedState(onDriverTap)
    val geometry = remember(circuitType) { getCircuitGeometry(circuitType) }

    val pulseScaleState: State<Float> = if (selectedDriverCode != null) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        remember { mutableFloatStateOf(1.0f) }
    }

    // Pre-allocate to prevent 12.5 Hz garbage collection churn
    val pathMeasure = remember { PathMeasure() }
    val cached = remember(circuitType) { CachedCircuitPath() }
    val labelPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val cornerPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val sectorTextPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    // Snapshot of driver dot + clamped pill bounds updated atomically during draw
    val tapTargetSnapshot = remember { TapTargetSnapshot() }

    val canvasModifier = if (onDriverTap != null) {
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val targets = tapTargetSnapshot.targets
                    val maxTapDist = 40.dp.toPx()
                    val pillHitPad = 8.dp.toPx()
                    var closestDriver: String? = null
                    var bestIsExactPill = false
                    var minDist = Float.MAX_VALUE

                    for (target in targets) {
                        val exactPill = target.pillRect.contains(tapOffset)
                        val paddedPill =
                            tapOffset.x in (target.pillRect.left - pillHitPad)..(target.pillRect.right + pillHitPad) &&
                                tapOffset.y in (target.pillRect.top - pillHitPad)..(target.pillRect.bottom + pillHitPad)
                        val dotDist = hypot(tapOffset.x - target.dotPos.x, tapOffset.y - target.dotPos.y)
                        val pillCenter = target.pillRect.center
                        val pillDist = hypot(tapOffset.x - pillCenter.x, tapOffset.y - pillCenter.y)
                        val effectiveDist = minOf(dotDist, pillDist)

                        if (exactPill) {
                            if (!bestIsExactPill || pillDist <= minDist) {
                                bestIsExactPill = true
                                minDist = pillDist
                                closestDriver = target.code
                            }
                        } else if (!bestIsExactPill && (paddedPill || dotDist < maxTapDist)) {
                            if (effectiveDist < minDist) {
                                minDist = effectiveDist
                                closestDriver = target.code
                            }
                        }
                    }
                    closestDriver?.let { currentOnDriverTap?.invoke(it) }
                }
            }
    } else {
        modifier.fillMaxSize()
    }

    val isDark = LocalF1Accent.current.isDark
    val outerRibbonColor = if (isDark) Color(0xFF1E1E26) else Color(0xFFD4D4E0)
    val innerRibbonColor = if (isDark) Color(0xFF2C2C38) else Color(0xFFAEAEC0)
    val sectorTickColor = if (isDark) Color.White else Color(0xFF1E1E28)

    Canvas(
        modifier = canvasModifier
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        if (cached.width != w || cached.height != h || cached.circuitType != circuitType) {
            cached.path = buildCircuitPath(geometry.allPoints, w, h, close = true)
            pathMeasure.setPath(cached.path, false)
            cached.pathLength = pathMeasure.length
            cached.width = w
            cached.height = h
            cached.circuitType = circuitType
        }
        val fullPath = cached.path
        val pathLength = cached.pathLength
        if (pathLength <= 0f) return@Canvas

        // Outer Track Ribbon
        drawPath(
            path = fullPath,
            color = outerRibbonColor,
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Inner Track Ribbon
        drawPath(
            path = fullPath,
            color = innerRibbonColor,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Corner Numbers (1..15)
        cornerPaint.textSize = 8.dp.toPx()
        geometry.corners.forEach { (num, pos) ->
            val cx = pos.x * w
            val cy = pos.y * h

            drawCircle(
                color = Color(0xFF14141A),
                radius = 6.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF454555),
                radius = 6.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 0.8.dp.toPx())
            )

            cornerPaint.color = android.graphics.Color.WHITE
            drawContext.canvas.nativeCanvas.drawText(
                num.toString(),
                cx,
                cy + 2.8.dp.toPx(),
                cornerPaint
            )
        }

        // Sector Divider Ticks with S1, S2, S3 labels
        val s1Pos = pathMeasure.getPosition(pathLength * geometry.s1CutoffProgress)
        val s2Pos = pathMeasure.getPosition(pathLength * geometry.s2CutoffProgress)
        val sfPos = pathMeasure.getPosition(pathLength * 0.02f)

        // S1 tick & badge
        drawCircle(color = sectorTickColor, radius = 3.dp.toPx(), center = s1Pos)
        drawSectorDividerBadge(text = "S1", pos = s1Pos, paint = sectorTextPaint)

        // S2 tick & badge
        drawCircle(color = sectorTickColor, radius = 3.dp.toPx(), center = s2Pos)
        drawSectorDividerBadge(text = "S2", pos = s2Pos, paint = sectorTextPaint)

        // S3 / Start-Finish tick & badge
        drawCircle(color = Color(0xFFE1251B), radius = 4.dp.toPx(), center = sfPos)
        drawSectorDividerBadge(text = "S3", pos = sfPos, paint = sectorTextPaint)

        // Draw Drivers
        labelPaint.textSize = 9.dp.toPx()
        val nextTargets = ArrayList<DriverTapTarget>(currentDrivers.size)
        val pillW = 28.dp.toPx()
        val pillH = 14.dp.toPx()
        val pad = 2.dp.toPx()
        val maxPillLeft = (w - pillW - pad).coerceAtLeast(pad)
        val maxPillTop = (h - pillH - pad).coerceAtLeast(pad)

        currentDrivers.forEach { driver ->
            val progress = driver.trackProgress.coerceIn(0f, 1f)
            val dotPos = pathMeasure.getPosition(pathLength * progress)

            val isSelected = driver.code.equals(selectedDriverCode, ignoreCase = true)
            val teamColor = Color(driver.team.colorHex)

            // Target ring if selected
            if (isSelected) {
                drawCircle(
                    color = Color(0xFFE1251B).copy(alpha = 0.35f),
                    radius = 18.dp.toPx() * pulseScaleState.value,
                    center = dotPos
                )
                drawCircle(
                    color = Color(0xFFE1251B),
                    radius = 12.dp.toPx(),
                    center = dotPos,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Driver Dot
            drawCircle(
                color = Color(0xFF141418),
                radius = 6.5.dp.toPx(),
                center = dotPos
            )
            drawCircle(
                color = if (isSelected) Color(0xFFE1251B) else teamColor,
                radius = 4.5.dp.toPx(),
                center = dotPos
            )

            // Pill Label clamped within canvas bounds
            val pillLeft = (dotPos.x - pillW / 2f).coerceIn(pad, maxPillLeft)
            val preferredTop = dotPos.y - 18.dp.toPx()
            val pillTop = if (preferredTop < pad) {
                (dotPos.y + 8.dp.toPx()).coerceIn(pad, maxPillTop)
            } else {
                preferredTop.coerceIn(pad, maxPillTop)
            }
            val pillTopLeft = Offset(pillLeft, pillTop)
            nextTargets.add(
                DriverTapTarget(
                    code = driver.code,
                    dotPos = dotPos,
                    pillRect = Rect(pillTopLeft, Size(pillW, pillH))
                )
            )

            if (isSelected) {
                drawRoundRect(
                    color = Color(0xFFE1251B),
                    topLeft = pillTopLeft,
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                labelPaint.color = android.graphics.Color.WHITE
            } else {
                drawRoundRect(
                    color = Color(0xEE141418),
                    topLeft = pillTopLeft,
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = teamColor.copy(alpha = 0.85f),
                    topLeft = pillTopLeft,
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
                labelPaint.color = android.graphics.Color.WHITE
            }

            drawContext.canvas.nativeCanvas.drawText(
                driver.code,
                pillTopLeft.x + pillW / 2f,
                pillTopLeft.y + 10.5.dp.toPx(),
                labelPaint
            )
        }
        tapTargetSnapshot.targets = nextTargets
    }
}

private fun DrawScope.drawSectorDividerBadge(
    text: String,
    pos: Offset,
    paint: android.graphics.Paint
) {
    val badgeW = 18.dp.toPx()
    val badgeH = 11.dp.toPx()
    val pad = 2.dp.toPx()
    val left = (pos.x - badgeW / 2f).coerceIn(pad, (size.width - badgeW - pad).coerceAtLeast(pad))
    val top = (pos.y + 7.dp.toPx()).coerceIn(pad, (size.height - badgeH - pad).coerceAtLeast(pad))
    val offsetBadge = Offset(left, top)

    drawRoundRect(
        color = Color(0xFF161620),
        topLeft = offsetBadge,
        size = Size(badgeW, badgeH),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.6f),
        topLeft = offsetBadge,
        size = Size(badgeW, badgeH),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        style = Stroke(width = 0.8.dp.toPx())
    )

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 7.5.dp.toPx()
    drawContext.canvas.nativeCanvas.drawText(
        text,
        left + badgeW / 2f,
        offsetBadge.y + 8.5.dp.toPx(),
        paint
    )
}

/**
 * 4. F1CarSideProfileCanvas
 * Renders real high-resolution F1 car cutout PNGs.
 */
@Composable
fun F1CarSideProfileCanvas(
    team: F1TeamId,
    modifier: Modifier = Modifier
) {
    val carRes = getTeamCarRes(team)
    Image(
        painter = painterResource(id = carRes),
        contentDescription = "${team.displayName} F1 Car",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
fun F1CarSideProfileCanvas(
    teamColor: Color,
    modifier: Modifier = Modifier
) {
    val carRes = getTeamCarResByColor(teamColor)
    Image(
        painter = painterResource(id = carRes),
        contentDescription = "F1 Car",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

/**
 * 5. F1CarTopDownCanvas
 * Renders real high-resolution F1 car cutout PNGs with vertical/horizontal orientation.
 */
@Composable
fun F1CarTopDownCanvas(
    team: F1TeamId,
    modifier: Modifier = Modifier,
    vertical: Boolean = false
) {
    val carRes = getTeamCarRes(team)
    Image(
        painter = painterResource(id = carRes),
        contentDescription = "${team.displayName} F1 Car",
        contentScale = ContentScale.Fit,
        modifier = if (vertical) modifier.graphicsLayer(rotationZ = -90f) else modifier
    )
}

@Composable
fun F1CarTopDownCanvas(
    teamColor: Color,
    modifier: Modifier = Modifier,
    vertical: Boolean = false
) {
    val carRes = getTeamCarResByColor(teamColor)
    Image(
        painter = painterResource(id = carRes),
        contentDescription = "F1 Car",
        contentScale = ContentScale.Fit,
        modifier = if (vertical) modifier.graphicsLayer(rotationZ = -90f) else modifier
    )
}

/**
 * 6. TeamLogoBadge
 * Renders distinctive team emblems for all 11 Formula 1 constructors:
 * Mercedes 3-pointed star, Ferrari yellow shield, Red Bull twin bulls/sun, McLaren papaya swoosh,
 * RB monogram, Alpine 'A', Audi 4 interlocking rings, Haas 'H', Williams 'W', Aston Martin wings, Cadillac crest.
 */
@Composable
fun TeamLogoBadge(
    team: F1TeamId,
    modifier: Modifier = Modifier,
    sizeDp: Int = 22
) {
    val isDark = LocalF1Accent.current.isDark
    val haasPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.parseColor("#222426")
        }
    }
    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = (w.coerceAtMost(h)) / 2f

        when (team) {
            F1TeamId.MERCEDES -> {
                // Mercedes Three-Pointed Star in Ring
                val ringColor = if (isDark) Color(0xFFD4D4D8) else Color(0xFF4A4A56)
                val starColor = if (isDark) Color.White else Color(0xFF1A1A22)
                drawCircle(
                    color = ringColor,
                    radius = r * 0.90f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.4.dp.toPx())
                )
                for (i in 0..2) {
                    val angle = -Math.PI / 2.0 + (i * 2.0 * Math.PI / 3.0)
                    val starEndX = (cx + r * 0.82f * cos(angle)).toFloat()
                    val starEndY = (cy + r * 0.82f * sin(angle)).toFloat()
                    drawLine(
                        color = starColor,
                        start = Offset(cx, cy),
                        end = Offset(starEndX, starEndY),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            F1TeamId.FERRARI -> {
                // Ferrari Canary Yellow Shield with Tricolor Top Bar
                val shieldPath = Path().apply {
                    moveTo(cx - 0.70f * r, cy - 0.85f * r)
                    lineTo(cx + 0.70f * r, cy - 0.85f * r)
                    lineTo(cx + 0.70f * r, cy + 0.20f * r)
                    cubicTo(cx + 0.65f * r, cy + 0.60f * r, cx + 0.20f * r, cy + 0.85f * r, cx, cy + 0.92f * r)
                    cubicTo(cx - 0.20f * r, cy + 0.85f * r, cx - 0.65f * r, cy + 0.60f * r, cx - 0.70f * r, cy + 0.20f * r)
                    close()
                }
                drawPath(shieldPath, color = Color(0xFFFFE000))
                drawPath(shieldPath, color = Color.Black, style = Stroke(width = 0.8.dp.toPx()))

                // Tricolor top bar (green, white, red)
                val barW = 1.4f * r / 3f
                drawRect(Color(0xFF009246), Offset(cx - 0.70f * r, cy - 0.85f * r), Size(barW, 0.22f * r))
                drawRect(Color.White, Offset(cx - 0.70f * r + barW, cy - 0.85f * r), Size(barW, 0.22f * r))
                drawRect(Color(0xFFCE2B37), Offset(cx - 0.70f * r + 2 * barW, cy - 0.85f * r), Size(barW, 0.22f * r))

                // Black Prancing Horse Silhouette
                val horsePath = Path().apply {
                    moveTo(cx, cy - 0.40f * r)
                    lineTo(cx + 0.20f * r, cy - 0.15f * r)
                    lineTo(cx + 0.05f * r, cy + 0.10f * r)
                    lineTo(cx + 0.22f * r, cy + 0.50f * r)
                    lineTo(cx - 0.05f * r, cy + 0.45f * r)
                    lineTo(cx - 0.20f * r, cy + 0.15f * r)
                    lineTo(cx - 0.10f * r, cy - 0.10f * r)
                    close()
                }
                drawPath(horsePath, color = Color.Black)
            }

            F1TeamId.RED_BULL -> {
                // Red Bull: Golden Sun with Charging Red Bulls
                drawCircle(color = Color(0xFFFFC700), radius = r * 0.70f, center = Offset(cx, cy))
                // Red bulls horns/charge shapes
                val bullLeft = Path().apply {
                    moveTo(cx - 0.10f * r, cy - 0.20f * r)
                    lineTo(cx - 0.65f * r, cy - 0.10f * r)
                    lineTo(cx - 0.50f * r, cy + 0.25f * r)
                    lineTo(cx - 0.15f * r, cy + 0.15f * r)
                    close()
                }
                val bullRight = Path().apply {
                    moveTo(cx + 0.10f * r, cy - 0.20f * r)
                    lineTo(cx + 0.65f * r, cy - 0.10f * r)
                    lineTo(cx + 0.50f * r, cy + 0.25f * r)
                    lineTo(cx + 0.15f * r, cy + 0.15f * r)
                    close()
                }
                drawPath(bullLeft, color = Color(0xFFE1251B))
                drawPath(bullRight, color = Color(0xFFE1251B))
            }

            F1TeamId.MCLAREN -> {
                // McLaren Papaya Speedmark Boomerang Swoosh
                val swooshPath = Path().apply {
                    moveTo(cx - 0.70f * r, cy + 0.40f * r)
                    cubicTo(cx - 0.40f * r, cy - 0.60f * r, cx + 0.60f * r, cy - 0.60f * r, cx + 0.75f * r, cy - 0.20f * r)
                    cubicTo(cx + 0.40f * r, cy - 0.35f * r, cx - 0.20f * r, cy - 0.20f * r, cx - 0.70f * r, cy + 0.40f * r)
                    close()
                }
                drawPath(swooshPath, color = Color(0xFFFF8000))
            }

            F1TeamId.RACING_BULLS -> {
                // RB Royal Blue circle with stylized white 'RB' monogram
                drawCircle(color = Color(0xFF1E3D9E), radius = r * 0.90f, center = Offset(cx, cy))
                drawCircle(color = Color(0xFF6692FF), radius = r * 0.90f, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))

                // Stylized white 'R' and 'B' strokes
                drawLine(Color.White, Offset(cx - 0.45f * r, cy - 0.45f * r), Offset(cx - 0.45f * r, cy + 0.45f * r), 2.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(cx - 0.45f * r, cy - 0.45f * r), Offset(cx - 0.10f * r, cy - 0.45f * r), 2.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(cx - 0.10f * r, cy - 0.45f * r), Offset(cx - 0.10f * r, cy), 2.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(cx - 0.45f * r, cy), Offset(cx - 0.10f * r, cy), 2.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(cx - 0.25f * r, cy), Offset(cx - 0.05f * r, cy + 0.45f * r), 2.dp.toPx(), StrokeCap.Round)

                drawLine(Color.White, Offset(cx + 0.10f * r, cy - 0.45f * r), Offset(cx + 0.10f * r, cy + 0.45f * r), 2.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(cx + 0.10f * r, cy - 0.45f * r), Offset(cx + 0.40f * r, cy - 0.25f * r), 2.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(cx + 0.40f * r, cy - 0.25f * r), Offset(cx + 0.10f * r, cy), 2.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(cx + 0.10f * r, cy), Offset(cx + 0.45f * r, cy + 0.22f * r), 2.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(cx + 0.45f * r, cy + 0.22f * r), Offset(cx + 0.10f * r, cy + 0.45f * r), 2.dp.toPx(), StrokeCap.Round)
            }

            F1TeamId.ALPINE -> {
                // Alpine Stylized 'A' with angled crossbar and pink accent
                drawCircle(color = Color(0xFF0F1E36), radius = r * 0.90f, center = Offset(cx, cy))
                val aPath = Path().apply {
                    moveTo(cx, cy - 0.65f * r)
                    lineTo(cx + 0.45f * r, cy + 0.55f * r)
                    lineTo(cx + 0.25f * r, cy + 0.55f * r)
                    lineTo(cx + 0.12f * r, cy + 0.18f * r)
                    lineTo(cx - 0.12f * r, cy + 0.18f * r)
                    lineTo(cx - 0.25f * r, cy + 0.55f * r)
                    lineTo(cx - 0.45f * r, cy + 0.55f * r)
                    close()
                }
                drawPath(aPath, color = Color.White)
                // Bright pink crossbar arrow extension
                drawLine(Color(0xFFFF87BC), Offset(cx - 0.25f * r, cy + 0.18f * r), Offset(cx + 0.50f * r, cy + 0.18f * r), 2.dp.toPx(), StrokeCap.Round)
            }

            F1TeamId.AUDI_SAUBER -> {
                // Audi Four Interlocking Silver Rings
                val ringRadius = r * 0.32f
                val ringStroke = 1.3.dp.toPx()
                val ringSpacing = r * 0.42f
                val startX = cx - 1.5f * ringSpacing
                val audiColor = if (isDark) Color(0xFFF0F0F2) else Color(0xFF4A4A56)

                for (idx in 0..3) {
                    val ringX = startX + idx * ringSpacing
                    drawCircle(
                        color = audiColor,
                        radius = ringRadius,
                        center = Offset(ringX, cy),
                        style = Stroke(width = ringStroke)
                    )
                }
            }

            F1TeamId.HAAS -> {
                // Haas White Circle with Red horizontal stripe and bold 'H'
                drawCircle(color = Color.White, radius = r * 0.88f, center = Offset(cx, cy))
                if (!isDark) {
                    drawCircle(color = Color(0xFFC8C8D2), radius = r * 0.88f, center = Offset(cx, cy), style = Stroke(width = 1.dp.toPx()))
                }
                drawRect(
                    color = Color(0xFFE1251B),
                    topLeft = Offset(cx - 0.88f * r, cy - 0.20f * r),
                    size = Size(1.76f * r, 0.40f * r)
                )
                // Bold dark grey 'H'
                haasPaint.textSize = r * 1.15f
                drawContext.canvas.nativeCanvas.drawText("H", cx, cy + r * 0.38f, haasPaint)
            }

            F1TeamId.WILLIAMS -> {
                // Williams Geometric 'W'
                drawCircle(color = Color(0xFF041E42), radius = r * 0.90f, center = Offset(cx, cy))
                val wPath = Path().apply {
                    moveTo(cx - 0.55f * r, cy - 0.45f * r)
                    lineTo(cx - 0.32f * r, cy + 0.45f * r)
                    lineTo(cx - 0.05f * r, cy - 0.15f * r)
                    lineTo(cx + 0.05f * r, cy - 0.15f * r)
                    lineTo(cx + 0.32f * r, cy + 0.45f * r)
                    lineTo(cx + 0.55f * r, cy - 0.45f * r)
                }
                drawPath(wPath, color = Color(0xFF64C4FF), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            F1TeamId.ASTON_MARTIN -> {
                // Aston Martin Outstretched Wings
                drawCircle(color = Color(0xFF00594F), radius = r * 0.90f, center = Offset(cx, cy))
                val wingsPath = Path().apply {
                    moveTo(cx - 0.80f * r, cy - 0.15f * r)
                    cubicTo(cx - 0.40f * r, cy - 0.40f * r, cx + 0.40f * r, cy - 0.40f * r, cx + 0.80f * r, cy - 0.15f * r)
                    lineTo(cx + 0.65f * r, cy + 0.15f * r)
                    lineTo(cx, cy + 0.30f * r)
                    lineTo(cx - 0.65f * r, cy + 0.15f * r)
                    close()
                }
                drawPath(wingsPath, color = Color.White.copy(alpha = 0.9f))
                drawCircle(color = Color(0xFF229971), radius = r * 0.28f, center = Offset(cx, cy))
            }

            F1TeamId.CADILLAC -> {
                // Cadillac Gold Geometric Crest
                val crestPath = Path().apply {
                    moveTo(cx - 0.65f * r, cy - 0.60f * r)
                    lineTo(cx + 0.65f * r, cy - 0.60f * r)
                    lineTo(cx + 0.65f * r, cy + 0.10f * r)
                    lineTo(cx, cy + 0.80f * r)
                    lineTo(cx - 0.65f * r, cy + 0.10f * r)
                    close()
                }
                drawPath(crestPath, color = Color(0xFFD4AF37))
                drawPath(crestPath, color = Color.White, style = Stroke(width = 1.dp.toPx()))

                // Geometric quartering lines
                drawLine(Color(0xFF1E1E24), Offset(cx, cy - 0.60f * r), Offset(cx, cy + 0.80f * r), 1.2.dp.toPx())
                drawLine(Color(0xFF1E1E24), Offset(cx - 0.65f * r, cy), Offset(cx + 0.65f * r, cy), 1.2.dp.toPx())
            }

            F1TeamId.OTHER -> {
                // Neutral metallic shield / F1-style crest for historical or unlisted constructors
                val shieldPath = Path().apply {
                    moveTo(cx - 0.68f * r, cy - 0.72f * r)
                    lineTo(cx + 0.68f * r, cy - 0.72f * r)
                    lineTo(cx + 0.68f * r, cy + 0.14f * r)
                    cubicTo(
                        cx + 0.60f * r, cy + 0.56f * r,
                        cx + 0.20f * r, cy + 0.80f * r,
                        cx, cy + 0.88f * r
                    )
                    cubicTo(
                        cx - 0.20f * r, cy + 0.80f * r,
                        cx - 0.60f * r, cy + 0.56f * r,
                        cx - 0.68f * r, cy + 0.14f * r
                    )
                    close()
                }
                val shieldFill = if (isDark) Color(0xFF2C2C36) else Color(0xFFE6E6EE)
                val shieldStroke = if (isDark) Color(0xFFA0A0AE) else Color(0xFF5A5A68)
                val chevronColor = if (isDark) Color(0xFFE4E4EE) else Color(0xFF24242E)
                drawPath(shieldPath, color = shieldFill)
                drawPath(shieldPath, color = shieldStroke, style = Stroke(width = 1.1.dp.toPx()))

                val chevronPath = Path().apply {
                    moveTo(cx - 0.34f * r, cy - 0.18f * r)
                    lineTo(cx + 0.14f * r, cy - 0.18f * r)
                    lineTo(cx + 0.36f * r, cy + 0.04f * r)
                    lineTo(cx + 0.14f * r, cy + 0.26f * r)
                    lineTo(cx - 0.34f * r, cy + 0.26f * r)
                    lineTo(cx - 0.12f * r, cy + 0.04f * r)
                    close()
                }
                drawPath(chevronPath, color = chevronColor)
            }
        }
    }
}

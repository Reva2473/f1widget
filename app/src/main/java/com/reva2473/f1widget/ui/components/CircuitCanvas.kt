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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.reva2473.f1widget.domain.model.F1TeamId
import com.reva2473.f1widget.domain.model.LiveDriverTiming
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors
import java.text.Normalizer
import java.util.Locale
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

private val COMBINING_MARKS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
private val SPA_WORD_REGEX = Regex("\\bSPA\\b")

private fun mergeSectorPoints(
    sector1Points: List<Offset>,
    sector2Points: List<Offset>,
    sector3Points: List<Offset>
): List<Offset> {
    val list = ArrayList<Offset>(sector1Points.size + sector2Points.size + sector3Points.size)
    list.addAll(sector1Points)
    list.addAll(sector2Points.drop(1))
    list.addAll(sector3Points.drop(1))
    return list
}

/**
 * Normalized geometry for Shanghai International Circuit (China).
 */
object ShanghaiCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.28f, 0.78f),
        Offset(0.56f, 0.80f),
        Offset(0.76f, 0.82f), // T1 snail entry
        Offset(0.86f, 0.72f), // T1-T2 snail spiral
        Offset(0.74f, 0.64f),
        Offset(0.68f, 0.72f), // T3 exit
        Offset(0.58f, 0.64f), // T4
        Offset(0.46f, 0.56f)  // T6 hairpin entry
    )
    val sector2Points = listOf(
        Offset(0.46f, 0.56f),
        Offset(0.32f, 0.52f), // T6 hairpin
        Offset(0.28f, 0.36f), // T7-T8
        Offset(0.40f, 0.24f), // T9-T10
        Offset(0.54f, 0.30f), // T11
        Offset(0.62f, 0.22f)  // T12-T13 banked entry
    )
    val sector3Points = listOf(
        Offset(0.62f, 0.22f),
        Offset(0.82f, 0.26f), // T13
        Offset(0.76f, 0.42f), // Long back straight
        Offset(0.42f, 0.66f),
        Offset(0.18f, 0.74f), // T14 hairpin
        Offset(0.16f, 0.82f), // T16
        Offset(0.28f, 0.78f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.85f, 0.72f)),
        Pair(3, Offset(0.69f, 0.73f)),
        Pair(6, Offset(0.31f, 0.52f)),
        Pair(9, Offset(0.40f, 0.23f)),
        Pair(11, Offset(0.55f, 0.31f)),
        Pair(13, Offset(0.83f, 0.26f)),
        Pair(14, Offset(0.17f, 0.74f)),
        Pair(16, Offset(0.16f, 0.83f))
    )
}

/**
 * Normalized geometry for Jeddah Corniche Circuit (Saudi Arabia).
 */
object JeddahCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.78f, 0.82f), // Start/Finish
        Offset(0.66f, 0.74f), // T1-T2 chicane
        Offset(0.58f, 0.64f), // T4-T6 Esses
        Offset(0.48f, 0.54f),
        Offset(0.40f, 0.42f), // T8-T10
        Offset(0.30f, 0.30f)  // T12
    )
    val sector2Points = listOf(
        Offset(0.30f, 0.30f),
        Offset(0.18f, 0.16f), // T13 banked hairpin
        Offset(0.24f, 0.14f),
        Offset(0.36f, 0.28f), // T16-T17
        Offset(0.46f, 0.40f), // T19-T20
        Offset(0.56f, 0.52f)  // T22 chicane
    )
    val sector3Points = listOf(
        Offset(0.56f, 0.52f),
        Offset(0.66f, 0.62f), // T24
        Offset(0.78f, 0.70f),
        Offset(0.88f, 0.82f), // T27 final hairpin
        Offset(0.84f, 0.88f),
        Offset(0.78f, 0.82f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.66f, 0.75f)),
        Pair(4, Offset(0.57f, 0.64f)),
        Pair(8, Offset(0.39f, 0.42f)),
        Pair(13, Offset(0.17f, 0.16f)),
        Pair(18, Offset(0.37f, 0.28f)),
        Pair(22, Offset(0.56f, 0.53f)),
        Pair(27, Offset(0.88f, 0.83f))
    )
}

/**
 * Normalized geometry for Miami International Autodrome (USA).
 */
object MiamiCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.26f, 0.74f), // Start/Finish
        Offset(0.16f, 0.66f), // T1
        Offset(0.24f, 0.50f), // T2-T3
        Offset(0.38f, 0.36f), // T4-T6 sweeping
        Offset(0.54f, 0.28f), // T7
        Offset(0.62f, 0.38f)  // T8 hairpin
    )
    val sector2Points = listOf(
        Offset(0.62f, 0.38f),
        Offset(0.74f, 0.32f), // T9-T10
        Offset(0.86f, 0.24f), // Back straight
        Offset(0.88f, 0.42f), // T11
        Offset(0.78f, 0.52f), // T12-T14 tight chicane
        Offset(0.84f, 0.64f), // T15
        Offset(0.74f, 0.74f)  // T16
    )
    val sector3Points = listOf(
        Offset(0.74f, 0.74f),
        Offset(0.52f, 0.76f), // Long straight
        Offset(0.42f, 0.82f), // T17 hairpin
        Offset(0.34f, 0.76f), // T18-T19
        Offset(0.26f, 0.74f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.15f, 0.66f)),
        Pair(4, Offset(0.38f, 0.35f)),
        Pair(7, Offset(0.54f, 0.27f)),
        Pair(11, Offset(0.88f, 0.42f)),
        Pair(14, Offset(0.77f, 0.52f)),
        Pair(17, Offset(0.42f, 0.83f))
    )
}

/**
 * Normalized geometry for Autodromo Enzo e Dino Ferrari (Imola, Italy).
 */
object ImolaCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.60f, 0.74f), // Start/Finish
        Offset(0.42f, 0.72f), // Tamburello entry
        Offset(0.32f, 0.64f), // Tamburello chicane T2-T4
        Offset(0.24f, 0.52f), // Villeneuve T5-T6
        Offset(0.16f, 0.40f)  // Tosa hairpin T7
    )
    val sector2Points = listOf(
        Offset(0.16f, 0.40f),
        Offset(0.26f, 0.24f), // Piratella T9
        Offset(0.44f, 0.22f), // Acque Minerali entry
        Offset(0.52f, 0.34f), // Acque Minerali T12-T13
        Offset(0.66f, 0.28f)  // Variante Alta entry
    )
    val sector3Points = listOf(
        Offset(0.66f, 0.28f),
        Offset(0.78f, 0.38f), // Variante Alta T14-T15
        Offset(0.86f, 0.54f), // Downhill to Rivazza
        Offset(0.82f, 0.74f), // Rivazza 1 T17
        Offset(0.72f, 0.78f), // Rivazza 2 T18
        Offset(0.60f, 0.74f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(2, Offset(0.32f, 0.65f)),
        Pair(5, Offset(0.23f, 0.52f)),
        Pair(7, Offset(0.15f, 0.40f)),
        Pair(9, Offset(0.26f, 0.23f)),
        Pair(12, Offset(0.52f, 0.35f)),
        Pair(14, Offset(0.78f, 0.38f)),
        Pair(17, Offset(0.83f, 0.74f))
    )
}

/**
 * Normalized geometry for Circuit de Barcelona-Catalunya (Spain).
 */
object CatalunyaCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.36f, 0.80f), // Start/Finish
        Offset(0.72f, 0.78f), // Main straight
        Offset(0.84f, 0.66f), // Elf T1-T2
        Offset(0.78f, 0.48f), // Renault T3 long right
        Offset(0.62f, 0.44f)  // Repsol T4
    )
    val sector2Points = listOf(
        Offset(0.62f, 0.44f),
        Offset(0.52f, 0.54f), // Seat T5 hairpin
        Offset(0.42f, 0.38f), // T7-T8
        Offset(0.32f, 0.22f), // Campsa T9
        Offset(0.18f, 0.34f)  // Back straight to La Caixa
    )
    val sector3Points = listOf(
        Offset(0.18f, 0.34f),
        Offset(0.16f, 0.52f), // La Caixa T10 hairpin
        Offset(0.28f, 0.58f), // Banc Sabadell T11
        Offset(0.22f, 0.70f), // T12-T13
        Offset(0.24f, 0.80f), // Final sweeping T14
        Offset(0.36f, 0.80f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.85f, 0.66f)),
        Pair(3, Offset(0.79f, 0.48f)),
        Pair(4, Offset(0.62f, 0.43f)),
        Pair(5, Offset(0.52f, 0.55f)),
        Pair(9, Offset(0.32f, 0.21f)),
        Pair(10, Offset(0.15f, 0.52f)),
        Pair(14, Offset(0.23f, 0.81f))
    )
}

/**
 * Normalized geometry for Circuit Gilles Villeneuve (Montreal, Canada).
 */
object MontrealCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.30f, 0.78f), // Start/Finish
        Offset(0.20f, 0.70f), // T1-T2 Senna S
        Offset(0.26f, 0.56f), // T3-T4 chicane
        Offset(0.42f, 0.46f), // T5
        Offset(0.52f, 0.36f)  // T6-T7 chicane
    )
    val sector2Points = listOf(
        Offset(0.52f, 0.36f),
        Offset(0.68f, 0.26f), // T8-T9 chicane
        Offset(0.84f, 0.18f), // Approach to Hairpin
        Offset(0.88f, 0.28f), // T10 L'Epingle hairpin
        Offset(0.74f, 0.40f), // Casino Straight
        Offset(0.58f, 0.56f)
    )
    val sector3Points = listOf(
        Offset(0.58f, 0.56f),
        Offset(0.44f, 0.70f), // Wall of Champions T13-T14
        Offset(0.38f, 0.78f),
        Offset(0.30f, 0.78f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.19f, 0.70f)),
        Pair(3, Offset(0.26f, 0.55f)),
        Pair(6, Offset(0.52f, 0.35f)),
        Pair(8, Offset(0.68f, 0.25f)),
        Pair(10, Offset(0.89f, 0.28f)),
        Pair(13, Offset(0.44f, 0.71f))
    )
}

/**
 * Normalized geometry for Red Bull Ring (Spielberg, Austria).
 */
object SpielbergCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.44f, 0.80f), // Start/Finish
        Offset(0.76f, 0.76f), // Castrol Edge T1
        Offset(0.78f, 0.52f), // Uphill straight
        Offset(0.82f, 0.22f)  // Remus T3 hairpin
    )
    val sector2Points = listOf(
        Offset(0.82f, 0.22f),
        Offset(0.66f, 0.28f), // Straight to T4
        Offset(0.46f, 0.22f), // Rauch T4
        Offset(0.28f, 0.34f), // T6
        Offset(0.16f, 0.50f)  // T7
    )
    val sector3Points = listOf(
        Offset(0.16f, 0.50f),
        Offset(0.24f, 0.66f), // T8-T9 Rindt
        Offset(0.26f, 0.80f), // T10 final corner
        Offset(0.44f, 0.80f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.77f, 0.76f)),
        Pair(3, Offset(0.83f, 0.21f)),
        Pair(4, Offset(0.46f, 0.21f)),
        Pair(6, Offset(0.28f, 0.33f)),
        Pair(7, Offset(0.15f, 0.50f)),
        Pair(9, Offset(0.24f, 0.67f)),
        Pair(10, Offset(0.26f, 0.81f))
    )
}

/**
 * Normalized geometry for Hungaroring (Budapest, Hungary).
 */
object HungaroringCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.22f, 0.68f), // Start/Finish
        Offset(0.18f, 0.84f), // T1 hairpin
        Offset(0.36f, 0.80f), // T2-T3
        Offset(0.42f, 0.64f), // T4 blind crest
        Offset(0.56f, 0.56f)  // T5 Mansell
    )
    val sector2Points = listOf(
        Offset(0.56f, 0.56f),
        Offset(0.62f, 0.42f), // T6-T7 chicane
        Offset(0.74f, 0.36f), // T8-T9
        Offset(0.84f, 0.26f), // T10-T11
        Offset(0.72f, 0.18f)
    )
    val sector3Points = listOf(
        Offset(0.72f, 0.18f),
        Offset(0.56f, 0.24f), // T12 hairpin
        Offset(0.42f, 0.34f), // T13
        Offset(0.28f, 0.40f), // T14 final hairpin
        Offset(0.22f, 0.68f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.17f, 0.85f)),
        Pair(4, Offset(0.42f, 0.63f)),
        Pair(6, Offset(0.62f, 0.41f)),
        Pair(11, Offset(0.85f, 0.26f)),
        Pair(12, Offset(0.56f, 0.23f)),
        Pair(14, Offset(0.27f, 0.40f))
    )
}

/**
 * Normalized geometry for Circuit Zandvoort (Netherlands).
 */
object ZandvoortCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.38f, 0.82f), // Start/Finish
        Offset(0.76f, 0.82f), // Tarzanbocht T1 hairpin
        Offset(0.84f, 0.66f), // Gerlach T2
        Offset(0.74f, 0.56f), // Hugenholtz T3 banked hairpin
        Offset(0.82f, 0.36f)  // Hunserug T4-T6
    )
    val sector2Points = listOf(
        Offset(0.82f, 0.36f),
        Offset(0.68f, 0.22f), // Scheivlak T7
        Offset(0.50f, 0.24f), // Mastersbocht T8
        Offset(0.42f, 0.38f), // T9 hairpin
        Offset(0.30f, 0.30f)  // T10 Hans Ernst
    )
    val sector3Points = listOf(
        Offset(0.30f, 0.30f),
        Offset(0.18f, 0.46f), // T11-T12 chicane
        Offset(0.22f, 0.66f), // Kumho T13
        Offset(0.24f, 0.82f), // Arie Luyendykbocht T14
        Offset(0.38f, 0.82f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.77f, 0.83f)),
        Pair(3, Offset(0.73f, 0.56f)),
        Pair(7, Offset(0.68f, 0.21f)),
        Pair(9, Offset(0.42f, 0.39f)),
        Pair(11, Offset(0.17f, 0.46f)),
        Pair(14, Offset(0.23f, 0.83f))
    )
}

/**
 * Normalized geometry for Baku City Circuit (Azerbaijan).
 */
object BakuCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.44f, 0.80f), // Main Straight
        Offset(0.16f, 0.80f), // T1 90-left
        Offset(0.16f, 0.64f), // T2 90-left
        Offset(0.44f, 0.64f), // T3
        Offset(0.44f, 0.48f), // T4
        Offset(0.62f, 0.48f)  // T5-T6 chicane
    )
    val sector2Points = listOf(
        Offset(0.62f, 0.48f),
        Offset(0.62f, 0.28f), // T7
        Offset(0.76f, 0.20f), // T8-T11 Castle section
        Offset(0.86f, 0.30f), // T12
        Offset(0.78f, 0.46f), // T13-T14 sweeping
        Offset(0.84f, 0.60f)  // T15
    )
    val sector3Points = listOf(
        Offset(0.84f, 0.60f),
        Offset(0.78f, 0.78f), // T16
        Offset(0.64f, 0.80f), // T17-T20 flat out
        Offset(0.44f, 0.80f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.15f, 0.81f)),
        Pair(2, Offset(0.15f, 0.64f)),
        Pair(3, Offset(0.45f, 0.64f)),
        Pair(7, Offset(0.61f, 0.28f)),
        Pair(8, Offset(0.76f, 0.19f)),
        Pair(15, Offset(0.85f, 0.60f)),
        Pair(16, Offset(0.78f, 0.79f))
    )
}

/**
 * Normalized geometry for Marina Bay Street Circuit (Singapore).
 */
object SingaporeCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.34f, 0.78f), // Start/Finish
        Offset(0.20f, 0.74f), // Sheares T1-T3
        Offset(0.22f, 0.56f), // T4
        Offset(0.32f, 0.46f), // Memorial T5
        Offset(0.50f, 0.34f)  // Raffles Blvd
    )
    val sector2Points = listOf(
        Offset(0.50f, 0.34f),
        Offset(0.72f, 0.24f), // T7
        Offset(0.82f, 0.36f), // T8-T9
        Offset(0.74f, 0.50f), // T10-T12
        Offset(0.84f, 0.64f)  // Anderson Bridge T13 hairpin
    )
    val sector3Points = listOf(
        Offset(0.84f, 0.64f),
        Offset(0.68f, 0.70f), // Esplanade Drive
        Offset(0.58f, 0.64f), // T14
        Offset(0.50f, 0.78f), // T16-T17
        Offset(0.42f, 0.82f), // T18-T19
        Offset(0.34f, 0.78f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.19f, 0.74f)),
        Pair(5, Offset(0.32f, 0.45f)),
        Pair(7, Offset(0.72f, 0.23f)),
        Pair(10, Offset(0.73f, 0.50f)),
        Pair(13, Offset(0.85f, 0.64f)),
        Pair(16, Offset(0.50f, 0.79f))
    )
}

/**
 * Normalized geometry for Circuit of the Americas (Austin, USA).
 */
object CotaCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.64f, 0.76f), // Start/Finish
        Offset(0.84f, 0.78f), // Steep uphill T1 hairpin
        Offset(0.74f, 0.64f), // T2-T3
        Offset(0.64f, 0.58f), // Esses T4-T6
        Offset(0.56f, 0.50f), // T7-T9
        Offset(0.46f, 0.42f)  // T10
    )
    val sector2Points = listOf(
        Offset(0.46f, 0.42f),
        Offset(0.28f, 0.24f), // T11 hairpin
        Offset(0.16f, 0.28f),
        Offset(0.26f, 0.48f), // Long back straight
        Offset(0.36f, 0.66f)  // T12 hairpin
    )
    val sector3Points = listOf(
        Offset(0.36f, 0.66f),
        Offset(0.44f, 0.58f), // Stadium T13-T15
        Offset(0.52f, 0.68f), // Carousel T16-T18
        Offset(0.46f, 0.80f), // T19
        Offset(0.54f, 0.78f), // T20
        Offset(0.64f, 0.76f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.85f, 0.78f)),
        Pair(6, Offset(0.63f, 0.58f)),
        Pair(11, Offset(0.28f, 0.23f)),
        Pair(12, Offset(0.36f, 0.67f)),
        Pair(15, Offset(0.44f, 0.57f)),
        Pair(20, Offset(0.54f, 0.79f))
    )
}

/**
 * Normalized geometry for Autódromo Hermanos Rodríguez (Mexico City).
 */
object MexicoCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.48f, 0.82f), // Start/Finish
        Offset(0.84f, 0.80f), // Main straight
        Offset(0.86f, 0.66f), // T1-T3 chicane
        Offset(0.72f, 0.66f),
        Offset(0.64f, 0.52f)  // T4-T6
    )
    val sector2Points = listOf(
        Offset(0.64f, 0.52f),
        Offset(0.52f, 0.36f), // Esses T7-T9
        Offset(0.36f, 0.24f), // T10-T11
        Offset(0.22f, 0.32f), // Back straight
        Offset(0.18f, 0.52f)  // Foro Sol entry T12
    )
    val sector3Points = listOf(
        Offset(0.18f, 0.52f),
        Offset(0.28f, 0.58f), // Foro Sol stadium T13-T15
        Offset(0.22f, 0.72f), // Peraltada exit T16
        Offset(0.28f, 0.82f), // T17
        Offset(0.48f, 0.82f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.87f, 0.66f)),
        Pair(4, Offset(0.64f, 0.51f)),
        Pair(7, Offset(0.52f, 0.35f)),
        Pair(11, Offset(0.36f, 0.23f)),
        Pair(13, Offset(0.29f, 0.58f)),
        Pair(17, Offset(0.28f, 0.83f))
    )
}

/**
 * Normalized geometry for Las Vegas Strip Circuit (USA).
 */
object LasVegasCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.66f, 0.78f), // Start/Finish
        Offset(0.84f, 0.76f), // T1 hairpin
        Offset(0.80f, 0.58f), // T2-T4 around Sphere
        Offset(0.86f, 0.42f),
        Offset(0.72f, 0.28f)  // T5 Koval straight entry
    )
    val sector2Points = listOf(
        Offset(0.72f, 0.28f),
        Offset(0.48f, 0.26f), // Koval Straight
        Offset(0.36f, 0.34f), // T6-T9 chicane
        Offset(0.24f, 0.22f), // T10-T11
        Offset(0.16f, 0.36f)  // T12 onto Las Vegas Blvd
    )
    val sector3Points = listOf(
        Offset(0.16f, 0.36f),
        Offset(0.18f, 0.64f), // Las Vegas Strip straight
        Offset(0.24f, 0.80f), // T14-T16 chicane
        Offset(0.44f, 0.78f), // T17
        Offset(0.66f, 0.78f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.85f, 0.76f)),
        Pair(4, Offset(0.87f, 0.42f)),
        Pair(7, Offset(0.36f, 0.35f)),
        Pair(12, Offset(0.15f, 0.36f)),
        Pair(14, Offset(0.24f, 0.81f)),
        Pair(17, Offset(0.44f, 0.79f))
    )
}

/**
 * Normalized geometry for Lusail International Circuit (Qatar).
 */
object LosailCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.38f, 0.82f), // Main Straight
        Offset(0.76f, 0.80f), // T1
        Offset(0.84f, 0.64f), // T2-T3
        Offset(0.76f, 0.48f), // T4-T5
        Offset(0.62f, 0.50f)  // T6 hairpin
    )
    val sector2Points = listOf(
        Offset(0.62f, 0.50f),
        Offset(0.56f, 0.32f), // T7-T9
        Offset(0.44f, 0.20f), // T10 hairpin
        Offset(0.28f, 0.26f), // T11
        Offset(0.20f, 0.42f)  // Triple-apex T12
    )
    val sector3Points = listOf(
        Offset(0.20f, 0.42f),
        Offset(0.24f, 0.58f), // T13-T14
        Offset(0.16f, 0.72f), // T15
        Offset(0.22f, 0.82f), // T16
        Offset(0.38f, 0.82f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.77f, 0.81f)),
        Pair(4, Offset(0.77f, 0.48f)),
        Pair(6, Offset(0.62f, 0.51f)),
        Pair(10, Offset(0.44f, 0.19f)),
        Pair(12, Offset(0.19f, 0.42f)),
        Pair(16, Offset(0.21f, 0.83f))
    )
}

/**
 * Normalized geometry for Madring Circuit (IFEMA Madrid, Spain).
 */
object MadridCircuitGeometry {
    val sector1Points = listOf(
        Offset(0.36f, 0.80f), // Start/Finish
        Offset(0.68f, 0.80f), // Main straight
        Offset(0.84f, 0.72f), // T1-T2 chicane
        Offset(0.82f, 0.52f), // Fast sweepers T3-T4
        Offset(0.72f, 0.34f)  // T5-T6
    )
    val sector2Points = listOf(
        Offset(0.72f, 0.34f),
        Offset(0.56f, 0.20f), // Banked Valdebebas curve
        Offset(0.36f, 0.22f), // T9-T10
        Offset(0.24f, 0.36f), // Technical infield T11-T13
        Offset(0.34f, 0.48f)  // T14
    )
    val sector3Points = listOf(
        Offset(0.34f, 0.48f),
        Offset(0.20f, 0.58f), // T15-T17
        Offset(0.18f, 0.76f), // T18-T20 final corner
        Offset(0.36f, 0.80f)
    )
    val allPoints: List<Offset> by lazy { mergeSectorPoints(sector1Points, sector2Points, sector3Points) }
    val corners = listOf(
        Pair(1, Offset(0.85f, 0.72f)),
        Pair(5, Offset(0.73f, 0.34f)),
        Pair(8, Offset(0.56f, 0.19f)),
        Pair(11, Offset(0.23f, 0.36f)),
        Pair(15, Offset(0.19f, 0.58f)),
        Pair(20, Offset(0.17f, 0.76f))
    )
}

/**
 * Resolves circuit geometry by circuit type or circuitId/name string.
 */
fun getCircuitGeometry(circuitType: String): CircuitGeometryData {
    val key = Normalizer.normalize(circuitType.trim(), Normalizer.Form.NFD)
        .replace(COMBINING_MARKS_REGEX, "")
        .uppercase(Locale.ROOT)
    return when {
        key in setOf("INTERLAGOS", "BRAZIL", "BRAZILIAN", "SAO_PAULO", "SAO PAULO") ||
            key.contains("INTERLAGOS") || key.contains("SAO_PAULO") || key.contains("SAO PAULO") -> CircuitGeometryData(
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
        key in setOf("MELBOURNE", "ALBERT_PARK", "ALBERT PARK", "AUSTRALIA", "AUSTRALIAN") ||
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
        key in setOf("SHANGHAI", "CHINA", "CHINESE") ||
            key.contains("SHANGHAI") || key.contains("CHIN") -> CircuitGeometryData(
            sector1Points = ShanghaiCircuitGeometry.sector1Points,
            sector2Points = ShanghaiCircuitGeometry.sector2Points,
            sector3Points = ShanghaiCircuitGeometry.sector3Points,
            allPoints = ShanghaiCircuitGeometry.allPoints,
            corners = ShanghaiCircuitGeometry.corners,
            startFinishPos = ShanghaiCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.54f, 0.56f),
            speedTrapBadgePos = Offset(0.52f, 0.46f),
            s1CutoffProgress = 0.35f,
            s2CutoffProgress = 0.67f
        )
        key in setOf("JEDDAH", "SAUDI", "SAUDI_ARABIA", "SAUDI ARABIA") ||
            key.contains("JEDDAH") || key.contains("SAUDI") || key.contains("CORNICHE") -> CircuitGeometryData(
            sector1Points = JeddahCircuitGeometry.sector1Points,
            sector2Points = JeddahCircuitGeometry.sector2Points,
            sector3Points = JeddahCircuitGeometry.sector3Points,
            allPoints = JeddahCircuitGeometry.allPoints,
            corners = JeddahCircuitGeometry.corners,
            startFinishPos = JeddahCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.72f, 0.66f),
            speedTrapBadgePos = Offset(0.68f, 0.54f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key == "MIAMI" || key.contains("MIAMI") -> CircuitGeometryData(
            sector1Points = MiamiCircuitGeometry.sector1Points,
            sector2Points = MiamiCircuitGeometry.sector2Points,
            sector3Points = MiamiCircuitGeometry.sector3Points,
            allPoints = MiamiCircuitGeometry.allPoints,
            corners = MiamiCircuitGeometry.corners,
            startFinishPos = MiamiCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.60f, 0.76f),
            speedTrapBadgePos = Offset(0.58f, 0.65f),
            s1CutoffProgress = 0.33f,
            s2CutoffProgress = 0.69f
        )
        key in setOf("IMOLA", "EMILIA", "EMILIA_ROMAGNA", "EMILIA ROMAGNA") ||
            key.contains("IMOLA") || key.contains("EMILIA") || key.contains("DINO FERRARI") -> CircuitGeometryData(
            sector1Points = ImolaCircuitGeometry.sector1Points,
            sector2Points = ImolaCircuitGeometry.sector2Points,
            sector3Points = ImolaCircuitGeometry.sector3Points,
            allPoints = ImolaCircuitGeometry.allPoints,
            corners = ImolaCircuitGeometry.corners,
            startFinishPos = ImolaCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.46f, 0.72f),
            speedTrapBadgePos = Offset(0.46f, 0.61f),
            s1CutoffProgress = 0.33f,
            s2CutoffProgress = 0.67f
        )
        key in setOf("CATALUNYA", "BARCELONA", "SPAIN", "SPANISH", "MONTMELO") ||
            key.contains("CATALUNYA") || key.contains("BARCELONA") || key.contains("SPANISH") || key.contains("SPAIN") -> CircuitGeometryData(
            sector1Points = CatalunyaCircuitGeometry.sector1Points,
            sector2Points = CatalunyaCircuitGeometry.sector2Points,
            sector3Points = CatalunyaCircuitGeometry.sector3Points,
            allPoints = CatalunyaCircuitGeometry.allPoints,
            corners = CatalunyaCircuitGeometry.corners,
            startFinishPos = CatalunyaCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.58f, 0.79f),
            speedTrapBadgePos = Offset(0.56f, 0.68f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("MONTREAL", "VILLENEUVE", "CANADA", "CANADIAN") ||
            key.contains("MONTREAL") || key.contains("VILLENEUVE") || key.contains("CANAD") -> CircuitGeometryData(
            sector1Points = MontrealCircuitGeometry.sector1Points,
            sector2Points = MontrealCircuitGeometry.sector2Points,
            sector3Points = MontrealCircuitGeometry.sector3Points,
            allPoints = MontrealCircuitGeometry.allPoints,
            corners = MontrealCircuitGeometry.corners,
            startFinishPos = MontrealCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.64f, 0.50f),
            speedTrapBadgePos = Offset(0.60f, 0.40f),
            s1CutoffProgress = 0.33f,
            s2CutoffProgress = 0.69f
        )
        key in setOf("SPIELBERG", "RED_BULL_RING", "RED BULL RING", "AUSTRIA", "AUSTRIAN") ||
            key.contains("SPIELBERG") || key.contains("RED BULL RING") || key.contains("AUSTRIA") -> CircuitGeometryData(
            sector1Points = SpielbergCircuitGeometry.sector1Points,
            sector2Points = SpielbergCircuitGeometry.sector2Points,
            sector3Points = SpielbergCircuitGeometry.sector3Points,
            allPoints = SpielbergCircuitGeometry.allPoints,
            corners = SpielbergCircuitGeometry.corners,
            startFinishPos = SpielbergCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.80f, 0.38f),
            speedTrapBadgePos = Offset(0.66f, 0.42f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("SILVERSTONE", "GREAT_BRITAIN", "BRITAIN", "BRITISH", "UK") ||
            key.contains("SILVERSTONE") || key.contains("BRIT") -> CircuitGeometryData(
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
            key.contains("MONZA") || key.contains("ITAL") -> CircuitGeometryData(
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
            key.contains("SUZUKA") || key.contains("JAPAN") -> CircuitGeometryData(
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
            key.contains("FRANCORCHAMPS") || key.contains("BELGI") || SPA_WORD_REGEX.containsMatchIn(key) -> CircuitGeometryData(
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
        key in setOf("HUNGARORING", "HUNGARY", "HUNGARIAN", "BUDAPEST") ||
            key.contains("HUNGAR") || key.contains("BUDAPEST") -> CircuitGeometryData(
            sector1Points = HungaroringCircuitGeometry.sector1Points,
            sector2Points = HungaroringCircuitGeometry.sector2Points,
            sector3Points = HungaroringCircuitGeometry.sector3Points,
            allPoints = HungaroringCircuitGeometry.allPoints,
            corners = HungaroringCircuitGeometry.corners,
            startFinishPos = HungaroringCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.20f, 0.76f),
            speedTrapBadgePos = Offset(0.32f, 0.72f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("ZANDVOORT", "NETHERLANDS", "DUTCH") ||
            key.contains("ZANDVOORT") || key.contains("NETHERLAND") || key.contains("DUTCH") -> CircuitGeometryData(
            sector1Points = ZandvoortCircuitGeometry.sector1Points,
            sector2Points = ZandvoortCircuitGeometry.sector2Points,
            sector3Points = ZandvoortCircuitGeometry.sector3Points,
            allPoints = ZandvoortCircuitGeometry.allPoints,
            corners = ZandvoortCircuitGeometry.corners,
            startFinishPos = ZandvoortCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.60f, 0.82f),
            speedTrapBadgePos = Offset(0.58f, 0.71f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.67f
        )
        key in setOf("MADRID", "MADRING", "IFEMA") ||
            key.contains("MADRID") || key.contains("MADRING") -> CircuitGeometryData(
            sector1Points = MadridCircuitGeometry.sector1Points,
            sector2Points = MadridCircuitGeometry.sector2Points,
            sector3Points = MadridCircuitGeometry.sector3Points,
            allPoints = MadridCircuitGeometry.allPoints,
            corners = MadridCircuitGeometry.corners,
            startFinishPos = MadridCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.58f, 0.80f),
            speedTrapBadgePos = Offset(0.56f, 0.69f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("BAKU", "AZERBAIJAN") ||
            key.contains("BAKU") || key.contains("AZERBAIJAN") -> CircuitGeometryData(
            sector1Points = BakuCircuitGeometry.sector1Points,
            sector2Points = BakuCircuitGeometry.sector2Points,
            sector3Points = BakuCircuitGeometry.sector3Points,
            allPoints = BakuCircuitGeometry.allPoints,
            corners = BakuCircuitGeometry.corners,
            startFinishPos = BakuCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.54f, 0.80f),
            speedTrapBadgePos = Offset(0.54f, 0.69f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("SINGAPORE", "MARINA_BAY", "MARINA BAY") ||
            key.contains("SINGAPORE") || key.contains("MARINA BAY") -> CircuitGeometryData(
            sector1Points = SingaporeCircuitGeometry.sector1Points,
            sector2Points = SingaporeCircuitGeometry.sector2Points,
            sector3Points = SingaporeCircuitGeometry.sector3Points,
            allPoints = SingaporeCircuitGeometry.allPoints,
            corners = SingaporeCircuitGeometry.corners,
            startFinishPos = SingaporeCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.58f, 0.30f),
            speedTrapBadgePos = Offset(0.56f, 0.41f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("COTA", "AMERICAS", "AUSTIN", "UNITED_STATES", "UNITED STATES") ||
            key.contains("COTA") || key.contains("AMERICAS") || key.contains("AUSTIN") || key.contains("UNITED STATES") -> CircuitGeometryData(
            sector1Points = CotaCircuitGeometry.sector1Points,
            sector2Points = CotaCircuitGeometry.sector2Points,
            sector3Points = CotaCircuitGeometry.sector3Points,
            allPoints = CotaCircuitGeometry.allPoints,
            corners = CotaCircuitGeometry.corners,
            startFinishPos = CotaCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.28f, 0.52f),
            speedTrapBadgePos = Offset(0.38f, 0.44f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("MEXICO", "RODRIGUEZ", "HERMANOS") ||
            key.contains("MEXIC") || key.contains("RODRIGUEZ") || key.contains("HERMANOS") -> CircuitGeometryData(
            sector1Points = MexicoCircuitGeometry.sector1Points,
            sector2Points = MexicoCircuitGeometry.sector2Points,
            sector3Points = MexicoCircuitGeometry.sector3Points,
            allPoints = MexicoCircuitGeometry.allPoints,
            corners = MexicoCircuitGeometry.corners,
            startFinishPos = MexicoCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.70f, 0.81f),
            speedTrapBadgePos = Offset(0.68f, 0.70f),
            s1CutoffProgress = 0.34f,
            s2CutoffProgress = 0.68f
        )
        key in setOf("LAS_VEGAS", "LAS VEGAS", "VEGAS") ||
            key.contains("VEGAS") -> CircuitGeometryData(
            sector1Points = LasVegasCircuitGeometry.sector1Points,
            sector2Points = LasVegasCircuitGeometry.sector2Points,
            sector3Points = LasVegasCircuitGeometry.sector3Points,
            allPoints = LasVegasCircuitGeometry.allPoints,
            corners = LasVegasCircuitGeometry.corners,
            startFinishPos = LasVegasCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.18f, 0.54f),
            speedTrapBadgePos = Offset(0.32f, 0.54f),
            s1CutoffProgress = 0.33f,
            s2CutoffProgress = 0.67f
        )
        key in setOf("LOSAIL", "LUSAIL", "QATAR") ||
            key.contains("LOSAIL") || key.contains("LUSAIL") || key.contains("QATAR") -> CircuitGeometryData(
            sector1Points = LosailCircuitGeometry.sector1Points,
            sector2Points = LosailCircuitGeometry.sector2Points,
            sector3Points = LosailCircuitGeometry.sector3Points,
            allPoints = LosailCircuitGeometry.allPoints,
            corners = LosailCircuitGeometry.corners,
            startFinishPos = LosailCircuitGeometry.sector1Points[0],
            speedTrapPos = Offset(0.62f, 0.81f),
            speedTrapBadgePos = Offset(0.60f, 0.70f),
            s1CutoffProgress = 0.34f,
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

        // Draw Drivers (leaders & selected driver drawn last/on top)
        labelPaint.textSize = 9.dp.toPx()
        val orderedDrivers = currentDrivers.sortedWith(
            compareBy<LiveDriverTiming> { it.code.equals(selectedDriverCode, ignoreCase = true) }
                .thenByDescending { it.position }
        )
        val nextTargets = ArrayList<DriverTapTarget>(orderedDrivers.size)
        val standardPillW = 28.dp.toPx()
        val pitPillW = 44.dp.toPx()
        val pillH = 14.dp.toPx()
        val pad = 2.dp.toPx()
        val maxPillTop = (h - pillH - pad).coerceAtLeast(pad)
        val pitDashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f)

        orderedDrivers.forEach { driver ->
            val progress = driver.trackProgress.coerceIn(0f, 1f)
            val dotPos = pathMeasure.getPosition(pathLength * progress)

            val isSelected = driver.code.equals(selectedDriverCode, ignoreCase = true)
            val isInPit = driver.isInPit
            val teamColor = Color(driver.team.colorHex)
            val dotAlpha = if (isInPit && !isSelected) 0.55f else 1f
            val pillW = if (isInPit) pitPillW else standardPillW
            val maxPillLeft = (w - pillW - pad).coerceAtLeast(pad)

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
            } else if (isInPit) {
                // Dashed pit-lane ring around cars currently in pits
                drawCircle(
                    color = P1TColors.InPitBlue.copy(alpha = 0.85f),
                    radius = 8.5.dp.toPx(),
                    center = dotPos,
                    style = Stroke(width = 1.4.dp.toPx(), pathEffect = pitDashEffect)
                )
            }

            // Driver Dot
            drawCircle(
                color = Color(0xFF141418).copy(alpha = dotAlpha),
                radius = 6.5.dp.toPx(),
                center = dotPos
            )
            drawCircle(
                color = (if (isSelected) Color(0xFFE1251B) else teamColor).copy(alpha = dotAlpha),
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
            } else if (isInPit) {
                drawRoundRect(
                    color = Color(0xCC141418),
                    topLeft = pillTopLeft,
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = P1TColors.InPitBlue.copy(alpha = 0.9f),
                    topLeft = pillTopLeft,
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = pitDashEffect)
                )
                labelPaint.color = android.graphics.Color.argb(200, 175, 215, 255)
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

            val pillText = if (isInPit) "${driver.code} PIT" else driver.code
            drawContext.canvas.nativeCanvas.drawText(
                pillText,
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
 * Renders real high-resolution F1 car cutout PNGs, or a team-colored vector fallback for unlisted teams.
 */
@Composable
fun F1CarSideProfileCanvas(
    team: F1TeamId,
    modifier: Modifier = Modifier
) {
    val carRes = getTeamCarResOrNull(team)
    if (carRes != null) {
        Image(
            painter = painterResource(id = carRes),
            contentDescription = "${team.displayName} F1 Car",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        GenericF1CarSideVector(
            accentColor = Color(team.colorHex),
            modifier = modifier
        )
    }
}

@Composable
fun F1CarSideProfileCanvas(
    teamColor: Color,
    modifier: Modifier = Modifier
) {
    val carRes = getTeamCarResByColorOrNull(teamColor)
    if (carRes != null) {
        Image(
            painter = painterResource(id = carRes),
            contentDescription = "F1 Car",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        GenericF1CarSideVector(
            accentColor = teamColor,
            modifier = modifier
        )
    }
}

/**
 * 5. F1CarTopDownCanvas
 * Renders a crisp top-down vector F1 car silhouette in the team's color (vertical or horizontal).
 */
@Composable
fun F1CarTopDownCanvas(
    team: F1TeamId,
    modifier: Modifier = Modifier,
    vertical: Boolean = false
) {
    F1CarTopDownCanvas(
        teamColor = Color(team.colorHex),
        modifier = modifier,
        vertical = vertical
    )
}

@Composable
fun F1CarTopDownCanvas(
    teamColor: Color,
    modifier: Modifier = Modifier,
    vertical: Boolean = false
) {
    val carbon = Color(0xFF1A1A22)
    val tyreRubber = Color(0xFF111116)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Canonical coordinate space: vertical top-down F1 car (nose at top y=0.04, rear wing at bottom y=0.96),
        // fitting cleanly into (cw x ch), rotated 90deg clockwise when vertical == false (nose pointing right).
        withTransform({
            if (!vertical) {
                rotate(degrees = 90f, pivot = Offset(w / 2f, h / 2f))
            }
        }) {
            val carLen = if (vertical) h else w
            val carWidth = (if (vertical) w else h).coerceAtMost(carLen * 0.52f)
            val left = (w - carWidth) / 2f
            val top = (h - carLen) / 2f

            fun pt(nx: Float, ny: Float) = Offset(left + nx * carWidth, top + ny * carLen)

            // Front & Rear suspension wishbones
            listOf(0.24f, 0.28f, 0.77f, 0.81f).forEach { yPos ->
                drawLine(
                    color = carbon,
                    start = pt(0.18f, yPos),
                    end = pt(0.82f, yPos),
                    strokeWidth = (carWidth * 0.045f).coerceAtLeast(1.2f)
                )
            }

            // 4 Pirelli Tyres (front narrower, rear wider)
            val tyreRadius = CornerRadius(carWidth * 0.06f, carWidth * 0.06f)
            // Front Left & Right
            drawRoundRect(tyreRubber, pt(0.05f, 0.17f), Size(0.19f * carWidth, 0.16f * carLen), tyreRadius)
            drawRoundRect(tyreRubber, pt(0.76f, 0.17f), Size(0.19f * carWidth, 0.16f * carLen), tyreRadius)
            // Rear Left & Right
            drawRoundRect(tyreRubber, pt(0.03f, 0.70f), Size(0.22f * carWidth, 0.18f * carLen), tyreRadius)
            drawRoundRect(tyreRubber, pt(0.75f, 0.70f), Size(0.22f * carWidth, 0.18f * carLen), tyreRadius)

            // Carbon Venturi floor edges
            val floorPath = Path().apply {
                moveTo(pt(0.26f, 0.36f).x, pt(0.26f, 0.36f).y)
                lineTo(pt(0.74f, 0.36f).x, pt(0.74f, 0.36f).y)
                lineTo(pt(0.76f, 0.78f).x, pt(0.76f, 0.78f).y)
                lineTo(pt(0.24f, 0.78f).x, pt(0.24f, 0.78f).y)
                close()
            }
            drawPath(floorPath, color = carbon)

            // Front Wing (wide swept aero plane + endplates)
            val frontWingPath = Path().apply {
                moveTo(pt(0.06f, 0.11f).x, pt(0.06f, 0.11f).y)
                lineTo(pt(0.50f, 0.05f).x, pt(0.50f, 0.05f).y)
                lineTo(pt(0.94f, 0.11f).x, pt(0.94f, 0.11f).y)
                lineTo(pt(0.94f, 0.17f).x, pt(0.94f, 0.17f).y)
                lineTo(pt(0.50f, 0.13f).x, pt(0.50f, 0.13f).y)
                lineTo(pt(0.06f, 0.17f).x, pt(0.06f, 0.17f).y)
                close()
            }
            drawPath(frontWingPath, color = teamColor)
            drawPath(frontWingPath, color = Color.White.copy(alpha = 0.25f), style = Stroke(width = 1f))

            // Rear Wing (DRS flap + endplates)
            drawRoundRect(
                color = teamColor,
                topLeft = pt(0.16f, 0.89f),
                size = Size(0.68f * carWidth, 0.08f * carLen),
                cornerRadius = CornerRadius(carWidth * 0.03f, carWidth * 0.03f)
            )
            drawLine(carbon, pt(0.16f, 0.88f), pt(0.16f, 0.98f), strokeWidth = 2.5f)
            drawLine(carbon, pt(0.84f, 0.88f), pt(0.84f, 0.98f), strokeWidth = 2.5f)

            // Main sculpted monocoque, sidepods & tapered engine cover
            val chassisPath = Path().apply {
                moveTo(pt(0.45f, 0.05f).x, pt(0.45f, 0.05f).y) // Nose tip left
                lineTo(pt(0.55f, 0.05f).x, pt(0.55f, 0.05f).y) // Nose tip right
                lineTo(pt(0.60f, 0.32f).x, pt(0.60f, 0.32f).y)
                cubicTo(
                    pt(0.75f, 0.38f).x, pt(0.75f, 0.38f).y,
                    pt(0.75f, 0.56f).x, pt(0.75f, 0.56f).y,
                    pt(0.62f, 0.72f).x, pt(0.62f, 0.72f).y
                )
                lineTo(pt(0.55f, 0.89f).x, pt(0.55f, 0.89f).y) // Coke-bottle rear
                lineTo(pt(0.45f, 0.89f).x, pt(0.45f, 0.89f).y)
                lineTo(pt(0.38f, 0.72f).x, pt(0.38f, 0.72f).y)
                cubicTo(
                    pt(0.25f, 0.56f).x, pt(0.25f, 0.56f).y,
                    pt(0.25f, 0.38f).x, pt(0.25f, 0.38f).y,
                    pt(0.40f, 0.32f).x, pt(0.40f, 0.32f).y
                )
                close()
            }
            drawPath(chassisPath, color = teamColor)
            drawPath(chassisPath, color = Color.White.copy(alpha = 0.25f), style = Stroke(width = 1f))

            // Dark cockpit opening + Halo Y-bar
            drawOval(
                color = Color(0xFF0E0E12),
                topLeft = pt(0.42f, 0.39f),
                size = Size(0.16f * carWidth, 0.16f * carLen)
            )
            drawLine(
                color = Color(0xFFE5E5EC),
                start = pt(0.50f, 0.37f),
                end = pt(0.50f, 0.45f),
                strokeWidth = (carWidth * 0.035f).coerceAtLeast(1.2f),
                cap = StrokeCap.Round
            )
            // Driver helmet dot
            drawCircle(
                color = Color.White,
                radius = (carWidth * 0.045f).coerceAtLeast(2f),
                center = pt(0.50f, 0.48f)
            )
        }
    }
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
                val crestOutline = if (isDark) Color.White else Color(0xFF4A4A56)
                val crestPath = Path().apply {
                    moveTo(cx - 0.65f * r, cy - 0.60f * r)
                    lineTo(cx + 0.65f * r, cy - 0.60f * r)
                    lineTo(cx + 0.65f * r, cy + 0.10f * r)
                    lineTo(cx, cy + 0.80f * r)
                    lineTo(cx - 0.65f * r, cy + 0.10f * r)
                    close()
                }
                drawPath(crestPath, color = Color(0xFFD4AF37))
                drawPath(crestPath, color = crestOutline, style = Stroke(width = 1.dp.toPx()))

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

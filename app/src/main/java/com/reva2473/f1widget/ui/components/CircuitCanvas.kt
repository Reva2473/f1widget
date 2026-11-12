package com.reva2473.f1widget.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.reva2473.f1widget.domain.model.LiveDriverTiming
import com.reva2473.f1widget.ui.theme.P1TColors
import kotlin.math.sqrt

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
 * Builds a smooth Catmull-Rom or cubic Bezier path from normalized waypoints.
 */
fun buildCircuitPath(points: List<Offset>, width: Float, height: Float, close: Boolean = false): Path {
    val path = Path()
    if (points.isEmpty()) return path

    val scaledPoints = points.map { Offset(it.x * width, it.y * height) }
    path.moveTo(scaledPoints.first().x, scaledPoints.first().y)

    for (i in 0 until scaledPoints.size - 1) {
        val p0 = if (i > 0) scaledPoints[i - 1] else scaledPoints[i]
        val p1 = scaledPoints[i]
        val p2 = scaledPoints[i + 1]
        val p3 = if (i + 2 < scaledPoints.size) scaledPoints[i + 2] else p2

        // Cubic control points
        val cp1X = p1.x + (p2.x - p0.x) / 6f
        val cp1Y = p1.y + (p2.y - p0.y) / 6f
        val cp2X = p2.x - (p3.x - p1.x) / 6f
        val cp2Y = p2.y - (p3.y - p1.y) / 6f

        path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p2.x, p2.y)
    }

    if (close) {
        path.close()
    }
    return path
}

/**
 * 1. SepangOutlineCanvas
 * Draws the double-stroke glowing white circuit outline of Sepang International Circuit as seen in home-1080.webp.
 */
@Composable
fun SepangOutlineCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val circuitPath = buildCircuitPath(SepangCircuitGeometry.allPoints, w, h, close = true)

        // Outer glow layer
        drawPath(
            path = circuitPath,
            color = Color.White.copy(alpha = 0.18f),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Mid glow layer
        drawPath(
            path = circuitPath,
            color = Color.White.copy(alpha = 0.40f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Crisp inner stroke
        drawPath(
            path = circuitPath,
            color = Color.White,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Start / Finish Line marker on Main Straight
        val sfX = 0.38f * w
        val sfY = 0.82f * h
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
 * 2. MultiSectorCircuitMap
 * Draws the 3-sector colored circuit map (Sector 1 Red #E1251B, Sector 2 Cyan #00B4D8, Sector 3 Yellow #FFD100)
 * with numbered corner markers (01..15), start/finish checkered square, SECTOR 1 / 2 / 3 labels,
 * and the magenta SPEED TRAP badge with callout line as seen in race-weekend-1080.webp & race-stints-720.webp.
 */
@Composable
fun MultiSectorCircuitMap(
    circuitType: String = "SEPANG",
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val s1Path = buildCircuitPath(SepangCircuitGeometry.sector1Points, w, h)
        val s2Path = buildCircuitPath(SepangCircuitGeometry.sector2Points, w, h)
        val s3Path = buildCircuitPath(SepangCircuitGeometry.sector3Points, w, h)

        val strokeWidth = 5.5.dp.toPx()

        // Background shadow track ribbon
        val fullPath = buildCircuitPath(SepangCircuitGeometry.allPoints, w, h, close = true)
        drawPath(
            path = fullPath,
            color = Color(0xFF1E1E26),
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

        // Corner Markers (01..15)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 9.dp.toPx()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }

        SepangCircuitGeometry.corners.forEach { (num, pos) ->
            val cx = pos.x * w
            val cy = pos.y * h

            // Background circle
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

            // Corner number
            paint.color = android.graphics.Color.WHITE
            val text = if (num < 10) "0$num" else "$num"
            drawContext.canvas.nativeCanvas.drawText(
                text,
                cx,
                cy + 3.dp.toPx(),
                paint
            )
        }

        // Start / Finish Line (checkered pattern)
        val sfX = 0.38f * w
        val sfY = 0.82f * h
        val checkH = 5.dp.toPx()
        val checkW = 3.dp.toPx()
        for (row in 0..1) {
            for (col in 0..2) {
                val isWhite = (row + col) % 2 == 0
                drawRect(
                    color = if (isWhite) Color.White else Color.Black,
                    topLeft = Offset(sfX + col * checkW - 4.5.dp.toPx(), sfY - checkH + row * checkH),
                    size = androidx.compose.ui.geometry.Size(checkW, checkH)
                )
            }
        }

        // Sector Badges
        drawSectorLabel(
            text = "SECTOR 1",
            color = Color(0xFFE1251B),
            center = Offset(0.85f * w, 0.93f * h),
            drawScope = this
        )
        drawSectorLabel(
            text = "SECTOR 2",
            color = Color(0xFF00B4D8),
            center = Offset(0.65f * w, 0.08f * h),
            drawScope = this
        )
        drawSectorLabel(
            text = "SECTOR 3",
            color = Color(0xFFFFD100),
            center = Offset(0.20f * w, 0.44f * h),
            drawScope = this
        )

        // SPEED TRAP Callout & Badge
        val trapTarget = Offset(0.68f * w, 0.72f * h) // On back straight
        val trapBadgePos = Offset(0.68f * w, 0.63f * h)

        // Callout Line
        drawLine(
            color = P1TColors.SpeedTrapMagenta,
            start = trapBadgePos,
            end = trapTarget,
            strokeWidth = 1.2.dp.toPx()
        )
        drawCircle(
            color = P1TColors.SpeedTrapMagenta,
            radius = 3.dp.toPx(),
            center = trapTarget
        )

        // Badge Pill
        val trapBadgeWidth = 68.dp.toPx()
        val trapBadgeHeight = 16.dp.toPx()
        val trapLeft = trapBadgePos.x - trapBadgeWidth / 2f
        val trapTop = trapBadgePos.y - trapBadgeHeight

        drawRoundRect(
            color = P1TColors.SpeedTrapMagenta,
            topLeft = Offset(trapLeft, trapTop),
            size = androidx.compose.ui.geometry.Size(trapBadgeWidth, trapBadgeHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        paint.color = android.graphics.Color.WHITE
        paint.textSize = 8.dp.toPx()
        drawContext.canvas.nativeCanvas.drawText(
            "SPEED TRAP",
            trapBadgePos.x,
            trapTop + 11.5.dp.toPx(),
            paint
        )
    }
}

private fun drawSectorLabel(text: String, color: Color, center: Offset, drawScope: DrawScope) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 9.dp.toPx()
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = android.graphics.Paint.Align.CENTER
        this.color = android.graphics.Color.WHITE
    }

    val badgeW = 60.dp.toPx()
    val badgeH = 17.dp.toPx()
    val left = center.x - badgeW / 2f
    val top = center.y - badgeH / 2f

    drawScope.drawRoundRect(
        color = Color(0xFF14141A),
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(badgeW, badgeH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx(), 9.dp.toPx())
    )
    drawScope.drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(badgeW, badgeH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx(), 9.dp.toPx()),
        style = Stroke(width = 1.2.dp.toPx())
    )

    drawScope.drawContext.canvas.nativeCanvas.drawText(
        text,
        center.x,
        top + 12.dp.toPx(),
        paint
    )
}

/**
 * 3. LiveTrackMapCanvas
 * Draws the grey ribbon circuit with white sector divider ticks (S1, S2, S3),
 * start/finish checkered line, and moving team-colored driver dots with dark rounded pill labels (VER, HAM, LEC, etc.).
 * When selectedDriverCode matches a driver, draws glowing red target ring and highlights pill in red!
 */
@Composable
fun LiveTrackMapCanvas(
    drivers: List<LiveDriverTiming>,
    selectedDriverCode: String? = null,
    onDriverTap: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Store calculated driver screen positions for tap detection
    val driverTapTargets = remember { mutableMapOf<String, Offset>() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(drivers) {
                detectTapGestures { tapOffset ->
                    var closestDriver: String? = null
                    var minDist = Float.MAX_VALUE
                    driverTapTargets.forEach { (code, pos) ->
                        val dx = tapOffset.x - pos.x
                        val dy = tapOffset.y - pos.y
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist < 40.dp.toPx() && dist < minDist) {
                            minDist = dist
                            closestDriver = code
                        }
                    }
                    closestDriver?.let { onDriverTap(it) }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        val fullPath = buildCircuitPath(SepangCircuitGeometry.allPoints, w, h, close = true)
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(fullPath, false)
        val pathLength = pathMeasure.length

        // Outer Dark Ribbon
        drawPath(
            path = fullPath,
            color = Color(0xFF1E1E26),
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Inner Track Ribbon
        drawPath(
            path = fullPath,
            color = Color(0xFF2C2C38),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Sector Divider Ticks (approximate sector cutoffs 33% and 66%)
        val s1Pos = pathMeasure.getPosition(pathLength * 0.35f)
        val s2Pos = pathMeasure.getPosition(pathLength * 0.70f)
        val sfPos = pathMeasure.getPosition(pathLength * 0.02f)

        // S1 tick
        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = s1Pos)
        // S2 tick
        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = s2Pos)

        // Start / Finish Line
        drawCircle(color = Color(0xFFE1251B), radius = 4.dp.toPx(), center = sfPos)

        // Text Paint for Driver Labels
        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 9.dp.toPx()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }

        // Draw Drivers
        driverTapTargets.clear()
        drivers.forEach { driver ->
            val progress = (driver.trackProgress.coerceIn(0f, 1f))
            val dotPos = pathMeasure.getPosition(pathLength * progress)
            driverTapTargets[driver.code] = dotPos

            val isSelected = driver.code.equals(selectedDriverCode, ignoreCase = true)
            val teamColor = Color(driver.team.colorHex)

            // Target ring if selected
            if (isSelected) {
                // Outer glowing pulse ring
                drawCircle(
                    color = Color(0xFFE1251B).copy(alpha = 0.35f),
                    radius = 18.dp.toPx() * pulseScale,
                    center = dotPos
                )
                // Sharp red target ring
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

            // Pill Label
            val pillW = 28.dp.toPx()
            val pillH = 14.dp.toPx()
            val pillTopLeft = Offset(dotPos.x - pillW / 2f, dotPos.y - 18.dp.toPx())

            if (isSelected) {
                // Red background pill
                drawRoundRect(
                    color = Color(0xFFE1251B),
                    topLeft = pillTopLeft,
                    size = androidx.compose.ui.geometry.Size(pillW, pillH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                labelPaint.color = android.graphics.Color.WHITE
            } else {
                // Dark pill with team-colored border
                drawRoundRect(
                    color = Color(0xEE141418),
                    topLeft = pillTopLeft,
                    size = androidx.compose.ui.geometry.Size(pillW, pillH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = teamColor.copy(alpha = 0.8f),
                    topLeft = pillTopLeft,
                    size = androidx.compose.ui.geometry.Size(pillW, pillH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
                labelPaint.color = android.graphics.Color.WHITE
            }

            drawContext.canvas.nativeCanvas.drawText(
                driver.code,
                dotPos.x,
                pillTopLeft.y + 10.5.dp.toPx(),
                labelPaint
            )
        }
    }
}

package com.reva2473.f1widget.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.domain.model.SectorStatus
import com.reva2473.f1widget.domain.model.TyreCompound
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors

/**
 * Full screen dark background (#0B0B0E) with the top radial team-colored glow (LocalF1Accent.current.headerGlow).
 */
@Composable
fun P1TRadialBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glowColor = LocalF1Accent.current.headerGlow
    val bgPrimary = P1TColors.BackgroundPrimary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgPrimary)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Top radial glow
            val radialBrush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.55f),
                    glowColor.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(canvasWidth / 2f, -canvasHeight * 0.05f),
                radius = canvasWidth * 0.95f
            )

            drawRect(brush = radialBrush)
        }

        content()
    }
}

/**
 * 44.dp circular button with subtle dark translucent fill and 1.dp border (#383842 / subtle red-tinted border in header).
 */
@Composable
fun CircularHeaderButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0x331C1C22))
            .border(1.dp, Color(0xFF383844), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, color = Color.White),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

/**
 * Floating rounded pill bottom bar (Home, Season, Settings) matching home-1080.webp
 * with translucent dark surface, 1.dp border, and active red house/calendar/gear icon highlight.
 */
@Composable
fun FloatingPillBottomBar(
    currentTab: String,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xEE16161C),
            modifier = Modifier
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.Black)
                .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(32.dp))
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // HOME TAB
                BottomBarItem(
                    label = "Home",
                    isSelected = currentTab == "Home",
                    icon = { selected ->
                        Icon(
                            imageVector = if (selected) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home",
                            tint = if (selected) P1TColors.AccentRed else Color(0xFF888894),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = { onSelectTab("Home") }
                )

                // SEASON TAB
                BottomBarItem(
                    label = "Season",
                    isSelected = currentTab == "Season",
                    icon = { selected ->
                        Icon(
                            imageVector = if (selected) Icons.Filled.DateRange else Icons.Outlined.DateRange,
                            contentDescription = "Season",
                            tint = if (selected) P1TColors.AccentRed else Color(0xFF888894),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = { onSelectTab("Season") }
                )

                // SETTINGS / STANDINGS TAB
                BottomBarItem(
                    label = "Settings",
                    isSelected = currentTab == "Settings" || currentTab == "Standings",
                    icon = { selected ->
                        Icon(
                            imageVector = if (selected) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = if (selected) P1TColors.AccentRed else Color(0xFF888894),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = { onSelectTab("Settings") }
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    label: String,
    isSelected: Boolean,
    icon: @Composable (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color(0x22E1251B) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, color = P1TColors.AccentRed),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon(isSelected)
    }
}

/**
 * TyreCompoundBadge
 * Circular tyre ring icon with (S) red, (M) yellow, (H) white letter inside.
 */
@Composable
fun TyreCompoundBadge(
    compound: TyreCompound,
    sizeDp: Int = 20,
    modifier: Modifier = Modifier
) {
    val ringColor = Color(compound.colorHex)

    Box(
        modifier = modifier
            .size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = (sizeDp * 0.14f).dp.toPx()
            val radius = (size.minDimension - strokeW) / 2f
            drawCircle(
                color = ringColor,
                radius = radius,
                style = Stroke(width = strokeW)
            )
        }
        Text(
            text = compound.code,
            color = ringColor,
            fontSize = (sizeDp * 0.55f).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * MiniSectorBars
 * 3 horizontal rounded bars (22.dp × 5.dp) colored by SectorStatus.
 */
@Composable
fun MiniSectorBars(
    s1: SectorStatus,
    s2: SectorStatus,
    s3: SectorStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectorBarItem(status = s1)
        SectorBarItem(status = s2)
        SectorBarItem(status = s3)
    }
}

@Composable
private fun SectorBarItem(status: SectorStatus) {
    Box(
        modifier = Modifier
            .width(22.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .background(Color(status.colorHex))
    )
}

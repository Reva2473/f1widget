package com.reva2473.f1widget.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reva2473.f1widget.domain.model.SectorStatus
import com.reva2473.f1widget.domain.model.TyreCompound
import com.reva2473.f1widget.ui.theme.LocalF1Accent
import com.reva2473.f1widget.ui.theme.P1TColors
import com.reva2473.f1widget.ui.theme.P1TTheme

/**
 * Full-screen obsidian dark background (#070709) with atmospheric top ambient glow
 * (inspired by luxury motorsport telemetry apps). Supports optional per-screen glow overrides.
 */
@Composable
fun P1TRadialBackground(
    modifier: Modifier = Modifier,
    overrideGlowColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glowColor = overrideGlowColor ?: LocalF1Accent.current.headerGlow
    val accentColor = overrideGlowColor ?: LocalF1Accent.current.primaryAccent
    val isDark = LocalF1Accent.current.isDark
    val bgPrimary = MaterialTheme.colorScheme.background

    val topLinearColors = remember(glowColor, accentColor, isDark) {
        if (isDark) {
            listOf(
                accentColor.copy(alpha = 0.28f),
                glowColor.copy(alpha = 0.22f),
                Color.Transparent
            )
        } else {
            listOf(
                accentColor.copy(alpha = 0.14f),
                glowColor.copy(alpha = 0.06f),
                Color.Transparent
            )
        }
    }

    val radialColors = remember(glowColor, isDark) {
        if (isDark) {
            listOf(
                glowColor.copy(alpha = 0.58f),
                glowColor.copy(alpha = 0.22f),
                Color.Transparent
            )
        } else {
            listOf(
                glowColor.copy(alpha = 0.18f),
                glowColor.copy(alpha = 0.06f),
                Color.Transparent
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgPrimary)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val topWash = Brush.verticalGradient(
                        colors = topLinearColors,
                        startY = 0f,
                        endY = size.height * 0.34f
                    )
                    val radialBrush = Brush.radialGradient(
                        colors = radialColors,
                        center = Offset(size.width * 0.5f, -size.height * 0.04f),
                        radius = size.width * 1.05f
                    )
                    onDrawBehind {
                        drawRect(brush = topWash)
                        drawRect(brush = radialBrush)
                    }
                }
        )

        content()
    }
}

/**
 * 44.dp circular glass button with crisp hairline border, wrapped in a 48.dp touch target.
 */
@Composable
fun CircularHeaderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val isDark = LocalF1Accent.current.isDark
    val bgColor = if (isDark) Color(0x4D1E1E26) else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = if (isDark) Color.White else Color.Black),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

/**
 * Floating pill bottom bar with animated active label expansion
 * (circular icon buttons for inactive tabs + expanded pill with icon & label for the active tab).
 */
@Composable
fun FloatingPillBottomBar(
    currentTab: String,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalF1Accent.current.isDark
    val primaryAccent = LocalF1Accent.current.primaryAccent
    val activeAccent = P1TTheme.adaptiveTeamText(primaryAccent)
    val surfaceColor = if (isDark) Color(0xF0111116) else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.outline
    val inactiveTint = if (isDark) P1TColors.TextSecondary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(34.dp),
            color = surfaceColor,
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(34.dp),
                    spotColor = if (isDark) Color.Black else Color(0x40000000)
                )
                .border(1.dp, borderColor, RoundedCornerShape(34.dp))
        ) {
            Row(
                modifier = Modifier
                    .selectableGroup()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isHomeSelected = currentTab.equals("Home", ignoreCase = true)
                BottomBarPillItem(
                    label = "Home",
                    isSelected = isHomeSelected,
                    primaryAccent = primaryAccent,
                    activeTextColor = activeAccent,
                    inactiveTint = inactiveTint,
                    isDark = isDark,
                    icon = { selected, tint ->
                        Icon(
                            imageVector = if (selected) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home",
                            tint = tint,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    onClick = { onSelectTab("Home") }
                )

                val isStandingsSelected = currentTab.equals("Standings", ignoreCase = true)
                BottomBarPillItem(
                    label = "Stands",
                    isSelected = isStandingsSelected,
                    primaryAccent = primaryAccent,
                    activeTextColor = activeAccent,
                    inactiveTint = inactiveTint,
                    isDark = isDark,
                    icon = { selected, tint ->
                        Icon(
                            imageVector = if (selected) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                            contentDescription = "Standings",
                            tint = tint,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    onClick = { onSelectTab("Standings") }
                )

                val isSeasonSelected = currentTab.equals("Season", ignoreCase = true) ||
                    currentTab.equals("Schedule", ignoreCase = true)
                BottomBarPillItem(
                    label = "Schedule",
                    isSelected = isSeasonSelected,
                    primaryAccent = primaryAccent,
                    activeTextColor = activeAccent,
                    inactiveTint = inactiveTint,
                    isDark = isDark,
                    icon = { selected, tint ->
                        Icon(
                            imageVector = if (selected) Icons.Filled.DateRange else Icons.Outlined.DateRange,
                            contentDescription = "Schedule",
                            tint = tint,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    onClick = { onSelectTab("Season") }
                )

                val isSettingsSelected = currentTab.equals("Settings", ignoreCase = true)
                BottomBarPillItem(
                    label = "Widgets",
                    isSelected = isSettingsSelected,
                    primaryAccent = primaryAccent,
                    activeTextColor = activeAccent,
                    inactiveTint = inactiveTint,
                    isDark = isDark,
                    icon = { selected, tint ->
                        Icon(
                            imageVector = if (selected) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings and Widgets",
                            tint = tint,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    onClick = { onSelectTab("Settings") }
                )
            }
        }
    }
}

@Composable
private fun BottomBarPillItem(
    label: String,
    isSelected: Boolean,
    primaryAccent: Color,
    activeTextColor: Color,
    inactiveTint: Color,
    isDark: Boolean,
    icon: @Composable (Boolean, Color) -> Unit,
    onClick: () -> Unit
) {
    val activeBg = if (isDark) Color(0xFF23232D) else primaryAccent.copy(alpha = 0.14f)
    val inactiveBg = if (isDark) Color(0xFF17171E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeBg else inactiveBg,
        label = "NavPillBg"
    )
    val contentTint by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) Color.White else activeTextColor
        } else {
            inactiveTint
        },
        label = "NavPillTint"
    )
    val borderColor = if (isSelected) {
        primaryAccent.copy(alpha = if (isDark) 0.55f else 0.35f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .semantics {
                role = Role.Tab
                this.selected = isSelected
                contentDescription = "$label Tab"
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = primaryAccent),
                onClick = onClick
            )
            .padding(horizontal = if (isSelected) 16.dp else 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon(isSelected, contentTint)
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = label,
                    color = contentTint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Clean high-contrast segmented pill toggle (Image 4 style: active white pill in dark mode,
 * subtle dark container).
 */
@Composable
fun SegmentedPillToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalF1Accent.current.isDark
    val containerBg = if (isDark) Color(0xFF15151C) else MaterialTheme.colorScheme.surfaceVariant
    val containerBorder = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outline

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(containerBg)
            .border(1.dp, containerBorder, RoundedCornerShape(26.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            val pillBg by animateColorAsState(
                targetValue = if (selected) {
                    if (isDark) Color.White else MaterialTheme.colorScheme.onBackground
                } else {
                    Color.Transparent
                },
                label = "SegmentBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) {
                    if (isDark) Color(0xFF0A0A0E) else Color.White
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "SegmentText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 42.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(pillBg)
                    .semantics {
                        role = Role.Tab
                        this.selected = selected
                    }
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

enum class BroadcastFlagBannerType {
    GREEN_FLAG,
    SAFETY_CAR,
    VSC,
    RED_FLAG,
    FASTEST_LAP,
    NEUTRAL
}

fun resolveBroadcastFlagType(statusText: String): BroadcastFlagBannerType {
    val upper = statusText.uppercase()
    return when {
        upper.contains("RED") -> BroadcastFlagBannerType.RED_FLAG
        upper.contains("VSC") || upper.contains("VIRTUAL") -> BroadcastFlagBannerType.VSC
        upper.contains("SAFETY") || upper.contains("YELLOW") -> BroadcastFlagBannerType.SAFETY_CAR
        upper.contains("FASTEST") || upper.contains("PURPLE") -> BroadcastFlagBannerType.FASTEST_LAP
        upper.contains("GREEN") || upper.contains("CLEAR") || upper.contains("ALL CLEAR") -> BroadcastFlagBannerType.GREEN_FLAG
        else -> BroadcastFlagBannerType.NEUTRAL
    }
}

/**
 * Broadcast-grade F1 Race Control status banner (Image 3 inspired):
 * High-contrast Green Flag, Safety Car, Red Flag, and Fastest Lap purple chevron cards.
 */
@Composable
fun RaceControlStatusBanner(
    headline: String,
    subtitle: String,
    bannerType: BroadcastFlagBannerType,
    modifier: Modifier = Modifier,
    headerLabel: String = "RACE CONTROL",
    trailingBadge: String? = null
) {
    val isDark = LocalF1Accent.current.isDark
    val (bgColor, fgColor, icon) = when (bannerType) {
        BroadcastFlagBannerType.GREEN_FLAG -> Triple(P1TColors.GreenFlagBg, Color(0xFF07160F), Icons.Filled.Flag)
        BroadcastFlagBannerType.SAFETY_CAR -> Triple(P1TColors.SafetyCarBg, Color(0xFF141104), Icons.Filled.DirectionsCar)
        BroadcastFlagBannerType.VSC -> Triple(P1TColors.SafetyCarBg, Color(0xFF141104), Icons.Filled.Warning)
        BroadcastFlagBannerType.RED_FLAG -> Triple(P1TColors.RedFlagSolidBg, Color.White, Icons.Filled.NotInterested)
        BroadcastFlagBannerType.FASTEST_LAP -> Triple(P1TColors.FastestLapPurple, Color.White, Icons.Filled.Bolt)
        BroadcastFlagBannerType.NEUTRAL -> Triple(
            if (isDark) Color(0xFF1A1A22) else MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurface,
            Icons.Filled.Flag
        )
    }

    val topStripBg = if (bannerType == BroadcastFlagBannerType.FASTEST_LAP) {
        Color(0xFF14091E)
    } else if (isDark) {
        Color(0xFF0D0D11)
    } else {
        Color(0xFF16161C)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.dp,
                if (bannerType == BroadcastFlagBannerType.NEUTRAL) P1TTheme.borderSubtle else bgColor.copy(alpha = 0.45f),
                RoundedCornerShape(18.dp)
            )
    ) {
        // Dark motorsport header strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topStripBg)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (bannerType == BroadcastFlagBannerType.NEUTRAL) P1TColors.AccentRed else bgColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = headerLabel.uppercase(),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!trailingBadge.isNullOrBlank()) {
                Text(
                    text = trailingBadge,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // High-contrast body banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (bannerType == BroadcastFlagBannerType.FASTEST_LAP) {
                        Brush.horizontalGradient(
                            colors = listOf(P1TColors.FastestLapPurple, P1TColors.FastestLapPurpleDark)
                        )
                    } else {
                        Brush.horizontalGradient(colors = listOf(bgColor, bgColor))
                    }
                )
        ) {
            if (bannerType == BroadcastFlagBannerType.FASTEST_LAP) {
                // Diagonal motorsport chevrons (Image 3 Fastest Lap card)
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    for (i in 0..2) {
                        val offset = w * (0.42f + i * 0.16f)
                        val chevron = Path().apply {
                            moveTo(offset, 0f)
                            lineTo(offset + 28.dp.toPx(), 0f)
                            lineTo(offset + 44.dp.toPx(), h / 2f)
                            lineTo(offset + 28.dp.toPx(), h)
                            lineTo(offset, h)
                            lineTo(offset + 16.dp.toPx(), h / 2f)
                            close()
                        }
                        drawPath(chevron, color = Color.White.copy(alpha = 0.10f))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headline.uppercase(),
                        color = fgColor,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = fgColor.copy(alpha = 0.82f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = headline,
                    tint = fgColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * TyreCompoundBadge
 * Circular tyre ring icon with (S) red, (M) yellow, (H) white letter inside.
 */
@Composable
fun TyreCompoundBadge(
    compound: TyreCompound,
    modifier: Modifier = Modifier,
    sizeDp: Int = 20
) {
    val isDark = LocalF1Accent.current.isDark
    val ringColor = if (!isDark) {
        when (compound) {
            TyreCompound.HARD -> Color(0xFF4A4A56)
            TyreCompound.MEDIUM -> Color(0xFF8C6200)
            TyreCompound.INTERMEDIATE -> P1TTheme.sectorGreen
            TyreCompound.WET -> Color(0xFF007EA7)
            TyreCompound.SOFT -> P1TTheme.accentRed
        }
    } else {
        Color(compound.colorHex)
    }

    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val badgeScale = fontScale.coerceIn(1f, 1.35f)
    val scaledSizeDp = sizeDp * badgeScale
    val badgeFontSize = ((scaledSizeDp * 0.52f) / fontScale).sp

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = scaledSizeDp.dp, minHeight = scaledSizeDp.dp)
            .size(scaledSizeDp.dp)
            .semantics {
                contentDescription = "${compound.label} Tyre Compound (${compound.code})"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = size.minDimension * 0.14f
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
            fontSize = badgeFontSize,
            lineHeight = badgeFontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * MiniSectorBars
 * 3 horizontal rounded bars (22.dp x 5.dp) colored by SectorStatus.
 */
@Composable
fun MiniSectorBars(
    s1: SectorStatus,
    s2: SectorStatus,
    s3: SectorStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.semantics {
            contentDescription = "Mini sectors: Sector 1 ${s1.name}, Sector 2 ${s2.name}, Sector 3 ${s3.name}"
        },
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
    val isDark = LocalF1Accent.current.isDark
    val barColor = if (isDark) {
        Color(status.colorHex)
    } else {
        when (status) {
            SectorStatus.OVERALL_BEST -> P1TTheme.sectorPurple
            SectorStatus.PERSONAL_BEST -> P1TTheme.sectorGreen
            SectorStatus.NORMAL -> P1TTheme.sectorYellow
            SectorStatus.INACTIVE -> Color(0xFFD0D0DC)
        }
    }
    Box(
        modifier = Modifier
            .width(22.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .background(barColor)
            .semantics {
                contentDescription = status.name
            }
    )
}

package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalGripAccents
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class LiquidNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val accentColor: Color,
    val testTag: String
)

/**
 * Material 3 Expressive soft-scalloped badge shape (cookie / flower burst silhouette)
 * ideal for highlighting scores, live indicators, and top grip icons.
 */
class ExpressiveScallopShape(
    private val lobes: Int = 8,
    private val amplitudeFraction: Float = 0.085f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = size.minDimension / 2f * (1f - amplitudeFraction)
        val amp = size.minDimension / 2f * amplitudeFraction
        val steps = 120
        for (i in 0..steps) {
            val theta = (i.toFloat() / steps) * (2.0 * PI)
            val r = baseRadius + amp * cos(lobes * theta).toFloat()
            val x = cx + r * cos(theta).toFloat()
            val y = cy + r * sin(theta).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Floating Liquid Glass Pill Navigation Bar with spring-animated sliding translucent droplet
 * indicator, multi-stop frosted glass fill, specular top reflection, and safe navigationBar insets.
 */
@Composable
fun LiquidGlassFloatingNavBar(
    items: List<LiquidNavItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accents = LocalGripAccents.current
    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    val activeAccent by animateColorAsState(
        targetValue = items[selectedIndex].accentColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glass_nav_accent"
    )

    val pillShape = RoundedCornerShape(36.dp)
    val glassFillTop = if (accents.isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f)
    } else {
        Color.White.copy(alpha = 0.80f)
    }
    val glassFillBottom = if (accents.isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f)
    }
    val borderTopHighlight = if (accents.isDark) {
        Color.White.copy(alpha = 0.34f)
    } else {
        Color.White.copy(alpha = 0.92f)
    }
    val borderMidHighlight = if (accents.isDark) {
        activeAccent.copy(alpha = 0.35f)
    } else {
        activeAccent.copy(alpha = 0.28f)
    }
    val borderBottom = if (accents.isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 22.dp,
                    shape = pillShape,
                    ambientColor = activeAccent.copy(alpha = 0.38f),
                    spotColor = Color.Black.copy(alpha = if (accents.isDark) 0.65f else 0.22f)
                )
                .clip(pillShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(glassFillTop, glassFillBottom)
                    )
                )
                .drawBehind {
                    // Subtle internal liquid refraction glow from active tab color
                    val slotWidth = size.width / items.size.coerceAtLeast(1)
                    val glowCenterX = slotWidth * (selectedIndex + 0.5f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                activeAccent.copy(alpha = if (accents.isDark) 0.26f else 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(glowCenterX, size.height * 0.55f),
                            radius = size.height * 1.45f
                        ),
                        center = Offset(glowCenterX, size.height * 0.55f),
                        radius = size.height * 1.45f
                    )
                    // Specular glossy light streak across upper curve of the liquid pill
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (accents.isDark) 0.18f else 0.55f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.44f
                        ),
                        topLeft = Offset(8.dp.toPx(), 2.dp.toPx()),
                        size = Size(size.width - 16.dp.toPx(), size.height * 0.44f),
                        cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx())
                    )
                }
                .border(
                    width = 1.25.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(borderTopHighlight, borderMidHighlight, borderBottom)
                    ),
                    shape = pillShape
                )
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .testTag("liquid_glass_bottom_bar")
        ) {
            val itemCount = items.size.coerceAtLeast(1)
            val slotWidth = maxWidth / itemCount
            val indicatorOffset by animateDpAsState(
                targetValue = slotWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "liquid_droplet_offset"
            )

            // Sliding Liquid Glass Droplet Pill behind active tab
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(slotWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                activeAccent.copy(alpha = if (accents.isDark) 0.34f else 0.22f),
                                activeAccent.copy(alpha = if (accents.isDark) 0.16f else 0.10f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (accents.isDark) 0.45f else 0.85f),
                                activeAccent.copy(alpha = 0.45f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Interactive Tab Items
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 0.96f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tab_icon_scale_${item.route}"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) {
                            if (accents.isDark) Color.White else item.accentColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "tab_color_${item.route}"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = item.accentColor),
                                onClick = { onSelect(item.route) }
                            )
                            .testTag(item.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = contentColor,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(iconScale)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.2.sp
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating Liquid Glass Pill Rail for Medium/Expanded tablets and foldables.
 */
@Composable
fun LiquidGlassFloatingRail(
    items: List<LiquidNavItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accents = LocalGripAccents.current
    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    val activeAccent = items[selectedIndex].accentColor
    val railShape = RoundedCornerShape(36.dp)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(start = 14.dp, top = 20.dp, bottom = 20.dp, end = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(76.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = railShape,
                    ambientColor = activeAccent.copy(alpha = 0.30f)
                )
                .clip(railShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.84f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                        )
                    )
                )
                .border(
                    width = 1.25.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (accents.isDark) 0.34f else 0.85f),
                            activeAccent.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    ),
                    shape = railShape
                )
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val contentColor = if (selected) {
                    if (accents.isDark) Color.White else item.accentColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Column(
                    modifier = Modifier
                        .size(width = 62.dp, height = 58.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (selected) item.accentColor.copy(alpha = 0.24f) else Color.Transparent
                        )
                        .border(
                            width = if (selected) 1.dp else 0.dp,
                            color = if (selected) item.accentColor.copy(alpha = 0.55f) else Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { onSelect(item.route) }
                        .testTag(item.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

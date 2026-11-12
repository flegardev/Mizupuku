package app.hydronudge.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import app.hydronudge.data.preferences.VesselStyle
import app.hydronudge.domain.*
import kotlin.math.sin

/**
 * Ocean 2.0 Hero Water Vessel & Progress Indicator.
 * Supports Wave Ring, Water Glass, Hydro Bottle, and Minimal Arc styles with smooth
 * spring-driven liquid fills while respecting reduced-motion and accessibility.
 */
@Composable
fun WaterProgressVisualization(
    totalMl: Long,
    goalMl: Int,
    unit: VolumeUnit,
    vesselStyle: VesselStyle = VesselStyle.WAVE_RING,
    reducedMotion: Boolean = false
) {
    if (goalMl <= 0) {
        OceanCard(Modifier.fillMaxWidth()) {
            Text(unit.format(totalMl), style = MaterialTheme.typography.displayLarge)
            Text(
                "water recorded today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("No daily goal set", style = MaterialTheme.typography.titleMedium)
            Text(
                "A goal is optional. Needs vary; Mizupuku does not calculate medical advice. For a child, ask a parent or caregiver to choose a goal with appropriate professional guidance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    val ratio = (totalMl.toFloat() / goalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animate = ValueAnimator.areAnimatorsEnabled() && !reducedMotion
    val progress by animateFloatAsState(
        targetValue = ratio,
        animationSpec = if (animate) {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        } else {
            tween(0)
        },
        label = "Water progress"
    )
    val colors = MaterialTheme.colorScheme
    val largeText = LocalDensity.current.fontScale > 1.3f

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${unit.format(totalMl)} of ${unit.format(goalMl.toLong())}, ${goalPercent(totalMl, goalMl)} percent completed"
                progressBarRangeInfo = ProgressBarRangeInfo(ratio, 0f..1f)
            }
    ) {
        val stacked = largeText || maxWidth < 280.dp
        val numbers: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AnimatedContent(
                    targetState = totalMl,
                    transitionSpec = {
                        fadeIn(tween(if (animate) 220 else 0)) togetherWith fadeOut(tween(if (animate) 140 else 0))
                    },
                    label = "Water total"
                ) { displayed ->
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            unit.format(displayed).removeSuffix(" ${unit.label}"),
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            unit.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                Text(
                    "of ${unit.format(goalMl.toLong())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Text(
                    "${goalPercent(totalMl, goalMl)}% completed",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (stacked) {
                numbers()
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) { numbers() }
                    Box(
                        Modifier.size(116.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VesselCanvas(
                            progress = progress,
                            vesselStyle = vesselStyle,
                            primary = colors.primary,
                            secondary = colors.secondary,
                            tertiary = colors.tertiary,
                            onSurface = colors.onSurface
                        )
                        if (vesselStyle == VesselStyle.MINIMAL_ARC || vesselStyle == VesselStyle.WAVE_RING) {
                            Icon(
                                if (totalMl >= goalMl) HydroIcons.Check else HydroIcons.Drop,
                                null,
                                Modifier.size(28.dp),
                                tint = colors.primary
                            )
                        }
                    }
                }
            }
            if (stacked) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            HorizontalDivider(color = colors.primary.copy(alpha = 0.14f))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (totalMl < goalMl) "${unit.format(goalMl - totalMl)} to go"
                    else "Daily water goal reached.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                if (totalMl >= goalMl) {
                    Text(
                        "Goal met",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun VesselCanvas(
    progress: Float,
    vesselStyle: VesselStyle,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    onSurface: Color
) {
    Canvas(Modifier.fillMaxSize().padding(6.dp)) {
        val d = size.minDimension
        val stroke = Stroke(7.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(
            Brush.radialGradient(listOf(primary.copy(alpha = 0.15f), Color.Transparent)),
            d / 2
        )

        when (vesselStyle) {
            VesselStyle.WAVE_RING -> {
                // Inner liquid wave sphere + outer progress arc
                val innerRadius = d / 2 - 12.dp.toPx()
                val circlePath = Path().apply {
                    addOval(
                        Rect(
                            center.x - innerRadius,
                            center.y - innerRadius,
                            center.x + innerRadius,
                            center.y + innerRadius
                        )
                    )
                }
                if (progress > 0f) {
                    clipPath(circlePath) {
                        val waterTop = (center.y + innerRadius) - (2f * innerRadius * progress)
                        val wavePath = Path().apply {
                            moveTo(center.x - innerRadius, size.height)
                            lineTo(center.x - innerRadius, waterTop)
                            val steps = 20
                            val span = 2f * innerRadius
                            val amp = 3.5.dp.toPx() * (1f - kotlin.math.abs(progress - 0.5f))
                            for (i in 0..steps) {
                                val x = (center.x - innerRadius) + (span * i / steps)
                                val y = waterTop + sin(i * 0.45f + progress * 6f) * amp
                                lineTo(x, y)
                            }
                            lineTo(center.x + innerRadius, size.height)
                            close()
                        }
                        drawPath(
                            wavePath,
                            Brush.verticalGradient(
                                listOf(
                                    tertiary.copy(alpha = 0.34f),
                                    primary.copy(alpha = 0.22f)
                                )
                            )
                        )
                    }
                }
                drawArc(
                    onSurface.copy(alpha = 0.09f),
                    130f,
                    280f,
                    false,
                    Offset.Zero,
                    Size(d, d),
                    style = stroke
                )
                if (progress > 0f) {
                    drawArc(
                        Brush.linearGradient(listOf(secondary, primary, tertiary)),
                        130f,
                        280f * progress,
                        false,
                        Offset.Zero,
                        Size(d, d),
                        style = stroke
                    )
                }
                drawCircle(
                    primary.copy(alpha = 0.16f),
                    innerRadius,
                    style = Stroke(0.9.dp.toPx())
                )
            }

            VesselStyle.TEA_CUP -> {
                // Cozy Kyoto ceramic teacup (yunomi) with rising liquid and subtle steam wisps
                val cupTopW = d * 0.68f
                val cupBottomW = d * 0.46f
                val cupH = d * 0.62f
                val topY = d * 0.24f
                val bottomY = topY + cupH
                val cupPath = Path().apply {
                    moveTo(center.x - cupTopW / 2f, topY)
                    lineTo(center.x + cupTopW / 2f, topY)
                    cubicTo(
                        center.x + cupTopW * 0.48f,
                        topY + cupH * 0.65f,
                        center.x + cupBottomW * 0.65f,
                        bottomY,
                        center.x + cupBottomW / 2f,
                        bottomY
                    )
                    lineTo(center.x - cupBottomW / 2f, bottomY)
                    cubicTo(
                        center.x - cupBottomW * 0.65f,
                        bottomY,
                        center.x - cupTopW * 0.48f,
                        topY + cupH * 0.65f,
                        center.x - cupTopW / 2f,
                        topY
                    )
                    close()
                }
                if (progress > 0f) {
                    clipPath(cupPath) {
                        val waterTop = bottomY - cupH * progress
                        drawRect(
                            Brush.verticalGradient(
                                listOf(tertiary.copy(alpha = 0.60f), primary.copy(alpha = 0.42f)),
                                startY = waterTop,
                                endY = bottomY
                            ),
                            topLeft = Offset(0f, waterTop),
                            size = Size(size.width, bottomY - waterTop)
                        )
                    }
                }
                drawPath(
                    cupPath,
                    color = primary.copy(alpha = 0.72f),
                    style = Stroke(width = 2.4.dp.toPx(), join = StrokeJoin.Round)
                )
                // Base foot ring of ceramic cup
                drawLine(
                    primary.copy(alpha = 0.55f),
                    Offset(center.x - cupBottomW * 0.36f, bottomY + 3.dp.toPx()),
                    Offset(center.x + cupBottomW * 0.36f, bottomY + 3.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Gentle rising steam wisps above the teacup
                for (wisp in -1..1 step 2) {
                    val wx = center.x + wisp * (d * 0.11f)
                    val steamPath = Path().apply {
                        moveTo(wx, topY - 4.dp.toPx())
                        cubicTo(
                            wx - 4.dp.toPx(),
                            topY - 10.dp.toPx(),
                            wx + 4.dp.toPx(),
                            topY - 14.dp.toPx(),
                            wx,
                            topY - 20.dp.toPx()
                        )
                    }
                    drawPath(
                        steamPath,
                        color = primary.copy(alpha = 0.38f),
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            VesselStyle.WATER_GLASS -> {
                // Sculpted tumbler glass with rising water level
                val topWidth = d * 0.66f
                val bottomWidth = d * 0.50f
                val glassHeight = d * 0.78f
                val topY = (d - glassHeight) / 2f
                val bottomY = topY + glassHeight
                val glassPath = Path().apply {
                    moveTo(center.x - topWidth / 2f, topY)
                    lineTo(center.x + topWidth / 2f, topY)
                    lineTo(center.x + bottomWidth / 2f, bottomY)
                    lineTo(center.x - bottomWidth / 2f, bottomY)
                    close()
                }
                if (progress > 0f) {
                    clipPath(glassPath) {
                        val waterTop = bottomY - glassHeight * progress
                        drawRect(
                            Brush.verticalGradient(
                                listOf(tertiary.copy(alpha = 0.55f), primary.copy(alpha = 0.38f)),
                                startY = waterTop,
                                endY = bottomY
                            ),
                            topLeft = Offset(0f, waterTop),
                            size = Size(size.width, bottomY - waterTop)
                        )
                    }
                }
                drawPath(
                    glassPath,
                    color = primary.copy(alpha = 0.65f),
                    style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round)
                )
            }

            VesselStyle.BOTTLE -> {
                // Ergonomic hydro flask silhouette with volume ticks
                val bodyW = d * 0.52f
                val bodyH = d * 0.66f
                val neckW = d * 0.26f
                val neckH = d * 0.14f
                val topY = (d - (bodyH + neckH)) / 2f
                val bodyTop = topY + neckH
                val bodyBottom = bodyTop + bodyH
                val bottlePath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(center.x - neckW / 2f, topY, center.x + neckW / 2f, bodyTop + 4f),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    )
                    addRoundRect(
                        RoundRect(
                            rect = Rect(center.x - bodyW / 2f, bodyTop, center.x + bodyW / 2f, bodyBottom),
                            cornerRadius = CornerRadius(12.dp.toPx())
                        )
                    )
                }
                if (progress > 0f) {
                    clipPath(bottlePath) {
                        val waterTop = bodyBottom - bodyH * progress
                        drawRect(
                            Brush.verticalGradient(
                                listOf(tertiary.copy(alpha = 0.55f), primary.copy(alpha = 0.38f)),
                                startY = waterTop,
                                endY = bodyBottom
                            ),
                            topLeft = Offset(center.x - bodyW / 2f, waterTop),
                            size = Size(bodyW, bodyBottom - waterTop)
                        )
                    }
                }
                drawPath(
                    bottlePath,
                    color = primary.copy(alpha = 0.65f),
                    style = Stroke(width = 2.2.dp.toPx())
                )
                for (tick in 1..3) {
                    val ty = bodyBottom - (bodyH * tick / 4f)
                    drawLine(
                        primary.copy(alpha = 0.35f),
                        Offset(center.x + bodyW * 0.15f, ty),
                        Offset(center.x + bodyW * 0.38f, ty),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }

            VesselStyle.DROPLET -> {
                // Sculpted water droplet with rising liquid wave and outer progress arc
                val dropTopY = d * 0.14f
                val dropBottomY = d * 0.86f
                val dropHeight = dropBottomY - dropTopY
                val dropRadius = d * 0.28f
                val dropCenterY = dropBottomY - dropRadius
                val dropletPath = Path().apply {
                    moveTo(center.x, dropTopY)
                    cubicTo(
                        center.x - dropRadius * 0.3f,
                        dropTopY + dropHeight * 0.28f,
                        center.x - dropRadius * 1.15f,
                        dropCenterY - dropRadius * 0.35f,
                        center.x - dropRadius,
                        dropCenterY
                    )
                    arcTo(
                        rect = Rect(
                            center.x - dropRadius,
                            dropCenterY - dropRadius,
                            center.x + dropRadius,
                            dropCenterY + dropRadius
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    cubicTo(
                        center.x + dropRadius * 1.15f,
                        dropCenterY - dropRadius * 0.35f,
                        center.x + dropRadius * 0.3f,
                        dropTopY + dropHeight * 0.28f,
                        center.x,
                        dropTopY
                    )
                    close()
                }
                if (progress > 0f) {
                    clipPath(dropletPath) {
                        val waterTop = dropBottomY - dropHeight * progress
                        drawRect(
                            Brush.verticalGradient(
                                listOf(tertiary.copy(alpha = 0.65f), primary.copy(alpha = 0.42f)),
                                startY = waterTop,
                                endY = dropBottomY
                            ),
                            topLeft = Offset(0f, waterTop),
                            size = Size(size.width, dropBottomY - waterTop)
                        )
                    }
                }
                drawPath(
                    dropletPath,
                    color = primary.copy(alpha = 0.72f),
                    style = Stroke(width = 2.4.dp.toPx(), join = StrokeJoin.Round)
                )
            }

            VesselStyle.MINIMAL_ARC -> {
                drawArc(
                    onSurface.copy(alpha = 0.09f),
                    130f,
                    280f,
                    false,
                    Offset.Zero,
                    Size(d, d),
                    style = stroke
                )
                if (progress > 0f) {
                    drawArc(
                        Brush.linearGradient(listOf(secondary, primary)),
                        130f,
                        280f * progress,
                        false,
                        Offset.Zero,
                        Size(d, d),
                        style = stroke
                    )
                }
                drawCircle(
                    primary.copy(alpha = 0.12f),
                    d / 2 - 12.dp.toPx(),
                    style = Stroke(0.7.dp.toPx())
                )
            }
        }
    }
}

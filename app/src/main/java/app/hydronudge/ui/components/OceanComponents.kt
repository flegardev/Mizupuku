package app.hydronudge.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.hydronudge.domain.DrinkCategory

/** Original 24-unit line icons. No font glyphs or platform-dependent emoji. */
object HydroIcons {
    val Drop = icon("Water") {
        moveTo(12f, 2f); curveTo(10f, 6f, 4f, 11f, 4f, 15f)
        curveTo(4f, 24f, 20f, 24f, 20f, 15f); curveTo(20f, 11f, 14f, 6f, 12f, 2f); close()
    }
    val Chart = icon("History") {
        moveTo(4f, 20f); lineTo(4f, 13f)
        moveTo(12f, 20f); lineTo(12f, 4f)
        moveTo(20f, 20f); lineTo(20f, 9f)
    }
    val Settings = icon("Settings") {
        moveTo(4f, 6f); lineTo(20f, 6f)
        moveTo(4f, 18f); lineTo(20f, 18f)
        moveTo(8f, 3f); lineTo(8f, 9f)
        moveTo(16f, 15f); lineTo(16f, 21f)
    }
    val Plus = icon("Add") {
        moveTo(12f, 4f); lineTo(12f, 20f)
        moveTo(4f, 12f); lineTo(20f, 12f)
    }
    val Close = icon("Close") {
        moveTo(6f, 6f); lineTo(18f, 18f)
        moveTo(18f, 6f); lineTo(6f, 18f)
    }
    val Arrow = icon("Continue") {
        moveTo(4f, 12f); lineTo(20f, 12f)
        moveTo(14f, 6f); lineTo(20f, 12f); lineTo(14f, 18f)
    }
    val More = icon("More") {
        moveTo(5f, 11f); lineTo(5f, 13f)
        moveTo(12f, 11f); lineTo(12f, 13f)
        moveTo(19f, 11f); lineTo(19f, 13f)
    }
    val Bell = icon("Reminders") {
        moveTo(18f, 8f); curveTo(18f, 4.7f, 15.3f, 2f, 12f, 2f)
        curveTo(8.7f, 2f, 6f, 4.7f, 6f, 8f); lineTo(6f, 15f)
        lineTo(4f, 17f); lineTo(20f, 17f); lineTo(18f, 15f); close()
        moveTo(10f, 20f); lineTo(14f, 20f)
    }
    val Star = icon("Favorite") {
        moveTo(12f, 3f); lineTo(14.8f, 8.6f); lineTo(21f, 9.5f)
        lineTo(16.5f, 13.9f); lineTo(17.6f, 20f); lineTo(12f, 17.1f)
        lineTo(6.4f, 20f); lineTo(7.5f, 13.9f); lineTo(3f, 9.5f)
        lineTo(9.2f, 8.6f); close()
    }
    val Cup = icon("Cup") {
        moveTo(5f, 5f); lineTo(17f, 5f); lineTo(16f, 19f)
        lineTo(6f, 19f); close()
        moveTo(17f, 7f); lineTo(20f, 7f); lineTo(20f, 12f); lineTo(16.5f, 12f)
    }
    val Sparkle = icon("Milestone") {
        moveTo(12f, 2f); lineTo(14.2f, 9.8f); lineTo(22f, 12f)
        lineTo(14.2f, 14.2f); lineTo(12f, 22f); lineTo(9.8f, 14.2f)
        lineTo(2f, 12f); lineTo(9.8f, 9.8f); close()
    }
    val Check = icon("Completed") {
        moveTo(5f, 13f); lineTo(10f, 18f); lineTo(19f, 7f)
    }
    val Palette = icon("Theme Studio") {
        moveTo(12f, 3f)
        curveTo(7f, 3f, 3f, 7f, 3f, 12f)
        curveTo(3f, 17f, 7f, 21f, 12f, 21f)
        curveTo(13.8f, 21f, 15f, 19.8f, 15f, 18.2f)
        curveTo(15f, 17.4f, 14.7f, 16.8f, 14.2f, 16.2f)
        curveTo(13.8f, 15.7f, 13.6f, 15.2f, 13.6f, 14.6f)
        curveTo(13.6f, 13.2f, 14.8f, 12f, 16.2f, 12f)
        lineTo(18f, 12f)
        curveTo(19.7f, 12f, 21f, 10.7f, 21f, 9f)
        curveTo(21f, 5.7f, 16.9f, 3f, 12f, 3f); close()
        moveTo(7.5f, 10.5f); lineTo(7.6f, 10.5f)
        moveTo(10.5f, 7.5f); lineTo(10.6f, 7.5f)
        moveTo(14.5f, 7.5f); lineTo(14.6f, 7.5f)
    }
    val Sun = icon("Light theme") {
        moveTo(12f, 8f); curveTo(9.8f, 8f, 8f, 9.8f, 8f, 12f)
        curveTo(8f, 14.2f, 9.8f, 16f, 12f, 16f)
        curveTo(14.2f, 16f, 16f, 14.2f, 16f, 12f)
        curveTo(16f, 9.8f, 14.2f, 8f, 12f, 8f); close()
        moveTo(12f, 2f); lineTo(12f, 4f)
        moveTo(12f, 20f); lineTo(12f, 22f)
        moveTo(2f, 12f); lineTo(4f, 12f)
        moveTo(20f, 12f); lineTo(22f, 12f)
        moveTo(4.9f, 4.9f); lineTo(6.3f, 6.3f)
        moveTo(17.7f, 17.7f); lineTo(19.1f, 19.1f)
        moveTo(19.1f, 4.9f); lineTo(17.7f, 6.3f)
        moveTo(6.3f, 17.7f); lineTo(4.9f, 19.1f)
    }
    val Moon = icon("Dark theme") {
        moveTo(20f, 14.5f)
        curveTo(18.8f, 15.1f, 17.4f, 15.5f, 16f, 15.5f)
        curveTo(11.3f, 15.5f, 7.5f, 11.7f, 7.5f, 7f)
        curveTo(7.5f, 5.6f, 7.9f, 4.2f, 8.5f, 3f)
        curveTo(5.3f, 4.4f, 3f, 7.7f, 3f, 11.5f)
        curveTo(3f, 16.7f, 7.3f, 21f, 12.5f, 21f)
        curveTo(16.3f, 21f, 19.6f, 18.7f, 20f, 14.5f); close()
    }
    val Search = icon("Search") {
        moveTo(11f, 4f)
        curveTo(7.1f, 4f, 4f, 7.1f, 4f, 11f)
        curveTo(4f, 14.9f, 7.1f, 18f, 11f, 18f)
        curveTo(14.9f, 18f, 18f, 14.9f, 18f, 11f)
        curveTo(18f, 7.1f, 14.9f, 4f, 11f, 4f); close()
        moveTo(16.2f, 16.2f); lineTo(20f, 20f)
    }

    private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.65f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block
            )
        }.build()
}

@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier,
        shape = RoundedCornerShape(if (hero) 32.dp else 28.dp),
        color = colors.surface,
        tonalElevation = if (hero) 4.dp else 1.dp,
        border = BorderStroke(0.75.dp, colors.primary.copy(alpha = if (hero) 0.24f else 0.16f))
    ) {
        Column(
            Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.primary.copy(alpha = if (hero) 0.14f else 0.05f),
                            colors.tertiary.copy(alpha = if (hero) 0.05f else 0.015f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun OceanCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    PremiumGlassCard(modifier, content = content)

@Composable
fun OceanHeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    PremiumGlassCard(modifier, hero = true, content = content)

@Composable
fun OceanBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.background(
            Brush.verticalGradient(
                listOf(
                    colors.surfaceContainerLowest,
                    colors.background,
                    colors.background
                )
            )
        ),
        content = content
    )
}

@Composable
fun HydroPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick,
        modifier.heightIn(min = 56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        content = content
    )
}

@Composable
fun HydroQuickAddButton(
    amount: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "Quick add press"
    )
    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .heightIn(min = 64.dp),
        interactionSource = interactions,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    ) {
        Row(
            Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "+ $amount",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
            Icon(
                HydroIcons.Drop,
                null,
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun OceanStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun HydroSectionHeader(title: String, detail: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (detail != null) {
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HydroEmptyState(title: String, message: String, action: (() -> Unit)? = null) {
    PremiumGlassCard(Modifier.fillMaxWidth()) {
        WaterBadge()
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (action != null) {
            TextButton(onClick = action, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Add water")
            }
        }
    }
}

@Composable
fun HydroSettingsGroup(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    PremiumGlassCard(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydroBottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        content = content
    )
}

@Composable
fun WaterBadge(modifier: Modifier = Modifier, category: String = "WATER") {
    val drinkCategory = DrinkCategory.fromCode(category)
    val badgeColor = if (category == "WATER" || drinkCategory == null) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color(drinkCategory.colorHex).copy(alpha = 0.22f)
    }
    Surface(
        modifier.size(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = badgeColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (category == "WATER" || drinkCategory == null) {
                Icon(
                    HydroIcons.Drop,
                    null,
                    Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    drinkCategory.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PageHeading(eyebrow: String, title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(title, style = MaterialTheme.typography.headlineLarge)
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ColorSwatchDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(14.dp),
        shape = RoundedCornerShape(50),
        color = color,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
    ) {}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickThemeStudioSheet(
    settings: app.hydronudge.data.preferences.HydroSettings,
    onSave: (app.hydronudge.data.preferences.HydroSettings) -> Unit,
    onDismiss: () -> Unit
) {
    HydroBottomSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "YOUR WATER CAFÉ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Themes & Pastel Palettes", style = MaterialTheme.typography.titleLarge)
                }
                TextButton(onClick = onDismiss) { Text("Done") }
            }

            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Text("Mode (Light, Dark, True OLED, System)", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                app.hydronudge.data.preferences.ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.theme == mode,
                        onClick = { onSave(settings.copy(theme = mode)) },
                        label = { Text(mode.label) }
                    )
                }
            }

            Text("Choose your café palette", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                app.hydronudge.data.preferences.OceanPalette.entries.forEach { palette ->
                    FilterChip(
                        selected = settings.palette == palette,
                        onClick = {
                            onSave(
                                settings.copy(
                                    palette = palette,
                                    dynamicColors = palette == app.hydronudge.data.preferences.OceanPalette.MATERIAL_YOU
                                )
                            )
                        },
                        leadingIcon = {
                            ColorSwatchDot(Color(palette.swatchHex))
                        },
                        label = { Text(palette.title) }
                    )
                }
            }

            Text("Custom Accent Override", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                app.hydronudge.ui.theme.CustomAccentSwatches.forEach { swatch ->
                    FilterChip(
                        selected = settings.customAccentHex == swatch.hex,
                        onClick = { onSave(settings.copy(customAccentHex = swatch.hex)) },
                        leadingIcon = if (swatch.hex != 0L) {
                            { ColorSwatchDot(Color(swatch.hex)) }
                        } else null,
                        label = { Text(swatch.name) }
                    )
                }
            }

            Text("Hydration Vessel Style", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                app.hydronudge.data.preferences.VesselStyle.entries.forEach { vessel ->
                    FilterChip(
                        selected = settings.vesselStyle == vessel,
                        onClick = { onSave(settings.copy(vesselStyle = vessel)) },
                        label = { Text(vessel.label) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            }
        }
    }
}

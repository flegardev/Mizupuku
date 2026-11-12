package app.hydronudge.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.hydronudge.data.local.*
import app.hydronudge.data.preferences.HydroSettings
import app.hydronudge.domain.*
import app.hydronudge.reminders.ReminderPolicy
import app.hydronudge.ui.HydroState
import app.hydronudge.ui.components.*
import app.hydronudge.ui.theme.CafeIllustration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Dashboard(
    state: HydroState,
    quickAdd: (Int) -> Unit,
    addDrink: () -> Unit,
    edit: (DrinkEntry) -> Unit,
    delete: (DrinkEntry) -> Unit,
    settings: () -> Unit,
    history: () -> Unit,
    onLogFavorite: (FavoriteDrink) -> Unit = {},
    onDeleteFavorite: (String) -> Unit = {},
    onClearToday: () -> Unit = {},
    onSaveSettings: (HydroSettings) -> Unit = {}
) {
    val headerDensity = androidx.compose.ui.platform.LocalDensity.current
    val largeType = headerDensity.fontScale >= 1.3f
    val compactHeader = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width / headerDensity.density < 380f || headerDensity.fontScale > 1.3f
    var confirmClearToday by rememberSaveable { mutableStateOf(false) }
    var showThemeStudio by rememberSaveable { mutableStateOf(false) }
    var showCupPresets by rememberSaveable { mutableStateOf(false) }
    val todayDisplayed = if (state.settings.beverageTrackingEnabled) state.todayAllBeverages else state.today
    val lastDrink = todayDisplayed.maxByOrNull { it.timestamp }
    val recentAmounts = remember(state.entries, state.settings.quickAmounts) {
        recentlyUsedAmounts(state.entries, 4).filter { it !in state.settings.quickAmounts }
    }
    val nowZdt = ZonedDateTime.now(state.zone)
    val currentMinuteOfDay = nowZdt.hour * 60 + nowZdt.minute

    LazyColumn(
        modifier = Modifier.testTag("dashboard-list"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mizupuku", style = MaterialTheme.typography.headlineMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            state.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            onClick = { showThemeStudio = true },
                            modifier = Modifier.testTag("open-theme-studio"),
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                ColorSwatchDot(
                                    Color(
                                        if (state.settings.customAccentHex != 0L) state.settings.customAccentHex
                                        else state.settings.palette.swatchHex
                                    )
                                )
                                Text(
                                    state.settings.palette.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = { showThemeStudio = true }) {
                    Icon(HydroIcons.Palette, "Themes & Pastel Palettes")
                }
                if (!compactHeader) {
                    IconButton(onClick = history) { Icon(HydroIcons.Chart, "History") }
                    IconButton(onClick = settings) { Icon(HydroIcons.Settings, "Settings") }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (!largeType) Text("Your little water café.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${state.settings.unit.format(state.totalMl)} today", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                if (!largeType) CafeIllustration(Modifier.width(128.dp).height(48.dp), 0)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HydroSectionHeader("Add water")
                    if (!state.settings.minimalistMode) {
                        if (largeType) {
                            IconButton(onClick = { showCupPresets = !showCupPresets }) {
                                Icon(HydroIcons.Cup, if (showCupPresets) "Hide vessels" else "Cup presets")
                            }
                        } else {
                        TextButton(onClick = { showCupPresets = !showCupPresets }) {
                            Icon(HydroIcons.Cup, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (showCupPresets) "Hide vessels" else "Cup presets")
                        }
                        }
                    }
                }
                state.settings.quickAmounts.chunked(2).forEach { amounts ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        amounts.forEach { ml ->
                            HydroQuickAddButton(
                                state.settings.unit.format(ml.toLong()),
                                { quickAdd(ml) },
                                Modifier.weight(1f).testTag("quick-add:$ml")
                            )
                        }
                    }
                }
                if (showCupPresets && !state.settings.minimalistMode) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DefaultCupPresets.forEach { cup ->
                            AssistChip(
                                onClick = { quickAdd(cup.amountMl) },
                                leadingIcon = { Icon(HydroIcons.Cup, null, Modifier.size(16.dp)) },
                                label = { Text("${cup.label} · +${state.settings.unit.format(cup.amountMl.toLong())}") }
                            )
                        }
                    }
                }
                if (!state.settings.minimalistMode && (state.favorites.isNotEmpty() || recentAmounts.isNotEmpty())) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.favorites.forEach { fav ->
                            AssistChip(
                                onClick = { onLogFavorite(fav) },
                                label = {
                                    Text("${fav.name} · +${state.settings.unit.format(fav.amountMl.toLong())}")
                                },
                                leadingIcon = {
                                    Icon(HydroIcons.Star, null, Modifier.size(16.dp))
                                }
                            )
                        }
                        recentAmounts.take(2).forEach { recentMl ->
                            SuggestionChip(
                                onClick = { quickAdd(recentMl) },
                                label = { Text("Recent +${state.settings.unit.format(recentMl.toLong())}") }
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = addDrink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("choose-drink"),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(HydroIcons.Plus, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Custom amount")
                    Spacer(Modifier.weight(1f))
                    Icon(HydroIcons.Arrow, null, Modifier.size(18.dp))
                }
            }
        }
        item {
            OceanHeroCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TODAY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!state.settings.minimalistMode && state.effectiveGoalMl > 0) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                            ) {
                                Text(
                                    hydrationPaceSummary(
                                        totalMl = state.totalMl,
                                        goalMl = state.effectiveGoalMl,
                                        currentMinuteOfDay = currentMinuteOfDay,
                                        wakeMinute = state.settings.wakeMinute,
                                        sleepMinute = state.settings.sleepMinute,
                                        unit = state.settings.unit
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (state.settings.showStreaks && !state.settings.minimalistMode && state.streak.currentStreakDays > 0) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "${state.streak.currentStreakDays}d streak",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                WaterProgressVisualization(
                    totalMl = state.totalMl,
                    goalMl = state.effectiveGoalMl,
                    unit = state.settings.unit,
                    vesselStyle = state.settings.vesselStyle,
                    reducedMotion = state.settings.reducedMotion
                )
                if (!state.settings.minimalistMode) {
                    if (state.settings.beverageTrackingEnabled && state.totalBeverageMl > state.totalMl) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        Text(
                            "Total beverages: ${state.settings.unit.format(state.totalBeverageMl)} · Est. hydration: ${state.settings.unit.format(state.estimatedHydrationMl)} (non-medical estimate)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OceanStatPill(
                            label = "Last drink",
                            value = if (lastDrink == null) "None today"
                            else Instant.ofEpochMilli(lastDrink.timestamp)
                                .atZone(state.zone)
                                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                            modifier = Modifier.weight(1f)
                        )
                        OceanStatPill(
                            label = "Reminders",
                            value = ReminderPolicy.nextReminderSummary(
                                state.settings,
                                nowZdt,
                                state.totalMl,
                                lastDrink?.timestamp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    val missedFeedback = ReminderPolicy.missedReminderFeedback(
                        state.settings,
                        nowZdt,
                        state.totalMl,
                        lastDrink?.timestamp
                    )
                    if (missedFeedback != null) {
                        Text(
                            missedFeedback,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (state.settings.showMotivational) {
                        Text(
                            motivationalMessage(state.totalMl, state.effectiveGoalMl, state.streak.currentStreakDays),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Recent", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (todayDisplayed.isNotEmpty() && !state.settings.minimalistMode) {
                    TextButton(onClick = { confirmClearToday = true }) {
                        Text("Clear today")
                    }
                }
                if (state.today.size > 3) {
                    TextButton(onClick = history, modifier = Modifier.testTag("view-all-water")) {
                        Text("View all (${state.today.size})")
                    }
                }
            }
        }
        if (todayDisplayed.isEmpty()) {
            item {
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    HydroEmptyState("No water yet", "Tap an amount to start.")
                }
            }
        }
        items(todayDisplayed.take(3), key = { it.id }) {
            HydroActivityRow(
                entry = it,
                unit = state.settings.unit,
                zone = state.zone,
                edit = { edit(it) },
                delete = { delete(it) },
                showDate = false
            )
        }
    }

    if (showThemeStudio) {
        QuickThemeStudioSheet(
            settings = state.settings,
            onSave = onSaveSettings,
            onDismiss = { showThemeStudio = false }
        )
    }

    if (confirmClearToday) {
        AlertDialog(
            onDismissRequest = { confirmClearToday = false },
            title = { Text("Clear today's timeline?") },
            text = { Text("This removes today's logged entries from your timeline. You can immediately tap Undo in the snackbar if needed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearToday = false
                    onClearToday()
                }) { Text("Clear today") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearToday = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun HydroActivityRow(
    entry: DrinkEntry,
    unit: VolumeUnit,
    zone: ZoneId,
    edit: () -> Unit,
    delete: () -> Unit,
    showDate: Boolean = true
) {
    var menu by remember { mutableStateOf(false) }
    val titleText = if (entry.isWater()) {
        unit.format(entry.amountMl.toLong())
    } else {
        "${entry.name} · ${unit.format(entry.amountMl.toLong())}"
    }
    Surface(
        Modifier
            .fillMaxWidth()
            .testTag("drink:${entry.id}"),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WaterBadge(category = entry.category)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(titleText, style = MaterialTheme.typography.titleMedium)
                    Text(
                        Instant.ofEpochMilli(entry.timestamp).atZone(zone).format(
                            if (showDate) DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                            else DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(
                        onClick = { menu = true },
                        modifier = Modifier.testTag("actions:${entry.id}")
                    ) {
                        Icon(HydroIcons.More, "Actions for water")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit water") },
                            onClick = {
                                menu = false
                                edit()
                            },
                            modifier = Modifier.testTag("edit:${entry.id}")
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menu = false
                                delete()
                            },
                            modifier = Modifier.testTag("delete:${entry.id}")
                        )
                    }
                }
            }
            if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

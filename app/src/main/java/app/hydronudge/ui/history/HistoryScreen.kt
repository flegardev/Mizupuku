package app.hydronudge.ui.history

import android.animation.ValueAnimator
import android.app.DatePickerDialog
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hydronudge.R
import app.hydronudge.data.local.DrinkEntry
import app.hydronudge.domain.*
import app.hydronudge.ui.HydroState
import app.hydronudge.ui.components.*
import app.hydronudge.ui.dashboard.HydroActivityRow
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    state: HydroState,
    edit: (DrinkEntry) -> Unit,
    delete: (DrinkEntry) -> Unit
) {
    val context = LocalContext.current
    var range by rememberSaveable { mutableStateOf("7 days") }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var customStartEpochDay by rememberSaveable { mutableLongStateOf(state.date.minusDays(13).toEpochDay()) }
    var customEndEpochDay by rememberSaveable { mutableLongStateOf(state.date.toEpochDay()) }
    var chartMode by rememberSaveable { mutableStateOf("Bars") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var entryFilter by rememberSaveable { mutableStateOf("All") }
    var showStatsHelp by rememberSaveable { mutableStateOf(false) }

    val month = YearMonth.from(state.date).plusMonths(monthOffset.toLong())
    val customStart = LocalDate.ofEpochDay(customStartEpochDay)
    val customEnd = LocalDate.ofEpochDay(customEndEpochDay).let { if (it.isBefore(customStart)) customStart else it }

    val start = when (range) {
        "Today" -> state.date
        "30 days" -> state.date.minusDays(29)
        "Month" -> month.atDay(1)
        "Year" -> state.date.minusDays(364)
        "Custom" -> customStart
        else -> state.date.minusDays(6)
    }
    val end = when {
        range == "Month" && monthOffset < 0 -> month.atEndOfMonth()
        range == "Custom" -> customEnd
        else -> state.date
    }

    val effectiveGoal = state.effectiveGoalMl
    val report = remember(state.allEntries, start, end, state.zone, effectiveGoal, state.settings.dayStartMinute) {
        historyReport(
            records = state.allEntries,
            start = start,
            end = end,
            zone = state.zone,
            goalMl = effectiveGoal,
            dayStartMinute = state.settings.dayStartMinute
        )
    }
    val entries = if (state.settings.beverageTrackingEnabled) {
        val wStart = dayWindow(start, state.zone, state.settings.dayStartMinute).start
        val wEnd = dayWindow(end, state.zone, state.settings.dayStartMinute).endExclusive
        state.allEntries.filter { it.timestamp >= wStart && it.timestamp < wEnd }
    } else {
        report.entries
    }
    val total = report.totalMl
    val bars = report.dailyMl
    val milestones = remember(state.allEntries, state.streak, effectiveGoal, state.zone, state.settings.dayStartMinute) {
        computeMilestones(state.allEntries, state.streak, effectiveGoal, state.zone, state.settings.dayStartMinute)
    }

    val reveal = remember { Animatable(1f) }
    val animate = ValueAnimator.areAnimatorsEnabled() && !state.settings.reducedMotion
    LaunchedEffect(start, end) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(if (animate) 260 else 0))
    }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant

    LazyColumn(
        modifier = Modifier.testTag("history-list"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Text("History", style = MaterialTheme.typography.headlineLarge) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Today", "7 days", "30 days", "Month", "Year", "Custom").forEach { r ->
                    FilterChip(
                        modifier = Modifier.testTag("history-range:$r"),
                        selected = range == r,
                        onClick = { range = r },
                        label = { Text(r) }
                    )
                }
            }
        }
        if (range == "Month") {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { monthOffset-- }) { Text("Previous month") }
                    Text(month.toString(), Modifier.padding(12.dp))
                    OutlinedButton(onClick = { monthOffset++ }, enabled = monthOffset < 0) { Text("Next month") }
                }
            }
        }
        if (range == "Custom") {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> customStartEpochDay = LocalDate.of(y, m + 1, d).toEpochDay() },
                            customStart.year,
                            customStart.monthValue - 1,
                            customStart.dayOfMonth
                        ).show()
                    }) { Text("From $customStart") }
                    OutlinedButton(onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> customEndEpochDay = LocalDate.of(y, m + 1, d).toEpochDay() },
                            customEnd.year,
                            customEnd.monthValue - 1,
                            customEnd.dayOfMonth
                        ).show()
                    }) { Text("To $customEnd") }
                }
            }
        }
        item {
            OceanCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "TOTAL WATER",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(state.settings.unit.format(total), style = MaterialTheme.typography.displayLarge)
                    Text(
                        if (report.recordedDays == 0) "Daily average available after your first record"
                        else stringResource(
                            R.string.history_average,
                            pluralStringResource(R.plurals.recorded_days, report.recordedDays, report.recordedDays),
                            state.settings.unit.format(report.averageRecordedDayMl)
                        )
                    )
                    Text(
                        if (effectiveGoal > 0) stringResource(
                            R.string.history_goal_days,
                            pluralStringResource(R.plurals.recorded_days, report.goalDays, report.goalDays)
                        ) else "Set an optional goal to view goal days"
                    )
                    if (!state.settings.minimalistMode) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OceanStatPill(
                                label = "Consistency",
                                value = "${report.consistencyPercent}% (${report.recordedDays}/${report.calendarDays}d)",
                                modifier = Modifier.weight(1f)
                            )
                            OceanStatPill(
                                label = "Score",
                                value = "${report.hydrationScore}/100",
                                modifier = Modifier.weight(1f)
                            )
                            if (state.settings.showStreaks) {
                                OceanStatPill(
                                    label = "Streaks",
                                    value = "${state.streak.currentStreakDays}d · ${state.streak.longestStreakDays}d",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (report.mostFrequentServingMl != null) {
                            Text(
                                "Most frequent serving: ${state.settings.unit.format(report.mostFrequentServingMl.toLong())} · Peak day: ${state.settings.unit.format(report.peakDayMl)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = { showStatsHelp = true }) { Text("About these stats") }
                }
            }
        }
        item {
            OceanCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily water", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Bars", "Trend", "Hourly").forEach { mode ->
                            FilterChip(
                                selected = chartMode == mode,
                                onClick = { chartMode = mode },
                                label = { Text(mode) }
                            )
                        }
                    }
                }
                Text(
                    if (chartMode == "Hourly") {
                        "Hourly rhythm (00:00 to 23:00) · Peak hour: ${
                            state.settings.unit.format(report.hourlyMl.maxOrNull() ?: 0L)
                        }"
                    } else {
                        "Daily water · 0 to ${
                            state.settings.unit.format(
                                maxOf(
                                    effectiveGoal.toLong(),
                                    bars.maxOrNull() ?: 0
                                )
                            )
                        }"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .semantics {
                            contentDescription = "Daily logged volumes, oldest to newest: ${
                                bars.joinToString { state.settings.unit.format(it) }
                            }"
                        }
                ) {
                    val activeSeries = if (chartMode == "Hourly") report.hourlyMl else bars
                    val max = if (chartMode == "Hourly") {
                        (activeSeries.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
                    } else {
                        maxOf(effectiveGoal.toLong(), activeSeries.maxOrNull() ?: 1).coerceAtLeast(1L).toFloat()
                    }
                    for (step in 1..3) {
                        drawLine(
                            track,
                            Offset(0f, size.height * step / 3),
                            Offset(size.width, size.height * step / 3),
                            1.dp.toPx()
                        )
                    }
                    if (chartMode != "Hourly" && effectiveGoal > 0) {
                        val goalY = size.height - (size.height * (effectiveGoal / max)).coerceIn(0f, size.height)
                        drawLine(
                            primary.copy(alpha = 0.45f),
                            Offset(0f, goalY),
                            Offset(size.width, goalY),
                            1.5.dp.toPx()
                        )
                    }
                    if (chartMode == "Trend" && activeSeries.size > 1) {
                        val path = Path()
                        val fillPath = Path()
                        activeSeries.forEachIndexed { i, ml ->
                            val x = if (activeSeries.size == 1) size.width / 2f else size.width * i / (activeSeries.size - 1)
                            val y = size.height - (size.height * (ml / max) * reveal.value)
                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, size.height)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                            if (i == activeSeries.lastIndex) {
                                fillPath.lineTo(x, size.height)
                                fillPath.close()
                            }
                        }
                        drawPath(
                            fillPath,
                            Brush.verticalGradient(listOf(primary.copy(alpha = 0.28f), Color.Transparent))
                        )
                        drawPath(
                            path,
                            Brush.horizontalGradient(listOf(secondary, primary)),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    } else {
                        val width = size.width / activeSeries.size.coerceAtLeast(1)
                        val gap = if (activeSeries.size > 15) 2.dp.toPx() else 7.dp.toPx()
                        activeSeries.forEachIndexed { i, ml ->
                            val h = size.height * ml / max * reveal.value
                            if (h > 0) {
                                drawRoundRect(
                                    Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.4f))),
                                    Offset(i * width + gap / 2, size.height - h),
                                    Size((width - gap).coerceAtLeast(1f), h),
                                    CornerRadius(5.dp.toPx())
                                )
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (chartMode == "Hourly") "00:00" else start.toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (chartMode == "Hourly") {
                        Text("12:00", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        if (chartMode == "Hourly") "23:00" else end.toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (entries.isEmpty()) {
                    Text(
                        "Your first water entry will bring this chart to life.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Calendar Heatmap for multi-day periods (accessible with color-independent symbols)
        if (!state.settings.minimalistMode && report.dailyDates.size in 2..35) {
            item {
                OceanCard(Modifier.fillMaxWidth()) {
                    Text("Calendar heatmap", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Color-independent indicators: ✓ = goal met, • = logged, – = no record",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        report.dailyDates.forEachIndexed { idx, day ->
                            val ml = report.dailyMl.getOrElse(idx) { 0L }
                            val metGoal = effectiveGoal > 0 && ml >= effectiveGoal
                            val symbol = when {
                                metGoal -> "✓"
                                ml > 0L -> "•"
                                else -> "–"
                            }
                            val alpha = when {
                                metGoal -> 0.35f
                                ml > 0L -> 0.16f
                                else -> 0.04f
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                border = BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = if (ml > 0L) 0.3f else 0.1f)
                                ),
                                modifier = Modifier
                                    .widthIn(min = 44.dp)
                                    .semantics {
                                        contentDescription = "${day}: ${state.settings.unit.format(ml)}"
                                    }
                            ) {
                                Column(
                                    Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "${day.dayOfMonth}",
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        symbol,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Beverage breakdown when optional beverage categories or legacy non-water records exist
        if (report.beverageBreakdown.size > 1 || (state.settings.beverageTrackingEnabled && report.beverageBreakdown.isNotEmpty())) {
            item {
                OceanCard(Modifier.fillMaxWidth()) {
                    Text("Beverage breakdown", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Total beverage volume: ${state.settings.unit.format(report.totalBeverageMl)} · Plain water: ${state.settings.unit.format(report.totalMl)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    report.beverageBreakdown.forEach { (catCode, volumeMl) ->
                        val cat = DrinkCategory.fromCode(catCode)
                        val title = cat?.displayTitle ?: catCode
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(title, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                state.settings.unit.format(volumeMl),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Personal Milestones
        if (!state.settings.minimalistMode) {
            item {
                OceanCard(Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(HydroIcons.Sparkle, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Personal milestones", style = MaterialTheme.typography.titleMedium)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        milestones.forEach { badge ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (badge.achieved) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = if (badge.achieved) 0.4f else 0.1f)
                                )
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        if (badge.achieved) "✓ ${badge.title}" else badge.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (badge.achieved) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val filteredEntries = entries.filter { item ->
            val matchesFilter = when (entryFilter) {
                "≥ 500 mL" -> item.amountMl >= 500
                "With notes" -> item.note.isNotBlank()
                else -> true
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                item.name.contains(q, ignoreCase = true) ||
                item.note.contains(q, ignoreCase = true) ||
                item.amountMl.toString().contains(q)
            matchesFilter && matchesQuery
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HydroSectionHeader("Water entries")
                if (entries.size > 3 && !state.settings.minimalistMode) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search notes or volume") },
                        leadingIcon = { Icon(HydroIcons.Search, null, Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history-search")
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "≥ 500 mL", "With notes").forEach { f ->
                            FilterChip(
                                selected = entryFilter == f,
                                onClick = { entryFilter = f },
                                label = { Text(f) }
                            )
                        }
                    }
                }
            }
        }
        if (filteredEntries.isEmpty()) item { HydroEmptyState("No entries", "Try another period.") }
        items(filteredEntries, key = { it.id }) {
            HydroActivityRow(it, state.settings.unit, state.zone, { edit(it) }, { delete(it) })
        }
    }

    if (showStatsHelp) {
        AlertDialog(
            onDismissRequest = { showStatsHelp = false },
            title = { Text("About these stats") },
            text = {
                Text("Averages use days with water records. Blank days mean no records, not confirmed zero intake. Goal days use your current goal, including when you review older entries.")
            },
            confirmButton = {
                TextButton(onClick = { showStatsHelp = false }) { Text("Close") }
            }
        )
    }
}

package app.hydronudge.ui.drinks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.hydronudge.data.local.*
import app.hydronudge.data.preferences.HydroSettings
import app.hydronudge.domain.*
import app.hydronudge.ui.components.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DrinkEditor(
    settings: HydroSettings,
    entry: DrinkEntry?,
    onDismiss: () -> Unit,
    onSave: (DrinkEntry) -> Unit,
    onSaveAsFavorite: (FavoriteDrink) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedCategoryName by rememberSaveable {
        mutableStateOf(
            if (settings.beverageTrackingEnabled) (entry?.category ?: DrinkCategory.WATER.name)
            else DrinkCategory.WATER.name
        )
    }
    val selectedCategory = DrinkCategory.fromCode(selectedCategoryName) ?: DrinkCategory.WATER
    var customName by rememberSaveable {
        mutableStateOf(
            if (settings.beverageTrackingEnabled) (entry?.name ?: selectedCategory.displayTitle)
            else "Water"
        )
    }
    val name = if (settings.beverageTrackingEnabled) customName.ifBlank { selectedCategory.displayTitle } else "Water"

    var amount by rememberSaveable { mutableStateOf(settings.unit.input(entry?.amountMl ?: 250)) }
    var unitName by rememberSaveable { mutableStateOf(settings.unit.name) }
    val unit = VolumeUnit.valueOf(unitName)
    var timestamp by rememberSaveable { mutableLongStateOf(entry?.timestamp ?: System.currentTimeMillis()) }
    var note by rememberSaveable { mutableStateOf(entry?.note ?: "") }
    var saveFavoriteToggle by rememberSaveable { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val ml = amount.replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 && it <= 20000 / unit.millilitres }
        ?.let(unit::toMl)
    val nowMs = System.currentTimeMillis()
    val valid = ml != null &&
        ml in 1..20000 &&
        name.isNotBlank() &&
        name.length <= 80 &&
        note.length <= 1000 &&
        timestamp <= nowMs + 5000L
    val time = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())

    var confirmAmount by rememberSaveable { mutableStateOf(false) }
    val saveAction: () -> Unit = {
                    if (valid && !submitted) {
                        submitted = true
                        val catCode = if (settings.beverageTrackingEnabled) selectedCategory.name else DrinkCategory.WATER.name
                        val finalName = if (settings.beverageTrackingEnabled) name.trim() else "Water"
                        val clampedTs = minOf(timestamp, System.currentTimeMillis())
                        if (saveFavoriteToggle && entry == null) {
                            onSaveAsFavorite(
                                FavoriteDrink(
                                    name = finalName,
                                    category = catCode,
                                    amountMl = ml
                                )
                            )
                        }
                        onSave(
                            DrinkEntry(
                                id = entry?.id ?: UUID.randomUUID().toString(),
                                name = finalName,
                                category = catCode,
                                amountMl = ml,
                                timestamp = clampedTs,
                                note = note.trim(),
                                enteredUnit = unit.name
                            )
                        )
                    }
    }
    if (confirmAmount) {
        AlertDialog(
            onDismissRequest = { confirmAmount = false },
            title = { Text("Check the amount") },
            text = { Text("You entered ${unit.format((ml ?: 0).toLong())}. Save this as one entry?") },
            confirmButton = { TextButton(onClick = { confirmAmount = false; saveAction() }) { Text("Save entry") } },
            dismissButton = { TextButton(onClick = { confirmAmount = false }) { Text("Review amount") } }
        )
    }

    HydroBottomSheet(onDismiss) {
        Column(
            Modifier
                .testTag("drink-editor")
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "YOUR DAILY RITUAL",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (entry == null) "Add water" else "Edit water",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    IconButton(onClick = onDismiss) { Icon(HydroIcons.Close, "Close water editor") }
                }

                OutlinedTextField(
                    amount,
                    { amount = it },
                    textStyle = MaterialTheme.typography.displayLarge,
                    label = { Text("Water amount (${unit.label})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = ml == null || ml !in 1..20000,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drink-amount")
                )

                // Cup & bottle presets
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultCupPresets.forEach { preset ->
                        SuggestionChip(
                            onClick = { amount = unit.input(preset.amountMl) },
                            label = { Text("${preset.label} · ${unit.format(preset.amountMl.toLong())}") }
                        )
                    }
                }

                if (ml == null || ml !in 1..20000) {
                    Text(
                        "Enter an amount between 1 and 20,000 mL.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (ml > settings.accidentalEntryLimitMl) {
                    Text(
                        "Large single entry (${unit.format(ml.toLong())}). Verify this volume is intentional and avoid excessive rapid fluid intake.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VolumeUnit.entries.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = {
                                if (ml != null) amount = u.input(ml)
                                unitName = u.name
                            },
                            label = { Text(u.label) }
                        )
                    }
                }

                if (settings.beverageTrackingEnabled) {
                    Text(
                        "Beverage category (optional)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DrinkCategory.entries.forEach { cat ->
                            FilterChip(
                                modifier = Modifier.testTag("category:${cat.name}"),
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategoryName = cat.name
                                    customName = cat.displayTitle
                                    if (entry == null) amount = unit.input(cat.defaultServingMl)
                                },
                                label = { Text("${cat.symbol} ${cat.displayTitle}") }
                            )
                        }
                    }
                    OutlinedTextField(
                        customName,
                        { customName = it.take(80) },
                        label = { Text("Beverage name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drink-name")
                    )
                    Text(
                        "Plain water counts toward your water total. Other beverages are tracked in total beverage volume and optional non-medical hydration estimates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)),
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                timestamp = time.withDayOfMonth(1).withYear(y).withMonth(m + 1)
                                    .withDayOfMonth(d).toInstant().toEpochMilli()
                            },
                            time.year,
                            time.monthValue - 1,
                            time.dayOfMonth
                        ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                    }) { Text("Change date") }
                    TextButton(onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                timestamp = time.withHour(h).withMinute(m).withSecond(0).toInstant().toEpochMilli()
                            },
                            time.hour,
                            time.minute,
                            DateFormat.is24HourFormat(context)
                        ).show()
                    }) { Text("Change time") }
                    SuggestionChip(
                        onClick = { timestamp = System.currentTimeMillis() - 15 * 60_000L },
                        label = { Text("15m ago") }
                    )
                    SuggestionChip(
                        onClick = { timestamp = System.currentTimeMillis() - 60 * 60_000L },
                        label = { Text("1h ago") }
                    )
                }
                if (timestamp > System.currentTimeMillis() + 5000L) {
                    Text("Choose a time in the past.", color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    note,
                    { note = it.take(1000) },
                    label = { Text("Note (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("water-note")
                )

                if (entry == null) {
                    FilterChip(
                        selected = saveFavoriteToggle,
                        onClick = { saveFavoriteToggle = !saveFavoriteToggle },
                        label = { Text("Also save as favorite shortcut") },
                        leadingIcon = { Icon(HydroIcons.Star, null, Modifier.size(16.dp)) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HydroPrimaryButton(
                onClick = {
                    if (ml != null && ml > 1000 && ml != entry?.amountMl) confirmAmount = true else saveAction()
                },
                enabled = valid && !submitted,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("save-drink")
            ) { Text("Save water") }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

package app.hydronudge.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.hydronudge.data.BackupPreview
import app.hydronudge.data.preferences.*
import app.hydronudge.domain.VolumeUnit
import app.hydronudge.domain.parseQuickAmounts
import app.hydronudge.ui.components.*
import app.hydronudge.ui.theme.CustomAccentSwatches
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: HydroSettings,
    save: (HydroSettings) -> Unit,
    clear: () -> Unit,
    export: (Uri) -> Unit,
    import: (Uri) -> Unit,
    exportCsv: (Uri) -> Unit = {},
    inspectBackup: (Uri, (BackupPreview) -> Unit) -> Unit = { _, _ -> }
) {
    var goal by rememberSaveable(settings.goalMl, settings.unit) {
        mutableStateOf(if (settings.goalMl > 0) settings.unit.input(settings.goalMl) else "")
    }
    var weekendGoal by rememberSaveable(settings.weekendGoalMl, settings.unit) {
        mutableStateOf(if (settings.weekendGoalMl > 0) settings.unit.input(settings.weekendGoalMl) else "")
    }
    var quick by rememberSaveable(settings.quickAmounts, settings.unit) {
        mutableStateOf(settings.quickAmounts.joinToString(", ") { settings.unit.input(it) })
    }
    var widgetQuick by rememberSaveable(settings.widgetQuickAmounts, settings.unit) {
        mutableStateOf(settings.widgetQuickAmounts.joinToString(", ") { settings.unit.input(it) })
    }
    var confirm by rememberSaveable { mutableStateOf(false) }
    var confirmImport by rememberSaveable { mutableStateOf(false) }
    var backupPreview by remember { mutableStateOf<BackupPreview?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var legalDocument by rememberSaveable { mutableStateOf<String?>(null) }
    var help by rememberSaveable { mutableStateOf<String?>(null) }

    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { it?.let(export) }

    val csvExportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { it?.let(exportCsv) }

    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { it?.let(import) }

    val previewPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            inspectBackup(uri) { preview -> backupPreview = preview }
        }
    }

    var tileAmount by rememberSaveable(settings.tileAmountMl, settings.unit) {
        mutableStateOf(settings.unit.input(settings.tileAmountMl))
    }
    val tileMl = tileAmount.replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 && it <= 20000 / settings.unit.millilitres }
        ?.let(settings.unit::toMl)
    val goalMl = goal.replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 && it <= 20000 / settings.unit.millilitres }
        ?.let(settings.unit::toMl)
    val weekendGoalMl = weekendGoal.replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 && it <= 20000 / settings.unit.millilitres }
        ?.let(settings.unit::toMl)
    val amounts = parseQuickAmounts(quick, settings.unit)
    val widgetAmounts = widgetQuick.split(',').takeIf { it.size == 2 }?.mapNotNull { token ->
        val v = token.trim().toDoubleOrNull() ?: return@mapNotNull null
        if (!v.isFinite() || v <= 0 || v > 20000 / settings.unit.millilitres) return@mapNotNull null
        settings.unit.toMl(v).takeIf { it in 1..20000 }
    }?.takeIf { it.size == 2 }

    LazyColumn(
        modifier = Modifier.testTag("settings-list"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge) }

        item {
            HydroSettingsGroup("Daily goal", HydroIcons.Drop) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VolumeUnit.entries.forEach { u ->
                        FilterChip(
                            selected = settings.unit == u,
                            onClick = {
                                save(settings.copy(unit = u))
                                goal = if (settings.goalMl > 0) u.input(settings.goalMl) else ""
                                quick = settings.quickAmounts.joinToString(", ") { u.input(it) }
                                widgetQuick = settings.widgetQuickAmounts.joinToString(", ") { u.input(it) }
                            },
                            label = { Text(u.label) }
                        )
                    }
                }
                OutlinedTextField(
                    goal,
                    { goal = it },
                    label = { Text("Daily water goal (${settings.unit.label})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = goalMl == null || goalMl !in 100..20000,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily-goal")
                )
                Text(
                    "A goal is optional and is not medical advice. Needs vary. For a child, a parent or caregiver should choose any goal with appropriate professional guidance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { goalMl?.let { save(settings.copy(goalMl = it)) } },
                    enabled = goalMl != null && goalMl in 100..20000,
                    modifier = Modifier.testTag("save-goal")
                ) { Text("Save goal") }
                if (settings.goalMl > 0) {
                    TextButton(
                        onClick = {
                            save(settings.copy(goalMl = 0))
                            goal = ""
                        },
                        modifier = Modifier.testTag("clear-goal")
                    ) { Text("Remove daily goal") }
                }

                SettingsDisclosure("Habits, schedules & beverage options", "settings:habits") {
                    OutlinedTextField(
                        weekendGoal,
                        { weekendGoal = it },
                        label = { Text("Optional weekend goal (${settings.unit.label})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { weekendGoalMl?.let { save(settings.copy(weekendGoalMl = it)) } },
                            enabled = weekendGoalMl != null && weekendGoalMl in 100..20000
                        ) { Text("Save weekend goal") }
                        if (settings.weekendGoalMl > 0) {
                            TextButton(onClick = {
                                save(settings.copy(weekendGoalMl = 0))
                                weekendGoal = ""
                            }) { Text("Clear weekend goal") }
                        }
                    }

                    SettingToggle("Track other beverages (tea, coffee, milk, sparkling)", settings.beverageTrackingEnabled) {
                        save(settings.copy(beverageTrackingEnabled = it))
                    }
                    SettingToggle("Show hydration streaks", settings.showStreaks) {
                        save(settings.copy(showStreaks = it))
                    }
                    SettingToggle("Show calm motivational messages", settings.showMotivational) {
                        save(settings.copy(showMotivational = it))
                    }
                    SettingToggle("Minimalist tracking mode", settings.minimalistMode) {
                        save(settings.copy(minimalistMode = it))
                    }

                    Text("Large single-entry safeguard warning", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(500, 1000, 1500, 2000).forEach { threshold ->
                            FilterChip(
                                selected = settings.accidentalEntryLimitMl == threshold,
                                onClick = { save(settings.copy(accidentalEntryLimitMl = threshold)) },
                                label = { Text(settings.unit.format(threshold.toLong())) }
                            )
                        }
                    }

                    Text("Quantity rounding for quick-add", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "Exact", 5 to "5 mL", 10 to "10 mL", 25 to "25 mL", 50 to "50 mL").forEach { (step, label) ->
                            FilterChip(
                                selected = settings.roundingStepMl == step,
                                onClick = { save(settings.copy(roundingStepMl = step)) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("Day reset boundary", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "Midnight (00:00)", 120 to "02:00 AM", 240 to "04:00 AM").forEach { (mins, label) ->
                            FilterChip(
                                selected = settings.dayStartMinute == mins,
                                onClick = { save(settings.copy(dayStartMinute = mins)) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        item {
            HydroSettingsGroup("Appearance", HydroIcons.Settings) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { theme ->
                        FilterChip(
                            modifier = Modifier.testTag("theme:${theme.name}"),
                            selected = settings.theme == theme,
                            onClick = { save(settings.copy(theme = theme)) },
                            label = { Text(theme.label) }
                        )
                    }
                }
                SettingToggle("Use device colors (Android 12+)", settings.dynamicColors) {
                    save(settings.copy(dynamicColors = it))
                }

                SettingsDisclosure("Theme Studio & Vessel Styles", "settings:theme-studio") {
                    Text("Pastel & Ocean color themes (100% free)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OceanPalette.entries.forEach { palette ->
                            FilterChip(
                                modifier = Modifier.testTag("palette:${palette.name}"),
                                selected = settings.palette == palette,
                                onClick = {
                                    save(
                                        settings.copy(
                                            palette = palette,
                                            dynamicColors = palette == OceanPalette.MATERIAL_YOU || settings.dynamicColors
                                        )
                                    )
                                },
                                leadingIcon = { ColorSwatchDot(Color(palette.swatchHex)) },
                                label = { Text(palette.title) }
                            )
                        }
                    }
                    Text("Custom accent color", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomAccentSwatches.forEach { swatch ->
                            FilterChip(
                                selected = settings.customAccentHex == swatch.hex,
                                onClick = { save(settings.copy(customAccentHex = swatch.hex)) },
                                leadingIcon = if (swatch.hex != 0L) {
                                    { ColorSwatchDot(Color(swatch.hex)) }
                                } else null,
                                label = { Text(swatch.name) }
                            )
                        }
                    }
                    Text("Dashboard hydration vessel", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VesselStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.vesselStyle == style,
                                onClick = { save(settings.copy(vesselStyle = style)) },
                                label = { Text(style.label) }
                            )
                        }
                    }
                    SettingToggle("Reduced motion", settings.reducedMotion) {
                        save(settings.copy(reducedMotion = it))
                    }
                    SettingToggle("Haptic feedback", settings.hapticsEnabled) {
                        save(settings.copy(hapticsEnabled = it))
                    }
                }
            }
        }

        item { NotificationSettings(settings, save) }

        item {
            HydroSettingsGroup("Quick add & tile", HydroIcons.Plus) {
                SettingsDisclosure("Customize quick add", "settings:quick-add") {
                    OutlinedTextField(
                        quick,
                        { quick = it },
                        label = { Text("Quick-add amounts (${settings.unit.label})") },
                        supportingText = { Text("Four positive amounts, separated by commas. Use a dot for decimals.") },
                        isError = amounts == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { amounts?.let { save(settings.copy(quickAmounts = it)) } },
                        enabled = amounts != null
                    ) { Text("Save quick amounts") }
                }
                SettingsDisclosure("Quick Settings serving", "settings:tile") {
                    OutlinedTextField(
                        tileAmount,
                        { tileAmount = it },
                        label = { Text("Quick Settings serving (${settings.unit.label})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = tileMl == null || tileMl !in 1..20000,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { tileMl?.let { save(settings.copy(tileAmountMl = it)) } },
                        enabled = tileMl != null && tileMl in 1..20000
                    ) { Text("Save tile amount") }
                }
                SettingsDisclosure("Home-screen widget studio", "settings:widget-studio") {
                    Text("Widget layout preset (12 native styles)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WidgetPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = settings.widgetPreset == preset,
                                onClick = { save(settings.copy(widgetPreset = preset)) },
                                label = { Text(preset.title) }
                            )
                        }
                    }
                    Text("Widget surface theme", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WidgetThemeStyle.entries.forEach { wStyle ->
                            FilterChip(
                                selected = settings.widgetThemeStyle == wStyle,
                                onClick = { save(settings.copy(widgetThemeStyle = wStyle)) },
                                label = { Text(wStyle.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        widgetQuick,
                        { widgetQuick = it },
                        label = { Text("Widget quick-log amounts (${settings.unit.label})") },
                        supportingText = { Text("Two positive amounts separated by a comma.") },
                        isError = widgetAmounts == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { widgetAmounts?.let { save(settings.copy(widgetQuickAmounts = it)) } },
                        enabled = widgetAmounts != null
                    ) { Text("Save widget amounts") }
                }
                TextButton(onClick = { help = "home" }) { Text("Widget & tile setup") }
            }
        }

        item {
            HydroSettingsGroup("Data and privacy", HydroIcons.Settings) {
                Text("Local records. No accounts, ads or subscriptions.")
                Text(
                    "Your chosen document provider may use cloud storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportPicker.launch("Mizupuku-${LocalDate.now()}.json") }) {
                        Text("Export full backup")
                    }
                    OutlinedButton(onClick = { confirmImport = true }) {
                        Text("Import backup")
                    }
                    OutlinedButton(onClick = { csvExportPicker.launch("Mizupuku-History-${LocalDate.now()}.csv") }) {
                        Text("Export CSV")
                    }
                    OutlinedButton(onClick = {
                        previewPicker.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    }) {
                        Text("Preview backup")
                    }
                }
                TextButton(onClick = { help = "backup" }) { Text("How backups work") }
                TextButton(onClick = { confirm = true }) {
                    Text("Delete all local data", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            HydroSettingsGroup("About Mizupuku", HydroIcons.Drop) {
                Text("Mizupuku · 1.0.0", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Free offline hydration habit tracker. Not a medical device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { legalDocument = "privacy-policy.txt" }) { Text("Privacy policy") }
                TextButton(onClick = { legalDocument = "third-party-notices.txt" }) { Text("Third-party licenses") }
            }
        }
    }

    help?.let { topic ->
        AlertDialog(
            onDismissRequest = { help = null },
            title = { Text(if (topic == "backup") "How backups work" else "Widget & tile setup") },
            text = {
                Text(
                    if (topic == "backup") "Android cloud backup is disabled. Schema v2 full exports are unencrypted JSON and include water, preserved legacy records, saved servings, and user settings (goals, units, quick amounts, themes, and reminders). For legacy v1 backups without settings, Preferences stay on this device. Only water counts in totals. Import merges new IDs and keeps existing records. Your chosen document provider may use cloud storage. Deleting app data does not remove exported files. Keep a private copy before changing devices."
                    else "Add Mizupuku from your launcher's widget picker. Resize it for water buttons. Add Log water in Android's Quick Settings editor; tapping logs your chosen serving after unlocking.",
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                )
            },
            confirmButton = { TextButton(onClick = { help = null }) { Text("Close") } }
        )
    }

    backupPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = {
                backupPreview = null
                pendingImportUri = null
            },
            title = { Text("Backup restore preview") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Schema version: ${preview.schemaVersion} (valid Mizupuku backup)")
                    Text("Exported at: ${preview.exportedAt}")
                    Text("Total entries in file: ${preview.totalEntries} (${preview.waterEntries} water, ${preview.legacyOrBeverageEntries} other/legacy)")
                    Text("New entries to import: ${preview.newEntries} (${preview.duplicateEntries} already on device)")
                    Text("Saved favorites: ${preview.totalFavorites} (${preview.newFavorites} new)")
                    Text("Total volume in file: ${settings.unit.format(preview.totalVolumeMl)}")
                    Text(
                        if (preview.includesPreferences) "Includes user preferences: Yes (goal: ${preview.previewGoalMl ?: 0} mL, unit: ${preview.previewUnit ?: "mL"})"
                        else "Includes user preferences: No (legacy v1 backup; current settings kept)"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = pendingImportUri
                    backupPreview = null
                    pendingImportUri = null
                    if (target != null) import(target)
                }) { Text("Merge backup now") }
            },
            dismissButton = {
                TextButton(onClick = {
                    backupPreview = null
                    pendingImportUri = null
                }) { Text("Cancel") }
            }
        )
    }

    legalDocument?.let { asset ->
        LegalDocumentDialog(
            if (asset == "privacy-policy.txt") "Privacy policy" else "Third-party licenses",
            asset
        ) { legalDocument = null }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Delete all local data?") },
            text = {
                Text("Water and preserved legacy records will be permanently removed. Preferences will reset and reminders will turn off. Exported files must be deleted separately.")
            },
            confirmButton = {
                TextButton(onClick = {
                    clear()
                    confirm = false
                }) { Text("Delete all data") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Keep my data") }
            }
        )
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Merge a backup?") },
            text = {
                Text("Water, legacy records, and v2 settings will be restored. Only water counts in the app. Records with an existing ID are kept unchanged. Invalid files are rejected before anything is written. Safe import limit: 32 MiB.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    importPicker.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }) { Text("Choose backup") }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) { Text("Cancel") }
            }
        )
    }
}

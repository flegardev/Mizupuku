package app.hydronudge.data

import app.hydronudge.data.local.*
import app.hydronudge.data.preferences.*
import app.hydronudge.domain.VolumeUnit
import app.hydronudge.domain.isWater
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FullHydroBackup(
    val schemaVersion: Int,
    val drinkBackup: DrinkBackup,
    val settings: HydroSettings? = null
)

data class BackupPreview(
    val schemaVersion: Int,
    val exportedAt: String,
    val totalEntries: Int,
    val newEntries: Int,
    val duplicateEntries: Int,
    val waterEntries: Int,
    val legacyOrBeverageEntries: Int,
    val totalFavorites: Int,
    val newFavorites: Int,
    val totalVolumeMl: Long,
    val includesPreferences: Boolean = false,
    val previewGoalMl: Int? = null,
    val previewUnit: String? = null
)

/**
 * Portable, backward-compatible backup codec:
 * - Schema version 1: entries + favorites (legacy drink backup).
 * - Schema version 2: entries + favorites + full user preferences (goals, quick amounts,
 *   units, appearance/themes, and reminder schedules).
 * Volumes are integer mL, timestamps UTC epoch milliseconds. IDs survive merges.
 */
object BackupCodec {
    private fun integer(value: JSONObject, key: String): Long {
        val number = value.get(key)
        require(number is Int || number is Long) { "$key must be an integer." }
        return (number as Number).toLong()
    }

    private fun amount(value: JSONObject): Int {
        val ml = integer(value, "amountMl")
        require(ml in 1L..20000L) { "Invalid backup volume." }
        return ml.toInt()
    }

    private fun encodeEntriesAndFavorites(root: JSONObject, backup: DrinkBackup) {
        root.put("entries", JSONArray().apply {
            backup.entries.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("name", e.name)
                    put("category", e.category)
                    put("amountMl", e.amountMl)
                    put("timestamp", e.timestamp)
                    put("note", e.note)
                    put("enteredUnit", e.enteredUnit)
                })
            }
        })
        root.put("favorites", JSONArray().apply {
            backup.favorites.forEach { f ->
                put(JSONObject().apply {
                    put("id", f.id)
                    put("name", f.name)
                    put("category", f.category)
                    put("amountMl", f.amountMl)
                })
            }
        })
    }

    private fun encodeSettingsObject(s: HydroSettings): JSONObject = JSONObject().apply {
        put("goalMl", s.goalMl)
        put("unit", s.unit.name)
        put("quickAmounts", JSONArray(s.quickAmounts))
        put("theme", s.theme.name)
        put("dynamicColors", s.dynamicColors)
        put("reminders", s.reminders)
        put("intervalMinutes", s.intervalMinutes)
        put("wakeMinute", s.wakeMinute)
        put("sleepMinute", s.sleepMinute)
        put("style", s.style.name)
        put("sound", s.sound)
        put("vibration", s.vibration)
        put("skipAtGoal", s.skipAtGoal)
        put("pausedUntil", s.pausedUntil)
        put("statusNotification", s.statusNotification)
        put("statusDismissed", s.statusDismissed)
        put("tileAmountMl", s.tileAmountMl)
        put("palette", s.palette.name)
        put("customAccentHex", s.customAccentHex)
        put("vesselStyle", s.vesselStyle.name)
        put("reducedMotion", s.reducedMotion)
        put("hapticsEnabled", s.hapticsEnabled)
        put("soundEffects", s.soundEffects)
        put("minimalistMode", s.minimalistMode)
        put("showStreaks", s.showStreaks)
        put("showMotivational", s.showMotivational)
        put("celebrationsEnabled", s.celebrationsEnabled)
        put("beverageTrackingEnabled", s.beverageTrackingEnabled)
        put("dayStartMinute", s.dayStartMinute)
        put("roundingStepMl", s.roundingStepMl)
        put("accidentalEntryLimitMl", s.accidentalEntryLimitMl)
        put("weekendGoalMl", s.weekendGoalMl)
        put("weeklyGoalMl", s.weeklyGoalMl)
        put("adaptiveReminders", s.adaptiveReminders)
        put("activeDaysCsv", s.activeDaysCsv)
        put("weekendScheduleEnabled", s.weekendScheduleEnabled)
        put("weekendWakeMinute", s.weekendWakeMinute)
        put("weekendSleepMinute", s.weekendSleepMinute)
        put("weekendIntervalMinutes", s.weekendIntervalMinutes)
        put("quietHoursEnabled", s.quietHoursEnabled)
        put("quietStartMinute", s.quietStartMinute)
        put("quietEndMinute", s.quietEndMinute)
        put("customReminderMessage", s.customReminderMessage)
        put("widgetPreset", s.widgetPreset.name)
        put("widgetThemeStyle", s.widgetThemeStyle.name)
        put("widgetQuickAmounts", JSONArray(s.widgetQuickAmounts))
    }

    private fun decodeSettingsObject(obj: JSONObject): HydroSettings {
        val goalMl = integer(obj, "goalMl").toInt()
        require(goalMl == 0 || goalMl in 100..20000) { "Invalid backup goalMl." }

        val unitName = obj.getString("unit")
        val unit = VolumeUnit.entries.firstOrNull { it.name == unitName }
            ?: throw IllegalArgumentException("Invalid backup unit: $unitName")

        val quickArr = obj.getJSONArray("quickAmounts")
        require(quickArr.length() == 4) { "Backup quickAmounts must contain exactly 4 values." }
        val quickAmounts = List(4) { idx ->
            val v = quickArr.get(idx)
            require(v is Int || v is Long) { "Quick amount must be an integer." }
            val ml = (v as Number).toInt()
            require(ml in 1..20000) { "Quick amount out of range." }
            ml
        }

        val themeName = obj.getString("theme")
        val theme = ThemeMode.entries.firstOrNull { it.name == themeName }
            ?: throw IllegalArgumentException("Invalid backup theme: $themeName")

        val intervalMinutes = integer(obj, "intervalMinutes").toInt()
        val wakeMinute = integer(obj, "wakeMinute").toInt()
        val sleepMinute = integer(obj, "sleepMinute").toInt()
        require(intervalMinutes in 15..1440 && wakeMinute in 0..1439 && sleepMinute in 0..1439) {
            "Invalid backup reminder schedule."
        }

        val styleName = obj.getString("style")
        val style = ReminderStyle.entries.firstOrNull { it.name == styleName }
            ?: throw IllegalArgumentException("Invalid backup reminder style: $styleName")

        val tileAmountMl = integer(obj, "tileAmountMl").toInt()
        require(tileAmountMl in 1..20000) { "Invalid backup tileAmountMl." }

        val widgetQuickArr = obj.optJSONArray("widgetQuickAmounts")
        val widgetQuickAmounts = if (widgetQuickArr != null) {
            require(widgetQuickArr.length() == 2) { "Backup widgetQuickAmounts must contain 2 values." }
            List(2) { idx ->
                val v = widgetQuickArr.get(idx)
                require(v is Int || v is Long) { "Widget quick amount must be an integer." }
                val ml = (v as Number).toInt()
                require(ml in 1..20000) { "Widget quick amount out of range." }
                ml
            }
        } else listOf(250, 500)

        val weekendGoalMl = obj.optInt("weekendGoalMl", 0)
        require(weekendGoalMl == 0 || weekendGoalMl in 100..20000) { "Invalid weekendGoalMl." }
        val weeklyGoalMl = obj.optInt("weeklyGoalMl", 0)
        require(weeklyGoalMl == 0 || weeklyGoalMl in 500..140000) { "Invalid weeklyGoalMl." }

        val palette = OceanPalette.entries.firstOrNull { it.name == obj.optString("palette", "OCEAN_BLUE") }
            ?: OceanPalette.OCEAN_BLUE
        val vesselStyle = VesselStyle.entries.firstOrNull { it.name == obj.optString("vesselStyle", "WAVE_RING") }
            ?: VesselStyle.WAVE_RING
        val widgetPreset = WidgetPreset.entries.firstOrNull { it.name == obj.optString("widgetPreset", "DAILY_SUMMARY") }
            ?: WidgetPreset.DAILY_SUMMARY
        val widgetThemeStyle = WidgetThemeStyle.entries.firstOrNull { it.name == obj.optString("widgetThemeStyle", "OCEAN_GRADIENT") }
            ?: WidgetThemeStyle.OCEAN_GRADIENT

        return HydroSettings(
            goalMl = goalMl,
            unit = unit,
            quickAmounts = quickAmounts,
            theme = theme,
            dynamicColors = obj.optBoolean("dynamicColors", false),
            reminders = obj.optBoolean("reminders", false),
            intervalMinutes = intervalMinutes,
            wakeMinute = wakeMinute,
            sleepMinute = sleepMinute,
            style = style,
            sound = obj.optBoolean("sound", false),
            vibration = obj.optBoolean("vibration", false),
            skipAtGoal = obj.optBoolean("skipAtGoal", true),
            pausedUntil = obj.optLong("pausedUntil", 0L),
            statusNotification = obj.optBoolean("statusNotification", false),
            statusDismissed = obj.optBoolean("statusDismissed", false),
            tileAmountMl = tileAmountMl,
            palette = palette,
            customAccentHex = obj.optLong("customAccentHex", 0L),
            vesselStyle = vesselStyle,
            reducedMotion = obj.optBoolean("reducedMotion", false),
            hapticsEnabled = obj.optBoolean("hapticsEnabled", true),
            soundEffects = obj.optBoolean("soundEffects", false),
            minimalistMode = obj.optBoolean("minimalistMode", false),
            showStreaks = obj.optBoolean("showStreaks", true),
            showMotivational = obj.optBoolean("showMotivational", true),
            celebrationsEnabled = obj.optBoolean("celebrationsEnabled", true),
            beverageTrackingEnabled = obj.optBoolean("beverageTrackingEnabled", false),
            dayStartMinute = obj.optInt("dayStartMinute", 0).coerceIn(0, 720),
            roundingStepMl = obj.optInt("roundingStepMl", 0).coerceIn(0, 250),
            accidentalEntryLimitMl = obj.optInt("accidentalEntryLimitMl", 2000).coerceIn(500, 20000),
            weekendGoalMl = weekendGoalMl,
            weeklyGoalMl = weeklyGoalMl,
            adaptiveReminders = obj.optBoolean("adaptiveReminders", false),
            activeDaysCsv = obj.optString("activeDaysCsv", "1,2,3,4,5,6,7").ifBlank { "1,2,3,4,5,6,7" },
            weekendScheduleEnabled = obj.optBoolean("weekendScheduleEnabled", false),
            weekendWakeMinute = obj.optInt("weekendWakeMinute", 540).coerceIn(0, 1439),
            weekendSleepMinute = obj.optInt("weekendSleepMinute", 1320).coerceIn(0, 1439),
            weekendIntervalMinutes = obj.optInt("weekendIntervalMinutes", 90).coerceIn(15, 1440),
            quietHoursEnabled = obj.optBoolean("quietHoursEnabled", false),
            quietStartMinute = obj.optInt("quietStartMinute", 780).coerceIn(0, 1439),
            quietEndMinute = obj.optInt("quietEndMinute", 840).coerceIn(0, 1439),
            customReminderMessage = obj.optString("customReminderMessage", "").take(160),
            widgetPreset = widgetPreset,
            widgetThemeStyle = widgetThemeStyle,
            widgetQuickAmounts = widgetQuickAmounts
        )
    }

    /** Schema version 1 encoding (entries + favorites). Preserves exact existing unit test contracts. */
    fun encode(backup: DrinkBackup): String = JSONObject().apply {
        put("format", "HydroNudge")
        put("schemaVersion", 1)
        put("exportedAt", Instant.now().toString())
        encodeEntriesAndFavorites(this, backup)
    }.toString(2)

    /** Schema version 2 full backup encoding (entries + favorites + user preferences). */
    fun encodeFull(backup: DrinkBackup, settings: HydroSettings): String = JSONObject().apply {
        put("format", "HydroNudge")
        put("schemaVersion", 2)
        put("exportedAt", Instant.now().toString())
        encodeEntriesAndFavorites(this, backup)
        put("settings", encodeSettingsObject(settings))
    }.toString(2)

    /** Decodes either schemaVersion 1 (records only) or schemaVersion 2 (records + settings). */
    fun decodeFull(json: String): FullHydroBackup {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Malformed backup JSON.", e)
        }
        val format = if (root.has("format")) root.getString("format") else ""
        val schemaVersion = if (root.has("schemaVersion")) integer(root, "schemaVersion") else -1L
        require(format == "HydroNudge" && (schemaVersion == 1L || schemaVersion == 2L)) {
            "This is not a supported HydroNudge backup."
        }
        if (schemaVersion == 2L) {
            require(root.has("settings") && !root.isNull("settings")) {
                "This is not a supported HydroNudge backup."
            }
        }
        val array = root.getJSONArray("entries")
        val entries = List(array.length()) { i ->
            array.getJSONObject(i).let { e ->
                val enteredUnit = e.optString("enteredUnit", "ML")
                require(VolumeUnit.entries.any { it.name == enteredUnit }) { "Invalid backup enteredUnit." }
                DrinkEntry(
                    e.getString("id"),
                    e.getString("name"),
                    e.getString("category"),
                    amount(e),
                    integer(e, "timestamp"),
                    e.optString("note", ""),
                    enteredUnit
                )
            }
        }
        val favoriteArray = root.optJSONArray("favorites") ?: JSONArray()
        val favorites = List(favoriteArray.length()) { i ->
            favoriteArray.getJSONObject(i).let { f ->
                FavoriteDrink(
                    f.getString("id"),
                    f.getString("name"),
                    f.getString("category"),
                    amount(f)
                )
            }
        }
        require(
            entries.map { it.id }.distinct().size == entries.size &&
                favorites.map { it.id }.distinct().size == favorites.size
        ) { "Backup contains duplicate IDs." }

        val settings = if (root.has("settings") && !root.isNull("settings")) {
            decodeSettingsObject(root.getJSONObject("settings"))
        } else null

        return FullHydroBackup(
            schemaVersion = schemaVersion.toInt(),
            drinkBackup = DrinkBackup(entries, favorites),
            settings = settings
        )
    }

    fun decode(json: String): DrinkBackup = decodeFull(json).drinkBackup

    fun preview(
        json: String,
        existingEntryIds: Set<String> = emptySet(),
        existingFavoriteIds: Set<String> = emptySet()
    ): BackupPreview {
        val root = JSONObject(json)
        val full = decodeFull(json)
        val decoded = full.drinkBackup
        val newEntries = decoded.entries.count { it.id !in existingEntryIds }
        val dupEntries = decoded.entries.size - newEntries
        val waterCount = decoded.entries.count { it.isWater() }
        val newFavs = decoded.favorites.count { it.id !in existingFavoriteIds }
        return BackupPreview(
            schemaVersion = full.schemaVersion,
            exportedAt = root.optString("exportedAt", "Unknown"),
            totalEntries = decoded.entries.size,
            newEntries = newEntries,
            duplicateEntries = dupEntries,
            waterEntries = waterCount,
            legacyOrBeverageEntries = decoded.entries.size - waterCount,
            totalFavorites = decoded.favorites.size,
            newFavorites = newFavs,
            totalVolumeMl = decoded.entries.sumOf { it.amountMl.toLong() },
            includesPreferences = full.settings != null,
            previewGoalMl = full.settings?.goalMl,
            previewUnit = full.settings?.unit?.label
        )
    }

    /** Exports human-readable and spreadsheet-compatible RFC 4180 CSV of drink history. */
    fun encodeCsv(entries: List<DrinkEntry>, zone: ZoneId = ZoneId.systemDefault()): String {
        fun escapeCsv(field: String): String {
            val sanitized = field.replace("\r\n", "\n").replace("\r", "\n")
            return if (sanitized.any { it == ',' || it == '"' || it == '\n' }) {
                "\"${sanitized.replace("\"", "\"\"")}\""
            } else {
                sanitized
            }
        }
        val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val sb = StringBuilder()
        sb.append("id,localDateTime,localDate,timestampEpochMs,name,category,amountMl,enteredUnit,note\n")
        for (e in entries.sortedByDescending { it.timestamp }) {
            val zdt = Instant.ofEpochMilli(e.timestamp).atZone(zone)
            sb.append(escapeCsv(e.id)).append(',')
                .append(escapeCsv(zdt.format(isoFormatter))).append(',')
                .append(escapeCsv(zdt.toLocalDate().toString())).append(',')
                .append(e.timestamp).append(',')
                .append(escapeCsv(e.name)).append(',')
                .append(escapeCsv(e.category)).append(',')
                .append(e.amountMl).append(',')
                .append(escapeCsv(e.enteredUnit)).append(',')
                .append(escapeCsv(e.note)).append('\n')
        }
        return sb.toString()
    }
}

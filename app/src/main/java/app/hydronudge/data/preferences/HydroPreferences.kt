package app.hydronudge.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import app.hydronudge.domain.VolumeUnit
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.hydroDataStore by preferencesDataStore("hydronudge_preferences")

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    OLED("OLED")
}

enum class ReminderStyle { GENTLE, NORMAL, FUNNY, MINIMAL }

enum class OceanPalette(
    val title: String,
    val subtitle: String,
    val swatchHex: Long,
    val forcesOledBlack: Boolean = false
) {
    // Core Lofi Japanese-Inspired Collection
    MOONLIGHT_BLUE("Moonlight Blue", "Lofi bathhouse & moonlit water", 0xFF6BA3D6),
    SAKURA_BLUSH("Sakura Blush", "Muted sakura petals & warm washi paper", 0xFFE598A8),
    MATCHA_MIST("Matcha Mist", "Tranquil Uji matcha & morning steam", 0xFF72B088),
    KYOTO_SKY("Kyoto Sky", "Faded evening sky & quiet veranda", 0xFF7AAAD6),
    LANTERN_AMBER("Lantern Amber", "Warm paper lantern & twilight glow", 0xFFE89E5A),
    INDIGO_NIGHT("Indigo Night", "Deep aizome indigo & rainy window", 0xFF6C82C4),
    RAINY_TOKYO("Rainy Tokyo", "City reflections & soft neon aqua", 0xFF5BC0CE),
    PLUM_INK("Plum Ink", "Dusty lavender & sumi ink notebook", 0xFF9E7FB8),
    BAMBOO_GREEN("Bamboo Green", "Arashiyama grove & muted moss teal", 0xFF5E9F87),
    SOFT_CREAM("Soft Cream", "Warm washi stationery & gentle ink", 0xFFC29B70),
    // Classic Ocean & Pastel Collection
    OCEAN_BLUE("Ocean", "Signature coastal blue & aqua", 0xFF289CE4),
    SKY_BLUE("Sky Blue", "Soft pastel azure & morning cloud", 0xFF5AB2FF),
    LAVENDER("Lavender", "Soft pastel lilac & wisteria mist", 0xFF9B7EDE),
    MINT("Mint", "Bright pastel mint & spring dew", 0xFF42C89E),
    PEACH("Peach", "Warm pastel apricot & nectar glow", 0xFFFF9671),
    ROSE("Rose", "Soft pastel rosewater & blossom pink", 0xFFF27398),
    MIDNIGHT_WAVE("Midnight", "Indigo twilight & moonlit surf", 0xFF6B8AFD),
    DEEP_SEA("Deep Sea", "Abyssal navy & bioluminescent cyan", 0xFF00B4D8),
    ARCTIC_ICE("Arctic Ice", "Crisp glacier blue & frost", 0xFF72EFDD),
    TROPICAL_AQUA("Tropical Aqua", "Lagoon turquoise & reef teal", 0xFF48BFE3),
    SOFT_MINT("Soft Mint", "Seafoam green & tranquil mist", 0xFF7AE5C6),
    CORAL_SUNSET("Coral Sunset", "Warm reef coral & golden horizon", 0xFFFF8563),
    TRUE_OLED_BLACK("True OLED Black", "Pure pitch black for AMOLED displays", 0xFF38BDF8, true),
    MINIMAL_WHITE("Minimal White", "High-contrast editorial monochrome", 0xFF64748B),
    MATERIAL_YOU("Material You", "Adapts to your Android wallpaper", 0xFF60A5FA)
}

enum class VesselStyle(val label: String) {
    WAVE_RING("Wave Ring"),
    TEA_CUP("Kyoto Teacup"),
    WATER_GLASS("Bathhouse Glass"),
    BOTTLE("Hydro Flask"),
    DROPLET("Rain Droplet"),
    MINIMAL_ARC("Ink Brush Arc")
}

enum class WidgetPreset(val title: String, val description: String) {
    DAILY_SUMMARY("Daily Hydration Summary", "Progress, percentage & two quick-log buttons"),
    COMPACT_PROGRESS("Compact Water Progress", "Clean percentage & volume counter"),
    WATER_GLASS("Animated-Style Water Glass", "Visual glass fill level & quick sip"),
    ONE_TAP_QUICK_LOG("One-Tap Quick Log", "Four instant serving buttons"),
    WEEKLY_CHART("Weekly Intake Chart", "7-day intake overview & today's total"),
    CURRENT_STREAK("Current Streak", "Active hydration streak & today's status"),
    NEXT_REMINDER("Next Reminder", "Reminder schedule status & quick check-in"),
    BOTTLE_PROGRESS("Bottle Progress", "Flask capacity tracker & refill action"),
    TODAY_TIMELINE("Today's Drink Timeline", "Recent drink timestamps & quick add"),
    MINIMAL_COUNTER("Minimal Hydration Counter", "Ultra-clean typography counter"),
    GOAL_RING("Hydration Goal Ring", "Goal progress & remaining amount"),
    FAVORITE_SHORTCUTS("Favorite Drink Shortcuts", "One-tap buttons for your saved favorites")
}

enum class WidgetThemeStyle(val label: String) {
    OCEAN_GRADIENT("Lofi Palette"),
    PASTEL_MIST("Washi Paper"),
    LIGHT("Soft Daylight"),
    DARK("Rainy Night"),
    OLED_BLACK("True OLED"),
    TRANSPARENT("Rainy Window")
}

data class HydroSettings(
    /** Zero means the user has not chosen a personal water goal. */
    val goalMl: Int = 0,
    val unit: VolumeUnit = VolumeUnit.ML,
    val quickAmounts: List<Int> = listOf(150, 250, 330, 500),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColors: Boolean = false,
    val reminders: Boolean = false,
    val intervalMinutes: Int = 60,
    val wakeMinute: Int = 8 * 60,
    val sleepMinute: Int = 22 * 60,
    val style: ReminderStyle = ReminderStyle.GENTLE,
    val sound: Boolean = false,
    val vibration: Boolean = false,
    val skipAtGoal: Boolean = true,
    val pausedUntil: Long = 0,
    val statusNotification: Boolean = false,
    val statusDismissed: Boolean = false,
    val tileAmountMl: Int = 250,
    // HydroNudge 3.0 Extended Customization & Features
    val palette: OceanPalette = OceanPalette.SAKURA_BLUSH,
    val customAccentHex: Long = 0L,
    val vesselStyle: VesselStyle = VesselStyle.WAVE_RING,
    val reducedMotion: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val soundEffects: Boolean = false,
    val minimalistMode: Boolean = false,
    val showStreaks: Boolean = true,
    val showMotivational: Boolean = true,
    val celebrationsEnabled: Boolean = true,
    val beverageTrackingEnabled: Boolean = false,
    val dayStartMinute: Int = 0,
    val roundingStepMl: Int = 0,
    val accidentalEntryLimitMl: Int = 2000,
    val weekendGoalMl: Int = 0,
    val weeklyGoalMl: Int = 0,
    val adaptiveReminders: Boolean = false,
    val activeDaysCsv: String = "1,2,3,4,5,6,7",
    val weekendScheduleEnabled: Boolean = false,
    val weekendWakeMinute: Int = 9 * 60,
    val weekendSleepMinute: Int = 22 * 60,
    val weekendIntervalMinutes: Int = 90,
    val quietHoursEnabled: Boolean = false,
    val quietStartMinute: Int = 13 * 60,
    val quietEndMinute: Int = 14 * 60,
    val customReminderMessage: String = "",
    val widgetPreset: WidgetPreset = WidgetPreset.DAILY_SUMMARY,
    val widgetThemeStyle: WidgetThemeStyle = WidgetThemeStyle.OCEAN_GRADIENT,
    val widgetQuickAmounts: List<Int> = listOf(250, 500)
) {
    fun effectiveGoalMl(dayOfWeekValue: Int): Int {
        val isWeekend = dayOfWeekValue == 6 || dayOfWeekValue == 7
        return if (isWeekend && weekendGoalMl in 100..20000) weekendGoalMl else goalMl
    }

    fun activeDaysSet(): Set<Int> =
        activeDaysCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull()?.takeIf { d -> d in 1..7 } }
            .toSet()
            .ifEmpty { (1..7).toSet() }
}

class HydroPreferences(private val context: Context) {
    private val goal = intPreferencesKey("goal_ml")
    private val unit = stringPreferencesKey("unit")
    private val quick = stringPreferencesKey("quick_amounts_ml")
    private val theme = stringPreferencesKey("theme")
    private val dynamic = booleanPreferencesKey("dynamic_colors")
    private val reminders = booleanPreferencesKey("reminders")
    private val interval = intPreferencesKey("interval_minutes")
    private val wake = intPreferencesKey("wake_minute")
    private val sleep = intPreferencesKey("sleep_minute")
    private val style = stringPreferencesKey("reminder_style")
    private val sound = booleanPreferencesKey("sound")
    private val vibration = booleanPreferencesKey("vibration")
    private val skip = booleanPreferencesKey("skip_at_goal")
    private val paused = longPreferencesKey("paused_until")
    private val status = booleanPreferencesKey("status_notification")
    private val dismissed = booleanPreferencesKey("status_dismissed")
    private val tileAmount = intPreferencesKey("tile_amount_ml")

    private val paletteKey = stringPreferencesKey("ocean_palette")
    private val customAccentKey = longPreferencesKey("custom_accent_hex")
    private val vesselKey = stringPreferencesKey("vessel_style")
    private val reducedMotionKey = booleanPreferencesKey("reduced_motion")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val soundEffectsKey = booleanPreferencesKey("sound_effects")
    private val minimalistKey = booleanPreferencesKey("minimalist_mode")
    private val showStreaksKey = booleanPreferencesKey("show_streaks")
    private val showMotivationalKey = booleanPreferencesKey("show_motivational")
    private val celebrationsKey = booleanPreferencesKey("celebrations_enabled")
    private val beverageTrackingKey = booleanPreferencesKey("beverage_tracking_enabled")
    private val dayStartKey = intPreferencesKey("day_start_minute")
    private val roundingStepKey = intPreferencesKey("rounding_step_ml")
    private val accidentalLimitKey = intPreferencesKey("accidental_entry_limit_ml")
    private val weekendGoalKey = intPreferencesKey("weekend_goal_ml")
    private val weeklyGoalKey = intPreferencesKey("weekly_goal_ml")
    private val adaptiveRemindersKey = booleanPreferencesKey("adaptive_reminders")
    private val activeDaysKey = stringPreferencesKey("active_days_csv")
    private val weekendScheduleKey = booleanPreferencesKey("weekend_schedule_enabled")
    private val weekendWakeKey = intPreferencesKey("weekend_wake_minute")
    private val weekendSleepKey = intPreferencesKey("weekend_sleep_minute")
    private val weekendIntervalKey = intPreferencesKey("weekend_interval_minutes")
    private val quietHoursKey = booleanPreferencesKey("quiet_hours_enabled")
    private val quietStartKey = intPreferencesKey("quiet_start_minute")
    private val quietEndKey = intPreferencesKey("quiet_end_minute")
    private val customReminderMsgKey = stringPreferencesKey("custom_reminder_message")
    private val widgetPresetKey = stringPreferencesKey("widget_preset")
    private val widgetThemeStyleKey = stringPreferencesKey("widget_theme_style")
    private val widgetQuickKey = stringPreferencesKey("widget_quick_amounts_ml")

    val settings = context.hydroDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            HydroSettings(
                goalMl = p[goal]?.takeIf { it == 0 || it in 100..20000 } ?: 0,
                unit = VolumeUnit.entries.firstOrNull { it.name == p[unit] } ?: VolumeUnit.ML,
                quickAmounts = p[quick]?.split(",")?.mapNotNull { it.toIntOrNull()?.takeIf { n -> n in 1..20000 } }?.takeIf { it.size == 4 } ?: listOf(150, 250, 330, 500),
                theme = ThemeMode.entries.firstOrNull { it.name == p[theme] } ?: ThemeMode.SYSTEM,
                dynamicColors = p[dynamic] ?: false,
                reminders = p[reminders] ?: false,
                intervalMinutes = p[interval]?.coerceIn(15, 1440) ?: 60,
                wakeMinute = p[wake]?.coerceIn(0, 1439) ?: 480,
                sleepMinute = p[sleep]?.coerceIn(0, 1439) ?: 1320,
                style = ReminderStyle.entries.firstOrNull { it.name == p[style] } ?: ReminderStyle.GENTLE,
                sound = p[sound] ?: false,
                vibration = p[vibration] ?: false,
                skipAtGoal = p[skip] ?: true,
                pausedUntil = p[paused] ?: 0,
                statusNotification = p[status] ?: false,
                statusDismissed = p[dismissed] ?: false,
                tileAmountMl = p[tileAmount]?.coerceIn(1, 20000) ?: 250,
                palette = OceanPalette.entries.firstOrNull { it.name == p[paletteKey] } ?: OceanPalette.SAKURA_BLUSH,
                customAccentHex = p[customAccentKey] ?: 0L,
                vesselStyle = VesselStyle.entries.firstOrNull { it.name == p[vesselKey] } ?: VesselStyle.WAVE_RING,
                reducedMotion = p[reducedMotionKey] ?: false,
                hapticsEnabled = p[hapticsKey] ?: true,
                soundEffects = p[soundEffectsKey] ?: false,
                minimalistMode = p[minimalistKey] ?: false,
                showStreaks = p[showStreaksKey] ?: true,
                showMotivational = p[showMotivationalKey] ?: true,
                celebrationsEnabled = p[celebrationsKey] ?: true,
                beverageTrackingEnabled = p[beverageTrackingKey] ?: false,
                dayStartMinute = p[dayStartKey]?.coerceIn(0, 720) ?: 0,
                roundingStepMl = p[roundingStepKey]?.coerceIn(0, 250) ?: 0,
                accidentalEntryLimitMl = p[accidentalLimitKey]?.coerceIn(500, 20000) ?: 2000,
                weekendGoalMl = p[weekendGoalKey]?.takeIf { it == 0 || it in 100..20000 } ?: 0,
                weeklyGoalMl = p[weeklyGoalKey]?.takeIf { it == 0 || it in 500..140000 } ?: 0,
                adaptiveReminders = p[adaptiveRemindersKey] ?: false,
                activeDaysCsv = p[activeDaysKey]?.takeIf { it.isNotBlank() } ?: "1,2,3,4,5,6,7",
                weekendScheduleEnabled = p[weekendScheduleKey] ?: false,
                weekendWakeMinute = p[weekendWakeKey]?.coerceIn(0, 1439) ?: 540,
                weekendSleepMinute = p[weekendSleepKey]?.coerceIn(0, 1439) ?: 1320,
                weekendIntervalMinutes = p[weekendIntervalKey]?.coerceIn(15, 1440) ?: 90,
                quietHoursEnabled = p[quietHoursKey] ?: false,
                quietStartMinute = p[quietStartKey]?.coerceIn(0, 1439) ?: 780,
                quietEndMinute = p[quietEndKey]?.coerceIn(0, 1439) ?: 840,
                customReminderMessage = p[customReminderMsgKey] ?: "",
                widgetPreset = WidgetPreset.entries.firstOrNull { it.name == p[widgetPresetKey] } ?: WidgetPreset.DAILY_SUMMARY,
                widgetThemeStyle = WidgetThemeStyle.entries.firstOrNull { it.name == p[widgetThemeStyleKey] } ?: WidgetThemeStyle.OCEAN_GRADIENT,
                widgetQuickAmounts = p[widgetQuickKey]?.split(",")?.mapNotNull { it.toIntOrNull()?.takeIf { n -> n in 1..20000 } }?.takeIf { it.size == 2 } ?: listOf(250, 500)
            )
        }

    suspend fun save(value: HydroSettings) {
        require(value.goalMl == 0 || value.goalMl in 100..20000)
        require(value.quickAmounts.size == 4 && value.quickAmounts.all { it in 1..20000 })
        require(value.intervalMinutes in 15..1440 && value.wakeMinute in 0..1439 && value.sleepMinute in 0..1439)
        require(value.tileAmountMl in 1..20000)
        context.hydroDataStore.edit { p ->
            p[goal] = value.goalMl
            p[unit] = value.unit.name
            p[quick] = value.quickAmounts.joinToString(",")
            p[theme] = value.theme.name
            p[dynamic] = value.dynamicColors
            p[reminders] = value.reminders
            p[interval] = value.intervalMinutes
            p[wake] = value.wakeMinute
            p[sleep] = value.sleepMinute
            p[style] = value.style.name
            p[sound] = value.sound
            p[vibration] = value.vibration
            p[skip] = value.skipAtGoal
            p[paused] = value.pausedUntil
            p[status] = value.statusNotification
            p[tileAmount] = value.tileAmountMl
            p[dismissed] = value.statusDismissed
            p[paletteKey] = value.palette.name
            p[customAccentKey] = value.customAccentHex
            p[vesselKey] = value.vesselStyle.name
            p[reducedMotionKey] = value.reducedMotion
            p[hapticsKey] = value.hapticsEnabled
            p[soundEffectsKey] = value.soundEffects
            p[minimalistKey] = value.minimalistMode
            p[showStreaksKey] = value.showStreaks
            p[showMotivationalKey] = value.showMotivational
            p[celebrationsKey] = value.celebrationsEnabled
            p[beverageTrackingKey] = value.beverageTrackingEnabled
            p[dayStartKey] = value.dayStartMinute
            p[roundingStepKey] = value.roundingStepMl
            p[accidentalLimitKey] = value.accidentalEntryLimitMl
            p[weekendGoalKey] = value.weekendGoalMl
            p[weeklyGoalKey] = value.weeklyGoalMl
            p[adaptiveRemindersKey] = value.adaptiveReminders
            p[activeDaysKey] = value.activeDaysCsv
            p[weekendScheduleKey] = value.weekendScheduleEnabled
            p[weekendWakeKey] = value.weekendWakeMinute
            p[weekendSleepKey] = value.weekendSleepMinute
            p[weekendIntervalKey] = value.weekendIntervalMinutes
            p[quietHoursKey] = value.quietHoursEnabled
            p[quietStartKey] = value.quietStartMinute
            p[quietEndKey] = value.quietEndMinute
            p[customReminderMsgKey] = value.customReminderMessage.take(160)
            p[widgetPresetKey] = value.widgetPreset.name
            p[widgetThemeStyleKey] = value.widgetThemeStyle.name
            p[widgetQuickKey] = value.widgetQuickAmounts.joinToString(",")
        }
    }

    suspend fun dismissStatus() {
        context.hydroDataStore.edit { it[dismissed] = true }
    }

    suspend fun pauseRemindersUntil(epochMillis: Long) {
        context.hydroDataStore.edit { it[paused] = epochMillis }
    }
}

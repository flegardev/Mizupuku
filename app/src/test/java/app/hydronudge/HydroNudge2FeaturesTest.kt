package app.hydronudge

import app.hydronudge.data.BackupCodec
import app.hydronudge.data.local.DrinkBackup
import app.hydronudge.data.local.DrinkEntry
import app.hydronudge.data.local.FavoriteDrink
import app.hydronudge.data.preferences.HydroSettings
import app.hydronudge.data.preferences.OceanPalette
import app.hydronudge.data.preferences.ThemeMode
import app.hydronudge.data.preferences.VesselStyle
import app.hydronudge.data.preferences.WidgetPreset
import app.hydronudge.domain.*
import app.hydronudge.reminders.ReminderPolicy
import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class HydroNudge2FeaturesTest {
    private val zone = ZoneId.of("Europe/Ljubljana")

    @Test
    fun streakCalculationAndDeletionDoNotCorruptStreaks() {
        val today = LocalDate.of(2026, 11, 10)
        fun entryOn(date: LocalDate, id: String, ml: Int) = DrinkEntry(
            id = id,
            name = "Water",
            category = "WATER",
            amountMl = ml,
            timestamp = date.atStartOfDay(zone).plusHours(10).toInstant().toEpochMilli()
        )
        val e1 = entryOn(today.minusDays(2), "d1", 2000)
        val e2 = entryOn(today.minusDays(1), "d2", 2000)
        val e3 = entryOn(today, "d3", 2000)
        val streak3 = calculateStreak(listOf(e1, e2, e3), today, zone, 2000)
        assertEquals(3, streak3.currentStreakDays)
        assertEquals(3, streak3.longestStreakDays)
        assertTrue(streak3.todayActive)

        // Deleting today's entry keeps yesterday's 2-day streak intact
        val afterDeleteToday = calculateStreak(listOf(e1, e2), today, zone, 2000)
        assertEquals(2, afterDeleteToday.currentStreakDays)
        assertEquals(2, afterDeleteToday.longestStreakDays)

        // Deleting middle day splits streak cleanly
        val afterDeleteMiddle = calculateStreak(listOf(e1, e3), today, zone, 2000)
        assertEquals(1, afterDeleteMiddle.currentStreakDays)
        assertEquals(1, afterDeleteMiddle.longestStreakDays)
    }

    @Test
    fun customDayBoundaryShiftsMidnightCorrectly() {
        val date = LocalDate.of(2026, 11, 10)
        // 02:00 AM day boundary (120 minutes)
        val oneAmMs = date.atStartOfDay(zone).plusHours(1).toInstant().toEpochMilli()
        val threeAmMs = date.atStartOfDay(zone).plusHours(3).toInstant().toEpochMilli()
        assertEquals(date.minusDays(1), localDay(oneAmMs, zone, 120))
        assertEquals(date, localDay(threeAmMs, zone, 120))
    }

    @Test
    fun quantityRoundingAndRecentlyUsedAmountsAreDeterministic() {
        assertEquals(250, roundVolumeMl(243, 25))
        assertEquals(240, roundVolumeMl(243, 10))
        assertEquals(243, roundVolumeMl(243, 0))

        val entries = listOf(
            DrinkEntry(id = "1", name = "Water", category = "WATER", amountMl = 250, timestamp = 1000L),
            DrinkEntry(id = "2", name = "Water", category = "WATER", amountMl = 330, timestamp = 2000L),
            DrinkEntry(id = "3", name = "Water", category = "WATER", amountMl = 250, timestamp = 3000L),
            DrinkEntry(id = "4", name = "Water", category = "WATER", amountMl = 750, timestamp = 4000L)
        )
        assertEquals(listOf(750, 250, 330), recentlyUsedAmounts(entries, 4))
    }

    @Test
    fun adaptiveRemindersQuietHoursAndWeekdaySchedules() {
        val saturdayNoon = ZonedDateTime.parse("2026-10-31T13:30:00+01:00[Europe/Ljubljana]")
        val base = HydroSettings(
            goalMl = 2500,
            reminders = true,
            intervalMinutes = 60,
            quietHoursEnabled = true,
            quietStartMinute = 13 * 60,
            quietEndMinute = 14 * 60
        )
        // Inside quiet hours (13:30 is between 13:00 and 14:00)
        assertFalse(ReminderPolicy.shouldRemind(base, saturdayNoon, 500, null))
        // Outside quiet hours
        val saturdayThreePm = saturdayNoon.withHour(15)
        assertTrue(ReminderPolicy.shouldRemind(base, saturdayThreePm, 500, null))

        // Exclude Saturday (day 6) from activeDaysCsv
        val weekdaysOnly = base.copy(quietHoursEnabled = false, activeDaysCsv = "1,2,3,4,5")
        assertFalse(ReminderPolicy.shouldRemind(weekdaysOnly, saturdayThreePm, 500, null))

        // Adaptive interval adjusts deterministically within safe bounds
        val adaptive = base.copy(quietHoursEnabled = false, adaptiveReminders = true)
        val lowIntakeInterval = ReminderPolicy.effectiveIntervalMinutes(adaptive, saturdayThreePm, 200)
        val highIntakeInterval = ReminderPolicy.effectiveIntervalMinutes(adaptive, saturdayThreePm, 2300)
        assertTrue(lowIntakeInterval in 45..60)
        assertTrue(highIntakeInterval > 60)
    }

    @Test
    fun backupPreviewAndCsvExport() {
        val e1 = DrinkEntry(id = "e1", name = "Water", category = "WATER", amountMl = 250, timestamp = 1700000000000L, note = "Morning, fresh")
        val e2 = DrinkEntry(id = "e2", name = "Green Tea", category = "TEA", amountMl = 200, timestamp = 1700003600000L)
        val fav = FavoriteDrink(id = "f1", name = "Desk Bottle", category = "WATER", amountMl = 500)
        val json = BackupCodec.encode(DrinkBackup(listOf(e1, e2), listOf(fav)))

        val preview = BackupCodec.preview(json, existingEntryIds = setOf("e1"), existingFavoriteIds = emptySet())
        assertEquals(2, preview.totalEntries)
        assertEquals(1, preview.newEntries)
        assertEquals(1, preview.duplicateEntries)
        assertEquals(1, preview.waterEntries)
        assertEquals(1, preview.legacyOrBeverageEntries)
        assertEquals(1, preview.newFavorites)
        assertEquals(450L, preview.totalVolumeMl)

        val csv = BackupCodec.encodeCsv(listOf(e1, e2), zone)
        assertTrue(csv.startsWith("id,localDateTime,localDate,timestampEpochMs,name,category,amountMl,enteredUnit,note\n"))
        assertTrue(csv.contains("\"Morning, fresh\""))
        assertTrue(csv.contains("Green Tea,TEA,200"))
    }

    @Test
    fun fullBackupSchemaV2PreservesPreferencesAndRemainsBackwardCompatibleWithV1() {
        val e1 = DrinkEntry(id = "e1", name = "Water", category = "WATER", amountMl = 300, timestamp = 1700000000000L, note = "Gym")
        val fav = FavoriteDrink(id = "f1", name = "Gym Bottle", category = "WATER", amountMl = 750)
        val customSettings = HydroSettings(
            goalMl = 3100,
            unit = VolumeUnit.US_OZ,
            reminders = true,
            intervalMinutes = 75,
            wakeMinute = 7 * 60 + 30,
            sleepMinute = 21 * 60 + 30,
            skipAtGoal = false,
            sound = false,
            vibration = true,
            theme = ThemeMode.OLED,
            quickAmounts = listOf(200, 350, 500, 750),
            palette = OceanPalette.LAVENDER,
            customAccentHex = 0xFF9B7EDE,
            vesselStyle = VesselStyle.DROPLET,
            widgetPreset = WidgetPreset.CURRENT_STREAK,
            widgetQuickAmounts = listOf(200, 500),
            accidentalEntryLimitMl = 1500,
            activeDaysCsv = "1,2,3,4,5"
        )
        val v2Json = BackupCodec.encodeFull(DrinkBackup(listOf(e1), listOf(fav)), customSettings)
        val decodedV2 = BackupCodec.decodeFull(v2Json)
        assertEquals(1, decodedV2.drinkBackup.entries.size)
        assertEquals("e1", decodedV2.drinkBackup.entries.first().id)
        assertEquals(1, decodedV2.drinkBackup.favorites.size)
        assertEquals(customSettings, decodedV2.settings)

        // Preview reflects includedPreferences = true for v2 and false for v1
        val previewV2 = BackupCodec.preview(v2Json)
        assertTrue(previewV2.includesPreferences)

        val v1Json = BackupCodec.encode(DrinkBackup(listOf(e1), listOf(fav)))
        val decodedV1 = BackupCodec.decodeFull(v1Json)
        assertEquals(1, decodedV1.drinkBackup.entries.size)
        assertNull(decodedV1.settings)
        assertFalse(BackupCodec.preview(v1Json).includesPreferences)

        // Malformed settings in v2 are rejected cleanly
        val corruptedV2 = v2Json.replace("\"goalMl\": 3100", "\"goalMl\": 999999")
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decodeFull(corruptedV2)
        }
    }

    @Test
    fun missedReminderFeedbackDetectsElapsedWindowsWithoutClaimingExactAlarms() {
        val now = ZonedDateTime.parse("2026-11-02T15:00:00+01:00[Europe/Ljubljana]")
        val settings = HydroSettings(reminders = true, intervalMinutes = 60, goalMl = 2500)
        // Last drink 3 hours ago -> >= 2 intervals elapsed -> returns non-null feedback
        val threeHoursAgoMs = now.minusHours(3).toInstant().toEpochMilli()
        val feedback = ReminderPolicy.missedReminderFeedback(settings, now, 500, threeHoursAgoMs)
        assertNotNull(feedback)
        assertTrue(feedback!!.contains("No water logged for"))

        // Last drink 30 minutes ago -> < 2 intervals -> null
        val thirtyMinAgoMs = now.minusMinutes(30).toInstant().toEpochMilli()
        assertNull(ReminderPolicy.missedReminderFeedback(settings, now, 500, thirtyMinAgoMs))
    }

    @Test
    fun hydrationPaceAndHourlyAnalyticsReportAreAccurate() {
        val paceMidday = hydrationPaceSummary(
            totalMl = 1000L,
            goalMl = 2000,
            currentMinuteOfDay = 15 * 60, // halfway between 08:00 and 22:00
            wakeMinute = 8 * 60,
            sleepMinute = 22 * 60,
            unit = VolumeUnit.ML
        )
        assertEquals("Right on pace", paceMidday)

        val day = LocalDate.of(2026, 11, 10)
        val morningEntry = DrinkEntry(
            id = "h1",
            name = "Water",
            category = "WATER",
            amountMl = 400,
            timestamp = day.atStartOfDay(zone).plusHours(9).toInstant().toEpochMilli()
        )
        val eveningEntry = DrinkEntry(
            id = "h2",
            name = "Water",
            category = "WATER",
            amountMl = 600,
            timestamp = day.atStartOfDay(zone).plusHours(18).toInstant().toEpochMilli()
        )
        val rep = historyReport(listOf(morningEntry, eveningEntry), day, day, zone, 1000)
        assertEquals(24, rep.hourlyMl.size)
        assertEquals(400L, rep.hourlyMl[9])
        assertEquals(600L, rep.hourlyMl[18])
        assertEquals(100, rep.hydrationScore)
    }

    @Test
    fun allThemePalettesVesselStylesAndWidgetPresetsExist() {
        assertEquals(25, OceanPalette.entries.size)
        assertTrue(
            listOf(
                OceanPalette.MOONLIGHT_BLUE,
                OceanPalette.SAKURA_BLUSH,
                OceanPalette.MATCHA_MIST,
                OceanPalette.KYOTO_SKY,
                OceanPalette.LANTERN_AMBER,
                OceanPalette.INDIGO_NIGHT,
                OceanPalette.RAINY_TOKYO,
                OceanPalette.PLUM_INK,
                OceanPalette.BAMBOO_GREEN,
                OceanPalette.SOFT_CREAM,
                OceanPalette.LAVENDER,
                OceanPalette.SKY_BLUE,
                OceanPalette.MINT,
                OceanPalette.PEACH,
                OceanPalette.ROSE,
                OceanPalette.OCEAN_BLUE,
                OceanPalette.MIDNIGHT_WAVE
            ).all { it in OceanPalette.entries }
        )
        assertEquals(4, ThemeMode.entries.size)
        assertEquals(6, VesselStyle.entries.size)
        assertTrue(VesselStyle.TEA_CUP in VesselStyle.entries)
        assertEquals(12, WidgetPreset.entries.size)
    }
}

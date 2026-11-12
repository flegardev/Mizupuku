package app.hydronudge.reminders

import app.hydronudge.data.preferences.*
import java.time.ZonedDateTime

object ReminderPolicy {
    fun withinWakingHours(minute: Int, wake: Int, sleep: Int): Boolean = when {
        wake == sleep -> true // Equal endpoints explicitly mean all day.
        wake < sleep -> minute >= wake && minute < sleep
        else -> minute >= wake || minute < sleep
    }

    fun withinQuietHours(minute: Int, quietStart: Int, quietEnd: Int): Boolean = when {
        quietStart == quietEnd -> false
        quietStart < quietEnd -> minute >= quietStart && minute < quietEnd
        else -> minute >= quietStart || minute < quietEnd
    }

    /**
     * Deterministic adaptive interval calculation (no AI).
     * Never shortens below max(15, baseInterval * 3 / 4) to avoid aggressive reminders.
     */
    fun effectiveIntervalMinutes(
        settings: HydroSettings,
        now: ZonedDateTime,
        totalMl: Long
    ): Int {
        val dayOfWeek = now.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
        val isWeekend = dayOfWeek == 6 || dayOfWeek == 7
        val baseInterval = if (settings.weekendScheduleEnabled && isWeekend) {
            settings.weekendIntervalMinutes
        } else {
            settings.intervalMinutes
        }.coerceIn(15, 1440)

        if (!settings.adaptiveReminders) return baseInterval

        val effectiveGoal = settings.effectiveGoalMl(dayOfWeek)
        if (effectiveGoal <= 0) return baseInterval

        val ratio = totalMl.toDouble() / effectiveGoal.toDouble()
        return when {
            ratio >= 0.85 -> (baseInterval * 1.25).toInt().coerceIn(baseInterval, 1440)
            ratio < 0.35 && now.hour >= 14 -> (baseInterval * 0.85).toInt().coerceAtLeast(maxOf(15, baseInterval * 3 / 4))
            else -> baseInterval
        }
    }

    fun shouldRemind(
        settings: HydroSettings,
        now: ZonedDateTime,
        totalMl: Long,
        lastDrink: Long?
    ): Boolean {
        if (!settings.reminders) return false
        val nowMs = now.toInstant().toEpochMilli()
        if (nowMs < settings.pausedUntil) return false

        val dayOfWeek = now.dayOfWeek.value
        if (dayOfWeek !in settings.activeDaysSet()) return false

        val isWeekend = dayOfWeek == 6 || dayOfWeek == 7
        val wake = if (settings.weekendScheduleEnabled && isWeekend) settings.weekendWakeMinute else settings.wakeMinute
        val sleep = if (settings.weekendScheduleEnabled && isWeekend) settings.weekendSleepMinute else settings.sleepMinute
        val currentMinute = now.hour * 60 + now.minute

        if (!withinWakingHours(currentMinute, wake, sleep)) return false
        if (settings.quietHoursEnabled && withinQuietHours(currentMinute, settings.quietStartMinute, settings.quietEndMinute)) {
            return false
        }

        val effectiveGoal = settings.effectiveGoalMl(dayOfWeek)
        if (settings.skipAtGoal && effectiveGoal > 0 && totalMl >= effectiveGoal) return false

        val intervalMin = effectiveIntervalMinutes(settings, now, totalMl)
        return lastDrink == null || nowMs - lastDrink >= intervalMin * 60000L
    }

    fun message(style: ReminderStyle, percent: Long): String = when (style) {
        ReminderStyle.GENTLE -> "Mizupuku check-in. Drink if you feel thirsty."
        ReminderStyle.NORMAL -> "Mizupuku check-in. Your water log is here if you want it."
        ReminderStyle.FUNNY -> "A quiet Mizupuku check-in. No need to log anything."
        ReminderStyle.MINIMAL -> "Optional water check-in"
    }

    fun resolveMessage(settings: HydroSettings, percent: Long): String {
        val custom = settings.customReminderMessage.trim()
        return if (custom.isNotEmpty()) custom else message(settings.style, percent)
    }

    /**
     * Detects when at least two reminder intervals have elapsed during an eligible waking window
     * so the app can give honest, battery-aware missed-reminder feedback without claiming exact alarms.
     */
    fun missedReminderFeedback(
        settings: HydroSettings,
        now: ZonedDateTime,
        totalMl: Long,
        lastDrink: Long?
    ): String? {
        if (!shouldRemind(settings, now, totalMl, lastDrink)) return null
        val nowMs = now.toInstant().toEpochMilli()
        val intervalMs = effectiveIntervalMinutes(settings, now, totalMl) * 60000L
        if (lastDrink != null && nowMs - lastDrink >= intervalMs * 2) {
            val hoursAgo = ((nowMs - lastDrink) / 3600000L).coerceAtLeast(1L)
            return "No water logged for ${hoursAgo}h during your active reminder window. Android battery saver or Doze may delay background notifications."
        }
        return null
    }

    fun nextReminderSummary(
        settings: HydroSettings,
        now: ZonedDateTime,
        totalMl: Long,
        lastDrink: Long?
    ): String {
        if (!settings.reminders) return "Reminders off"
        val nowMs = now.toInstant().toEpochMilli()
        if (settings.pausedUntil > nowMs) {
            val resumeTime = java.time.Instant.ofEpochMilli(settings.pausedUntil).atZone(now.zone)
            return "Paused until %02d:%02d".format(resumeTime.hour, resumeTime.minute)
        }
        val dayOfWeek = now.dayOfWeek.value
        if (dayOfWeek !in settings.activeDaysSet()) return "Rest day (no reminders today)"
        val effectiveGoal = settings.effectiveGoalMl(dayOfWeek)
        if (settings.skipAtGoal && effectiveGoal > 0 && totalMl >= effectiveGoal) {
            return "Paused for today (goal reached)"
        }
        val currentMinute = now.hour * 60 + now.minute
        if (settings.quietHoursEnabled && withinQuietHours(currentMinute, settings.quietStartMinute, settings.quietEndMinute)) {
            return "Quiet hours until %02d:%02d".format(settings.quietEndMinute / 60, settings.quietEndMinute % 60)
        }
        val isWeekend = dayOfWeek == 6 || dayOfWeek == 7
        val wake = if (settings.weekendScheduleEnabled && isWeekend) settings.weekendWakeMinute else settings.wakeMinute
        val sleep = if (settings.weekendScheduleEnabled && isWeekend) settings.weekendSleepMinute else settings.sleepMinute
        if (!withinWakingHours(currentMinute, wake, sleep)) {
            return "Next window at %02d:%02d".format(wake / 60, wake % 60)
        }
        val intervalMin = effectiveIntervalMinutes(settings, now, totalMl)
        val modeTag = if (settings.adaptiveReminders) "Adaptive" else "Every $intervalMin min"
        if (lastDrink != null && lastDrink <= nowMs) {
            val nextMs = lastDrink + intervalMin * 60000L
            if (nextMs > nowMs) {
                val minsLeft = ((nextMs - nowMs + 59999L) / 60000L).coerceAtLeast(1L)
                return "$modeTag · ~${minsLeft}m window"
            }
        }
        return "$modeTag · Active window"
    }
}

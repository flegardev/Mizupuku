package app.hydronudge.reminders

import android.content.Context
import androidx.work.*
import app.hydronudge.HydroApplication
import app.hydronudge.domain.*
import app.hydronudge.notifications.HydroNotifications
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as HydroApplication
        val settings = app.preferences.settings.first()
        val entries = app.repository.all()
        val now = ZonedDateTime.now()
        val day = dayWindow(now.toLocalDate(), now.zone, settings.dayStartMinute)
        val total = entries
            .filter { it.isWater() && it.timestamp >= day.start && it.timestamp < day.endExclusive }
            .sumOf { it.amountMl.toLong() }
        val lastDrink = entries
            .filter { it.isWater() && it.timestamp <= now.toInstant().toEpochMilli() }
            .maxOfOrNull { it.timestamp }
        val effectiveGoal = settings.effectiveGoalMl(now.dayOfWeek.value)
        if (ReminderPolicy.shouldRemind(settings, now, total, lastDrink)) {
            HydroNotifications(app).remind(
                settings,
                ReminderPolicy.resolveMessage(settings, goalPercent(total, effectiveGoal))
            )
        }
        return Result.success()
    }
}

class SurfaceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        (applicationContext as HydroApplication).refreshSurfaces()
        return Result.success()
    }
}

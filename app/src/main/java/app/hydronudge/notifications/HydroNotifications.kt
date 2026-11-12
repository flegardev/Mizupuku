package app.hydronudge.notifications

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import app.hydronudge.*
import app.hydronudge.data.preferences.HydroSettings
import app.hydronudge.domain.*
import app.hydronudge.ui.theme.OceanTokens
import kotlinx.coroutines.flow.first
import java.time.*
import java.util.UUID

class HydroNotifications(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    private fun allowed() =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun open(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun channel(id: String, name: String, sound: Boolean = false, vibration: Boolean = false) {
        manager.createNotificationChannel(
            NotificationChannel(
                id,
                name,
                if (sound || vibration) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_LOW
            ).apply {
                if (!sound) setSound(null, null)
                enableVibration(vibration)
                description = "Mizupuku. You control sound, vibration and lock-screen visibility in Android settings."
            }
        )
    }

    private fun drinkActionIntent(ml: Int, dismissReminder: Boolean = false): PendingIntent {
        val token = UUID.randomUUID().toString()
        val intent = Intent(context, DrinkActionReceiver::class.java)
            .setAction("app.hydronudge.ADD")
            .setData("hydronudge://drink/$token".toUri())
            .putExtra("token", token)
            .putExtra("ml", ml)
            .putExtra("dismissReminder", dismissReminder)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun remind(settings: HydroSettings, message: String) {
        val id = "reminders-${settings.sound}-${settings.vibration}"
        channel(
            id,
            "Water reminders (${if (settings.sound) "sound" else "silent"}, ${if (settings.vibration) "vibrate" else "no vibration"})",
            settings.sound,
            settings.vibration
        )
        if (!allowed()) return
        val quickMl = settings.quickAmounts.getOrElse(1) { 250 }.coerceIn(1, 20000)
        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            101,
            Intent(context, DrinkActionReceiver::class.java).setAction("app.hydronudge.SNOOZE_REMINDER"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val skipIntent = PendingIntent.getBroadcast(
            context,
            102,
            Intent(context, DrinkActionReceiver::class.java).setAction("app.hydronudge.SKIP_REMINDER"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        try {
            manager.notify(
                2,
                NotificationCompat.Builder(context, id)
                    .setSmallIcon(R.drawable.ic_drop)
                    .setContentTitle("A little Mizupuku")
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setColor(OceanTokens.LightBlue.toArgb())
                    .setGroup("hydronudge_notifications")
                    .setContentIntent(open())
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .addAction(0, "+ ${settings.unit.format(quickMl.toLong())}", drinkActionIntent(quickMl, dismissReminder = true))
                    .addAction(0, "Snooze 30m", snoozeIntent)
                    .addAction(0, "Skip", skipIntent)
                    .build()
            )
        } catch (_: SecurityException) {
            /* Permission can be revoked between check and post. */
        }
    }

    suspend fun refresh() {
        val app = context.applicationContext as HydroApplication
        val settings = app.preferences.settings.first()
        if (!settings.statusNotification) {
            manager.cancel(1)
            return
        }
        if (settings.statusDismissed) return
        channel("hydration-status", "Hydration status")
        if (!allowed()) return
        val now = LocalDate.now()
        val w = dayWindow(now, ZoneId.systemDefault(), settings.dayStartMinute)
        val total = app.repository.all()
            .filter { it.isWater() && it.timestamp >= w.start && it.timestamp < w.endExclusive }
            .sumOf { it.amountMl.toLong() }

        val firstActionMl = settings.widgetQuickAmounts.getOrElse(0) { 250 }.coerceIn(1, 20000)
        val secondActionMl = settings.widgetQuickAmounts.getOrElse(1) { 500 }.coerceIn(1, 20000)
        val effectiveGoal = settings.effectiveGoalMl(now.dayOfWeek.value)

        val notification = NotificationCompat.Builder(context, "hydration-status")
            .setSmallIcon(R.drawable.ic_drop)
            .setContentTitle(
                if (effectiveGoal > 0) "Mizupuku · ${settings.unit.format(total)} / ${settings.unit.format(effectiveGoal.toLong())}"
                else "Mizupuku · ${settings.unit.format(total)} recorded"
            )
            .setContentText(
                if (effectiveGoal > 0) "${goalPercent(total, effectiveGoal)}% of your chosen water goal"
                else "Your water log"
            )
            .apply {
                if (effectiveGoal > 0) {
                    setProgress(100, goalPercent(total, effectiveGoal).coerceIn(0, 100).toInt(), false)
                }
            }
            .setColor(OceanTokens.LightBlue.toArgb())
            .setGroup("hydronudge_notifications")
            .setContentIntent(open())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setDeleteIntent(
                PendingIntent.getBroadcast(
                    context,
                    99,
                    Intent(context, DrinkActionReceiver::class.java).setAction("app.hydronudge.DISMISS_STATUS"),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .addAction(0, "+ ${settings.unit.format(firstActionMl.toLong())} Water", drinkActionIntent(firstActionMl))
            .addAction(0, "+ ${settings.unit.format(secondActionMl.toLong())} Water", drinkActionIntent(secondActionMl))
            .addAction(0, "Open app", open())
            .build()
        try {
            manager.notify(1, notification)
        } catch (_: SecurityException) {
        }
    }
}

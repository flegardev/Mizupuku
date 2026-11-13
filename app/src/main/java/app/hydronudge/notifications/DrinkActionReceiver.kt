package app.hydronudge.notifications

import android.app.NotificationManager
import android.content.*
import app.hydronudge.HydroApplication
import app.hydronudge.data.local.DrinkEntry
import app.hydronudge.reminders.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DrinkActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext as HydroApplication

        when (action) {
            "com.tiniflegar.mizupuku.DISMISS_STATUS" -> {
                val result = goAsync()
                app.applicationScope.launch {
                    try {
                        app.preferences.dismissStatus()
                    } finally {
                        result.finish()
                    }
                }
            }
            "com.tiniflegar.mizupuku.SNOOZE_REMINDER" -> {
                val result = goAsync()
                app.applicationScope.launch {
                    try {
                        context.getSystemService(NotificationManager::class.java)?.cancel(2)
                        app.preferences.pauseRemindersUntil(System.currentTimeMillis() + 30 * 60_000L)
                    } finally {
                        result.finish()
                    }
                }
            }
            "com.tiniflegar.mizupuku.SKIP_REMINDER" -> {
                context.getSystemService(NotificationManager::class.java)?.cancel(2)
            }
            "com.tiniflegar.mizupuku.ADD" -> {
                val token = intent.getStringExtra("token") ?: return
                val ml = intent.getIntExtra("ml", 0)
                if (ml !in 1..20000) return
                val dismissReminder = intent.getBooleanExtra("dismissReminder", false)
                val result = goAsync()
                app.applicationScope.launch {
                    try {
                        if (dismissReminder) {
                            context.getSystemService(NotificationManager::class.java)?.cancel(2)
                        }
                        app.repository.add(
                            DrinkEntry(
                                id = token,
                                name = "Water",
                                category = "WATER",
                                amountMl = ml
                            )
                        )
                    } finally {
                        result.finish()
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val result = goAsync()
                app.applicationScope.launch {
                    try {
                        val settings = app.preferences.settings.first()
                        ReminderScheduler.apply(context, settings)
                        app.refreshSurfaces()
                    } finally {
                        result.finish()
                    }
                }
            }
        }
    }
}

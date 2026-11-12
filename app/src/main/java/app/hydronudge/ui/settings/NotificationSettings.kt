package app.hydronudge.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.hydronudge.HydroApplication
import app.hydronudge.data.preferences.*
import app.hydronudge.notifications.HydroNotifications
import app.hydronudge.reminders.ReminderPolicy
import app.hydronudge.ui.components.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingToggle(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f))
        Switch(
            checked,
            change,
            modifier = Modifier
                .testTag("toggle:$label")
                .semantics { contentDescription = label }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationSettings(value: HydroSettings, save: (HydroSettings) -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (it) {
            (context.applicationContext as HydroApplication).let { app ->
                app.applicationScope.launch { app.refreshSurfaces() }
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    fun askIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            request.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var interval by rememberSaveable(value.intervalMinutes) { mutableStateOf(value.intervalMinutes.toString()) }
    var customMsg by rememberSaveable(value.customReminderMessage) { mutableStateOf(value.customReminderMessage) }
    fun timeLabel(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HydroSettingsGroup("Reminders", HydroIcons.Drop) {
            SettingToggle("Water reminders", value.reminders) {
                save(value.copy(reminders = it))
                if (it) askIfNeeded()
            }
            Text("Best effort. Android may delay reminders during Doze or Battery Saver.", style = MaterialTheme.typography.bodySmall)
            if (value.reminders) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 30, 45, 60, 90, 120).forEach { minutes ->
                        FilterChip(
                            selected = value.intervalMinutes == minutes,
                            onClick = { save(value.copy(intervalMinutes = minutes)) },
                            label = { Text("$minutes min") }
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m -> save(value.copy(wakeMinute = h * 60 + m)) },
                            value.wakeMinute / 60,
                            value.wakeMinute % 60,
                            DateFormat.is24HourFormat(context)
                        ).show()
                    }) { Text("Wake ${timeLabel(value.wakeMinute)}") }
                    OutlinedButton(onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m -> save(value.copy(sleepMinute = h * 60 + m)) },
                            value.sleepMinute / 60,
                            value.sleepMinute % 60,
                            DateFormat.is24HourFormat(context)
                        ).show()
                    }) { Text("Sleep ${timeLabel(value.sleepMinute)}") }
                }

                SettingsDisclosure("Reminder options", "settings:reminder-options") {
                    OutlinedTextField(
                        interval,
                        { interval = it },
                        label = { Text("Custom interval (15–1440 minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { interval.toIntOrNull()?.let { save(value.copy(intervalMinutes = it)) } },
                        enabled = interval.toIntOrNull() in 15..1440
                    ) { Text("Save interval") }

                    Text("Matching wake and sleep times allow reminders all day.", style = MaterialTheme.typography.bodySmall)

                    SettingToggle("Adaptive reminder mode (deterministic, no AI)", value.adaptiveReminders) {
                        save(value.copy(adaptiveReminders = it))
                    }

                    Text("Active days", style = MaterialTheme.typography.labelLarge)
                    val activeDays = value.activeDaysSet()
                    val dayLabels = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayLabels.forEach { (dayNum, label) ->
                            FilterChip(
                                selected = dayNum in activeDays,
                                onClick = {
                                    val updated = if (dayNum in activeDays && activeDays.size > 1) {
                                        activeDays - dayNum
                                    } else {
                                        activeDays + dayNum
                                    }
                                    save(value.copy(activeDaysCsv = updated.sorted().joinToString(",")))
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    SettingToggle("Separate weekend schedule", value.weekendScheduleEnabled) {
                        save(value.copy(weekendScheduleEnabled = it))
                    }
                    if (value.weekendScheduleEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m -> save(value.copy(weekendWakeMinute = h * 60 + m)) },
                                    value.weekendWakeMinute / 60,
                                    value.weekendWakeMinute % 60,
                                    DateFormat.is24HourFormat(context)
                                ).show()
                            }) { Text("Weekend wake ${timeLabel(value.weekendWakeMinute)}") }
                            OutlinedButton(onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m -> save(value.copy(weekendSleepMinute = h * 60 + m)) },
                                    value.weekendSleepMinute / 60,
                                    value.weekendSleepMinute % 60,
                                    DateFormat.is24HourFormat(context)
                                ).show()
                            }) { Text("Weekend sleep ${timeLabel(value.weekendSleepMinute)}") }
                        }
                    }

                    SettingToggle("Quiet hours window", value.quietHoursEnabled) {
                        save(value.copy(quietHoursEnabled = it))
                    }
                    if (value.quietHoursEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m -> save(value.copy(quietStartMinute = h * 60 + m)) },
                                    value.quietStartMinute / 60,
                                    value.quietStartMinute % 60,
                                    DateFormat.is24HourFormat(context)
                                ).show()
                            }) { Text("Quiet from ${timeLabel(value.quietStartMinute)}") }
                            OutlinedButton(onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m -> save(value.copy(quietEndMinute = h * 60 + m)) },
                                    value.quietEndMinute / 60,
                                    value.quietEndMinute % 60,
                                    DateFormat.is24HourFormat(context)
                                ).show()
                            }) { Text("Quiet until ${timeLabel(value.quietEndMinute)}") }
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReminderStyle.entries.forEach { style ->
                            FilterChip(
                                selected = value.style == style,
                                onClick = { save(value.copy(style = style)) },
                                label = {
                                    Text(
                                        if (style == ReminderStyle.FUNNY) "Aggressive / funny"
                                        else style.name.lowercase().replaceFirstChar { it.uppercase() }
                                    )
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        customMsg,
                        { customMsg = it.take(160) },
                        label = { Text("Custom reminder message (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (customMsg != value.customReminderMessage) {
                        Button(onClick = { save(value.copy(customReminderMessage = customMsg.trim())) }) {
                            Text("Save reminder message")
                        }
                    }

                    SettingToggle("Reminder sound", value.sound) { save(value.copy(sound = it)) }
                    SettingToggle("Reminder vibration", value.vibration) { save(value.copy(vibration = it)) }
                    SettingToggle("Skip reminders after reaching my goal", value.skipAtGoal) {
                        save(value.copy(skipAtGoal = it))
                    }
                    if (value.pausedUntil > System.currentTimeMillis()) {
                        Text(
                            "Paused until ${
                                Instant.ofEpochMilli(value.pausedUntil).atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("EEE HH:mm"))
                            }"
                        )
                        OutlinedButton(onClick = { save(value.copy(pausedUntil = 0)) }) {
                            Text("Resume reminders")
                        }
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { save(value.copy(pausedUntil = System.currentTimeMillis() + 30 * 60_000L)) }) {
                                Text("Snooze 30m")
                            }
                            OutlinedButton(onClick = { save(value.copy(pausedUntil = System.currentTimeMillis() + 2 * 3600000L)) }) {
                                Text("Pause for 2 hours")
                            }
                            if (granted) {
                                OutlinedButton(onClick = {
                                    HydroNotifications(context).remind(
                                        value,
                                        ReminderPolicy.resolveMessage(value, 50)
                                    )
                                }) {
                                    Text("Send test check-in")
                                }
                            }
                        }
                    }
                }
            }
        }
        HydroSettingsGroup("Notifications", HydroIcons.Settings) {
            SettingToggle("Hydration status notification", value.statusNotification) {
                save(value.copy(statusNotification = it, statusDismissed = false))
                if (it) askIfNeeded()
            }
            if (value.statusNotification && value.statusDismissed) {
                Text("Status dismissed. It stays hidden until you choose to show it again.")
                OutlinedButton(onClick = { save(value.copy(statusDismissed = false)) }) {
                    Text("Show status again")
                }
            }
            Text(
                "Today's total and quick-add buttons. Lock-screen visibility follows Android settings.",
                style = MaterialTheme.typography.bodySmall
            )
            if (!granted && (value.reminders || value.statusNotification)) {
                Text(
                    "Notifications are blocked. Allow them in Android settings.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }) { Text("Android notification settings") }
            Text("Sound and vibration follow Android channels and Do Not Disturb.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

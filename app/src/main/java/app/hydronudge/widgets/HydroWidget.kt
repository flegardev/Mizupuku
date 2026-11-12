package app.hydronudge.widgets

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.layout.*
import androidx.glance.material3.ColorProviders
import androidx.glance.text.*
import app.hydronudge.HydroApplication
import app.hydronudge.MainActivity
import app.hydronudge.data.local.DrinkEntry
import app.hydronudge.data.local.FavoriteDrink
import app.hydronudge.data.preferences.HydroSettings
import app.hydronudge.data.preferences.WidgetPreset
import app.hydronudge.data.preferences.WidgetThemeStyle
import app.hydronudge.domain.*
import app.hydronudge.reminders.ReminderPolicy
import app.hydronudge.reminders.ReminderScheduler
import app.hydronudge.ui.theme.HydroDarkColors
import app.hydronudge.ui.theme.HydroLightColors
import app.hydronudge.ui.theme.HydroOledDarkColors
import app.hydronudge.ui.theme.paletteColorScheme
import app.hydronudge.ui.theme.kissatenColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

class HydroWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(240.dp, 150.dp),
            DpSize(280.dp, 240.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as HydroApplication
        val initialEntries = app.repository.all()
        val initialFavorites = app.repository.snapshot().favorites
        val initialSettings = app.preferences.settings.first()
        provideContent {
            val entries by app.repository.entries.collectAsState(initialEntries)
            val favorites by app.repository.favorites.collectAsState(initialFavorites)
            val settings by app.preferences.settings.collectAsState(initialSettings)
            val zone = ZoneId.systemDefault()
            val todayDate = LocalDate.now()
            val w = dayWindow(todayDate, zone, settings.dayStartMinute)
            val today = entries.filter { it.isWater() && it.timestamp >= w.start && it.timestamp < w.endExclusive }
            val total = today.sumOf { it.amountMl.toLong() }
            val effectiveGoal = settings.effectiveGoalMl(todayDate.dayOfWeek.value)
            val token = remember(entries, settings, favorites) { UUID.randomUUID().toString() }
            val size = LocalSize.current
            val compact = size.height < 200.dp

            val lightScheme = kissatenColors(paletteColorScheme(
                palette = settings.palette,
                dark = false,
                oled = false,
                customAccentHex = settings.customAccentHex
            ))
            val darkScheme = kissatenColors(paletteColorScheme(
                palette = settings.palette,
                dark = true,
                oled = false,
                customAccentHex = settings.customAccentHex
            ))
            val oledScheme = kissatenColors(paletteColorScheme(
                palette = settings.palette,
                dark = true,
                oled = true,
                customAccentHex = settings.customAccentHex
            ), oled = true)

            val providers = when (settings.widgetThemeStyle) {
                WidgetThemeStyle.PASTEL_MIST, WidgetThemeStyle.LIGHT -> ColorProviders(light = lightScheme, dark = lightScheme)
                WidgetThemeStyle.DARK -> ColorProviders(light = darkScheme, dark = darkScheme)
                WidgetThemeStyle.OLED_BLACK -> ColorProviders(light = oledScheme, dark = oledScheme)
                else -> ColorProviders(light = lightScheme, dark = darkScheme)
            }

            GlanceTheme(colors = providers) {
                val bgModifier = if (settings.widgetThemeStyle == WidgetThemeStyle.TRANSPARENT) {
                    GlanceModifier.fillMaxSize().background(Color(0xCC061522)).appWidgetBackground().cornerRadius(28.dp)
                } else {
                    GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).appWidgetBackground().cornerRadius(28.dp)
                }

                Column(
                    bgModifier.padding(if (compact) 12.dp else 16.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    val q1 = settings.widgetQuickAmounts.getOrElse(0) { 250 }.coerceIn(1, 20000)
                    val q2 = settings.widgetQuickAmounts.getOrElse(1) { 500 }.coerceIn(1, 20000)

                    // Header row (always keeps "WATER" on small width <240dp and "Mizupuku" on >=240dp)
                    if (compact && size.width >= 240.dp) {
                        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text(
                                "Mizupuku",
                                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()),
                                style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            )
                            if (effectiveGoal > 0) {
                                Text(
                                    "${goalPercent(total, effectiveGoal)}%",
                                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                )
                            }
                        }
                    } else {
                        Text(
                            if (size.width >= 240.dp) "Mizupuku" else "WATER",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (compact) 12.sp else 14.sp
                            ),
                            modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>())
                        )
                        if (effectiveGoal > 0) {
                            Text(
                                "${goalPercent(total, effectiveGoal)}%",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (size.width < 240.dp) 22.sp else if (compact) 26.sp else 32.sp
                                )
                            )
                        }
                    }

                    if (size.width >= 240.dp) {
                        WidgetPresetBody(
                            preset = settings.widgetPreset,
                            settings = settings,
                            entries = entries,
                            today = today,
                            favorites = favorites,
                            total = total,
                            effectiveGoal = effectiveGoal,
                            compact = compact,
                            q1 = q1,
                            q2 = q2,
                            token = token
                        )
                    }

                    if (size.height >= 240.dp) {
                        Spacer(GlanceModifier.height(4.dp))
                        val last = today.maxByOrNull { it.timestamp }
                        val extraLine = when (settings.widgetPreset) {
                            WidgetPreset.CURRENT_STREAK -> {
                                val streak = calculateStreak(entries, todayDate, zone, effectiveGoal, settings.dayStartMinute)
                                "Streak: ${streak.currentStreakDays}d · Best: ${streak.longestStreakDays}d"
                            }
                            WidgetPreset.NEXT_REMINDER -> {
                                ReminderPolicy.nextReminderSummary(settings, ZonedDateTime.now(), total, last?.timestamp)
                            }
                            WidgetPreset.WEEKLY_CHART -> {
                                val report = historyReport(entries, todayDate.minusDays(6), todayDate, zone, effectiveGoal, settings.dayStartMinute)
                                "7d avg: ${settings.unit.format(report.averageRecordedDayMl)} · ${report.recordedDays}/7 days"
                            }
                            else -> {
                                if (last == null) "No water recorded today"
                                else "Last: Water · ${
                                    Instant.ofEpochMilli(last.timestamp).atZone(zone)
                                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                                }"
                            }
                        }
                        Text(
                            extraLine,
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp),
                            maxLines = 2
                        )
                    }

                    if (size.width < 240.dp) {
                        Text(
                            settings.unit.format(total),
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp),
                            modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPresetBody(
    preset: WidgetPreset,
    settings: HydroSettings,
    entries: List<DrinkEntry>,
    today: List<DrinkEntry>,
    favorites: List<FavoriteDrink>,
    total: Long,
    effectiveGoal: Int,
    compact: Boolean,
    q1: Int,
    q2: Int,
    token: String
) {
    val subline = when (preset) {
        WidgetPreset.WATER_GLASS -> {
            val glasses = (total / 250L)
            "${settings.unit.format(total)} ($glasses glasses today)"
        }
        WidgetPreset.BOTTLE_PROGRESS -> {
            val bottles = String.format(java.util.Locale.getDefault(), "%.1f", total / 500.0)
            "${settings.unit.format(total)} ($bottles × 500 mL bottles)"
        }
        WidgetPreset.GOAL_RING -> {
            if (effectiveGoal > 0) {
                val rem = (effectiveGoal - total).coerceAtLeast(0L)
                "${settings.unit.format(total)} · ${settings.unit.format(rem)} left"
            } else "${settings.unit.format(total)} recorded"
        }
        WidgetPreset.CURRENT_STREAK -> {
            val streak = calculateStreak(entries, LocalDate.now(), ZoneId.systemDefault(), effectiveGoal, settings.dayStartMinute)
            "${settings.unit.format(total)} · ${streak.currentStreakDays}-day streak"
        }
        WidgetPreset.NEXT_REMINDER -> {
            val last = today.maxByOrNull { it.timestamp }?.timestamp
            ReminderPolicy.nextReminderSummary(settings, ZonedDateTime.now(), total, last)
        }
        else -> {
            if (effectiveGoal > 0) "${settings.unit.format(total)} of ${settings.unit.format(effectiveGoal.toLong())}"
            else "${settings.unit.format(total)} recorded"
        }
    }

    Text(
        subline,
        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = if (compact) 12.sp else 14.sp)
    )
    Spacer(GlanceModifier.height(if (compact) 4.dp else 8.dp))
    if (effectiveGoal > 0 && preset != WidgetPreset.MINIMAL_COUNTER) {
        LinearProgressIndicator(
            progress = (total.toFloat() / effectiveGoal).coerceIn(0f, 1f),
            modifier = GlanceModifier.fillMaxWidth().height(4.dp),
            color = GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.surfaceVariant
        )
        Spacer(GlanceModifier.height(if (compact) 4.dp else 8.dp))
    }

    if (preset == WidgetPreset.FAVORITE_SHORTCUTS && favorites.size >= 2) {
        val f1 = favorites[0]
        val f2 = favorites[1]
        Row(GlanceModifier.fillMaxWidth()) {
            Button(
                "+ ${settings.unit.format(f1.amountMl.toLong())}",
                modifier = GlanceModifier.defaultWeight().height(48.dp),
                onClick = actionRunCallback<WidgetDrinkAction>(
                    actionParametersOf(
                        WidgetDrinkAction.Amount to f1.amountMl,
                        WidgetDrinkAction.Token to "$token:${f1.id}"
                    )
                )
            )
            Spacer(GlanceModifier.width(4.dp))
            Button(
                "+ ${settings.unit.format(f2.amountMl.toLong())}",
                modifier = GlanceModifier.defaultWeight().height(48.dp),
                onClick = actionRunCallback<WidgetDrinkAction>(
                    actionParametersOf(
                        WidgetDrinkAction.Amount to f2.amountMl,
                        WidgetDrinkAction.Token to "$token:${f2.id}"
                    )
                )
            )
        }
    } else {
        Row(GlanceModifier.fillMaxWidth()) {
            Button(
                "+ ${settings.unit.format(q1.toLong())}",
                modifier = GlanceModifier.defaultWeight().height(48.dp),
                onClick = actionRunCallback<WidgetDrinkAction>(
                    actionParametersOf(
                        WidgetDrinkAction.Amount to q1,
                        WidgetDrinkAction.Token to "$token:$q1"
                    )
                )
            )
            Spacer(GlanceModifier.width(4.dp))
            Button(
                "+ ${settings.unit.format(q2.toLong())}",
                modifier = GlanceModifier.defaultWeight().height(48.dp),
                onClick = actionRunCallback<WidgetDrinkAction>(
                    actionParametersOf(
                        WidgetDrinkAction.Amount to q2,
                        WidgetDrinkAction.Token to "$token:$q2"
                    )
                )
            )
        }
    }
}

class WidgetDrinkAction : ActionCallback {
    companion object {
        val Amount = ActionParameters.Key<Int>("amount")
        val Token = ActionParameters.Key<String>("token")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val amount = parameters[Amount] ?: return
        val token = parameters[Token] ?: return
        if (amount !in 1..20000) return
        val app = context.applicationContext as HydroApplication
        app.repository.add(
            DrinkEntry(
                id = "widget:$glanceId:$token",
                name = "Water",
                category = "WATER",
                amountMl = amount
            )
        )
        HydroWidget().updateAll(context)
    }
}

class HydroWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = HydroWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        schedule(context)
    }

    private fun schedule(context: Context) {
        val app = context.applicationContext as HydroApplication
        app.applicationScope.launch {
            ReminderScheduler.apply(context, app.preferences.settings.first())
        }
    }
}

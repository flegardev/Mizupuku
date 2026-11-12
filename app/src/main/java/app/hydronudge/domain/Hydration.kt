package app.hydronudge.domain

import app.hydronudge.data.local.DrinkEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Preserves all 12 canonical categories for schema/test compatibility while adding
 * rich metadata for optional beverage tracking (Water, Sparkling/Soda, Tea, Coffee,
 * Milk, Juice, Sports drinks, Smoothie, Protein, Soup, Energy, Custom).
 */
enum class DrinkCategory(
    val label: String,
    val symbol: String,
    val displayTitle: String = label,
    val defaultServingMl: Int = 250,
    val estimatedHydrationFactor: Double = 1.0,
    val colorHex: Long = 0xFF57B8F1
) {
    WATER("Water", "◉", "Water", 250, 1.0, 0xFF57B8F1),
    MILK("Milk", "◌", "Milk", 250, 1.0, 0xFFAEE4FF),
    COFFEE("Coffee", "☕", "Coffee", 200, 0.90, 0xFFD4A373),
    TEA("Tea", "♧", "Tea", 250, 0.98, 0xFF80E6D9),
    PROTEIN("Protein shake", "◈", "Protein shake", 350, 0.90, 0xFF98C1D9),
    ENERGY("Energy drink", "ϟ", "Energy drink", 250, 0.88, 0xFFFFCB77),
    SODA("Soda", "○", "Sparkling / Soda", 330, 0.98, 0xFF7EDCEB),
    JUICE("Fruit juice", "◐", "Fruit juice", 200, 0.92, 0xFFFF9F68),
    SMOOTHIE("Smoothie", "✿", "Smoothie", 300, 0.90, 0xFFF48FB1),
    SPORTS("Sports drink", "◇", "Sports drink", 500, 1.0, 0xFF48CAE4),
    SOUP("Soup or broth", "≈", "Soup or broth", 300, 0.92, 0xFFE9C46A),
    CUSTOM("Custom drink", "✦", "Other custom drink", 250, 1.0, 0xFF90E0EF);

    companion object {
        fun fromCode(code: String): DrinkCategory? = entries.firstOrNull { it.name == code }
    }
}

data class CupPreset(
    val id: String,
    val label: String,
    val subtitle: String,
    val amountMl: Int,
    val vesselType: String
)

val DefaultCupPresets = listOf(
    CupPreset("cup_150", "Yunomi Cup", "Small ceramic teacup", 150, "CUP"),
    CupPreset("glass_250", "Bathhouse Glass", "Standard tumbler", 250, "GLASS"),
    CupPreset("can_330", "Café Tall Glass", "Sparkling / mug", 330, "MUG"),
    CupPreset("bottle_500", "Travel Flask", "Compact desk bottle", 500, "BOTTLE"),
    CupPreset("sport_750", "Onsen Bottle", "Active flask", 750, "SPORT"),
    CupPreset("carafe_1000", "1L Carafe", "Tabletop pitcher", 1000, "CARAFE")
)

enum class VolumeUnit(val label: String, val millilitres: Double) {
    ML("mL", 1.0), US_OZ("US fl oz", 29.5735295625);

    fun toMl(value: Double): Int = kotlin.math.round(value * millilitres).toInt()
    fun format(ml: Long): String = if (this == ML) String.format(Locale.getDefault(), "%,d mL", ml)
    else String.format(Locale.getDefault(), "%.1f US fl oz", ml / millilitres)

    fun input(ml: Int): String = if (this == ML) ml.toString()
    else String.format(Locale.ROOT, "%.2f", ml / millilitres)
}

data class DayWindow(val start: Long, val endExclusive: Long)

fun dayWindow(date: LocalDate, zone: ZoneId): DayWindow = DayWindow(
    date.atStartOfDay(zone).toInstant().toEpochMilli(),
    date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
)

fun dayWindow(date: LocalDate, zone: ZoneId, dayStartMinute: Int): DayWindow {
    if (dayStartMinute <= 0) return dayWindow(date, zone)
    val clamped = dayStartMinute.coerceIn(0, 720)
    val startZdt = date.atStartOfDay(zone).plusMinutes(clamped.toLong())
    val endZdt = date.plusDays(1).atStartOfDay(zone).plusMinutes(clamped.toLong())
    return DayWindow(startZdt.toInstant().toEpochMilli(), endZdt.toInstant().toEpochMilli())
}

fun localDay(epochMillis: Long, zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

fun localDay(epochMillis: Long, zone: ZoneId, dayStartMinute: Int): LocalDate {
    if (dayStartMinute <= 0) return localDay(epochMillis, zone)
    val clamped = dayStartMinute.coerceIn(0, 720)
    return Instant.ofEpochMilli(epochMillis).atZone(zone).minusMinutes(clamped.toLong()).toLocalDate()
}

fun goalPercent(totalMl: Long, goalMl: Int): Long = if (goalMl > 0) (totalMl * 100L / goalMl) else 0

fun validateVolume(ml: Int) {
    require(ml in 1..20000) { "Enter a volume between 1 and 20,000 mL." }
}

fun roundVolumeMl(ml: Int, stepMl: Int): Int {
    if (stepMl <= 1) return ml.coerceIn(1, 20000)
    val rounded = ((ml.toDouble() / stepMl).roundToInt() * stepMl).coerceAtLeast(stepMl)
    return rounded.coerceIn(1, 20000)
}

/** Exact legacy identifier: unknown categories never become water. */
fun DrinkEntry.isWater(): Boolean = category == "WATER"

fun DrinkEntry.estimatedHydrationMl(): Int {
    val factor = DrinkCategory.fromCode(category)?.estimatedHydrationFactor ?: 1.0
    return (amountMl * factor).roundToInt().coerceIn(1, 20000)
}

data class StreakSummary(
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val todayActive: Boolean = false,
    val totalActiveDays: Int = 0
)

/**
 * Deterministic streak calculation directly from stored records.
 * Deleting or editing any entry automatically recomputes accurate streaks without corruption.
 */
fun calculateStreak(
    entries: List<DrinkEntry>,
    today: LocalDate,
    zone: ZoneId,
    goalMl: Int,
    dayStartMinute: Int = 0
): StreakSummary {
    val waterEntries = entries.filter { it.isWater() }
    if (waterEntries.isEmpty()) return StreakSummary()
    val byDay = waterEntries
        .groupBy { localDay(it.timestamp, zone, dayStartMinute) }
        .mapValues { (_, list) -> list.sumOf { it.amountMl.toLong() } }

    val threshold = if (goalMl > 0) goalMl.toLong() else 1L
    val qualifyingDays = byDay.filterValues { it >= threshold }.keys.sorted()
    val anyLoggedDays = byDay.filterValues { it > 0L }.keys.sorted()
    val activeSet = if (qualifyingDays.isNotEmpty()) qualifyingDays.toSet() else anyLoggedDays.toSet()
    val sortedDays = activeSet.sorted()
    if (sortedDays.isEmpty()) return StreakSummary(totalActiveDays = byDay.size)

    var longest = 1
    var run = 1
    for (i in 1 until sortedDays.size) {
        if (sortedDays[i - 1].plusDays(1) == sortedDays[i]) {
            run++
            if (run > longest) longest = run
        } else {
            run = 1
        }
    }

    val todayMet = today in activeSet
    var cursor = if (todayMet) today else today.minusDays(1)
    var current = 0
    while (cursor in activeSet) {
        current++
        cursor = cursor.minusDays(1)
    }

    return StreakSummary(
        currentStreakDays = current,
        longestStreakDays = maxOf(longest, current),
        todayActive = todayMet,
        totalActiveDays = byDay.size
    )
}

fun recentlyUsedAmounts(entries: List<DrinkEntry>, limit: Int = 4): List<Int> {
    val result = mutableListOf<Int>()
    for (entry in entries.sortedByDescending { it.timestamp }) {
        if (entry.amountMl in 1..20000 && entry.amountMl !in result) {
            result.add(entry.amountMl)
            if (result.size >= limit) break
        }
    }
    return result
}

fun motivationalMessage(totalMl: Long, goalMl: Int, streakDays: Int): String = when {
    totalMl <= 0L -> "Start whenever you feel thirsty — every sip is logged privately on your device."
    goalMl <= 0 -> if (streakDays > 1) "Calm & steady — $streakDays active days in your personal water journal." else "Your water log is updated. Drink at your own comfortable pace."
    totalMl >= goalMl -> "Daily target reached. Listen to your body and avoid over-drinking."
    totalMl * 4 >= goalMl * 3 -> "Almost at your chosen daily target — steady progress today."
    totalMl * 2 >= goalMl -> "Halfway through your chosen target for today."
    else -> "Gentle waves build steady habits — log whenever you take a drink."
}

/**
 * Computes a calm, non-medical daytime pacing summary comparing today's logged intake
 * against the fraction of the user's waking window elapsed so far.
 */
fun hydrationPaceSummary(
    totalMl: Long,
    goalMl: Int,
    currentMinuteOfDay: Int,
    wakeMinute: Int,
    sleepMinute: Int,
    unit: VolumeUnit
): String {
    if (goalMl <= 0) return if (totalMl > 0) "${unit.format(totalMl)} logged" else "Flexible pace"
    if (totalMl >= goalMl) return "Goal reached"
    val span = (sleepMinute - wakeMinute).let { if (it <= 0) 840 else it }
    val elapsed = (currentMinuteOfDay - wakeMinute).coerceIn(0, span)
    val expectedMl = (goalMl.toLong() * elapsed) / span
    val delta = totalMl - expectedMl
    return when {
        elapsed == 0 -> "Morning start"
        delta >= 150L -> "Ahead of pace (+${unit.format(delta)})"
        delta >= -200L -> "Right on pace"
        else -> "Gentle catch-up (${unit.format(-delta)} behind)"
    }
}

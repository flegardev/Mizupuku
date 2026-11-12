package app.hydronudge.domain

import app.hydronudge.data.local.DrinkEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class MilestoneBadge(
    val id: String,
    val title: String,
    val subtitle: String,
    val achieved: Boolean
)

data class HistoryReport(
    val entries: List<DrinkEntry>,
    val totalMl: Long,
    val recordedDays: Int,
    val averageRecordedDayMl: Long,
    val goalDays: Int,
    val dailyMl: List<Long>,
    val dailyDates: List<LocalDate> = emptyList(),
    val calendarDays: Int = dailyMl.size,
    val averageCalendarDayMl: Long = if (dailyMl.isEmpty()) 0L else totalMl / dailyMl.size,
    val consistencyPercent: Int = if (dailyMl.isEmpty()) 0 else ((recordedDays * 100L) / dailyMl.size).toInt().coerceIn(0, 100),
    val mostFrequentDrinkName: String? = null,
    val mostFrequentServingMl: Int? = null,
    val beverageBreakdown: Map<String, Long> = emptyMap(),
    val totalBeverageMl: Long = totalMl,
    val estimatedHydrationMl: Long = totalMl,
    val peakDayMl: Long = dailyMl.maxOrNull() ?: 0L,
    val hourlyMl: List<Long> = List(24) { 0L },
    val hydrationScore: Int = 0
)

fun historyReport(
    records: List<DrinkEntry>,
    start: LocalDate,
    end: LocalDate,
    zone: ZoneId,
    goalMl: Int,
    dayStartMinute: Int = 0
): HistoryReport {
    require(!end.isBefore(start))
    val windowStart = dayWindow(start, zone, dayStartMinute).start
    val windowEnd = dayWindow(end, zone, dayStartMinute).endExclusive
    val allWindowEntries = records.filter { it.timestamp >= windowStart && it.timestamp < windowEnd }
    val entries = allWindowEntries.filter { it.isWater() }
    val days = entries.groupBy { localDay(it.timestamp, zone, dayStartMinute) }
        .mapValues { (_, drinks) -> drinks.sumOf { it.amountMl.toLong() } }
    val total = entries.sumOf { it.amountMl.toLong() }
    val dateList = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    val dailyVolumes = dateList.map { days[it] ?: 0L }
    val calDays = dateList.size.coerceAtLeast(1)

    val frequentName = entries.groupingBy { it.name.ifBlank { "Water" } }.eachCount().maxByOrNull { it.value }?.key
    val frequentServing = entries.groupingBy { it.amountMl }.eachCount().maxByOrNull { it.value }?.key
    val breakdown = allWindowEntries.groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amountMl.toLong() } }
        .toList()
        .sortedByDescending { it.second }
        .toMap()
    val totalBev = allWindowEntries.sumOf { it.amountMl.toLong() }
    val estHydration = allWindowEntries.sumOf { it.estimatedHydrationMl().toLong() }

    val hourlyBuckets = MutableList(24) { 0L }
    for (entry in entries) {
        val hour = Instant.ofEpochMilli(entry.timestamp).atZone(zone).hour.coerceIn(0, 23)
        hourlyBuckets[hour] += entry.amountMl.toLong()
    }

    val consistency = ((days.size * 100L) / calDays).toInt().coerceIn(0, 100)
    val avgRecorded = if (days.isEmpty()) 0L else total / days.size
    val score = if (entries.isEmpty()) 0 else if (goalMl > 0) {
        val goalRatio = ((avgRecorded * 100L) / goalMl).coerceIn(0L, 100L).toInt()
        ((consistency * 45 + goalRatio * 55) / 100).coerceIn(1, 100)
    } else {
        consistency
    }

    return HistoryReport(
        entries = entries,
        totalMl = total,
        recordedDays = days.size,
        averageRecordedDayMl = avgRecorded,
        goalDays = if (goalMl > 0) days.values.count { it >= goalMl } else 0,
        dailyMl = dailyVolumes,
        dailyDates = dateList,
        calendarDays = calDays,
        averageCalendarDayMl = total / calDays,
        consistencyPercent = consistency,
        mostFrequentDrinkName = frequentName,
        mostFrequentServingMl = frequentServing,
        beverageBreakdown = breakdown,
        totalBeverageMl = totalBev,
        estimatedHydrationMl = estHydration,
        peakDayMl = dailyVolumes.maxOrNull() ?: 0L,
        hourlyMl = hourlyBuckets,
        hydrationScore = score
    )
}

fun computeMilestones(
    allRecords: List<DrinkEntry>,
    streak: StreakSummary,
    goalMl: Int,
    zone: ZoneId,
    dayStartMinute: Int = 0
): List<MilestoneBadge> {
    val waterRecords = allRecords.filter { it.isWater() }
    val totalVolumeMl = waterRecords.sumOf { it.amountMl.toLong() }
    val byDay = waterRecords.groupBy { localDay(it.timestamp, zone, dayStartMinute) }
        .mapValues { (_, list) -> list.sumOf { it.amountMl.toLong() } }
    val goalReachedDays = if (goalMl > 0) byDay.values.count { it >= goalMl } else 0

    return listOf(
        MilestoneBadge("first_drop", "First Drop", "Logged your first water entry", waterRecords.isNotEmpty()),
        MilestoneBadge("three_day_wave", "3-Day Ripple", "3-day active hydration streak", streak.longestStreakDays >= 3),
        MilestoneBadge("seven_day_tide", "7-Day Tide", "Full week consistency streak", streak.longestStreakDays >= 7),
        MilestoneBadge("thirty_day_ocean", "Ocean Explorer", "30 recorded days in your journal", byDay.size >= 30),
        MilestoneBadge("ten_liters", "10 Liter Current", "Recorded 10,000 mL total water", totalVolumeMl >= 10_000L),
        MilestoneBadge("fifty_entries", "Steady Logger", "Logged 50 individual drink entries", waterRecords.size >= 50),
        MilestoneBadge("goal_keeper", "Target Keeper", "Reached your chosen daily goal 7 times", goalReachedDays >= 7)
    )
}

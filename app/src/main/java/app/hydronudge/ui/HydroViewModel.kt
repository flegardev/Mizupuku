package app.hydronudge.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.hydronudge.HydroApplication
import app.hydronudge.data.BackupCodec
import app.hydronudge.data.BackupPreview
import app.hydronudge.data.local.DrinkEntry
import app.hydronudge.data.local.FavoriteDrink
import app.hydronudge.data.preferences.HydroSettings
import app.hydronudge.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream
import java.time.*

data class HydroState(
    val entries: List<DrinkEntry> = emptyList(),
    val favorites: List<FavoriteDrink> = emptyList(),
    val settings: HydroSettings = HydroSettings(),
    val date: LocalDate = LocalDate.now(),
    val zone: ZoneId = ZoneId.systemDefault(),
    val ready: Boolean = false,
    val allEntries: List<DrinkEntry> = entries
) {
    val todayWindow: DayWindow
        get() = dayWindow(date, zone, settings.dayStartMinute)

    val today: List<DrinkEntry>
        get() {
            val w = todayWindow
            return entries.filter { it.isWater() && it.timestamp >= w.start && it.timestamp < w.endExclusive }
        }

    val todayAllBeverages: List<DrinkEntry>
        get() {
            val w = todayWindow
            return allEntries.filter { it.timestamp >= w.start && it.timestamp < w.endExclusive }
        }

    val totalMl: Long
        get() = today.sumOf { it.amountMl.toLong() }

    val totalBeverageMl: Long
        get() = todayAllBeverages.sumOf { it.amountMl.toLong() }

    val estimatedHydrationMl: Long
        get() = todayAllBeverages.sumOf { it.estimatedHydrationMl().toLong() }

    val effectiveGoalMl: Int
        get() = settings.effectiveGoalMl(date.dayOfWeek.value)

    val streak: StreakSummary
        get() = calculateStreak(entries, date, zone, effectiveGoalMl, settings.dayStartMinute)
}

data class Feedback(
    val message: String,
    val undoEntry: DrinkEntry? = null,
    val undoDeletes: Boolean = false,
    val previousEditedEntry: DrinkEntry? = null,
    val undoBatch: List<DrinkEntry> = emptyList()
)

class HydroViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HydroApplication
    private val clock = flow {
        while (currentCoroutineContext().isActive) {
            val now = ZonedDateTime.now()
            emit(now.toLocalDate() to now.zone)
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(minOf(30000L, Duration.between(now, midnight).toMillis().coerceAtLeast(1)))
        }
    }
    private val events = Channel<Feedback>(Channel.UNLIMITED)
    val feedback = events.receiveAsFlow()

    val state = combine(
        app.repository.entries,
        app.repository.favorites,
        app.preferences.settings,
        clock
    ) { entries, favorites, settings, now ->
        HydroState(
            entries = entries.filter { it.isWater() },
            favorites = favorites,
            settings = settings,
            date = now.first,
            zone = now.second,
            ready = true,
            allEntries = entries
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HydroState())

    fun quickAdd(ml: Int) {
        val step = state.value.settings.roundingStepMl
        val finalMl = roundVolumeMl(ml, step)
        save(DrinkEntry(name = "Water", category = DrinkCategory.WATER.name, amountMl = finalMl))
    }

    fun logFavorite(favorite: FavoriteDrink) = perform {
        val allowBeverages = state.value.settings.beverageTrackingEnabled
        val category = if (allowBeverages) favorite.category else DrinkCategory.WATER.name
        val name = if (allowBeverages) favorite.name else "Water"
        val entry = DrinkEntry(name = name, category = category, amountMl = favorite.amountMl)
        app.repository.addBeverage(entry, allowBeverages)
        events.send(Feedback(if (category == "WATER") "Water added" else "${favorite.name} added", entry))
    }

    fun saveFavorite(favorite: FavoriteDrink) = perform {
        app.repository.saveFavorite(favorite)
        events.send(Feedback("Saved favorite preset: ${favorite.name}"))
    }

    fun deleteFavorite(id: String) = perform {
        app.repository.deleteFavorite(id)
        events.send(Feedback("Favorite preset removed"))
    }

    fun save(entry: DrinkEntry, editing: Boolean = false) = perform {
        val allowBeverages = state.value.settings.beverageTrackingEnabled
        val previous = if (editing) state.value.allEntries.firstOrNull { it.id == entry.id } else null
        if (editing) {
            app.repository.editBeverage(entry, allowBeverages)
        } else {
            app.repository.addBeverage(entry, allowBeverages)
        }
        val label = if (entry.isWater()) "Water" else entry.name
        events.send(
            Feedback(
                message = if (editing) "$label updated" else "$label added",
                undoEntry = entry,
                undoDeletes = false,
                previousEditedEntry = previous
            )
        )
    }

    fun delete(entry: DrinkEntry) = perform {
        app.repository.delete(entry.id)
        val label = if (entry.isWater()) "Water" else entry.name
        events.send(Feedback("$label deleted", entry, undoDeletes = true))
    }

    fun clearToday() = perform {
        val current = state.value
        val toClear = if (current.settings.beverageTrackingEnabled) current.todayAllBeverages else current.today
        if (toClear.isEmpty()) return@perform
        val w = current.todayWindow
        app.repository.deleteRange(w.start, w.endExclusive)
        events.send(Feedback("Cleared ${toClear.size} entries from today", undoBatch = toClear))
    }

    fun undo(event: Feedback) = perform {
        if (event.undoBatch.isNotEmpty()) {
            app.repository.restoreMany(event.undoBatch)
            return@perform
        }
        if (event.previousEditedEntry != null) {
            app.repository.editBeverage(event.previousEditedEntry, allowBeverageCategories = true)
            return@perform
        }
        event.undoEntry?.let {
            if (event.undoDeletes) app.repository.restore(it) else app.repository.delete(it.id)
        }
    }

    fun saveSettings(settings: HydroSettings) = perform { app.preferences.save(settings) }

    fun clear() = perform {
        app.repository.clearAll()
        app.preferences.save(HydroSettings())
        events.send(Feedback("All local Mizupuku data deleted"))
    }

    fun export(uri: Uri) = perform {
        withContext(Dispatchers.IO) {
            val currentSettings = app.preferences.settings.first()
            val json = BackupCodec.encodeFull(app.repository.snapshot(), currentSettings)
            val stream = app.contentResolver.openOutputStream(uri, "wt") ?: error("Could not open the export file.")
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
        }
        events.send(Feedback("Backup exported, including preserved legacy records"))
    }

    fun exportCsv(uri: Uri) = perform {
        withContext(Dispatchers.IO) {
            val csv = BackupCodec.encodeCsv(app.repository.all(), state.value.zone)
            val stream = app.contentResolver.openOutputStream(uri, "wt") ?: error("Could not open the CSV export file.")
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(csv) }
        }
        events.send(Feedback("CSV history exported"))
    }

    private fun readSafeBackupBytes(uri: Uri): String {
        val stream = app.contentResolver.openInputStream(uri) ?: error("Could not open the backup.")
        val limit = 32 * 1024 * 1024
        val bytes = stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= limit) { "This backup exceeds the 32 MiB safe import size." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        return bytes.toString(Charsets.UTF_8)
    }

    fun inspectBackup(uri: Uri, onPreviewReady: (BackupPreview) -> Unit) = perform {
        val preview = withContext(Dispatchers.IO) {
            val json = readSafeBackupBytes(uri)
            val snap = app.repository.snapshot()
            BackupCodec.preview(
                json,
                snap.entries.map { it.id }.toSet(),
                snap.favorites.map { it.id }.toSet()
            )
        }
        onPreviewReady(preview)
    }

    fun import(uri: Uri) = perform {
        val count = withContext(Dispatchers.IO) {
            val json = readSafeBackupBytes(uri)
            val full = BackupCodec.decodeFull(json)
            val inserted = app.repository.merge(full.drinkBackup)
            full.settings?.let { restoredSettings ->
                app.preferences.save(restoredSettings)
            }
            inserted
        }
        events.send(Feedback("Imported $count new records; existing records kept"))
    }

    private fun perform(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                events.send(Feedback(e.message ?: "Unable to save. Please try again."))
            }
        }
    }
}

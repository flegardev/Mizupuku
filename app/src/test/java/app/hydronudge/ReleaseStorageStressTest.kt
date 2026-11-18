package app.hydronudge

import android.app.Application
import androidx.room.Room
import app.hydronudge.data.DrinkRepository
import app.hydronudge.data.local.DrinkEntry
import app.hydronudge.data.local.HydroDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ReleaseStorageStressTest {
    @Test fun fiveThousandDrinkRecordsSurviveReopenAndDuplicateRestore() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "release-stress-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, HydroDatabase::class.java, name).build()
        var db = open()
        try {
            val rows = (0 until 5000).map { DrinkEntry(id = "fixture-$it", name = "Water",
                category = "WATER", amountMl = 150 + it % 350, timestamp = 1700000000000L + it * 60000L) }
            var repository = DrinkRepository(db.drinks())
            assertEquals(5000, repository.restoreMany(rows))
            db.close(); db = open(); repository = DrinkRepository(db.drinks())
            assertEquals(rows.associate { it.id to it.amountMl }, repository.all().associate { it.id to it.amountMl })
            assertEquals(0, repository.restoreMany(rows))
            assertEquals(rows.sumOf { it.amountMl.toLong() }, repository.all().sumOf { it.amountMl.toLong() })
        } finally { db.close(); context.deleteDatabase(name) }
    }
}

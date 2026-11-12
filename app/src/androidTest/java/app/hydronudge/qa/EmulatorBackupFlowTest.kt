package app.hydronudge.qa

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import app.hydronudge.HydroApplication
import app.hydronudge.MainActivity
import app.hydronudge.data.local.DrinkEntry
import app.hydronudge.data.local.DrinkBackup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Exercises the real Android document picker. Deletes only its own fixture and export file. */
class EmulatorBackupFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun exportAndMergeThroughAndroidDocuments() {
        assumeTrue("Emulator only", Build.MODEL.contains("sdk", true))
        val app = compose.activity.application as HydroApplication
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val marker = UUID.randomUUID().toString()
        val entry = DrinkEntry(id="qa-backup:$marker",name="Backup QA",category="CUSTOM",amountMl=123)
        val filename = "HydroNudge-QA-$marker.json"
        val originalIds = runBlocking { app.repository.all() }.map { it.id }.toSet()
        try {
            runBlocking { app.repository.merge(DrinkBackup(listOf(entry),emptyList())) }
            compose.waitUntil(20000) { compose.onAllNodesWithTag("nav:settings").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("nav:settings").performClick()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Export full backup"))
            compose.onNodeWithText("Export full backup").performScrollTo().performClick()
            device.wait(Until.hasObject(By.clazz("android.widget.EditText")),10000)
            Thread.sleep(1000)
            device.dumpWindowHierarchy(java.io.File(QaArtifacts.folder,"backup-create-picker.xml"))
            val input = device.wait(Until.findObject(By.clazz("android.widget.EditText")),10000)
            checkNotNull(input) { "Android create-document filename input unavailable" }.apply { click(); clear(); text=filename }
            Thread.sleep(500)
            device.dumpWindowHierarchy(java.io.File(QaArtifacts.folder,"backup-named-picker.xml"))
            checkNotNull(device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("(?i)^save$"))),5000)).click()
            check(device.wait(Until.hasObject(By.pkg(app.packageName)),15000)) { "Document picker did not return to HydroNudge after Save" }
            compose.waitUntil(20000) { compose.onAllNodesWithText("Backup exported, including preserved legacy records").fetchSemanticsNodes().isNotEmpty() }
            if(compose.onAllNodesWithContentDescription("Dismiss").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithContentDescription("Dismiss").performClick()
            runBlocking { app.repository.delete(entry.id) }
            compose.onNodeWithText("Import backup").performScrollTo().performClick()
            compose.onNodeWithText("Choose backup").performClick()
            val file = device.wait(Until.findObject(By.text(filename)),10000)
            checkNotNull(file) { "Exported QA document not visible in Android picker" }.click()
            compose.waitUntil(20000) { runBlocking { app.repository.all() }.any { it.id==entry.id } }
            assertEquals(entry,runBlocking { app.repository.all() }.single { it.id==entry.id })
            assertTrue(runBlocking { app.repository.all() }.map { it.id }.containsAll(originalIds))
            QaArtifacts.note("PASS actual Android create/open-document export/import; stable ID and fields restored; existing IDs retained")
        } finally {
            runBlocking { app.repository.delete(entry.id) }
            QaArtifacts.shell("rm -f /sdcard/Download/$filename")
            assertEquals(originalIds,runBlocking { app.repository.all() }.map { it.id }.toSet())
            QaArtifacts.logcat("backup")
        }
    }
}

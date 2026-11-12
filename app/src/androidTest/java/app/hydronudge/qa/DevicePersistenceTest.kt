package app.hydronudge.qa

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.hydronudge.*
import app.hydronudge.data.local.DrinkEntry
import app.hydronudge.domain.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DevicePersistenceSeedTest {
    @Test fun seedRestartRecordWithoutChangingExistingData() = runBlocking {
        val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as HydroApplication
        val date=LocalDate.now();val zone=ZoneId.systemDefault()
        val before=app.repository.all().filter {it.isWater() && localDay(it.timestamp,zone)==date}.sumOf {it.amountMl.toLong()}
        val entry=DrinkEntry(id="qa-process:${UUID.randomUUID()}",name="QA restart persistence",category="WATER",amountMl=123)
        app.repository.add(entry)
        File(QaArtifacts.folder,"restart-marker.json").writeText(JSONObject().put("id",entry.id).put("expectedTotal",before+123).put("date",date.toString()).toString())
        QaArtifacts.note("PASS restart seed committed to Room; original records unchanged")
    }
}

@RunWith(AndroidJUnit4::class)
class DevicePersistenceVerifyTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun verifyRecordAfterExternalForceStopAndRelaunch() = runBlocking {
        val app=compose.activity.application as HydroApplication
        val marker=JSONObject(File(QaArtifacts.folder,"restart-marker.json").readText())
        val id=marker.getString("id")
        require(id.startsWith("qa-process:"))
        try {
            val entries=app.repository.all()
            val entry=entries.single {it.id==id}
            assertEquals(123,entry.amountMl)
            if(marker.getString("date")==LocalDate.now().toString()) {
                val total=entries.filter {it.isWater() && localDay(it.timestamp,ZoneId.systemDefault())==LocalDate.now()}.sumOf {it.amountMl.toLong()}
                assertEquals(marker.getLong("expectedTotal"),total)
            }
            compose.waitUntil(20000) {compose.onAllNodesWithTag("dashboard-list").fetchSemanticsNodes().isNotEmpty()}
            QaArtifacts.screenshot(compose.activity,"09-after-process-restart")
            QaArtifacts.note("PASS real app process restart: same Room record ID, volume and daily total retained")
        } finally {app.repository.delete(id);QaArtifacts.note("CLEANUP only restart seed record removed");QaArtifacts.logcat("restart")}
    }
}

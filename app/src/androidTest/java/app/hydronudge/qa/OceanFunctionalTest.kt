package app.hydronudge.qa

import android.os.Build
import android.app.NotificationManager
import android.Manifest
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.hydronudge.*
import app.hydronudge.data.*
import app.hydronudge.data.local.*
import app.hydronudge.data.preferences.*
import app.hydronudge.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.time.*

/** Disposable emulator fixtures; full original snapshot is exported before mutation and restored. */
open class OceanFixture {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    val app get()=compose.activity.application as HydroApplication
    lateinit var original: DrinkBackup
    lateinit var settings: HydroSettings
    fun records()=runBlocking {app.repository.all()}
    fun waitFor(check:()->Boolean)=compose.waitUntil(20000,check)
    fun tag(value:String)=compose.onNodeWithTag(value)
    fun dashboardTag(value:String): SemanticsNodeInteraction {
        tag("dashboard-list").performScrollToNode(hasTestTag(value))
        return tag(value)
    }
    fun dismiss() { if(compose.onAllNodesWithContentDescription("Dismiss").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithContentDescription("Dismiss").performClick() }
    fun capture(name:String) {
        compose.waitForIdle(); Thread.sleep(1000); compose.waitForIdle()
        check(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(QaArtifacts.folder,"$name.png")))
    }
    fun prefs(value:HydroSettings)=runBlocking {app.preferences.save(value)}
    fun seed(ml:Int,id:String="fixture")=runBlocking {app.repository.add(DrinkEntry(id=id,name="Water",category="WATER",amountMl=ml))}
    fun top()=tag("dashboard-list").performScrollToIndex(0)
    fun percent(value:Long) { tag("dashboard-list").performScrollToNode(hasText("$value% completed")); waitFor {compose.onAllNodesWithText("$value% completed").fetchSemanticsNodes().isNotEmpty()} }
    @Before fun isolate() {
        check(Build.MODEL.contains("sdk",true) && QaArtifacts.shell("getprop ro.kernel.qemu").trim()=="1") {"Emulator required"}
        original=runBlocking {app.repository.snapshot()}; settings=runBlocking {app.preferences.settings.first()}
        File(QaArtifacts.folder,"before-fixture.json").writeText(BackupCodec.encode(original))
        runBlocking {app.repository.clearAll()}
        // A goal is optional in production. These percentage scenarios explicitly
        // choose their own deterministic 2500 mL fixture goal.
        prefs(HydroSettings(goalMl=2500,theme=ThemeMode.DARK,dynamicColors=false))
        waitFor {compose.onAllNodesWithTag("dashboard-list").fetchSemanticsNodes().isNotEmpty()}
    }
    @After fun restore() {
        if(::original.isInitialized) runBlocking {app.repository.clearAll();app.repository.merge(original);app.preferences.save(settings)}
        if(::original.isInitialized) assertEquals(original,runBlocking {app.repository.snapshot()})
    }
}

open class OceanFunctionalTest : OceanFixture() {
    @Test fun completeThemeStudioScrollsWhileDoneRemainsReachable() {
        top()
        tag("open-theme-studio").performClick()
        compose.onNodeWithText("Done").assertIsDisplayed()
        compose.onNodeWithText("Hydration Vessel Style").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Done").assertIsDisplayed().performClick()
        tag("dashboard-list").assertExists()
    }
    @Test fun quickServingIsVisibleOnOpeningDashboard() {
        top()
        tag("quick-add:250").assertIsDisplayed()
    }
    @Test fun largeEntryAndQuickServingRequireReviewBeforeWriting() {
        dashboardTag("choose-drink").performClick()
        tag("drink-amount").performTextReplacement("2000")
        tag("save-drink").performClick()
        compose.onNodeWithText("Check the amount").assertIsDisplayed()
        assertTrue(records().isEmpty())
        compose.onNodeWithText("Review amount").performClick()
        tag("drink-amount").performTextReplacement("250")
        tag("save-drink").performClick()
        waitFor { records().size == 1 }
        assertEquals(250, records().single().amountMl)
        prefs(runBlocking { app.preferences.settings.first() }.copy(quickAmounts=listOf(2000,250,330,500)))
        dismiss()
        dashboardTag("quick-add:2000").performClick()
        compose.onNodeWithText("Check the amount").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(1, records().size)
        dashboardTag("quick-add:2000").performClick()
        compose.onNodeWithText("Log entry").performClick()
        waitFor { records().size == 2 }
        assertTrue(records().any { it.amountMl == 2000 })
    }
    @Test fun quickCustomEditDeleteUndoGoalsHistoryAndSettings() {
        percent(0)
        for(ml in listOf(150,250,330,500,250)) {
            dismiss(); val before=records().size
            dashboardTag("quick-add:$ml").performClick()
            waitFor {records().size==before+1}
            assertEquals(ml,records().first().amountMl);assertTrue(records().all {it.isWater()})
        }
        assertEquals(1480L,records().sumOf {it.amountMl.toLong()})
        dismiss();dashboardTag("choose-drink").performClick()
        for(value in listOf("0","-1","20001","999999999999","invalid")) {
            tag("drink-amount").performScrollTo().performTextReplacement(value)
            tag("save-drink").assertIsNotEnabled()
        }
        tag("drink-amount").performTextReplacement("270")
        tag("water-note").performScrollTo().performTextReplacement("Water QA note")
        // Save must work while IME remains visible, without an extra confirmation.
        tag("save-drink").assertIsDisplayed().performClick()
        waitFor {records().size==6};percent(70)
        val saved=records().first();assertEquals("Water QA note",saved.note)
        dismiss();tag("dashboard-list").performScrollToNode(hasTestTag("drink:${saved.id}"))
        tag("actions:${saved.id}").performClick();tag("edit:${saved.id}").performClick()
        tag("drink-amount").performTextReplacement("300")
        tag("save-drink").performClick();waitFor {records().single {it.id==saved.id}.amountMl==300}
        assertEquals(6,records().size)
        dismiss();tag("dashboard-list").performScrollToNode(hasTestTag("drink:${saved.id}"))
        tag("actions:${saved.id}").performClick();tag("delete:${saved.id}").performClick()
        waitFor {records().none {it.id==saved.id}}
        compose.onNodeWithText("Undo").performClick();waitFor {records().any {it.id==saved.id}}
        assertEquals(300,records().single {it.id==saved.id}.amountMl)
        dismiss();dashboardTag("choose-drink").performClick()
        compose.onNodeWithText("Cancel").performClick();assertEquals(6,records().size)
        tag("nav:history").performClick()
        for(range in listOf("Today","7 days","30 days","Month")) {
            tag("history-list").performScrollToIndex(0);tag("history-range:$range").performScrollTo().performClick()
            tag("history-list").assertExists()
        }
        tag("nav:settings").performClick();tag("daily-goal").performTextReplacement("3000")
        androidx.test.espresso.Espresso.closeSoftKeyboard();tag("save-goal").performScrollTo().performClick()
        waitFor {runBlocking {app.preferences.settings.first()}.goalMl==3000}
        tag("settings-list").performScrollToNode(hasTestTag("theme:LIGHT"));tag("theme:LIGHT").performClick()
        waitFor {runBlocking {app.preferences.settings.first()}.theme==ThemeMode.LIGHT}
        compose.activityRule.scenario.recreate()
        assertEquals(3000,runBlocking {app.preferences.settings.first()}.goalMl)
        tag("nav:today").performClick();percent(59)
        QaArtifacts.note("PASS quick taps/custom/IME/edit identity/delete/Undo/cancel/history ranges/goal/theme/recreation")
    }
    @Test fun legacyFilteringBackupAtomicityAndLargeHistory() {
        val categories=listOf("WATER","COFFEE","MILK","TEA","PROTEIN","LIQUID","CUSTOM","UNKNOWN","water")
        val entries=categories.mapIndexed {i,c->DrinkEntry(id="legacy:$i",name=c,category=c,amountMl=250)}
        val backup=DrinkBackup(entries,emptyList())
        runBlocking { assertEquals(9,app.repository.merge(backup));assertEquals(0,app.repository.merge(backup)) }
        percent(10);assertEquals(9,records().size)
        assertEquals(backup.entries.toSet(),BackupCodec.decode(BackupCodec.encode(runBlocking {app.repository.snapshot()})).entries.toSet())
        val day=LocalDate.now();val zone=ZoneId.systemDefault()
        for(days in listOf(1L,7L,30L)) {
            val report=historyReport(records(),day.minusDays(days-1),day,zone,2500)
            assertEquals(250L,report.totalMl);assertEquals(1,report.recordedDays);assertEquals(0,report.goalDays)
        }
        try {runBlocking {app.repository.merge(DrinkBackup(listOf(entries.first().copy(id="valid"),entries.first().copy(id="bad",amountMl=-1)),emptyList()))};fail("Invalid import accepted")} catch(_:IllegalArgumentException){}
        assertEquals(9,records().size)
        val many=List(10000) {DrinkEntry(id="large:$it",name="Water",category="WATER",amountMl=1,timestamp=dayWindow(day.minusDays((it%30).toLong()),zone).start)}
        val started=System.nanoTime();val report=historyReport(many,day.minusDays(29),day,zone,100)
        assertEquals(10000L,report.totalMl);assertEquals(30,report.recordedDays)
        QaArtifacts.note("MEASURE 10000-record history aggregation ${(System.nanoTime()-started)/1000000}ms; not a UI benchmark")
    }
    @Test fun nativeTimePickerRejectsFutureAndKeepsPastTimestamp() {
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val clockSetting=QaArtifacts.shell("settings get system time_12_24").trim()
        try {
            QaArtifacts.shell("settings put system time_12_24 24")
            dashboardTag("choose-drink").performClick()
            compose.onNodeWithText("Change time").performScrollTo().performClick()
            device.waitForIdle()
            device.dumpWindowHierarchy(File(QaArtifacts.folder,"time-picker.xml"))
            device.findObject(androidx.test.uiautomator.By.res("android:id/toggle_mode"))?.click()
            val hour=device.wait(androidx.test.uiautomator.Until.findObject(androidx.test.uiautomator.By.res("android:id/input_hour")),5000)
            checkNotNull(hour) {"Native time input unavailable; inspect time-picker.xml"}
            val minute=checkNotNull(device.findObject(androidx.test.uiautomator.By.res("android:id/input_minute")))
            val now=java.time.LocalTime.now()
            if(now.hour<23) {
                hour.text="23";minute.text="59"
                checkNotNull(device.findObject(androidx.test.uiautomator.By.res("android:id/button1"))).click()
                tag("save-drink").assertIsNotEnabled()
                compose.onNodeWithText("Change time").performScrollTo().performClick()
                device.findObject(androidx.test.uiautomator.By.res("android:id/toggle_mode"))?.click()
            }
            checkNotNull(device.wait(androidx.test.uiautomator.Until.findObject(androidx.test.uiautomator.By.res("android:id/input_hour")),5000)).text="0"
            checkNotNull(device.findObject(androidx.test.uiautomator.By.res("android:id/input_minute"))).text="0"
            checkNotNull(device.findObject(androidx.test.uiautomator.By.res("android:id/button1"))).click()
            // The custom-entry dialog owns focus, not the underlying Activity
            // root Espresso selects for closeSoftKeyboard on API 37.
            check(device.wait(androidx.test.uiautomator.Until.gone(androidx.test.uiautomator.By.res("android:id/input_hour")), 10000))
            device.waitForIdle()
            tag("save-drink").assertIsEnabled().performClick();waitFor {records().size==1}
            val timestamp=java.time.Instant.ofEpochMilli(records().single().timestamp).atZone(java.time.ZoneId.systemDefault())
            assertEquals(0,timestamp.hour);assertEquals(0,timestamp.minute)
        } finally {QaArtifacts.shell(if(clockSetting=="null") "settings delete system time_12_24" else "settings put system time_12_24 $clockSetting")}
    }
    @Test fun ongoingNotificationBothActionsDeduplicate() {
        if(Build.VERSION.SDK_INT>=33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(app.packageName,Manifest.permission.POST_NOTIFICATIONS)
        }
        prefs(HydroSettings(statusNotification=true,theme=ThemeMode.DARK))
        runBlocking {app.refreshSurfaces()}
        val manager=app.getSystemService(NotificationManager::class.java)
        waitFor {manager.activeNotifications.any {it.id==1}}
        for((index,ml) in listOf(0 to 250,1 to 500)) {
            val before=records().size
            val action=manager.activeNotifications.single {it.id==1}.notification.actions[index]
            action.actionIntent.send();action.actionIntent.send()
            waitFor {records().size==before+1};Thread.sleep(300)
            assertEquals(before+1,records().size);assertEquals(ml,records().first().amountMl)
        }
        assertTrue(records().all {it.isWater()});assertEquals(750,records().sumOf {it.amountMl})
        prefs(HydroSettings(statusNotification=false));runBlocking {app.refreshSurfaces()}
        assertFalse(manager.activeNotifications.any {it.id==1})
    }
}

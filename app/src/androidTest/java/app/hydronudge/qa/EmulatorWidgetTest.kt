package app.hydronudge.qa

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import app.hydronudge.*
import app.hydronudge.domain.*
import app.hydronudge.widgets.HydroWidgetReceiver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.*
import java.util.regex.Pattern

/** Real launcher pin/quick-action test. Never modifies an existing widget or another home-screen item. */
@RunWith(AndroidJUnit4::class)
open class EmulatorWidgetTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun pinOwnWidgetLogWaterAndRemoveOnlyTheCreatedInstance() {
        org.junit.Assume.assumeTrue(android.os.Build.MODEL.contains("sdk",true))
        val app=compose.activity.application as HydroApplication
        val manager=AppWidgetManager.getInstance(app)
        val provider=ComponentName(app,HydroWidgetReceiver::class.java)
        val originalIds=manager.getAppWidgetIds(provider).toSet()
        val original=runBlocking {app.preferences.settings.first()}
        val before=runBlocking {app.repository.all()}.map {it.id}.toSet()
        val ownedEntries=mutableSetOf<String>()
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        var widgetRoot: UiObject2?=null
        var newId: Int?=null
        try {
            runBlocking {app.preferences.save(original.copy(unit=VolumeUnit.ML))}
            compose.waitUntil(20000) {compose.onAllNodesWithTag("dashboard-list").fetchSemanticsNodes().isNotEmpty()}
            assertTrue("Launcher does not support widget pin requests",manager.isRequestPinAppWidgetSupported)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {assertTrue(manager.requestPinAppWidget(provider,null,null))}
            val add=device.wait(Until.findObject(By.text(Pattern.compile("(?i)^(Add|Add automatically|Add to home screen|Dodaj)$"))),10000)
            check(add!=null && device.hasObject(By.text(Pattern.compile(".*Mizupuku.*",Pattern.DOTALL)))) {"Own widget confirmation needs manual approval; no other UI was clicked"}
            add.click()
            compose.waitUntil(20000) {manager.getAppWidgetIds(provider).any {it !in originalIds}}
            val created=manager.getAppWidgetIds(provider).filter {it !in originalIds}
            assertEquals(1,created.size);newId=created.single()
            QaArtifacts.note("PASS emulator launcher created one new Mizupuku widget instance")
            device.pressHome()
            // emulator can leave a newly pinned widget in resize/selection mode, consuming its first tap.
            device.pressBack()
            device.wait(Until.hasObject(By.text("Mizupuku")),10000)
            val deadline=System.currentTimeMillis()+20000
            while(widgetRoot==null && System.currentTimeMillis()<deadline) {
                for(brand in device.findObjects(By.text("Mizupuku"))) {
                    var candidate: UiObject2?=brand
                    repeat(4) {
                        val node=candidate
                        if(node!=null && node.findObject(By.text("+ 250 mL"))!=null && node.visibleBounds.width()*node.visibleBounds.height()<device.displayWidth*device.displayHeight/2) widgetRoot=node
                        candidate=node?.parent
                    }
                    if(widgetRoot!=null) break
                }
                if(widgetRoot==null) Thread.sleep(200)
            }
            val root=checkNotNull(widgetRoot) {"Pinned widget exists but its own +250 action is not visible; no personal home item was selected"}
            val launcherPackage=device.currentPackageName
            val bounds=root.visibleBounds
            val initial=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            val initialCrop=Bitmap.createBitmap(initial,bounds.left,bounds.top,bounds.width(),bounds.height())
            File(QaArtifacts.folder,"11-widget-before-crop.png").outputStream().use {initialCrop.compress(Bitmap.CompressFormat.PNG,100,it)}
            initialCrop.recycle();initial.recycle()
            root.findObject(By.text("+ 250 mL")).click()
            compose.waitUntil(20000) {runBlocking {app.repository.all()}.any {it.id !in before}}
            val fresh=runBlocking {app.repository.all()}.filter {it.id !in before}
            assertEquals(1,fresh.size)
            val entry=fresh.single();ownedEntries+=entry.id
            assertTrue(entry.id.startsWith("widget:"));assertEquals(250,entry.amountMl);assertEquals("WATER",entry.category)
            assertEquals("Widget action must not open the app",launcherPackage,device.currentPackageName)
            QaArtifacts.note("PASS actual launcher widget quick action wrote exactly one 250 mL Water record to Room without opening the app")
            val total=runBlocking {app.repository.all()}.filter {it.isWater() && localDay(it.timestamp,ZoneId.systemDefault())==LocalDate.now()}.sumOf {it.amountMl.toLong()}
            device.wait(Until.hasObject(By.text("${goalPercent(total,original.goalMl)}%")),10000)
            // Save only the widget's pixels; never persist a full personal home-screen image.
            val full=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            val crop=Bitmap.createBitmap(full,bounds.left,bounds.top,bounds.width(),bounds.height())
            File(QaArtifacts.folder,"11-widget-medium-crop.png").outputStream().use {crop.compress(Bitmap.CompressFormat.PNG,100,it)}
            crop.recycle();full.recycle()
            QaArtifacts.note("SCREENSHOT 11-widget-medium-crop.png (only Mizupuku widget bounds)")
        } finally {
            if(newId!=null && manager.getAppWidgetIds(provider).contains(newId)) {
                runCatching {
                    widgetRoot?.longClick()
                    val remove=device.wait(Until.findObject(By.text(Pattern.compile("(?i)^(Remove|Remove from Home|Odstrani)$"))),3000)
                    if(remove!=null) {
                        remove.click()
                        compose.waitUntil(10000) {!manager.getAppWidgetIds(provider).contains(newId)}
                        QaArtifacts.note("CLEANUP only the newly created QA widget removed from emulator home screen")
                    } else QaArtifacts.note("REMAINING QA widget left pinned; removal menu was not available; pre-existing widgets untouched")
                }.onFailure {QaArtifacts.note("REMAINING QA widget removal: ${it.message}")}
            }
            runBlocking {ownedEntries.forEach {app.repository.delete(it)};app.preferences.save(original)}
            QaArtifacts.shell("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n app.hydronudge/.MainActivity")
            QaArtifacts.logcat("widget")
        }
    }
}

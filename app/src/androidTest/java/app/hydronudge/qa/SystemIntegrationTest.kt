package app.hydronudge.qa

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import app.hydronudge.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class SystemIntegrationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val app get()=compose.activity.application as HydroApplication

    @Test @SdkSuppress(minSdkVersion=33)
    fun notificationDenialLeavesAppUsableAndDoesNotPost() {
        val original=runBlocking {app.preferences.settings.first()}
        assertNotEquals(PackageManager.PERMISSION_GRANTED,app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS))
        try {
            runBlocking {app.preferences.save(original.copy(statusNotification=false))}
            compose.waitUntil(20000) {compose.onAllNodesWithTag("nav:settings").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithTag("nav:settings").performClick()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("toggle:Hydration status notification"))
            compose.onNodeWithTag("toggle:Hydration status notification").performClick()
            val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val deny=device.wait(Until.findObject(By.res(Pattern.compile(".*:id/permission_deny_button"))),5000)
            check(deny!=null && device.hasObject(By.text(Pattern.compile(".*HydroNudge.*",Pattern.DOTALL)))) {"HydroNudge notification permission dialog unavailable; no unrelated permission was changed"}
            deny.click()
            check(device.wait(Until.hasObject(By.pkg(app.packageName)),10000))
            compose.waitForIdle()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Notifications are blocked.",substring=true))
            compose.onNodeWithText("Notifications are blocked.",substring=true).assertIsDisplayed()
            assertNotEquals(PackageManager.PERMISSION_GRANTED,app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS))
            assertFalse(app.getSystemService(NotificationManager::class.java).activeNotifications.any {it.id==1})
            QaArtifacts.screenshot(compose.activity,"12-notifications-denied")
            compose.onNodeWithTag("nav:today").performClick()
            compose.onNodeWithTag("dashboard-list").assertExists()
            QaArtifacts.note("PASS actual notification denial: blocked explanation, no status notification, app remains usable")
        } finally {runBlocking {app.preferences.save(original)};QaArtifacts.logcat("notification-denial")}
    }

    @Test @SdkSuppress(minSdkVersion=33)
    fun notificationAllowPostsAndShowsSystemPrompt() {
        val original=runBlocking {app.preferences.settings.first()}
        assertNotEquals(PackageManager.PERMISSION_GRANTED,app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS))
        try {
            runBlocking {app.preferences.save(original.copy(statusNotification=false))}
            compose.waitUntil(20000) {compose.onAllNodesWithTag("nav:settings").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithTag("nav:settings").performClick()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("toggle:Hydration status notification"))
            compose.onNodeWithTag("toggle:Hydration status notification").performClick()
            val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            check(device.takeScreenshot(java.io.File(QaArtifacts.folder,"permission-prompt.png")))
            val deny=device.wait(Until.findObject(By.res(Pattern.compile(".*:id/permission_allow_button"))),5000)
            check(deny!=null && device.hasObject(By.text(Pattern.compile(".*HydroNudge.*",Pattern.DOTALL)))) {"HydroNudge notification permission dialog unavailable; no unrelated permission was changed"}
            deny.click()
            check(device.wait(Until.hasObject(By.pkg(app.packageName)),10000))
            compose.waitForIdle()
            compose.waitUntil(20000) {app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED && app.getSystemService(NotificationManager::class.java).activeNotifications.any {it.id==1}}
            QaArtifacts.screenshot(compose.activity,"notifications-allowed")
            compose.onNodeWithTag("nav:today").performClick()
            compose.onNodeWithTag("dashboard-list").assertExists()
            QaArtifacts.note("PASS actual Android permission Allow: permission granted and real status notification posted")
        } finally {runBlocking {app.preferences.save(original)};QaArtifacts.logcat("notification-denial")}
    }

    @Test fun quickSettingsNativeTileClickPersistsOneDrinkAndRestoresTilePresence() {
        val component="app.hydronudge/.quicksettings.HydrationTile"
        // Only return whether our own tile is present; do not capture the user's other tile names.
        val wasPresent=QaArtifacts.shell("if settings get secure sysui_qs_tiles | grep -F -q app.hydronudge; then echo present; else echo absent; fi").trim()=="present"
        val before=runBlocking {app.repository.all()}.map {it.id}.toSet()
        val amount=runBlocking {app.preferences.settings.first()}.tileAmountMl
        val owned=mutableSetOf<String>()
        try {
            if(!wasPresent) QaArtifacts.shell("cmd statusbar add-tile $component")
            // Open only Quick Settings to bind the TileService; never inspect or capture notifications.
            QaArtifacts.shell("cmd statusbar expand-settings")
            Thread.sleep(1000)
            QaArtifacts.shell("cmd statusbar click-tile $component")
            compose.waitUntil(20000) {runBlocking {app.repository.all()}.any {it.id !in before}}
            val fresh=runBlocking {app.repository.all()}.filter {it.id !in before}
            assertEquals(1,fresh.size)
            val entry=fresh.single();owned+=entry.id
            assertEquals(amount,entry.amountMl);assertEquals("WATER",entry.category)
            QaArtifacts.note("PASS native SystemUI Quick Settings tile add/click: configured Water volume persisted once; no other tile activated")
        } finally {
            QaArtifacts.shell("cmd statusbar collapse")
            if(!wasPresent) QaArtifacts.shell("cmd statusbar remove-tile $component")
            runBlocking {owned.forEach {app.repository.delete(it)}}
            QaArtifacts.note("CLEANUP original HydroNudge tile presence preserved; only known tile-test entry removed")
            QaArtifacts.logcat("quick-settings")
        }
    }
}

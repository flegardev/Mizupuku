package app.hydronudge.qa

import android.provider.Settings
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.hydronudge.MainActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DeviceAccessibilityTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun largerFontAndBackgroundTransitionsRestoreSystemPreference() {
        val original=Settings.System.getFloat(compose.activity.contentResolver,Settings.System.FONT_SCALE,1f)
        File(QaArtifacts.folder,"original-font-scale.txt").writeText(original.toString())
        try {
            QaArtifacts.shell("settings put system font_scale 1.5")
            compose.waitUntil(20000) {compose.activity.resources.configuration.fontScale>=1.49f}
            compose.waitUntil(20000) {compose.onAllNodesWithTag("dashboard-list").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithTag("dashboard-list").performScrollToIndex(0)
            compose.onNodeWithTag("nav:settings").assertIsDisplayed()
            QaArtifacts.screenshot(compose.activity,"08-font-150")
            compose.onNodeWithTag("dashboard-list").performScrollToNode(hasTestTag("choose-drink"))
            compose.onNodeWithTag("choose-drink").assertIsDisplayed()
            QaArtifacts.note("PASS 150% font scale: dashboard, progress and logging action remain available")
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome()
            QaArtifacts.shell("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n app.hydronudge/.MainActivity")
            compose.waitUntil(20000) {compose.activityRule.scenario.state==androidx.lifecycle.Lifecycle.State.RESUMED}
            compose.onNodeWithTag("dashboard-list").assertExists()
            QaArtifacts.note("PASS background-to-foreground transition")
        } finally {
            QaArtifacts.shell("settings put system font_scale $original")
            assertEquals(original,Settings.System.getFloat(compose.activity.contentResolver,Settings.System.FONT_SCALE,1f),0.001f)
            QaArtifacts.note("RESTORED original system font scale $original")
            QaArtifacts.logcat("accessibility")
        }
    }
}

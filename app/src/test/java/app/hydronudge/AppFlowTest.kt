package app.hydronudge

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import app.hydronudge.data.preferences.HydroSettings
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
// Avoid Robolectric #8460: an explicit width qualifier can make Dialog text fields never idle.
@Config(sdk=[35],application=HydroApplication::class,qualifiers="h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun screenshot(name: String) {
        val folder=File("build/screenshots").apply {mkdirs()}
        val view = compose.activity.window.decorView
        val bitmap = android.graphics.Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888).also {
            view.draw(android.graphics.Canvas(it))
        }
        File(folder,"$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun launchLogWaterCustomAmountAndSwitchToDarkTheme() {
        val app=compose.activity.application as HydroApplication
        runBlocking { app.repository.clearAll(); app.preferences.save(HydroSettings(goalMl=2500)) }
        compose.waitUntil(20000) { compose.onAllNodesWithText("0% completed").fetchSemanticsNodes().isNotEmpty() }
        screenshot("dashboard-light-empty")
        compose.onNodeWithText("+ 250 mL").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("10% completed").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Dismiss").performClick()
        compose.onNodeWithText("Custom amount").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("drink-editor").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("drink-name").assertDoesNotExist()
        compose.onNodeWithTag("category:MILK").assertDoesNotExist()
        compose.onNodeWithText("Save water").assertIsDisplayed().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("20% completed").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Settings").onLast().performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Dark"))
        compose.onNodeWithText("Dark").performClick()
        compose.onNodeWithText("Today").performClick()
        compose.waitForIdle()
        compose.onNode(hasScrollAction()).performScrollToIndex(0)
        screenshot("dashboard-dark-logged")
        compose.onNodeWithText("20% completed").assertExists()
    }

}

package app.hydronudge.qa

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.hydronudge.*
import app.hydronudge.domain.VolumeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickAmountValidationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun malformedFiveTokenInputMustNotBeSilentlyAccepted() {
        val app=compose.activity.application as HydroApplication
        val original=runBlocking {app.preferences.settings.first()}
        try {
            runBlocking {app.preferences.save(original.copy(unit=VolumeUnit.ML,quickAmounts=listOf(150,250,330,500)))}
            compose.waitUntil(20000) {compose.onAllNodesWithTag("nav:settings").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithTag("nav:settings").performClick()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("settings:quick-add"))
            compose.onNodeWithTag("settings:quick-add").performClick()
            compose.onNodeWithTag("settings-list").performScrollToNode(hasText("Save quick amounts"))
            compose.onNode(hasSetTextAction() and hasText("150, 250, 330, 500")).performTextReplacement("150, 250, invalid, 330, 500")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithText("Save quick amounts").performScrollTo()
            QaArtifacts.screenshot(compose.activity,"10-invalid-quick-amounts")
            compose.onNodeWithText("Save quick amounts").assertIsNotEnabled()
            QaArtifacts.note("PASS malformed quick-add input is rejected without silently discarding tokens")
        } catch(error:Throwable) {
            QaArtifacts.note("FAIL quick-add validation: ${error.message}")
            throw error
        } finally {runBlocking {app.preferences.save(original)}}
    }
}
